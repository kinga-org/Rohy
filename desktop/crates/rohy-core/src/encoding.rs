//! Base64 URL without padding, hex, key fingerprints and query percent-encoding.

use base64::Engine;
use base64::engine::general_purpose::URL_SAFE_NO_PAD;
use sha2::{Digest, Sha256};

pub fn base64url_encode(bytes: &[u8]) -> String {
    URL_SAFE_NO_PAD.encode(bytes)
}

pub fn base64url_decode(text: &str) -> Option<Vec<u8>> {
    URL_SAFE_NO_PAD.decode(text).ok()
}

pub fn hex_encode(bytes: &[u8]) -> String {
    bytes.iter().map(|byte| format!("{byte:02x}")).collect()
}

pub fn hex_decode(text: &str) -> Option<Vec<u8>> {
    if !text.len().is_multiple_of(2) || !text.is_ascii() {
        return None;
    }
    (0..text.len())
        .step_by(2)
        .map(|i| u8::from_str_radix(&text[i..i + 2], 16).ok())
        .collect()
}

/// Short public identifier of a device key, published in the Bonjour TXT record.
pub fn fingerprint(public_key: &[u8]) -> String {
    hex_encode(&Sha256::digest(public_key))[..16].to_string()
}

/// Percent-encodes a query value (RFC 3986 unreserved characters are kept).
pub fn percent_encode(value: &str) -> String {
    value
        .bytes()
        .map(|byte| match byte {
            b'A'..=b'Z' | b'a'..=b'z' | b'0'..=b'9' | b'-' | b'.' | b'_' | b'~' => {
                (byte as char).to_string()
            }
            _ => format!("%{byte:02X}"),
        })
        .collect()
}

/// Decodes a percent-encoded query value. A literal "+" stays a "+" (not a space).
pub fn percent_decode(value: &str) -> Option<String> {
    let bytes = value.as_bytes();
    let mut out = Vec::with_capacity(bytes.len());
    let mut i = 0;
    while i < bytes.len() {
        if bytes[i] == b'%' {
            let hex = value.get(i + 1..i + 3)?;
            out.push(u8::from_str_radix(hex, 16).ok()?);
            i += 3;
        } else {
            out.push(bytes[i]);
            i += 1;
        }
    }
    String::from_utf8(out).ok()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn base64url_round_trips_without_padding() {
        for length in 0..=40 {
            let bytes: Vec<u8> = (0..length).map(|i| ((i * 37 + 250) % 256) as u8).collect();
            let encoded = base64url_encode(&bytes);
            assert!(!encoded.contains(['=', '+', '/']));
            assert_eq!(base64url_decode(&encoded), Some(bytes));
        }
        assert_eq!(base64url_decode("ab=="), None);
        assert_eq!(base64url_decode("a$bc"), None);
    }

    #[test]
    fn hex_rejects_invalid_input() {
        assert_eq!(hex_decode("abc"), None);
        assert_eq!(hex_decode("zz"), None);
        assert_eq!(hex_decode("é1"), None);
        assert_eq!(hex_decode("00ff"), Some(vec![0, 255]));
    }

    #[test]
    fn percent_encoding_round_trips_and_keeps_plus() {
        let name = "Mac de test & co + plus é";
        let encoded = percent_encode(name);
        assert_eq!(percent_decode(&encoded).as_deref(), Some(name));
        assert_eq!(percent_decode("a+b").as_deref(), Some("a+b"));
        assert_eq!(percent_decode("%2"), None);
        assert_eq!(percent_decode("%zz"), None);
        assert_eq!(percent_decode("%FF"), None);
    }
}
