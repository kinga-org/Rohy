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
    pub last_event: Option<Activity>,
}

/// Last thing that happened, shown under the connection banner.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Activity {
    pub text: String,
    /// Seconds since the Unix epoch.
    pub at: u64,
}

/// Top of the "Téléphones associés" window.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Header {
    Starting,
    Failed(String),
    Waiting,
    Connected {
        names: String,
        activity: Option<String>,
    },
}

/// One line of the paired phones list.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct DeviceRow {
    pub id: String,
    pub name: String,
    pub connected: bool,
    /// "Connecté · associé le 18 nov."
    pub detail: String,
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
    pub fn apply(&mut self, event: Event, trusted_now: Vec<TrustedDevice>, now: u64) -> Effect {
        self.trusted = trusted_now;
        let mut activity = |text: String| self.last_event = Some(Activity { text, at: now });
        match event {
            Event::Paired(device) => {
                activity(format!("{} associé", device.name));
                self.pairing_uri = None;
                Effect::PairingDone
            }
            Event::Received { clip, device } => {
                activity(format!("Texte reçu de {}", device.name));
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

    pub fn header(&self, now: u64) -> Header {
        match &self.status {
            ServerStatus::Starting => Header::Starting,
            ServerStatus::Failed(error) => Header::Failed(error.clone()),
            ServerStatus::Listening(_) if self.connected.is_empty() => Header::Waiting,
            ServerStatus::Listening(_) => Header::Connected {
                names: self
                    .connected
                    .iter()
                    .map(|d| d.name.as_str())
                    .collect::<Vec<_>>()
                    .join(", "),
                activity: self.last_event.as_ref().map(|activity| {
                    format!(
                        "{} · {}",
                        activity.text,
                        relative_time(now.saturating_sub(activity.at))
                    )
                }),
            },
        }
    }

    pub fn device_rows(&self) -> Vec<DeviceRow> {
        self.trusted
            .iter()
            .map(|device| {
                let connected = self.is_connected(device);
                let state = if connected { "Connecté" } else { "Hors ligne" };
                DeviceRow {
                    id: device.id(),
                    name: device.name.clone(),
                    connected,
                    detail: format!("{state} · associé le {}", short_date(device.paired_at)),
                }
            })
            .collect()
    }

    pub fn port(&self) -> Option<u16> {
        match self.status {
            ServerStatus::Listening(port) => Some(port),
            _ => None,
        }
    }
}

/// Seconds since the Unix epoch.
pub fn now() -> u64 {
    std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .map(|elapsed| elapsed.as_secs())
        .unwrap_or_default()
}

/// "à l'instant", "il y a 12 s", "il y a 3 min", "il y a 2 h", "il y a 4 j".
pub fn relative_time(elapsed: u64) -> String {
    match elapsed {
        0..5 => "à l'instant".into(),
        5..60 => format!("il y a {elapsed} s"),
        60..3600 => format!("il y a {} min", elapsed / 60),
        3600..86400 => format!("il y a {} h", elapsed / 3600),
        _ => format!("il y a {} j", elapsed / 86400),
    }
}

/// Day and abbreviated French month in local time: "18 nov.".
pub fn short_date(seconds: u64) -> String {
    use chrono::{Datelike, Local, TimeZone};
    const MONTHS: [&str; 12] = [
        "janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.", "août", "sept.", "oct.", "nov.",
        "déc.",
    ];
    let date = i64::try_from(seconds)
        .ok()
        .and_then(|seconds| Local.timestamp_opt(seconds, 0).single());
    match date {
        Some(date) => format!("{} {}", date.day(), MONTHS[date.month0() as usize]),
        None => "date inconnue".into(),
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
            0,
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
            model.apply(Event::Paired(galaxy.clone()), vec![galaxy.clone()], 100),
            Effect::PairingDone
        );
        assert_eq!(model.pairing_uri, None);
        assert_eq!(model.trusted, vec![galaxy.clone()]);
        assert_eq!(
            model.last_event,
            Some(Activity {
                text: "Galaxy associé".into(),
                at: 100
            })
        );
        let effect = model.apply(
            Event::Received {
                clip: Clip::new("bonjour"),
                device: galaxy.clone(),
            },
            vec![galaxy],
            130,
        );
        assert_eq!(effect, Effect::WriteClipboard("bonjour".into()));
        assert_eq!(
            model.last_event.as_ref().map(|a| a.text.as_str()),
            Some("Texte reçu de Galaxy")
        );
    }

    #[test]
    fn header_and_rows_describe_each_state() {
        let galaxy = TrustedDevice {
            paired_at: 1_794_916_800, // 2026-11-17 12:00 UTC: the 17th from UTC−11 to UTC+11.
            ..device(1, "Galaxy")
        };
        let mut model = Model {
            trusted: vec![galaxy.clone(), device(2, "Pixel")],
            ..Model::default()
        };
        assert_eq!(model.header(0), Header::Starting);
        model.status = ServerStatus::Failed("port occupé".into());
        assert_eq!(model.header(0), Header::Failed("port occupé".into()));
        model.status = ServerStatus::Listening(47823);
        assert_eq!(model.header(0), Header::Waiting);
        model.connected = vec![galaxy.clone()];
        assert_eq!(
            model.header(0),
            Header::Connected {
                names: "Galaxy".into(),
                activity: None
            }
        );
        model.last_event = Some(Activity {
            text: "Texte reçu de Galaxy".into(),
            at: 100,
        });
        assert_eq!(
            model.header(112),
            Header::Connected {
                names: "Galaxy".into(),
                activity: Some("Texte reçu de Galaxy · il y a 12 s".into())
            }
        );
        let rows = model.device_rows();
        assert_eq!(rows[0].id, galaxy.id());
        assert!(rows[0].connected);
        assert_eq!(rows[0].detail, "Connecté · associé le 17 nov.");
        assert!(!rows[1].connected);
        assert!(rows[1].detail.starts_with("Hors ligne · associé le "));
    }

    #[test]
    fn times_read_naturally() {
        assert_eq!(relative_time(2), "à l'instant");
        assert_eq!(relative_time(12), "il y a 12 s");
        assert_eq!(relative_time(180), "il y a 3 min");
        assert_eq!(relative_time(7200), "il y a 2 h");
        assert_eq!(relative_time(4 * 86400), "il y a 4 j");
        assert_eq!(short_date(u64::MAX), "date inconnue");
        assert!(now() > 1_794_916_800);
    }
}
