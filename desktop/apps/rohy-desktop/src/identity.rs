//! The computer's X25519 private key, kept in the system credential store
//! (Windows Credential Manager, Secret Service on Linux, Keychain on macOS).

use rohy_core::crypto::Identity;

pub trait SecretStore {
    fn get(&self) -> Result<Option<Vec<u8>>, String>;
    fn set(&self, secret: &[u8]) -> Result<(), String>;
}

pub struct KeyringStore;

impl KeyringStore {
    fn entry() -> Result<keyring::Entry, String> {
        keyring::Entry::new("app.rohy.desktop", "x25519").map_err(|error| error.to_string())
    }
}

impl SecretStore for KeyringStore {
    fn get(&self) -> Result<Option<Vec<u8>>, String> {
        match Self::entry()?.get_secret() {
            Ok(secret) => Ok(Some(secret)),
            Err(keyring::Error::NoEntry) => Ok(None),
            Err(error) => Err(error.to_string()),
        }
    }

    fn set(&self, secret: &[u8]) -> Result<(), String> {
        Self::entry()?
            .set_secret(secret)
            .map_err(|error| error.to_string())
    }
}

/// Loads the identity, or creates and stores a new one on first launch.
pub fn load_or_create(store: &impl SecretStore) -> Result<Identity, String> {
    if let Some(secret) = store.get()? {
        let bytes: [u8; 32] = secret
            .try_into()
            .map_err(|_| "clé privée stockée invalide".to_string())?;
        return Ok(Identity::from_private(bytes));
    }
    let identity = Identity::generate();
    store.set(&identity.private_bytes())?;
    Ok(identity)
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::cell::RefCell;

    #[derive(Default)]
    struct Memory(RefCell<Option<Vec<u8>>>, bool);

    impl SecretStore for Memory {
        fn get(&self) -> Result<Option<Vec<u8>>, String> {
            Ok(self.0.borrow().clone())
        }

        fn set(&self, secret: &[u8]) -> Result<(), String> {
            if self.1 {
                return Err("refusé".into());
            }
            *self.0.borrow_mut() = Some(secret.to_vec());
            Ok(())
        }
    }

    #[test]
    fn creates_once_then_reloads_the_same_key() {
        let store = Memory::default();
        let first = load_or_create(&store).unwrap();
        assert_eq!(
            load_or_create(&store).unwrap().public_key(),
            first.public_key()
        );
    }

    #[test]
    fn reports_corrupted_or_unwritable_stores() {
        let corrupted = Memory(RefCell::new(Some(vec![1, 2, 3])), false);
        assert!(load_or_create(&corrupted).is_err());
        assert_eq!(
            load_or_create(&Memory(RefCell::default(), true))
                .err()
                .as_deref(),
            Some("refusé")
        );
    }
}
