//! Interoperability harness: runs the Rust server on a random local port so the Kotlin client can be
//! tested against it. Same stdio protocol as the Swift harness:
//!   stdout: "READY <pairing uri>", "PAIRED <name>", "RECEIVED <text>"
//!   stdin:  "SEND <text>"

use std::sync::Arc;

use rohy_core::crypto::Identity;
use rohy_core::messages::Clip;
use rohy_core::pairing::PairingPayload;
use rohy_core::trusted::{MemoryStore, Registry};
use rohy_server::{Event, ServerConfig};
use tokio::io::{AsyncBufReadExt, BufReader};
use tokio::sync::mpsc;

#[tokio::main]
async fn main() -> std::io::Result<()> {
    let identity = Arc::new(Identity::generate());
    let registry = Arc::new(Registry::new(MemoryStore::default()));
    let token = registry.pairing.lock().expect("pairing lock").open();
    let config = ServerConfig {
        identity: identity.clone(),
        name: "Rohy harness".into(),
        registry,
        port: 0,
        advertise: false,
    };
    let (events, mut received) = mpsc::unbounded_channel();
    let server = rohy_server::start(config, events).await?;
    let payload = PairingPayload {
        public_key: identity.public_key(),
        token,
        name: "Rohy harness".into(),
        hosts: vec!["127.0.0.1".into()],
        port: server.port(),
    };
    println!("READY {}", payload.uri());

    let mut stdin = BufReader::new(tokio::io::stdin()).lines();
    loop {
        tokio::select! {
            line = stdin.next_line() => match line? {
                Some(line) => if let Some(text) = line.strip_prefix("SEND ") { server.broadcast(&Clip::new(text)) },
                None => return Ok(()),
            },
            Some(event) = received.recv() => match event {
                Event::Paired(device) => println!("PAIRED {}", device.name),
                Event::Received { clip, .. } => println!("RECEIVED {}", clip.text),
                Event::ConnectionsChanged(_) => {}
            },
        }
    }
}
