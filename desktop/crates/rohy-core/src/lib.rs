//! Rohy protocol v1 (see `docs/protocol.md`): the third implementation after Swift and Kotlin,
//! validated by the shared vectors in `protocol/test-vectors.json`.

pub mod crypto;
pub mod encoding;
pub mod messages;
pub mod pairing;
pub mod policy;
pub mod session;
pub mod trusted;

pub const VERSION: u32 = 1;
pub const DEFAULT_PORT: u16 = 47823;
pub const SERVICE_TYPE: &str = "_rohy._tcp.local.";
pub const PATH: &str = "/rohy";
pub const NONCE_LENGTH: usize = 16;
pub const TOKEN_LENGTH: usize = 32;
pub const MAX_TEXT_BYTES: usize = 1 << 20;

/// WebSocket close codes defined by the protocol.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum CloseCode {
    InvalidFrame = 4400,
    Forbidden = 4403,
    UnsupportedVersion = 4426,
}

/// Cryptographically secure random bytes from the operating system.
pub fn random_bytes<const N: usize>() -> [u8; N] {
    let mut bytes = [0u8; N];
    getrandom::fill(&mut bytes).expect("operating system random source unavailable");
    bytes
}
