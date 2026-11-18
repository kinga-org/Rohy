//! Decides which clipboard contents leave the computer (policy + echo suppression).

use rohy_core::policy::{EchoGuard, should_sync};

pub struct ClipboardLogic {
    guard: EchoGuard,
}

impl ClipboardLogic {
    /// `current` is the clipboard content at startup: it is not sent.
    pub fn new(current: Option<String>) -> Self {
        Self {
            guard: EchoGuard::new(current),
        }
    }

    /// Text to send to the phone after a poll, if any.
    pub fn on_poll(&mut self, formats: &[String], text: Option<String>) -> Option<String> {
        let text = text?;
        if !self.guard.is_new(&text) || !should_sync(formats, Some(&text)) {
            return None;
        }
        Some(text)
    }

    /// Call after writing text received from the phone, so it is not sent back.
    pub fn mark_written(&mut self, text: &str) {
        self.guard.mark_written(text);
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn sends_new_text_once_and_never_secrets_or_echoes() {
        let mut logic = ClipboardLogic::new(Some("au démarrage".into()));
        assert_eq!(logic.on_poll(&[], Some("au démarrage".into())), None);
        assert_eq!(logic.on_poll(&[], None), None);
        assert_eq!(
            logic.on_poll(&[], Some("copié".into())).as_deref(),
            Some("copié")
        );
        assert_eq!(logic.on_poll(&[], Some("copié".into())), None);
        let secret = vec!["ExcludeClipboardContentFromMonitorProcessing".to_string()];
        assert_eq!(logic.on_poll(&secret, Some("mot de passe".into())), None);
        logic.mark_written("du téléphone");
        assert_eq!(logic.on_poll(&[], Some("du téléphone".into())), None);
    }
}
