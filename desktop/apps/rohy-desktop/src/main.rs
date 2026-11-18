//! Rohy for Windows and Linux: tray icon, pairing window and clipboard sync (iced daemon).

mod clipboard;
mod identity;
mod images;
mod platform;
mod service;
mod settings;
mod state;
mod ui;

use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use std::time::Duration;

use iced::widget::image;
use iced::{Element, Subscription, Task, window};
use rohy_core::crypto::Identity;
use rohy_core::pairing::PairingPayload;
use rohy_core::trusted::{FileStore, Registry};
use tray_icon::menu::MenuEvent;

use crate::clipboard::ClipboardLogic;
use crate::service::{Command, Service, ServiceEvent};
use crate::settings::Appearance;
use crate::state::{Effect, Model, ServerStatus};

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum View {
    Pairing,
    Devices,
}

#[derive(Debug, Clone)]
enum Message {
    Tick,
    Open(View),
    WindowClosed(window::Id),
    Forget(String),
    CopyCode,
}

struct App {
    model: Model,
    registry: Arc<Registry>,
    identity: Arc<Identity>,
    name: String,
    service: Service,
    clipboard_logic: Arc<Mutex<ClipboardLogic>>,
    writer: Option<arboard::Clipboard>,
    tray: Option<platform::Tray>,
    tray_error: Option<String>,
    windows: HashMap<window::Id, View>,
    qr: Option<image::Handle>,
    copied: bool,
    /// Windows to open once the server listens (`--appairage`, `--telephones`: manual checks).
    pending: Vec<View>,
    appearance: Appearance,
}

fn data_file(name: &str) -> std::path::PathBuf {
    directories::ProjectDirs::from("app", "rohy", "Rohy")
        .map(|dirs| dirs.data_dir().join(name))
        .unwrap_or_else(|| std::path::PathBuf::from(format!("rohy-{name}")))
}

fn boot() -> (App, Task<Message>) {
    let identity = identity::load_or_create(&identity::KeyringStore).unwrap_or_else(|error| {
        eprintln!("Rohy: trousseau indisponible ({error}), clé éphémère pour cette session");
        Identity::generate()
    });
    let identity = Arc::new(identity);
    let registry = Arc::new(Registry::new(FileStore::open(data_file("devices.json"))));
    let name = gethostname::gethostname().to_string_lossy().into_owned();
    let service = service::spawn(
        identity.clone(),
        name.clone(),
        registry.clone(),
        rohy_core::DEFAULT_PORT,
        true,
    );
    let clipboard_logic = Arc::new(Mutex::new(ClipboardLogic::new(
        platform::current_clipboard_text(),
    )));
    platform::watch_clipboard(clipboard_logic.clone(), service.commands.clone());
    let trusted = registry.store.lock().expect("store lock").all();
    let app = App {
        model: Model {
            trusted,
            ..Model::default()
        },
        registry,
        identity,
        name,
        service,
        clipboard_logic,
        writer: None,
        tray: None,
        tray_error: None,
        windows: HashMap::new(),
        qr: None,
        copied: false,
        pending: std::env::args()
            .filter_map(|arg| match arg.as_str() {
                "--appairage" => Some(View::Pairing),
                "--telephones" => Some(View::Devices),
                _ => None,
            })
            .collect(),
        appearance: Appearance::load(&data_file("appearance")),
    };
    (app, Task::none())
}

impl App {
    fn update(&mut self, message: Message) -> Task<Message> {
        match message {
            Message::Tick => self.tick(),
            Message::Open(view) => self.open(view),
            Message::WindowClosed(id) => {
                if self.windows.remove(&id) == Some(View::Pairing) {
                    self.registry.pairing.lock().expect("pairing lock").close();
                    self.model.pairing_uri = None;
                    self.qr = None;
                }
                Task::none()
            }
            Message::Forget(id) => {
                self.registry.store.lock().expect("store lock").remove(&id);
                let _ = self.service.commands.send(Command::Disconnect(id));
                self.model.trusted = self.registry.store.lock().expect("store lock").all();
                Task::none()
            }
            Message::CopyCode => match &self.model.pairing_uri {
                Some(uri) => {
                    self.copied = true;
                    iced::clipboard::write(uri.clone())
                }
                None => Task::none(),
            },
        }
    }

    fn tick(&mut self) -> Task<Message> {
        // The tray icon must be created on the UI thread once its event loop runs.
        if self.tray.is_none() && self.tray_error.is_none() {
            match platform::Tray::create(self.appearance) {
                Ok(tray) => self.tray = Some(tray),
                Err(error) => self.tray_error = Some(error),
            }
        }
        let mut tasks = Vec::new();
        while let Ok(event) = MenuEvent::receiver().try_recv() {
            let Some(tray) = &self.tray else { continue };
            if event.id == *tray.pair.id() {
                tasks.push(self.open(View::Pairing));
            } else if event.id == *tray.devices.id() {
                tasks.push(self.open(View::Devices));
            } else if event.id == *tray.quit.id() {
                return iced::exit();
            } else if let Some((choice, _)) = tray
                .appearance
                .iter()
                .find(|(_, item)| event.id == *item.id())
            {
                self.appearance = *choice;
                tray.show_appearance(self.appearance);
                if let Err(error) = self.appearance.save(&data_file("appearance")) {
                    eprintln!("Rohy: apparence non enregistrée ({error})");
                }
            }
        }
        while let Ok(event) = self.service.events.try_recv() {
            match event {
                ServiceEvent::Listening(port) => {
                    self.model.status = ServerStatus::Listening(port);
                    let pending = std::mem::take(&mut self.pending);
                    tasks.extend(pending.into_iter().map(|view| self.open(view)));
                }
                ServiceEvent::Failed(error) => self.model.status = ServerStatus::Failed(error),
                ServiceEvent::Server(event) => {
                    let trusted = self.registry.store.lock().expect("store lock").all();
                    match self.model.apply(event, trusted, state::now()) {
                        Effect::WriteClipboard(text) => {
                            self.clipboard_logic
                                .lock()
                                .expect("clipboard logic lock")
                                .mark_written(&text);
                            platform::write_clipboard(&mut self.writer, &text);
                        }
                        Effect::PairingDone => {
                            self.qr = None;
                            let pairing: Vec<_> = self
                                .windows
                                .iter()
                                .filter(|(_, v)| **v == View::Pairing)
                                .map(|(id, _)| *id)
                                .collect();
                            tasks.extend(pairing.into_iter().map(window::close));
                            tasks.push(self.open(View::Devices));
                        }
                        Effect::None => {}
                    }
                }
            }
        }
        if let Some(tray) = &self.tray {
            tray.status.set_text(self.model.status_line());
        }
        Task::batch(tasks)
    }

    fn open(&mut self, view: View) -> Task<Message> {
        if self.windows.values().any(|v| *v == view) {
            return Task::none();
        }
        if view == View::Pairing {
            let Some(port) = self.model.port() else {
                return Task::none();
            };
            let token = self.registry.pairing.lock().expect("pairing lock").open();
            let payload = PairingPayload {
                public_key: self.identity.public_key(),
                token,
                name: self.name.clone(),
                hosts: rohy_server::local_ipv4_addresses(),
                port,
            };
            let uri = payload.uri();
            self.copied = false;
            self.qr = images::qr_rgba(&uri, 8)
                .map(|(w, h, pixels)| image::Handle::from_rgba(w, h, pixels));
            self.model.pairing_uri = Some(uri);
        }
        let (width, height) = ui::WINDOW;
        let (id, task) = window::open(window::Settings {
            size: iced::Size::new(width, height),
            min_size: Some(iced::Size::new(400.0, 600.0)),
            ..window::Settings::default()
        });
        self.windows.insert(id, view);
        task.discard()
    }

    fn title(&self, id: window::Id) -> String {
        match self.windows.get(&id) {
            Some(View::Pairing) => "Rohy — Associer un téléphone".into(),
            _ => "Rohy — Téléphones associés".into(),
        }
    }

    fn view(&self, id: window::Id) -> Element<'_, Message> {
        match self.windows.get(&id) {
            Some(View::Pairing) => ui::pairing(self.qr.clone(), self.copied, Message::CopyCode),
            _ => {
                let mut footer = format!("Visible sur le réseau local comme « {} »", self.name);
                if let Some(port) = self.model.port() {
                    footer.push_str(&format!(" · port {port}"));
                }
                ui::devices(
                    self.model.header(state::now()),
                    self.model.device_rows(),
                    footer,
                    self.tray_error
                        .as_ref()
                        .map(|error| format!("Icône de barre des tâches indisponible : {error}")),
                    Message::Open(View::Pairing),
                    Message::Forget,
                )
            }
        }
    }

    fn subscription(&self) -> Subscription<Message> {
        Subscription::batch([
            iced::time::every(Duration::from_millis(200)).map(|_| Message::Tick),
            window::close_events().map(Message::WindowClosed),
        ])
    }
}

fn main() -> iced::Result {
    ui::FONTS
        .into_iter()
        .fold(
            iced::daemon(boot, App::update, App::view),
            |daemon, bytes| daemon.font(bytes),
        )
        .default_font(ui::BODY)
        .theme(|app: &App, _| app.appearance.theme())
        .title(App::title)
        .subscription(App::subscription)
        .run()
}
