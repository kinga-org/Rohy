//! Real WebSocket round trips against the server, with a phone implemented from rohy-core primitives.

use std::sync::Arc;
use std::time::Duration;

use futures_util::{SinkExt, StreamExt};
use rohy_core::crypto::{FrameOpener, FrameSealer, Identity, SessionKeys};
use rohy_core::encoding::{base64url_decode, base64url_encode};
use rohy_core::messages::{Clip, SecureMessage, Welcome, WelcomeStatus};
use rohy_core::trusted::{MemoryStore, Registry};
use rohy_server::{Event, ServerConfig, ServerHandle};
use tokio::net::TcpStream;
use tokio::sync::mpsc;
use tokio_tungstenite::tungstenite::Message;
use tokio_tungstenite::{MaybeTlsStream, WebSocketStream};

type Socket = WebSocketStream<MaybeTlsStream<TcpStream>>;

struct Phone {
    identity: Identity,
    socket: Socket,
    sealer: FrameSealer,
    opener: FrameOpener,
    status: WelcomeStatus,
}

async fn next(socket: &mut Socket) -> Message {
    tokio::time::timeout(Duration::from_secs(5), socket.next())
        .await
        .expect("timeout")
        .expect("closed")
        .expect("error")
}

impl Phone {
    async fn connect(port: u16, identity: Identity) -> Self {
        let (mut socket, _) =
            tokio_tungstenite::connect_async(format!("ws://127.0.0.1:{port}/rohy"))
                .await
                .unwrap();
        let nonce: [u8; 16] = rohy_core::random_bytes();
        let hello = format!(
            r#"{{"t":"hello","v":1,"pk":"{}","n":"{}","name":"Test phone"}}"#,
            base64url_encode(&identity.public_key()),
            base64url_encode(&nonce)
        );
        socket.send(Message::text(hello)).await.unwrap();
        let Message::Text(text) = next(&mut socket).await else {
            panic!("expected welcome")
        };
        let welcome: Welcome = serde_json::from_str(text.as_str()).unwrap();
        let server: [u8; 32] = base64url_decode(&welcome.pk).unwrap().try_into().unwrap();
        let keys = SessionKeys::derive(
            &identity,
            &server,
            &nonce,
            &base64url_decode(&welcome.n).unwrap(),
        );
        Self {
            identity,
            socket,
            sealer: FrameSealer::new(&keys.client_to_server),
            opener: FrameOpener::new(&keys.server_to_client),
            status: welcome.status,
        }
    }

    async fn send(&mut self, message: SecureMessage) {
        let frame = self.sealer.seal(&message.encode());
        self.socket.send(Message::binary(frame)).await.unwrap();
    }

    async fn receive(&mut self) -> SecureMessage {
        let Message::Binary(bytes) = next(&mut self.socket).await else {
            panic!("expected binary")
        };
        SecureMessage::decode(&self.opener.open(&bytes).unwrap()).unwrap()
    }
}

async fn server() -> (ServerHandle, Arc<Registry>, mpsc::UnboundedReceiver<Event>) {
    let registry = Arc::new(Registry::new(MemoryStore::default()));
    let config = ServerConfig {
        identity: Arc::new(Identity::generate()),
        name: "PC de test".into(),
        registry: registry.clone(),
        port: 0,
        advertise: false,
    };
    let (sender, events) = mpsc::unbounded_channel();
    (
        rohy_server::start(config, sender).await.unwrap(),
        registry,
        events,
    )
}

async fn event(events: &mut mpsc::UnboundedReceiver<Event>) -> Event {
    tokio::time::timeout(Duration::from_secs(5), events.recv())
        .await
        .expect("timeout")
        .expect("closed")
}

#[tokio::test]
async fn pairs_exchanges_reconnects_and_disconnects() {
    let (server, registry, mut events) = server().await;
    let token = registry.pairing.lock().unwrap().open();
    let mut phone = Phone::connect(server.port(), Identity::generate()).await;
    assert_eq!(phone.status, WelcomeStatus::Pairing);
    phone.send(SecureMessage::Pair { token }).await;
    assert_eq!(
        phone.receive().await,
        SecureMessage::Paired {
            name: "PC de test".into()
        }
    );
    let Event::Paired(device) = event(&mut events).await else {
        panic!("expected paired")
    };
    assert_eq!(
        event(&mut events).await,
        Event::ConnectionsChanged(vec![device.clone()])
    );
    assert_eq!(server.connected_devices(), vec![device.clone()]);

    let clip = Clip::new("depuis le téléphone");
    phone.send(SecureMessage::Clip(clip.clone())).await;
    assert_eq!(
        event(&mut events).await,
        Event::Received {
            clip,
            device: device.clone()
        }
    );

    let outgoing = Clip::new("depuis le PC");
    server.broadcast(&outgoing);
    assert_eq!(phone.receive().await, SecureMessage::Clip(outgoing));
    phone.send(SecureMessage::Ping).await;
    assert_eq!(phone.receive().await, SecureMessage::Pong);

    phone.socket.close(None).await.unwrap();
    assert_eq!(event(&mut events).await, Event::ConnectionsChanged(vec![]));

    let mut again = Phone::connect(server.port(), phone.identity.clone()).await;
    assert_eq!(again.status, WelcomeStatus::Trusted);
    assert_eq!(
        event(&mut events).await,
        Event::ConnectionsChanged(vec![device.clone()])
    );
    server.disconnect(&device.id());
    let Message::Close(Some(frame)) = next(&mut again.socket).await else {
        panic!("expected close")
    };
    assert_eq!(u16::from(frame.code), 4403);
}

#[tokio::test]
async fn stranger_is_rejected_with_4403() {
    let (server, _registry, _events) = server().await;
    let (mut socket, _) =
        tokio_tungstenite::connect_async(format!("ws://127.0.0.1:{}/rohy", server.port()))
            .await
            .unwrap();
    let hello = format!(
        r#"{{"t":"hello","v":1,"pk":"{}","n":"{}","name":"Intrus"}}"#,
        base64url_encode(&Identity::generate().public_key()),
        base64url_encode(&[0u8; 16])
    );
    socket.send(Message::text(hello)).await.unwrap();
    let Message::Close(Some(frame)) = next(&mut socket).await else {
        panic!("expected close")
    };
    assert_eq!(u16::from(frame.code), 4403);
    drop(server);
}

#[tokio::test]
async fn advertises_and_lists_addresses() {
    let registry = Arc::new(Registry::new(MemoryStore::default()));
    let config = ServerConfig {
        identity: Arc::new(Identity::generate()),
        name: "Rohy mDNS test".into(),
        registry,
        port: 0,
        advertise: true,
    };
    let (sender, _events) = mpsc::unbounded_channel();
    let server = rohy_server::start(config, sender).await.unwrap();
    assert_ne!(server.port(), 0);
    assert!(
        rohy_server::local_ipv4_addresses()
            .iter()
            .all(|ip| ip != "127.0.0.1")
    );
}
