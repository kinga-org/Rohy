//! Server side of one phone connection, without networking (see docs/protocol.md).

use std::sync::Arc;

use crate::CloseCode;
use crate::crypto::{FrameOpener, FrameSealer, Identity, SessionKeys};
use crate::encoding::{base64url_decode, base64url_encode};
use crate::messages::{Clip, Hello, SecureMessage, Welcome, WelcomeStatus};
use crate::trusted::{Registry, TrustedDevice};

/// What the network layer must do after the session processed an input.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Action {
    SendText(String),
    SendBinary(Vec<u8>),
    Close(CloseCode),
    Paired(TrustedDevice),
    Authenticated(TrustedDevice),
    Received(Clip),
}

struct Channel {
    sealer: FrameSealer,
    opener: FrameOpener,
}

enum State {
    AwaitingHello,
    AwaitingPair(Channel, [u8; 32], String),
    Ready(Channel, TrustedDevice),
    Closed,
}

pub struct ServerSession {
    identity: Arc<Identity>,
    server_name: String,
    registry: Arc<Registry>,
    make_nonce: fn() -> [u8; crate::NONCE_LENGTH],
    state: State,
}

fn now_seconds() -> u64 {
    std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .map(|duration| duration.as_secs())
        .unwrap_or_default()
}

impl ServerSession {
    pub fn new(
        identity: Arc<Identity>,
        server_name: impl Into<String>,
        registry: Arc<Registry>,
    ) -> Self {
        Self {
            identity,
            server_name: server_name.into(),
            registry,
            make_nonce: crate::random_bytes,
            state: State::AwaitingHello,
        }
    }

    pub fn device(&self) -> Option<&TrustedDevice> {
        match &self.state {
            State::Ready(_, device) => Some(device),
            _ => None,
        }
    }

    pub fn is_closed(&self) -> bool {
        matches!(self.state, State::Closed)
    }

    pub fn receive_text(&mut self, text: &str) -> Vec<Action> {
        if !matches!(self.state, State::AwaitingHello) {
            return self.fail(CloseCode::InvalidFrame);
        }
        let Some(hello) = serde_json::from_str::<Hello>(text)
            .ok()
            .filter(|h| h.t == "hello")
        else {
            return self.fail(CloseCode::InvalidFrame);
        };
        if hello.v != crate::VERSION {
            return self.fail(CloseCode::UnsupportedVersion);
        }
        let client_key: Option<[u8; 32]> =
            base64url_decode(&hello.pk).and_then(|k| k.try_into().ok());
        let client_nonce = base64url_decode(&hello.n).filter(|n| n.len() == crate::NONCE_LENGTH);
        let (Some(client_key), Some(client_nonce)) = (client_key, client_nonce) else {
            return self.fail(CloseCode::InvalidFrame);
        };

        let known = self
            .registry
            .store
            .lock()
            .expect("store lock")
            .find(&client_key);
        if known.is_none()
            && !self
                .registry
                .pairing
                .lock()
                .expect("pairing lock")
                .is_open()
        {
            return self.fail(CloseCode::Forbidden);
        }

        let server_nonce = (self.make_nonce)();
        let keys = SessionKeys::derive(&self.identity, &client_key, &client_nonce, &server_nonce);
        let channel = Channel {
            sealer: FrameSealer::new(&keys.server_to_client),
            opener: FrameOpener::new(&keys.client_to_server),
        };
        let welcome = Welcome {
            t: "welcome".into(),
            v: crate::VERSION,
            pk: base64url_encode(&self.identity.public_key()),
            n: base64url_encode(&server_nonce),
            status: if known.is_some() {
                WelcomeStatus::Trusted
            } else {
                WelcomeStatus::Pairing
            },
        };
        let reply = Action::SendText(serde_json::to_string(&welcome).expect("welcome serializes"));
        match known {
            Some(device) => {
                self.state = State::Ready(channel, device.clone());
                vec![reply, Action::Authenticated(device)]
            }
            None => {
                self.state = State::AwaitingPair(channel, client_key, hello.name);
                vec![reply]
            }
        }
    }

    pub fn receive_binary(&mut self, frame: &[u8]) -> Vec<Action> {
        match std::mem::replace(&mut self.state, State::Closed) {
            State::AwaitingHello | State::Closed => self.fail(CloseCode::InvalidFrame),
            State::AwaitingPair(mut channel, public_key, name) => {
                let Some(message) = open(&mut channel, frame) else {
                    return self.fail(CloseCode::InvalidFrame);
                };
                let SecureMessage::Pair { token } = message else {
                    return self.fail(CloseCode::Forbidden);
                };
                if !self
                    .registry
                    .pairing
                    .lock()
                    .expect("pairing lock")
                    .consume(&token)
                {
                    return self.fail(CloseCode::Forbidden);
                }
                let device = TrustedDevice {
                    public_key,
                    name,
                    paired_at: now_seconds(),
                };
                self.registry
                    .store
                    .lock()
                    .expect("store lock")
                    .save(device.clone());
                self.state = State::Ready(channel, device.clone());
                let mut actions = vec![Action::Paired(device)];
                actions.extend(self.send(&SecureMessage::Paired {
                    name: self.server_name.clone(),
                }));
                actions
            }
            State::Ready(mut channel, device) => {
                let Some(message) = open(&mut channel, frame) else {
                    return self.fail(CloseCode::InvalidFrame);
                };
                self.state = State::Ready(channel, device);
                match message {
                    SecureMessage::Clip(clip) => vec![Action::Received(clip)],
                    SecureMessage::Ping => self.send(&SecureMessage::Pong),
                    SecureMessage::Pong => vec![],
                    SecureMessage::Pair { .. } | SecureMessage::Paired { .. } => {
                        self.fail(CloseCode::InvalidFrame)
                    }
                }
            }
        }
    }

    /// Encrypts a message for the phone. Returns nothing when the session is not authenticated.
    pub fn send(&mut self, message: &SecureMessage) -> Vec<Action> {
        match &mut self.state {
            State::Ready(channel, _) => {
                vec![Action::SendBinary(channel.sealer.seal(&message.encode()))]
            }
            _ => vec![],
        }
    }

    fn fail(&mut self, code: CloseCode) -> Vec<Action> {
        self.state = State::Closed;
        vec![Action::Close(code)]
    }
}

fn open(channel: &mut Channel, frame: &[u8]) -> Option<SecureMessage> {
    SecureMessage::decode(&channel.opener.open(frame).ok()?)
}

#[cfg(test)]
pub(crate) mod tests {
    use super::*;
    use crate::trusted::MemoryStore;

    /// Minimal phone-side implementation used to drive the server session.
    pub(crate) struct TestPhone {
        pub identity: Identity,
        nonce: [u8; 16],
        sealer: Option<FrameSealer>,
        opener: Option<FrameOpener>,
    }

    impl TestPhone {
        pub fn new() -> Self {
            Self {
                identity: Identity::generate(),
                nonce: crate::random_bytes(),
                sealer: None,
                opener: None,
            }
        }

        pub fn hello(&self, version: u32) -> String {
            format!(
                r#"{{"t":"hello","v":{version},"pk":"{}","n":"{}","name":"Galaxy"}}"#,
                base64url_encode(&self.identity.public_key()),
                base64url_encode(&self.nonce)
            )
        }

        pub fn accept(&mut self, welcome: &str) -> Welcome {
            let welcome: Welcome = serde_json::from_str(welcome).unwrap();
            let server: [u8; 32] = base64url_decode(&welcome.pk).unwrap().try_into().unwrap();
            let keys = SessionKeys::derive(
                &self.identity,
                &server,
                &self.nonce,
                &base64url_decode(&welcome.n).unwrap(),
            );
            self.sealer = Some(FrameSealer::new(&keys.client_to_server));
            self.opener = Some(FrameOpener::new(&keys.server_to_client));
            welcome
        }

        pub fn seal(&mut self, message: &SecureMessage) -> Vec<u8> {
            self.sealer.as_mut().unwrap().seal(&message.encode())
        }

        pub fn open(&mut self, frame: &[u8]) -> SecureMessage {
            SecureMessage::decode(&self.opener.as_mut().unwrap().open(frame).unwrap()).unwrap()
        }
    }

    fn text(actions: &[Action]) -> String {
        actions
            .iter()
            .find_map(|a| {
                if let Action::SendText(t) = a {
                    Some(t.clone())
                } else {
                    None
                }
            })
            .unwrap()
    }

    fn binary(actions: &[Action]) -> Vec<u8> {
        actions
            .iter()
            .find_map(|a| {
                if let Action::SendBinary(b) = a {
                    Some(b.clone())
                } else {
                    None
                }
            })
            .unwrap()
    }

    fn setup() -> (Arc<Identity>, Arc<Registry>) {
        (
            Arc::new(Identity::generate()),
            Arc::new(Registry::new(MemoryStore::default())),
        )
    }

    fn session(identity: &Arc<Identity>, registry: &Arc<Registry>) -> ServerSession {
        ServerSession::new(identity.clone(), "PC", registry.clone())
    }

    #[test]
    fn pairs_with_valid_token_then_exchanges_clips() {
        let (identity, registry) = setup();
        registry.pairing.lock().unwrap().open_with(vec![42; 32]);
        let mut server = session(&identity, &registry);
        let mut phone = TestPhone::new();
        let welcome = phone.accept(&text(&server.receive_text(&phone.hello(1))));
        assert_eq!(welcome.status, WelcomeStatus::Pairing);
        assert_eq!(
            base64url_decode(&welcome.pk).unwrap(),
            identity.public_key()
        );
        assert_eq!(server.device(), None);

        let actions = server.receive_binary(&phone.seal(&SecureMessage::Pair {
            token: vec![42; 32],
        }));
        let Action::Paired(device) = &actions[0] else {
            panic!("not paired: {actions:?}")
        };
        assert_eq!(device.name, "Galaxy");
        assert!(
            registry
                .store
                .lock()
                .unwrap()
                .find(&phone.identity.public_key())
                .is_some()
        );
        assert_eq!(
            phone.open(&binary(&actions)),
            SecureMessage::Paired { name: "PC".into() }
        );
        assert!(!registry.pairing.lock().unwrap().is_open());
        assert_eq!(server.device(), Some(device));

        let clip = Clip::new("depuis le téléphone");
        assert_eq!(
            server.receive_binary(&phone.seal(&SecureMessage::Clip(clip.clone()))),
            vec![Action::Received(clip)]
        );
        let outgoing = Clip {
            sensitive: true,
            ..Clip::new("depuis le PC")
        };
        assert_eq!(
            phone.open(&binary(
                &server.send(&SecureMessage::Clip(outgoing.clone()))
            )),
            SecureMessage::Clip(outgoing)
        );
        let pong = server.receive_binary(&phone.seal(&SecureMessage::Ping));
        assert_eq!(phone.open(&binary(&pong)), SecureMessage::Pong);
        assert!(
            server
                .receive_binary(&phone.seal(&SecureMessage::Pong))
                .is_empty()
        );
    }

    #[test]
    fn known_device_is_authenticated_without_pairing() {
        let (identity, registry) = setup();
        let mut phone = TestPhone::new();
        let device = TrustedDevice {
            public_key: phone.identity.public_key(),
            name: "Galaxy".into(),
            paired_at: 1,
        };
        registry.store.lock().unwrap().save(device.clone());
        let mut server = session(&identity, &registry);
        let actions = server.receive_text(&phone.hello(1));
        assert_eq!(actions.last(), Some(&Action::Authenticated(device)));
        assert_eq!(phone.accept(&text(&actions)).status, WelcomeStatus::Trusted);
        let clip = Clip::new("x");
        assert_eq!(
            server.receive_binary(&phone.seal(&SecureMessage::Clip(clip.clone()))),
            vec![Action::Received(clip)]
        );
    }

    #[test]
    fn unknown_device_is_rejected_when_pairing_is_closed() {
        let (identity, registry) = setup();
        let mut server = session(&identity, &registry);
        assert_eq!(
            server.receive_text(&TestPhone::new().hello(1)),
            vec![Action::Close(CloseCode::Forbidden)]
        );
        assert!(server.is_closed());
        assert!(server.send(&SecureMessage::Ping).is_empty());
    }

    #[test]
    fn wrong_token_and_early_clip_are_forbidden() {
        for message in [
            SecureMessage::Pair { token: vec![1; 32] },
            SecureMessage::Clip(Clip::new("x")),
        ] {
            let (identity, registry) = setup();
            registry.pairing.lock().unwrap().open_with(vec![42; 32]);
            let mut server = session(&identity, &registry);
            let mut phone = TestPhone::new();
            phone.accept(&text(&server.receive_text(&phone.hello(1))));
            assert_eq!(
                server.receive_binary(&phone.seal(&message)),
                vec![Action::Close(CloseCode::Forbidden)]
            );
            assert!(registry.store.lock().unwrap().all().is_empty());
        }
    }

    #[test]
    fn replayed_or_invalid_frames_close_the_session() {
        let (identity, registry) = setup();
        let mut phone = TestPhone::new();
        registry.store.lock().unwrap().save(TrustedDevice {
            public_key: phone.identity.public_key(),
            name: "G".into(),
            paired_at: 1,
        });
        let mut server = session(&identity, &registry);
        phone.accept(&text(&server.receive_text(&phone.hello(1))));
        let frame = phone.seal(&SecureMessage::Ping);
        assert!(!server.receive_binary(&frame).is_empty());
        assert_eq!(
            server.receive_binary(&frame),
            vec![Action::Close(CloseCode::InvalidFrame)]
        );

        let mut other = session(&identity, &registry);
        let mut phone2 = TestPhone {
            identity: phone.identity.clone(),
            ..TestPhone::new()
        };
        phone2.accept(&text(&other.receive_text(&phone2.hello(1))));
        assert_eq!(
            other.receive_binary(&phone2.seal(&SecureMessage::Paired { name: "x".into() })),
            vec![Action::Close(CloseCode::InvalidFrame)]
        );

        registry.pairing.lock().unwrap().open_with(vec![1; 32]);
        let mut pairing = session(&identity, &registry);
        let mut stranger = TestPhone::new();
        stranger.accept(&text(&pairing.receive_text(&stranger.hello(1))));
        assert_eq!(
            pairing.receive_binary(&[0; 40]),
            vec![Action::Close(CloseCode::InvalidFrame)]
        );
    }

    #[test]
    fn rejects_invalid_hellos_and_wrong_states() {
        let (identity, registry) = setup();
        registry.pairing.lock().unwrap().open_with(vec![1; 32]);
        let phone = TestPhone::new();
        for (hello, code) in [
            ("not json".to_string(), CloseCode::InvalidFrame),
            (
                r#"{"t":"welcome","pk":"","n":"","name":""}"#.to_string(),
                CloseCode::InvalidFrame,
            ),
            (phone.hello(2), CloseCode::UnsupportedVersion),
            (
                r#"{"pk":"AAAA","n":"AAAAAAAAAAAAAAAAAAAAAA","name":"x"}"#.to_string(),
                CloseCode::InvalidFrame,
            ),
        ] {
            assert_eq!(
                session(&identity, &registry).receive_text(&hello),
                vec![Action::Close(code)],
                "{hello}"
            );
        }
        let mut fresh = session(&identity, &registry);
        assert_eq!(
            fresh.receive_binary(&[0; 40]),
            vec![Action::Close(CloseCode::InvalidFrame)]
        );
        assert_eq!(
            fresh.receive_binary(&[0; 40]),
            vec![Action::Close(CloseCode::InvalidFrame)]
        );
        let mut twice = session(&identity, &registry);
        twice.receive_text(&phone.hello(1));
        assert_eq!(
            twice.receive_text(&phone.hello(1)),
            vec![Action::Close(CloseCode::InvalidFrame)]
        );
    }
}
