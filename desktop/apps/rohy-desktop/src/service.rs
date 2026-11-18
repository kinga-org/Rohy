//! Runs the Rust server on its own tokio runtime thread and bridges it to the UI thread.

use std::sync::Arc;
use std::sync::mpsc as std_mpsc;

use rohy_core::crypto::Identity;
use rohy_core::messages::Clip;
use rohy_core::trusted::Registry;
use rohy_server::{Event, ServerConfig};
use tokio::sync::mpsc;

pub enum Command {
    Broadcast(Clip),
    Disconnect(String),
}

#[derive(Debug)]
pub enum ServiceEvent {
    Listening(u16),
    Failed(String),
    Server(Event),
}

pub struct Service {
    pub commands: mpsc::UnboundedSender<Command>,
    pub events: std_mpsc::Receiver<ServiceEvent>,
}

/// Starts the server on `port`, or on a free port when it is taken.
pub fn spawn(
    identity: Arc<Identity>,
    name: String,
    registry: Arc<Registry>,
    port: u16,
    advertise: bool,
) -> Service {
    let (commands, mut inbox) = mpsc::unbounded_channel();
    let (outbox, events) = std_mpsc::channel();
    std::thread::spawn(move || {
        let runtime = match tokio::runtime::Builder::new_multi_thread()
            .enable_all()
            .build()
        {
            Ok(runtime) => runtime,
            Err(error) => return drop(outbox.send(ServiceEvent::Failed(error.to_string()))),
        };
        runtime.block_on(async move {
            let (server_events, mut received) = mpsc::unbounded_channel();
            let config = |port| ServerConfig {
                identity: identity.clone(),
                name: name.clone(),
                registry: registry.clone(),
                port,
                advertise,
            };
            let server = match rohy_server::start(config(port), server_events.clone()).await {
                Ok(server) => server,
                Err(_) if port != 0 => match rohy_server::start(config(0), server_events).await {
                    Ok(server) => server,
                    Err(error) => {
                        return drop(outbox.send(ServiceEvent::Failed(error.to_string())));
                    }
                },
                Err(error) => return drop(outbox.send(ServiceEvent::Failed(error.to_string()))),
            };
            let _ = outbox.send(ServiceEvent::Listening(server.port()));
            loop {
                tokio::select! {
                    command = inbox.recv() => match command {
                        Some(Command::Broadcast(clip)) => server.broadcast(&clip),
                        Some(Command::Disconnect(id)) => server.disconnect(&id),
                        None => break,
                    },
                    Some(event) = received.recv() => {
                        if outbox.send(ServiceEvent::Server(event)).is_err() {
                            break;
                        }
                    }
                }
            }
        });
    });
    Service { commands, events }
}

#[cfg(test)]
mod tests {
    use super::*;
    use rohy_core::trusted::MemoryStore;
    use std::time::Duration;

    fn start(port: u16) -> Service {
        let registry = Arc::new(Registry::new(MemoryStore::default()));
        spawn(
            Arc::new(Identity::generate()),
            "PC de test".into(),
            registry,
            port,
            false,
        )
    }

    #[test]
    fn listens_and_falls_back_to_a_free_port() {
        let first = start(0);
        let Ok(ServiceEvent::Listening(port)) = first.events.recv_timeout(Duration::from_secs(5))
        else {
            panic!("first server did not start")
        };
        let second = start(port);
        let Ok(ServiceEvent::Listening(other)) = second.events.recv_timeout(Duration::from_secs(5))
        else {
            panic!("second server did not fall back")
        };
        assert_ne!(other, port);
        first
            .commands
            .send(Command::Broadcast(Clip::new("x")))
            .unwrap();
        first
            .commands
            .send(Command::Disconnect("absent".into()))
            .unwrap();
    }
}
