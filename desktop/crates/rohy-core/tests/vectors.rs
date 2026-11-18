//! Cross-implementation check against protocol/test-vectors.json (also used by Swift and Kotlin).

use rohy_core::crypto::{FrameOpener, FrameSealer, Identity, SessionKeys};
use rohy_core::encoding::{fingerprint, hex_decode, hex_encode};
use rohy_core::messages::SecureMessage;
use serde_json::Value;

fn vectors() -> Value {
    let path = concat!(
        env!("CARGO_MANIFEST_DIR"),
        "/../../../protocol/test-vectors.json"
    );
    serde_json::from_str(&std::fs::read_to_string(path).unwrap()).unwrap()
}

fn bytes(v: &Value, name: &str) -> Vec<u8> {
    hex_decode(v[name].as_str().unwrap()).unwrap()
}

fn key(v: &Value, name: &str) -> [u8; 32] {
    bytes(v, name).try_into().unwrap()
}

#[test]
fn keys_and_fingerprint_match_vectors() {
    let v = vectors();
    let client = Identity::from_private(key(&v, "client_private"));
    let server = Identity::from_private(key(&v, "server_private"));
    assert_eq!(client.public_key().to_vec(), bytes(&v, "client_public"));
    assert_eq!(server.public_key().to_vec(), bytes(&v, "server_public"));
    assert_eq!(
        client.shared_secret(&server.public_key()).to_vec(),
        bytes(&v, "shared_secret")
    );
    assert_eq!(
        fingerprint(&server.public_key()),
        v["server_fingerprint"].as_str().unwrap()
    );
    let keys = SessionKeys::derive(
        &client,
        &server.public_key(),
        &bytes(&v, "client_nonce"),
        &bytes(&v, "server_nonce"),
    );
    assert_eq!(
        hex_encode(&keys.client_to_server),
        v["key_c2s"].as_str().unwrap()
    );
    assert_eq!(
        hex_encode(&keys.server_to_client),
        v["key_s2c"].as_str().unwrap()
    );
}

#[test]
fn frames_match_vectors() {
    let v = vectors();
    let mut c2s = (
        FrameSealer::new(&key(&v, "key_c2s")),
        FrameOpener::new(&key(&v, "key_c2s")),
    );
    let mut s2c = (
        FrameSealer::new(&key(&v, "key_s2c")),
        FrameOpener::new(&key(&v, "key_s2c")),
    );
    for frame in v["frames"].as_array().unwrap() {
        let (sealer, opener) = if frame["direction"] == "c2s" {
            (&mut c2s.0, &mut c2s.1)
        } else {
            (&mut s2c.0, &mut s2c.1)
        };
        let plaintext = frame["plaintext"].as_str().unwrap();
        let expected = bytes(frame, "frame");
        assert_eq!(sealer.counter(), frame["counter"].as_u64().unwrap());
        assert_eq!(sealer.seal(plaintext.as_bytes()), expected);
        let opened = opener.open(&expected).unwrap();
        assert_eq!(opened, plaintext.as_bytes());
        assert!(SecureMessage::decode(&opened).is_some());
    }
}
