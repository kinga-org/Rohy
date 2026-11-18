//! QR code payload and single-use pairing token.

use crate::crypto::constant_time_eq;
use crate::encoding::{base64url_decode, base64url_encode, percent_decode, percent_encode};

/// Content of the QR code shown by the computer.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct PairingPayload {
    pub public_key: [u8; 32],
    pub token: Vec<u8>,
    pub name: String,
    pub hosts: Vec<String>,
    pub port: u16,
}

impl PairingPayload {
    pub fn uri(&self) -> String {
        format!(
            "rohy://pair?v={}&pk={}&t={}&n={}&h={}&p={}",
            crate::VERSION,
            base64url_encode(&self.public_key),
            base64url_encode(&self.token),
            percent_encode(&self.name),
            self.hosts.join(","),
            self.port
        )
    }

    pub fn parse(uri: &str) -> Option<Self> {
        let query = uri.strip_prefix("rohy://pair?")?;
        let mut fields = std::collections::HashMap::new();
        for part in query.split('&') {
            let (key, value) = part.split_once('=')?;
            fields.insert(key, percent_decode(value)?);
        }
        if fields.get("v")? != &crate::VERSION.to_string() {
            return None;
        }
        let public_key: [u8; 32] = base64url_decode(fields.get("pk")?)?.try_into().ok()?;
        let token = base64url_decode(fields.get("t")?)?;
        let name = fields.get("n")?.clone();
        let port: u16 = fields.get("p")?.parse().ok()?;
        if token.len() != crate::TOKEN_LENGTH || name.is_empty() || port == 0 {
            return None;
        }
        let hosts = fields
            .get("h")
            .map(|h| {
                h.split(',')
                    .filter(|s| !s.is_empty())
                    .map(String::from)
                    .collect()
            })
            .unwrap_or_default();
        Some(Self {
            public_key,
            token,
            name,
            hosts,
            port,
        })
    }
}

/// Single-use pairing token, valid while the pairing window is open on the computer.
#[derive(Debug, Default)]
pub struct PairingWindow {
    token: Option<Vec<u8>>,
}

impl PairingWindow {
    pub fn is_open(&self) -> bool {
        self.token.is_some()
    }

    pub fn open(&mut self) -> Vec<u8> {
        let token = crate::random_bytes::<{ crate::TOKEN_LENGTH }>().to_vec();
        self.token = Some(token.clone());
        token
    }

    pub fn open_with(&mut self, token: Vec<u8>) {
        self.token = Some(token);
    }

    pub fn close(&mut self) {
        self.token = None;
    }

    /// True when the candidate matches. The token is consumed either way.
    pub fn consume(&mut self, candidate: &[u8]) -> bool {
        self.token
            .take()
            .is_some_and(|token| constant_time_eq(&token, candidate))
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::encoding::base64url_encode as b64;

    fn payload() -> PairingPayload {
        PairingPayload {
            public_key: [1; 32],
            token: vec![2; 32],
            name: "Mac de test & co + plus".into(),
            hosts: vec!["192.168.1.10".into(), "10.0.0.2".into()],
            port: 47823,
        }
    }

    #[test]
    fn round_trips_and_parses_swift_encoding() {
        let payload = payload();
        assert!(payload.uri().starts_with("rohy://pair?v=1&"));
        assert_eq!(PairingPayload::parse(&payload.uri()), Some(payload.clone()));
        let swift = format!(
            "rohy://pair?v=1&pk={}&t={}&n=Mac%20de%20test%20%26%20co%20+%20plus&h=192.168.1.10,10.0.0.2&p=47823",
            b64(&[1; 32]),
            b64(&[2; 32])
        );
        assert_eq!(PairingPayload::parse(&swift), Some(payload));
    }

    #[test]
    fn rejects_invalid_uris() {
        let pk = b64(&[1; 32]);
        let t = b64(&[2; 32]);
        for uri in [
            "https://pair?v=1".to_string(),
            "rohy://pair?v".into(),
            format!("rohy://pair?v=2&pk={pk}&t={t}&n=M&p=1"),
            format!("rohy://pair?v=1&pk=AAAA&t={t}&n=M&p=1"),
            format!("rohy://pair?v=1&pk={pk}&t=AAAA&n=M&p=1"),
            format!("rohy://pair?v=1&pk={pk}&t={t}&n=&p=1"),
            format!("rohy://pair?v=1&pk={pk}&t={t}&n=M&p=0"),
            format!("rohy://pair?v=1&pk={pk}&t={t}&n=M&p=99999"),
            format!("rohy://pair?v=1&pk={pk}&t={t}&n=%zz&p=1"),
            format!("rohy://pair?v=1&pk={pk}&t={t}&n=M"),
        ] {
            assert_eq!(PairingPayload::parse(&uri), None, "{uri}");
        }
        let no_hosts =
            PairingPayload::parse(&format!("rohy://pair?v=1&pk={pk}&t={t}&n=M&p=1")).unwrap();
        assert!(no_hosts.hosts.is_empty());
    }

    #[test]
    fn window_token_is_single_use() {
        let mut window = PairingWindow::default();
        assert!(!window.is_open());
        assert!(!window.consume(&[9; 32]));
        window.open_with(vec![9; 32]);
        assert!(window.is_open());
        assert!(window.consume(&[9; 32]));
        assert!(!window.is_open());
        window.open_with(vec![9; 32]);
        assert!(!window.consume(&[8; 32]));
        assert!(!window.is_open());
        assert_eq!(window.open().len(), 32);
        window.close();
        assert!(!window.is_open());
    }
}
