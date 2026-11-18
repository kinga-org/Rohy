//! X25519 identities, HKDF-SHA256 session keys and ChaCha20-Poly1305 counter frames.

use chacha20poly1305::aead::{Aead, KeyInit, Payload};
use chacha20poly1305::{ChaCha20Poly1305, Nonce};
use hkdf::Hkdf;
use sha2::Sha256;
use x25519_dalek::{PublicKey, StaticSecret};

const AAD: &[u8] = b"rohy/v1";
const INFO_C2S: &[u8] = b"rohy/v1 c2s";
const INFO_S2C: &[u8] = b"rohy/v1 s2c";

/// Long-lived X25519 key pair of a device. The private key never leaves the device.
#[derive(Clone)]
pub struct Identity {
    secret: StaticSecret,
}

impl Identity {
    pub fn from_private(bytes: [u8; 32]) -> Self {
        Self {
            secret: StaticSecret::from(bytes),
        }
    }

    pub fn generate() -> Self {
        Self::from_private(crate::random_bytes())
    }

    pub fn private_bytes(&self) -> [u8; 32] {
        self.secret.to_bytes()
    }

    pub fn public_key(&self) -> [u8; 32] {
        PublicKey::from(&self.secret).to_bytes()
    }

    pub fn shared_secret(&self, peer_public_key: &[u8; 32]) -> [u8; 32] {
        self.secret
            .diffie_hellman(&PublicKey::from(*peer_public_key))
            .to_bytes()
    }
}

/// Per-direction session keys (see docs/protocol.md).
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct SessionKeys {
    pub client_to_server: [u8; 32],
    pub server_to_client: [u8; 32],
}

impl SessionKeys {
    pub fn derive(
        identity: &Identity,
        peer_public_key: &[u8; 32],
        client_nonce: &[u8],
        server_nonce: &[u8],
    ) -> Self {
        let shared = identity.shared_secret(peer_public_key);
        let salt = [client_nonce, server_nonce].concat();
        let hkdf = Hkdf::<Sha256>::new(Some(&salt), &shared);
        let expand = |info: &[u8]| {
            let mut key = [0u8; 32];
            hkdf.expand(info, &mut key)
                .expect("32 bytes is a valid HKDF-SHA256 output length");
            key
        };
        Self {
            client_to_server: expand(INFO_C2S),
            server_to_client: expand(INFO_S2C),
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum FrameError {
    TooShort,
    UnexpectedCounter,
    AuthenticationFailed,
}

fn counter_nonce(counter: u64) -> [u8; 12] {
    let mut nonce = [0u8; 12];
    nonce[4..].copy_from_slice(&counter.to_be_bytes());
    nonce
}

/// Seals outgoing frames: nonce (12 bytes, counter) ‖ ciphertext ‖ tag.
pub struct FrameSealer {
    cipher: ChaCha20Poly1305,
    counter: u64,
}

impl FrameSealer {
    pub fn new(key: &[u8; 32]) -> Self {
        Self {
            cipher: ChaCha20Poly1305::new(key.into()),
            counter: 0,
        }
    }

    pub fn counter(&self) -> u64 {
        self.counter
    }

    pub fn seal(&mut self, plaintext: &[u8]) -> Vec<u8> {
        let nonce = counter_nonce(self.counter);
        let sealed = self
            .cipher
            .encrypt(
                &Nonce::from(nonce),
                Payload {
                    msg: plaintext,
                    aad: AAD,
                },
            )
            .expect("ChaCha20-Poly1305 encryption of an in-memory buffer cannot fail");
        self.counter += 1;
        [nonce.as_slice(), &sealed].concat()
    }
}

/// Opens incoming frames, accepting only the next expected counter.
pub struct FrameOpener {
    cipher: ChaCha20Poly1305,
    expected: u64,
}

impl FrameOpener {
    pub fn new(key: &[u8; 32]) -> Self {
        Self {
            cipher: ChaCha20Poly1305::new(key.into()),
            expected: 0,
        }
    }

    pub fn expected_counter(&self) -> u64 {
        self.expected
    }

    pub fn open(&mut self, frame: &[u8]) -> Result<Vec<u8>, FrameError> {
        if frame.len() < 12 + 16 {
            return Err(FrameError::TooShort);
        }
        let nonce = counter_nonce(self.expected);
        if frame[..12] != nonce {
            return Err(FrameError::UnexpectedCounter);
        }
        let plaintext = self
            .cipher
            .decrypt(
                &Nonce::from(nonce),
                Payload {
                    msg: &frame[12..],
                    aad: AAD,
                },
            )
            .map_err(|_| FrameError::AuthenticationFailed)?;
        self.expected += 1;
        Ok(plaintext)
    }
}

/// Constant-time comparison for secrets such as the pairing token.
pub fn constant_time_eq(a: &[u8], b: &[u8]) -> bool {
    a.len() == b.len() && a.iter().zip(b).fold(0u8, |acc, (x, y)| acc | (x ^ y)) == 0
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn rejects_replay_and_reordering() {
        let key = crate::random_bytes::<32>();
        let mut sealer = FrameSealer::new(&key);
        let mut opener = FrameOpener::new(&key);
        let first = sealer.seal(b"a");
        let second = sealer.seal(b"b");
        assert_eq!(opener.open(&second), Err(FrameError::UnexpectedCounter));
        assert_eq!(opener.open(&first).unwrap(), b"a");
        assert_eq!(opener.open(&first), Err(FrameError::UnexpectedCounter));
        assert_eq!(opener.open(&second).unwrap(), b"b");
        assert_eq!(opener.expected_counter(), 2);
    }

    #[test]
    fn rejects_tampered_and_short_frames() {
        let key = crate::random_bytes::<32>();
        let mut opener = FrameOpener::new(&key);
        let mut frame = FrameSealer::new(&key).seal(b"secret");
        *frame.last_mut().unwrap() ^= 1;
        assert_eq!(opener.open(&frame), Err(FrameError::AuthenticationFailed));
        assert_eq!(opener.open(&[0u8; 10]), Err(FrameError::TooShort));
        assert_eq!(opener.expected_counter(), 0);
    }

    #[test]
    fn identity_round_trips_and_compares_in_constant_time() {
        let identity = Identity::generate();
        assert_eq!(
            Identity::from_private(identity.private_bytes()).public_key(),
            identity.public_key()
        );
        assert_ne!(Identity::generate().public_key(), identity.public_key());
        assert!(constant_time_eq(b"ab", b"ab"));
        assert!(!constant_time_eq(b"ab", b"ac"));
        assert!(!constant_time_eq(b"a", b"ab"));
        assert_eq!(hex_nonce(258), "000000000000000000000102");
    }

    fn hex_nonce(counter: u64) -> String {
        crate::encoding::hex_encode(&counter_nonce(counter))
    }
}
