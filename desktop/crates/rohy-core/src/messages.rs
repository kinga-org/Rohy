//! Plaintext handshake messages and encrypted secure messages (JSON).

use serde::{Deserialize, Serialize};

use crate::encoding::{base64url_decode, base64url_encode};

fn hello_tag() -> String {
    "hello".into()
}

fn welcome_tag() -> String {
    "welcome".into()
}

fn version() -> u32 {
    crate::VERSION
}

/// Sent by the phone in a text frame.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct Hello {
    #[serde(default = "hello_tag")]
    pub t: String,
    #[serde(default = "version")]
    pub v: u32,
    pub pk: String,
    pub n: String,
    pub name: String,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum WelcomeStatus {
    Trusted,
    Pairing,
}

/// Sent by the computer in a text frame.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct Welcome {
    #[serde(default = "welcome_tag")]
    pub t: String,
    #[serde(default = "version")]
    pub v: u32,
    pub pk: String,
    pub n: String,
    pub status: WelcomeStatus,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Clip {
    pub id: String,
    pub text: String,
    pub sensitive: bool,
}

impl Clip {
    /// New clip with a random UUID v4 identifier.
    pub fn new(text: impl Into<String>) -> Self {
        let mut bytes: [u8; 16] = crate::random_bytes();
        bytes[6] = (bytes[6] & 0x0f) | 0x40;
        bytes[8] = (bytes[8] & 0x3f) | 0x80;
        let hex = crate::encoding::hex_encode(&bytes);
        let id = format!(
            "{}-{}-{}-{}-{}",
            &hex[..8],
            &hex[8..12],
            &hex[12..16],
            &hex[16..20],
            &hex[20..]
        );
        Self {
            id,
            text: text.into(),
            sensitive: false,
        }
    }
}

/// Messages carried inside encrypted frames.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum SecureMessage {
    Pair { token: Vec<u8> },
    Paired { name: String },
    Clip(Clip),
    Ping,
    Pong,
}

#[derive(Serialize, Deserialize)]
struct Envelope {
    t: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    token: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    name: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    id: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    text: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    sensitive: Option<bool>,
}

impl Envelope {
    fn tag(t: &str) -> Self {
        Self {
            t: t.into(),
            token: None,
            name: None,
            id: None,
            text: None,
            sensitive: None,
        }
    }
}

impl SecureMessage {
    pub fn encode(&self) -> Vec<u8> {
        let envelope = match self {
            Self::Pair { token } => Envelope {
                token: Some(base64url_encode(token)),
                ..Envelope::tag("pair")
            },
            Self::Paired { name } => Envelope {
                name: Some(name.clone()),
                ..Envelope::tag("paired")
            },
            Self::Clip(clip) => Envelope {
                id: Some(clip.id.clone()),
                text: Some(clip.text.clone()),
                sensitive: Some(clip.sensitive),
                ..Envelope::tag("clip")
            },
            Self::Ping => Envelope::tag("ping"),
            Self::Pong => Envelope::tag("pong"),
        };
        serde_json::to_vec(&envelope)
            .expect("an envelope of strings and booleans always serializes")
    }

    pub fn decode(bytes: &[u8]) -> Option<Self> {
        let envelope: Envelope = serde_json::from_slice(bytes).ok()?;
        match envelope.t.as_str() {
            "pair" => Some(Self::Pair {
                token: base64url_decode(&envelope.token?)?,
            }),
            "paired" => Some(Self::Paired {
                name: envelope.name?,
            }),
            "clip" => Some(Self::Clip(Clip {
                id: envelope.id?,
                text: envelope.text?,
                sensitive: envelope.sensitive.unwrap_or(false),
            })),
            "ping" => Some(Self::Ping),
            "pong" => Some(Self::Pong),
            _ => None,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn secure_messages_round_trip() {
        let messages = [
            SecureMessage::Pair { token: vec![7; 32] },
            SecureMessage::Paired { name: "Mac".into() },
            SecureMessage::Clip(Clip {
                id: "1".into(),
                text: "héllo / \"quoted\"".into(),
                sensitive: true,
            }),
            SecureMessage::Ping,
            SecureMessage::Pong,
        ];
        for message in messages {
            assert_eq!(SecureMessage::decode(&message.encode()), Some(message));
        }
    }

    #[test]
    fn rejects_malformed_messages() {
        for json in [
            "nope",
            r#"{"t":"unknown"}"#,
            r#"{"t":"pair"}"#,
            r#"{"t":"pair","token":"*"}"#,
            r#"{"t":"paired"}"#,
            r#"{"t":"clip","id":"1"}"#,
            r#"{"t":"clip","text":"x"}"#,
        ] {
            assert_eq!(SecureMessage::decode(json.as_bytes()), None, "{json}");
        }
        assert_eq!(
            SecureMessage::decode(br#"{"t":"clip","id":"1","text":"x","extra":1}"#),
            Some(SecureMessage::Clip(Clip {
                id: "1".into(),
                text: "x".into(),
                sensitive: false
            }))
        );
    }

    #[test]
    fn handshake_defaults_and_uuid_format() {
        let hello: Hello = serde_json::from_str(r#"{"pk":"a","n":"b","name":"c"}"#).unwrap();
        assert_eq!((hello.t.as_str(), hello.v), ("hello", 1));
        let welcome: Welcome =
            serde_json::from_str(r#"{"pk":"a","n":"b","status":"pairing"}"#).unwrap();
        assert_eq!(
            (welcome.t.as_str(), welcome.status),
            ("welcome", WelcomeStatus::Pairing)
        );
        let clip = Clip::new("x");
        assert_eq!(clip.id.len(), 36);
        assert_eq!(&clip.id[14..15], "4");
        assert_ne!(Clip::new("x").id, clip.id);
    }
}
