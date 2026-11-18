//! Phones allowed to connect. Only their public keys are stored: they are not secrets.

use std::path::PathBuf;
use std::sync::Mutex;

use serde::{Deserialize, Serialize};

use crate::encoding::fingerprint;
use crate::pairing::PairingWindow;

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct TrustedDevice {
    #[serde(with = "key_base64")]
    pub public_key: [u8; 32],
    pub name: String,
    /// Seconds since the Unix epoch.
    pub paired_at: u64,
}

impl TrustedDevice {
    pub fn id(&self) -> String {
        fingerprint(&self.public_key)
    }
}

mod key_base64 {
    use serde::{Deserialize, Deserializer, Serializer, de::Error};

    pub fn serialize<S: Serializer>(key: &[u8; 32], serializer: S) -> Result<S::Ok, S::Error> {
        serializer.serialize_str(&crate::encoding::base64url_encode(key))
    }

    pub fn deserialize<'de, D: Deserializer<'de>>(deserializer: D) -> Result<[u8; 32], D::Error> {
        let text = String::deserialize(deserializer)?;
        crate::encoding::base64url_decode(&text)
            .and_then(|bytes| bytes.try_into().ok())
            .ok_or_else(|| D::Error::custom("invalid public key"))
    }
}

pub trait TrustedDeviceStore: Send {
    fn all(&self) -> Vec<TrustedDevice>;
    fn save(&mut self, device: TrustedDevice);
    fn remove(&mut self, id: &str);

    fn find(&self, public_key: &[u8; 32]) -> Option<TrustedDevice> {
        self.all()
            .into_iter()
            .find(|device| &device.public_key == public_key)
    }
}

#[derive(Debug, Default)]
pub struct MemoryStore {
    devices: Vec<TrustedDevice>,
}

impl TrustedDeviceStore for MemoryStore {
    fn all(&self) -> Vec<TrustedDevice> {
        self.devices.clone()
    }

    fn save(&mut self, device: TrustedDevice) {
        self.devices.retain(|d| d.public_key != device.public_key);
        self.devices.push(device);
    }

    fn remove(&mut self, id: &str) {
        self.devices.retain(|d| d.id() != id);
    }
}

/// JSON file in the user's application data directory.
#[derive(Debug)]
pub struct FileStore {
    path: PathBuf,
    cache: MemoryStore,
}

impl FileStore {
    pub fn open(path: impl Into<PathBuf>) -> Self {
        let path = path.into();
        let devices = std::fs::read(&path)
            .ok()
            .and_then(|data| serde_json::from_slice(&data).ok())
            .unwrap_or_default();
        Self {
            path,
            cache: MemoryStore { devices },
        }
    }

    fn persist(&self) {
        if let Some(parent) = self.path.parent() {
            let _ = std::fs::create_dir_all(parent);
        }
        let data =
            serde_json::to_vec_pretty(&self.cache.devices).expect("devices always serialize");
        let temporary = self.path.with_extension("tmp");
        if std::fs::write(&temporary, data).is_ok() {
            let _ = std::fs::rename(&temporary, &self.path);
        }
    }
}

impl TrustedDeviceStore for FileStore {
    fn all(&self) -> Vec<TrustedDevice> {
        self.cache.all()
    }

    fn save(&mut self, device: TrustedDevice) {
        self.cache.save(device);
        self.persist();
    }

    fn remove(&mut self, id: &str) {
        self.cache.remove(id);
        self.persist();
    }
}

/// State shared by every connection of a server: trusted phones and the pairing window.
pub struct Registry {
    pub store: Mutex<Box<dyn TrustedDeviceStore>>,
    pub pairing: Mutex<PairingWindow>,
}

impl Registry {
    pub fn new(store: impl TrustedDeviceStore + 'static) -> Self {
        Self {
            store: Mutex::new(Box::new(store)),
            pairing: Mutex::new(PairingWindow::default()),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn device(byte: u8, name: &str) -> TrustedDevice {
        TrustedDevice {
            public_key: [byte; 32],
            name: name.into(),
            paired_at: 1,
        }
    }

    #[test]
    fn file_store_persists_across_instances() {
        let dir = std::env::temp_dir().join(format!(
            "rohy-test-{}",
            crate::encoding::hex_encode(&crate::random_bytes::<8>())
        ));
        let path = dir.join("devices.json");
        let mut store = FileStore::open(&path);
        assert!(store.all().is_empty());
        store.save(device(3, "Galaxy"));
        store.save(device(3, "Galaxy S25"));
        let mut reloaded = FileStore::open(&path);
        assert_eq!(reloaded.all().len(), 1);
        assert_eq!(reloaded.find(&[3; 32]).unwrap().name, "Galaxy S25");
        reloaded.remove(&device(3, "").id());
        assert!(FileStore::open(&path).all().is_empty());
        std::fs::remove_dir_all(dir).unwrap();
    }

    #[test]
    fn rejects_corrupted_files_and_keys() {
        let path = std::env::temp_dir().join("rohy-corrupted-devices.json");
        std::fs::write(&path, r#"[{"public_key":"AAAA","name":"x","paired_at":1}]"#).unwrap();
        assert!(FileStore::open(&path).all().is_empty());
        std::fs::remove_file(path).unwrap();
    }

    #[test]
    fn memory_store_replaces_and_removes() {
        let mut store = MemoryStore::default();
        store.save(device(4, "Pixel"));
        store.save(device(4, "Pixel 9"));
        assert_eq!(store.all().len(), 1);
        assert_eq!(store.find(&[4; 32]).unwrap().name, "Pixel 9");
        store.remove(&device(4, "").id());
        assert_eq!(store.find(&[4; 32]), None);
    }
}
