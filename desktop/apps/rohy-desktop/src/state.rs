//! What the desktop app shows, updated from server events. No platform code: unit-tested.

use rohy_core::trusted::TrustedDevice;
use rohy_server::Event;

#[derive(Debug, Clone, PartialEq, Eq, Default)]
pub enum ServerStatus {
    #[default]
    Starting,
    Listening(u16),
    Failed(String),
}

#[derive(Debug, Default)]
pub struct Model {
    pub status: ServerStatus,
    pub trusted: Vec<TrustedDevice>,
    pub connected: Vec<TrustedDevice>,
    pub pairing_uri: Option<String>,
    pub last_event: Option<String>,
}

/// What the app must do after an event, besides refreshing the view.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Effect {
    None,
    /// Write this text to the computer clipboard.
    WriteClipboard(String),
    /// A phone just paired: close the pairing window.
    PairingDone,
}

impl Model {
    pub fn apply(&mut self, event: Event, trusted_now: Vec<TrustedDevice>) -> Effect {
        self.trusted = trusted_now;
        match event {
            Event::Paired(device) => {
                self.pairing_uri = None;
                self.last_event = Some(format!("{} associé", device.name));
                Effect::PairingDone
            }
            Event::Received { clip, device } => {
                self.last_event = Some(format!("Reçu de {}", device.name));
                Effect::WriteClipboard(clip.text)
            }
            Event::ConnectionsChanged(devices) => {
                self.connected = devices;
                Effect::None
            }
        }
    }

    pub fn is_connected(&self, device: &TrustedDevice) -> bool {
        self.connected
            .iter()
            .any(|d| d.public_key == device.public_key)
    }

    pub fn status_line(&self) -> String {
        match &self.status {
            ServerStatus::Starting => "Démarrage…".into(),
            ServerStatus::Failed(error) => format!("Erreur réseau : {error}"),
            ServerStatus::Listening(_) if self.connected.is_empty() => {
                "En attente du téléphone".into()
            }
            ServerStatus::Listening(_) => {
                let names: Vec<&str> = self.connected.iter().map(|d| d.name.as_str()).collect();
                format!("Connecté à {}", names.join(", "))
            }
        }
    }

    pub fn port(&self) -> Option<u16> {
        match self.status {
            ServerStatus::Listening(port) => Some(port),
            _ => None,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use rohy_core::messages::Clip;

    fn device(byte: u8, name: &str) -> TrustedDevice {
        TrustedDevice {
            public_key: [byte; 32],
            name: name.into(),
            paired_at: 1,
        }
    }

    #[test]
    fn status_line_follows_server_and_connections() {
        let mut model = Model::default();
        assert_eq!(model.status_line(), "Démarrage…");
        assert_eq!(model.port(), None);
        model.status = ServerStatus::Failed("port occupé".into());
        assert_eq!(model.status_line(), "Erreur réseau : port occupé");
        model.status = ServerStatus::Listening(47823);
        assert_eq!(model.port(), Some(47823));
        assert_eq!(model.status_line(), "En attente du téléphone");
        model.apply(
            Event::ConnectionsChanged(vec![device(1, "Galaxy"), device(2, "Pixel")]),
            vec![],
        );
        assert_eq!(model.status_line(), "Connecté à Galaxy, Pixel");
        assert!(model.is_connected(&device(1, "")));
        assert!(!model.is_connected(&device(3, "")));
    }

    #[test]
    fn pairing_and_clips_produce_effects() {
        let mut model = Model {
            pairing_uri: Some("rohy://pair?…".into()),
            ..Model::default()
        };
        let galaxy = device(1, "Galaxy");
        assert_eq!(
            model.apply(Event::Paired(galaxy.clone()), vec![galaxy.clone()]),
            Effect::PairingDone
        );
        assert_eq!(model.pairing_uri, None);
        assert_eq!(model.trusted, vec![galaxy.clone()]);
        assert_eq!(model.last_event.as_deref(), Some("Galaxy associé"));
        let effect = model.apply(
            Event::Received {
                clip: Clip::new("bonjour"),
                device: galaxy.clone(),
            },
            vec![galaxy],
        );
        assert_eq!(effect, Effect::WriteClipboard("bonjour".into()));
        assert_eq!(model.last_event.as_deref(), Some("Reçu de Galaxy"));
    }
}
