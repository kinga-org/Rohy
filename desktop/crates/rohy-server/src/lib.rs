//! Local network server for the desktop app: WebSocket (tokio-tungstenite) + Bonjour (mdns-sd),
//! one [`ServerSession`] per phone connection.

use std::collections::HashMap;
use std::net::{IpAddr, SocketAddr};
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::{Arc, Mutex};

use futures_util::{SinkExt, StreamExt};
use rohy_core::crypto::Identity;
use rohy_core::encoding::fingerprint;
use rohy_core::messages::{Clip, SecureMessage};
use rohy_core::session::{Action, ServerSession};
use rohy_core::trusted::{Registry, TrustedDevice};
use tokio::net::{TcpListener, TcpStream};
use tokio::sync::mpsc;
use tokio_tungstenite::tungstenite::Message;
use tokio_tungstenite::tungstenite::protocol::CloseFrame;
use tokio_tungstenite::tungstenite::protocol::frame::coding::CloseCode as WsCloseCode;

pub struct ServerConfig {
    pub identity: Arc<Identity>,
    pub name: String,
    pub registry: Arc<Registry>,
    /// 0 picks a free port.
    pub port: u16,
    pub advertise: bool,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Event {
    Paired(TrustedDevice),
    Received { clip: Clip, device: TrustedDevice },
    ConnectionsChanged(Vec<TrustedDevice>),
}

enum Command {
    Send(SecureMessage),
    Disconnect,
}

type Connections =
    Arc<Mutex<HashMap<u64, (Option<TrustedDevice>, mpsc::UnboundedSender<Command>)>>>;

/// Running server. Dropping it stops listening and closes every connection.
pub struct ServerHandle {
    port: u16,
    connections: Connections,
    accept_task: tokio::task::JoinHandle<()>,
    _mdns: Option<mdns_sd::ServiceDaemon>,
}

impl ServerHandle {
    pub fn port(&self) -> u16 {
        self.port
    }

    pub fn connected_devices(&self) -> Vec<TrustedDevice> {
        connected(&self.connections)
    }

    /// Sends a clip to every authenticated phone.
    pub fn broadcast(&self, clip: &Clip) {
        for (_, sender) in self.connections.lock().expect("connections lock").values() {
            let _ = sender.send(Command::Send(SecureMessage::Clip(clip.clone())));
        }
    }

    /// Closes (4403) the connections of a phone that is no longer trusted.
    pub fn disconnect(&self, device_id: &str) {
        for (device, sender) in self.connections.lock().expect("connections lock").values() {
            if device.as_ref().is_some_and(|d| d.id() == device_id) {
                let _ = sender.send(Command::Disconnect);
            }
        }
    }
}

impl Drop for ServerHandle {
    fn drop(&mut self) {
        self.accept_task.abort();
        for (_, sender) in self.connections.lock().expect("connections lock").values() {
            let _ = sender.send(Command::Disconnect);
        }
    }
}

fn connected(connections: &Connections) -> Vec<TrustedDevice> {
    connections
        .lock()
        .expect("connections lock")
        .values()
        .filter_map(|(device, _)| device.clone())
        .collect()
}

/// IPv4 addresses of non-loopback interfaces, used as a fallback in the QR code.
pub fn local_ipv4_addresses() -> Vec<String> {
    if_addrs::get_if_addrs()
        .unwrap_or_default()
        .into_iter()
        .filter(|interface| !interface.is_loopback())
        .filter_map(|interface| match interface.ip() {
            IpAddr::V4(ip) => Some(ip.to_string()),
            IpAddr::V6(_) => None,
        })
        .collect()
}

pub async fn start(
    config: ServerConfig,
    events: mpsc::UnboundedSender<Event>,
) -> std::io::Result<ServerHandle> {
    let listener = TcpListener::bind(SocketAddr::from(([0, 0, 0, 0], config.port))).await?;
    let port = listener.local_addr()?.port();
    let mdns = if config.advertise {
        advertise(&config, port).ok()
    } else {
        None
    };
    let connections: Connections = Arc::default();
    let config = Arc::new(config);
    let accept_connections = connections.clone();
    let accept_task = tokio::spawn(async move {
        let next_id = AtomicU64::new(0);
        while let Ok((stream, _)) = listener.accept().await {
            let id = next_id.fetch_add(1, Ordering::Relaxed);
            tokio::spawn(serve(
                stream,
                id,
                config.clone(),
                accept_connections.clone(),
                events.clone(),
            ));
        }
    });
    Ok(ServerHandle {
        port,
        connections,
        accept_task,
        _mdns: mdns,
    })
}

fn advertise(config: &ServerConfig, port: u16) -> Result<mdns_sd::ServiceDaemon, mdns_sd::Error> {
    let daemon = mdns_sd::ServiceDaemon::new()?;
    let fp = fingerprint(&config.identity.public_key());
    let host = format!("rohy-{fp}.local.");
    let info = mdns_sd::ServiceInfo::new(
        rohy_core::SERVICE_TYPE,
        &config.name,
        &host,
        "",
        port,
        &[("fp", fp.as_str())][..],
    )?
    .enable_addr_auto();
    daemon.register(info)?;
    Ok(daemon)
}

async fn serve(
    stream: TcpStream,
    id: u64,
    config: Arc<ServerConfig>,
    connections: Connections,
    events: mpsc::UnboundedSender<Event>,
) {
    let Ok(socket) = tokio_tungstenite::accept_async(stream).await else {
        return;
    };
    let (mut sink, mut source) = socket.split();
    let (commands, mut inbox) = mpsc::unbounded_channel();
    connections
        .lock()
        .expect("connections lock")
        .insert(id, (None, commands));
    let mut session = ServerSession::new(
        config.identity.clone(),
        config.name.clone(),
        config.registry.clone(),
    );

    'connection: loop {
        let actions = tokio::select! {
            message = source.next() => match message {
                Some(Ok(Message::Text(text))) => session.receive_text(text.as_str()),
                Some(Ok(Message::Binary(bytes))) => session.receive_binary(&bytes),
                Some(Ok(Message::Ping(_) | Message::Pong(_) | Message::Frame(_))) => continue,
                _ => break,
            },
            command = inbox.recv() => match command {
                Some(Command::Send(message)) => session.send(&message),
                Some(Command::Disconnect) => vec![Action::Close(rohy_core::CloseCode::Forbidden)],
                None => break,
            },
        };
        for action in actions {
            let outgoing = match action {
                Action::SendText(text) => Message::text(text),
                Action::SendBinary(bytes) => Message::binary(bytes),
                Action::Close(code) => {
                    let frame = CloseFrame {
                        code: WsCloseCode::from(code as u16),
                        reason: "".into(),
                    };
                    let _ = sink.send(Message::Close(Some(frame))).await;
                    break 'connection;
                }
                Action::Paired(device) => {
                    let _ = events.send(Event::Paired(device.clone()));
                    mark(&connections, id, device, &events);
                    continue;
                }
                Action::Authenticated(device) => {
                    mark(&connections, id, device, &events);
                    continue;
                }
                Action::Received(clip) => {
                    if let Some(device) = session.device() {
                        let _ = events.send(Event::Received {
                            clip,
                            device: device.clone(),
                        });
                    }
                    continue;
                }
            };
            if sink.send(outgoing).await.is_err() {
                break 'connection;
            }
        }
    }

    let removed = connections.lock().expect("connections lock").remove(&id);
    if removed.is_some_and(|(device, _)| device.is_some()) {
        let _ = events.send(Event::ConnectionsChanged(connected(&connections)));
    }
}

fn mark(
    connections: &Connections,
    id: u64,
    device: TrustedDevice,
    events: &mpsc::UnboundedSender<Event>,
) {
    if let Some(entry) = connections.lock().expect("connections lock").get_mut(&id) {
        entry.0 = Some(device);
    }
    let _ = events.send(Event::ConnectionsChanged(connected(connections)));
}
