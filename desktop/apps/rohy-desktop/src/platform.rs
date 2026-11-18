//! Platform glue: system clipboard access and the tray icon. Exercised by manual tests on each system.

use std::sync::{Arc, Mutex};
use std::time::Duration;

use rohy_core::messages::Clip;
use tokio::sync::mpsc::UnboundedSender;
use tray_icon::menu::{Menu, MenuItem, PredefinedMenuItem};
use tray_icon::{Icon, TrayIcon, TrayIconBuilder};

use crate::clipboard::ClipboardLogic;
use crate::service::Command;

/// Names of the formats currently on the Windows clipboard (password managers mark secrets with them).
#[cfg(windows)]
fn clipboard_formats() -> Vec<String> {
    let Ok(_open) = clipboard_win::Clipboard::new_attempts(10) else {
        return Vec::new();
    };
    clipboard_win::raw::EnumFormats::new()
        .filter_map(clipboard_win::raw::format_name_big)
        .collect()
}

/// Other systems: arboard does not expose formats (KDE password hint not checked yet).
#[cfg(not(windows))]
fn clipboard_formats() -> Vec<String> {
    Vec::new()
}

/// Polls the clipboard every 500 ms on its own thread and sends new text to the phones.
/// GNOME on Wayland refuses background reads: nothing is sent there (documented limitation).
pub fn watch_clipboard(logic: Arc<Mutex<ClipboardLogic>>, commands: UnboundedSender<Command>) {
    std::thread::spawn(move || {
        let Ok(mut clipboard) = arboard::Clipboard::new() else {
            return;
        };
        loop {
            std::thread::sleep(Duration::from_millis(500));
            let formats = clipboard_formats();
            let text = clipboard.get_text().ok();
            let outgoing = logic
                .lock()
                .expect("clipboard logic lock")
                .on_poll(&formats, text);
            if let Some(text) = outgoing
                && commands.send(Command::Broadcast(Clip::new(text))).is_err()
            {
                return;
            }
        }
    });
}

pub fn current_clipboard_text() -> Option<String> {
    arboard::Clipboard::new().ok()?.get_text().ok()
}

/// Writes text received from a phone. The `Clipboard` must outlive the call on Linux (X11 serves it).
pub fn write_clipboard(clipboard: &mut Option<arboard::Clipboard>, text: &str) -> bool {
    if clipboard.is_none() {
        *clipboard = arboard::Clipboard::new().ok();
    }
    clipboard
        .as_mut()
        .is_some_and(|c| c.set_text(text.to_string()).is_ok())
}

pub struct Tray {
    _icon: TrayIcon,
    pub status: MenuItem,
    pub pair: MenuItem,
    pub devices: MenuItem,
    pub quit: MenuItem,
}

impl Tray {
    pub fn create() -> Result<Self, String> {
        let status = MenuItem::new("Démarrage…", false, None);
        let pair = MenuItem::new("Associer un téléphone…", true, None);
        let devices = MenuItem::new("Téléphones associés…", true, None);
        let quit = MenuItem::new("Quitter Rohy", true, None);
        let menu = Menu::new();
        menu.append_items(&[
            &status,
            &PredefinedMenuItem::separator(),
            &pair,
            &devices,
            &PredefinedMenuItem::separator(),
            &quit,
        ])
        .map_err(|error| error.to_string())?;
        let size = 64;
        let icon = Icon::from_rgba(crate::images::tray_icon_rgba(size), size, size)
            .map_err(|error| error.to_string())?;
        let tray = TrayIconBuilder::new()
            .with_menu(Box::new(menu))
            .with_tooltip("Rohy")
            .with_icon(icon)
            .build()
            .map_err(|error| error.to_string())?;
        Ok(Self {
            _icon: tray,
            status,
            pair,
            devices,
            quit,
        })
    }
}
