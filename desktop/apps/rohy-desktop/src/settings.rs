//! User preferences kept between launches. For now: the appearance (system, light or dark).

use std::path::Path;

use iced::Theme;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum Appearance {
    /// Follow the light or dark mode of the operating system.
    #[default]
    System,
    Light,
    Dark,
}

impl Appearance {
    pub const ALL: [Appearance; 3] = [Appearance::System, Appearance::Light, Appearance::Dark];

    /// Label in the tray menu.
    pub fn label(self) -> &'static str {
        match self {
            Appearance::System => "Système",
            Appearance::Light => "Clair",
            Appearance::Dark => "Sombre",
        }
    }

    fn key(self) -> &'static str {
        match self {
            Appearance::System => "system",
            Appearance::Light => "light",
            Appearance::Dark => "dark",
        }
    }

    fn parse(text: &str) -> Self {
        Self::ALL
            .into_iter()
            .find(|appearance| appearance.key() == text.trim())
            .unwrap_or_default()
    }

    /// Theme forced on the windows; `None` lets iced follow the system mode.
    pub fn theme(self) -> Option<Theme> {
        match self {
            Appearance::System => None,
            Appearance::Light => Some(Theme::Light),
            Appearance::Dark => Some(Theme::Dark),
        }
    }

    /// Saved choice, or `System` when the file is missing or unreadable.
    pub fn load(path: &Path) -> Self {
        std::fs::read_to_string(path)
            .map(|text| Self::parse(&text))
            .unwrap_or_default()
    }

    pub fn save(self, path: &Path) -> std::io::Result<()> {
        if let Some(parent) = path.parent() {
            std::fs::create_dir_all(parent)?;
        }
        std::fs::write(path, self.key())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn round_trips_through_a_file() {
        let dir = std::env::temp_dir().join(format!(
            "rohy-settings-{}",
            rohy_core::encoding::hex_encode(&rohy_core::random_bytes::<8>())
        ));
        let path = dir.join("nested").join("appearance");
        assert_eq!(Appearance::load(&path), Appearance::System);
        for appearance in Appearance::ALL {
            appearance.save(&path).unwrap();
            assert_eq!(Appearance::load(&path), appearance);
        }
        std::fs::write(&path, "violet").unwrap();
        assert_eq!(Appearance::load(&path), Appearance::System);
        std::fs::remove_dir_all(dir).unwrap();
    }

    #[test]
    fn maps_to_labels_and_themes() {
        assert_eq!(Appearance::System.theme(), None);
        assert_eq!(Appearance::Light.theme(), Some(Theme::Light));
        assert_eq!(Appearance::Dark.theme(), Some(Theme::Dark));
        let labels: Vec<_> = Appearance::ALL.iter().map(|a| a.label()).collect();
        assert_eq!(labels, ["Système", "Clair", "Sombre"]);
        assert_eq!(Appearance::parse(" dark\n"), Appearance::Dark);
    }
}
