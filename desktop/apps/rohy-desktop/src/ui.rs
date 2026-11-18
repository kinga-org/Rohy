//! Look of the desktop windows, from the design board "Rohy bureau — refonte":
//! colors, Inter font, stroke icons, widget styles and the two window layouts.

use iced::widget::{
    button, column, container, image, row, rule, scrollable, space, stack, svg, text,
};
use iced::{Alignment, Background, Border, Color, Element, Font, Length, Theme, font};

use crate::state::{DeviceRow, Header};

pub const FONTS: [&[u8]; 4] = [
    include_bytes!("../assets/fonts/Inter-Regular.otf"),
    include_bytes!("../assets/fonts/Inter-Medium.otf"),
    include_bytes!("../assets/fonts/Inter-SemiBold.otf"),
    include_bytes!("../assets/fonts/Inter-Bold.otf"),
];

pub const BODY: Font = Font::with_name("Inter");
const MEDIUM: Font = weight(font::Weight::Medium);
const SEMIBOLD: Font = weight(font::Weight::Semibold);
const BOLD: Font = weight(font::Weight::Bold);

const fn weight(weight: font::Weight) -> Font {
    Font { weight, ..BODY }
}

/// Window content size on the board.
pub const WINDOW: (f32, f32) = (440.0, 640.0);

pub mod palette {
    use iced::Color;

    pub const GROUND: Color = Color::from_rgb8(0xF4, 0xF7, 0xF7);
    pub const SURFACE: Color = Color::WHITE;
    pub const LINE: Color = Color::from_rgb8(0xDC, 0xE4, 0xE6);
    pub const DIVIDER: Color = Color::from_rgb8(0xE6, 0xEC, 0xEE);
    pub const CONTROL_LINE: Color = Color::from_rgb8(0xC5, 0xD2, 0xD5);
    pub const INK: Color = Color::from_rgb8(0x12, 0x26, 0x2C);
    pub const MUTED: Color = Color::from_rgb8(0x4F, 0x63, 0x69);
    pub const ACCENT: Color = Color::from_rgb8(0x1B, 0x4B, 0x5A);
    pub const ACCENT_HOVER: Color = Color::from_rgb8(0x12, 0x35, 0x40);
    pub const ACCENT_SOFT: Color = Color::from_rgb8(0xE8, 0xF0, 0xF2);
    pub const ACCENT_PRESSED_SOFT: Color = Color::from_rgb8(0xDA, 0xE7, 0xEA);
    pub const ON_ACCENT_LABEL: Color = Color::from_rgb8(0xCF, 0xE3, 0xE8);
    pub const ON_ACCENT_BODY: Color = Color::from_rgb8(0xE3, 0xEE, 0xF1);
    pub const NEUTRAL_SOFT: Color = Color::from_rgb8(0xEE, 0xF1, 0xF2);
    pub const SUCCESS: Color = Color::from_rgb8(0x1E, 0x8A, 0x55);
    pub const SUCCESS_TEXT: Color = Color::from_rgb8(0x1E, 0x6B, 0x45);
    pub const OFFLINE: Color = Color::from_rgb8(0x7A, 0x8C, 0x91);
    pub const DANGER: Color = Color::from_rgb8(0xA1, 0x35, 0x2B);
    pub const DANGER_SOFT: Color = Color::from_rgb8(0xFB, 0xED, 0xEB);
}

use palette::*;

/// Stroke icons (24 × 24, tinted at draw time).
pub mod icons {
    macro_rules! stroke {
        ($body:literal) => {
            concat!(
                r#"<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="black" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">"#,
                $body,
                "</svg>"
            )
            .as_bytes()
        };
    }

    pub const COPY: &[u8] = stroke!(
        r#"<rect x="9" y="9" width="12" height="12" rx="2"/><path d="M5 15H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1h10a1 1 0 0 1 1 1v1"/>"#
    );
    pub const CHECK: &[u8] = stroke!(r#"<polyline points="5 12 10 17 19 7"/>"#);
    pub const LOCK: &[u8] = stroke!(
        r#"<rect x="4" y="11" width="16" height="10" rx="2"/><path d="M8 11V7a4 4 0 0 1 8 0v4"/>"#
    );
    pub const PHONE: &[u8] = stroke!(
        r#"<rect x="6" y="2" width="12" height="20" rx="2.5"/><line x1="11" y1="18" x2="13" y2="18"/>"#
    );
    pub const PLUS: &[u8] =
        stroke!(r#"<line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>"#);
    pub const LINK: &[u8] = stroke!(
        r#"<path d="M9 17H7a5 5 0 0 1 0-10h2"/><path d="M15 7h2a5 5 0 0 1 0 10h-2"/><line x1="8" y1="12" x2="16" y2="12"/>"#
    );
    pub const RECEIVED: &[u8] = stroke!(
        r#"<polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>"#
    );

    /// The Rohy mark in its own colors: two white links on the teal disc.
    pub const LOGO: &[u8] = br##"<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><circle cx="20" cy="20" r="20" fill="#1B4B5A"/><rect x="7.2" y="15.6" width="13.6" height="8.8" rx="4.4" fill="none" stroke="#fff" stroke-width="2.4"/><rect x="19.2" y="15.6" width="13.6" height="8.8" rx="4.4" fill="none" stroke="#fff" stroke-width="2.4"/></svg>"##;
}

fn icon<'a, M: 'a>(bytes: &'static [u8], size: f32, color: Color) -> Element<'a, M> {
    svg(svg::Handle::from_memory(bytes))
        .width(size)
        .height(size)
        .style(move |_, _| svg::Style { color: Some(color) })
        .into()
}

fn logo<'a, M: 'a>(size: f32) -> Element<'a, M> {
    svg(svg::Handle::from_memory(icons::LOGO))
        .width(size)
        .height(size)
        .into()
}

// Container styles.

fn page(_: &Theme) -> container::Style {
    container::Style {
        text_color: Some(INK),
        background: Some(GROUND.into()),
        ..container::Style::default()
    }
}

fn card(_: &Theme) -> container::Style {
    container::Style {
        background: Some(SURFACE.into()),
        border: Border::default().rounded(16).width(1).color(LINE),
        ..container::Style::default()
    }
}

fn banner(_: &Theme) -> container::Style {
    container::Style {
        text_color: Some(Color::WHITE),
        background: Some(ACCENT.into()),
        border: Border::default().rounded(16),
        ..container::Style::default()
    }
}

fn filled(color: Color, radius: f32) -> impl Fn(&Theme) -> container::Style {
    move |_| container::Style {
        background: Some(color.into()),
        border: Border::default().rounded(radius),
        ..container::Style::default()
    }
}

fn ring(color: Color, radius: f32, width: f32) -> impl Fn(&Theme) -> container::Style {
    move |_| container::Style {
        border: Border::default().rounded(radius).width(width).color(color),
        ..container::Style::default()
    }
}

fn line(color: Color) -> impl Fn(&Theme) -> rule::Style {
    move |_| rule::Style {
        color,
        radius: 0.0.into(),
        fill_mode: rule::FillMode::Full,
        snap: true,
    }
}

// Button styles.

fn primary(_: &Theme, status: button::Status) -> button::Style {
    let background = match status {
        button::Status::Active => ACCENT,
        button::Status::Hovered | button::Status::Pressed => ACCENT_HOVER,
        button::Status::Disabled => ACCENT.scale_alpha(0.5),
    };
    button::Style {
        background: Some(Background::Color(background)),
        text_color: Color::WHITE,
        border: Border::default().rounded(10),
        ..button::Style::default()
    }
}

fn secondary(_: &Theme, status: button::Status) -> button::Style {
    let background = match status {
        button::Status::Active | button::Status::Disabled => SURFACE,
        button::Status::Hovered => ACCENT_SOFT,
        button::Status::Pressed => ACCENT_PRESSED_SOFT,
    };
    button::Style {
        background: Some(Background::Color(background)),
        text_color: INK,
        border: Border::default().rounded(10).width(1).color(CONTROL_LINE),
        ..button::Style::default()
    }
}

fn quiet_danger(_: &Theme, status: button::Status) -> button::Style {
    let background = match status {
        button::Status::Hovered | button::Status::Pressed => Some(DANGER_SOFT.into()),
        button::Status::Active | button::Status::Disabled => None,
    };
    button::Style {
        background,
        text_color: DANGER,
        border: Border::default().rounded(8),
        ..button::Style::default()
    }
}

/// Full-width 44 px button: icon and label centered.
fn wide_button<'a, M: Clone + 'a>(
    glyph: &'static [u8],
    label: &'a str,
    color: Color,
    style: fn(&Theme, button::Status) -> button::Style,
    on_press: M,
) -> Element<'a, M> {
    let content = row![
        icon(glyph, 18.0, color),
        text(label).size(14).font(SEMIBOLD)
    ]
    .spacing(8)
    .align_y(Alignment::Center);
    button(container(content).center(Length::Fill))
        .width(Length::Fill)
        .height(44)
        .padding(0)
        .style(style)
        .on_press(on_press)
        .into()
}

fn dot<'a, M: 'a>(size: f32, color: Color, hollow: bool) -> Element<'a, M> {
    let shape = container(space()).width(size).height(size);
    if hollow {
        shape.style(ring(color, size / 2.0, 2.0)).into()
    } else {
        shape.style(filled(color, size / 2.0)).into()
    }
}

fn window_page<'a, M: 'a>(content: impl Into<Element<'a, M>>) -> Element<'a, M> {
    container(content)
        .padding(28)
        .width(Length::Fill)
        .height(Length::Fill)
        .style(page)
        .into()
}

/// "Associer un téléphone": QR code, copy fallback, single-use note.
pub fn pairing<'a, M: Clone + 'a>(
    qr: Option<image::Handle>,
    copied: bool,
    on_copy: M,
) -> Element<'a, M> {
    let heading = row![
        logo(40.0),
        column![
            text("Associer un téléphone").size(20).font(BOLD),
            text("Ouvre Rohy sur ton téléphone, puis scanne ce code.")
                .size(13)
                .color(MUTED),
        ]
        .spacing(2),
    ]
    .spacing(12)
    .align_y(Alignment::Center);

    let code: Element<'a, M> = match qr {
        Some(handle) => stack![
            image(handle).width(232).height(232),
            container(
                container(logo(36.0))
                    .padding(4)
                    .style(filled(SURFACE, 12.0))
            )
            .center(232),
        ]
        .into(),
        None => container(text("Le serveur démarre…").size(14).color(MUTED))
            .center(232)
            .into(),
    };
    let waiting = container(
        row![
            dot(8.0, ACCENT, false),
            text("En attente du scan…")
                .size(13)
                .font(MEDIUM)
                .color(ACCENT),
        ]
        .spacing(10)
        .align_y(Alignment::Center),
    )
    .padding([8, 14])
    .style(filled(ACCENT_SOFT, 999.0));
    let code_card = container(
        column![code, waiting]
            .spacing(18)
            .align_x(Alignment::Center),
    )
    .padding(24)
    .width(Length::Fill)
    .align_x(Alignment::Center)
    .style(card);

    let separator = row![
        rule::horizontal(1).style(line(LINE)),
        text("Sans caméra").size(12).color(MUTED),
        rule::horizontal(1).style(line(LINE)),
    ]
    .spacing(12)
    .align_y(Alignment::Center);

    let (glyph, label) = if copied {
        (icons::CHECK, "Code copié")
    } else {
        (icons::COPY, "Copier le code d'association")
    };
    let copy = column![
        wide_button(glyph, label, INK, secondary, on_copy),
        text("Colle-le ensuite dans l'app Rohy du téléphone.")
            .size(12)
            .color(MUTED)
            .width(Length::Fill)
            .align_x(Alignment::Center),
    ]
    .spacing(8);

    let note = row![
        icon(icons::LOCK, 16.0, MUTED),
        text(
            "Code à usage unique, valable tant que cette fenêtre est ouverte. \
             Les échanges sont chiffrés de bout en bout."
        )
        .size(12)
        .color(MUTED),
    ]
    .spacing(8);

    window_page(
        column![
            heading,
            code_card,
            separator,
            copy,
            space().height(Length::Fill),
            note
        ]
        .spacing(20),
    )
}

/// "Téléphones associés": connection state, paired phones (or the empty state), pair button.
pub fn devices<'a, M: Clone + 'a>(
    header: Header,
    rows: Vec<DeviceRow>,
    footer: String,
    warning: Option<String>,
    on_pair: M,
    on_forget: impl Fn(String) -> M,
) -> Element<'a, M> {
    let body: Element<'a, M> = if rows.is_empty() {
        empty_state()
    } else {
        let mut list = column![];
        let count = rows.len();
        for (index, device) in rows.into_iter().enumerate() {
            let forget = on_forget(device.id.clone());
            list = list.push(device_row(device, forget));
            if index + 1 < count {
                list =
                    list.push(container(rule::horizontal(1).style(line(DIVIDER))).padding([0, 16]));
            }
        }
        column![
            text("TÉLÉPHONES ASSOCIÉS")
                .size(13)
                .font(SEMIBOLD)
                .color(MUTED),
            container(scrollable(list)).style(card),
            space().height(Length::Fill),
        ]
        .spacing(10)
        .height(Length::Fill)
        .into()
    };

    let mut bottom = column![
        wide_button(
            icons::PLUS,
            "Associer un téléphone",
            Color::WHITE,
            primary,
            on_pair
        ),
        text(footer)
            .size(12)
            .color(MUTED)
            .width(Length::Fill)
            .align_x(Alignment::Center),
    ]
    .spacing(10);
    if let Some(warning) = warning {
        bottom = bottom.push(
            text(warning)
                .size(12)
                .color(DANGER)
                .width(Length::Fill)
                .align_x(Alignment::Center),
        );
    }

    window_page(column![header_view(header), body, bottom].spacing(20))
}

fn header_view<'a, M: 'a>(header: Header) -> Element<'a, M> {
    let (title, subtitle, marker) = match header {
        Header::Connected { names, activity } => return connected_banner(names, activity),
        Header::Waiting => (
            "En attente du téléphone".to_string(),
            "Rohy écoute sur le réseau local.".to_string(),
            dot(10.0, OFFLINE, true),
        ),
        Header::Starting => (
            "Démarrage…".to_string(),
            "Rohy prépare le serveur local.".to_string(),
            dot(10.0, OFFLINE, true),
        ),
        Header::Failed(error) => ("Erreur réseau".to_string(), error, dot(10.0, DANGER, false)),
    };
    container(
        row![
            marker,
            column![
                text(title).size(16).font(SEMIBOLD),
                text(subtitle).size(12).color(MUTED),
            ]
            .spacing(2),
        ]
        .spacing(12)
        .align_y(Alignment::Center),
    )
    .padding([18, 20])
    .width(Length::Fill)
    .style(card)
    .into()
}

fn connected_banner<'a, M: 'a>(names: String, activity: Option<String>) -> Element<'a, M> {
    let badge = container(icon(icons::LINK, 22.0, Color::WHITE))
        .center(40)
        .style(filled(Color::WHITE.scale_alpha(0.14), 20.0));
    let mut content = column![
        row![
            badge,
            column![
                text("SYNCHRONISATION ACTIVE")
                    .size(12)
                    .font(MEDIUM)
                    .color(ON_ACCENT_LABEL),
                text(format!("Connecté à {names}"))
                    .size(19)
                    .font(BOLD)
                    .color(Color::WHITE),
            ]
            .spacing(2),
        ]
        .spacing(12)
        .align_y(Alignment::Center)
    ]
    .spacing(12);
    if let Some(activity) = activity {
        content = content
            .push(rule::horizontal(1).style(line(Color::WHITE.scale_alpha(0.16))))
            .push(
                row![
                    icon(icons::RECEIVED, 16.0, ON_ACCENT_BODY),
                    text(activity).size(13).color(ON_ACCENT_BODY),
                ]
                .spacing(8)
                .align_y(Alignment::Center),
            );
    }
    container(content)
        .padding(20)
        .width(Length::Fill)
        .style(banner)
        .into()
}

fn device_row<'a, M: Clone + 'a>(device: DeviceRow, on_forget: M) -> Element<'a, M> {
    let (tile, glyph, status) = if device.connected {
        (ACCENT_SOFT, ACCENT, SUCCESS_TEXT)
    } else {
        (NEUTRAL_SOFT, MUTED, MUTED)
    };
    let marker = if device.connected {
        dot(7.0, SUCCESS, false)
    } else {
        dot(7.0, OFFLINE, true)
    };
    row![
        container(icon(icons::PHONE, 20.0, glyph))
            .center(40)
            .style(filled(tile, 10.0)),
        column![
            text(device.name).size(15).font(SEMIBOLD),
            row![marker, text(device.detail).size(12).color(status)]
                .spacing(6)
                .align_y(Alignment::Center),
        ]
        .spacing(3)
        .width(Length::Fill),
        button(container(text("Oublier").size(13).font(MEDIUM)).center_y(Length::Fill))
            .height(36)
            .padding([0, 12])
            .style(quiet_danger)
            .on_press(on_forget),
    ]
    .spacing(12)
    .align_y(Alignment::Center)
    .padding([14, 16])
    .into()
}

fn empty_state<'a, M: 'a>() -> Element<'a, M> {
    container(
        column![
            container(icon(icons::PHONE, 40.0, ACCENT))
                .center(88)
                .style(filled(ACCENT_SOFT, 44.0)),
            text("Aucun téléphone associé").size(18).font(BOLD),
            text("Associe ton téléphone Android pour copier d'un côté et coller de l'autre.")
                .size(14)
                .color(MUTED)
                .align_x(Alignment::Center),
        ]
        .spacing(16)
        .max_width(320)
        .align_x(Alignment::Center),
    )
    .center(Length::Fill)
    .into()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[derive(Debug, Clone, PartialEq)]
    enum Probe {
        Copy,
        Pair,
        Forget(String),
    }

    fn row(connected: bool) -> DeviceRow {
        DeviceRow {
            id: format!("id-{connected}"),
            name: "Galaxy".into(),
            connected,
            detail: "Connecté · associé le 18 nov.".into(),
        }
    }

    #[test]
    fn every_window_state_builds() {
        let qr = image::Handle::from_rgba(1, 1, vec![0, 0, 0, 255]);
        for (handle, copied) in [(Some(qr), false), (None, true)] {
            let _: Element<'_, Probe> = pairing(handle, copied, Probe::Copy);
        }
        let headers = [
            Header::Starting,
            Header::Waiting,
            Header::Failed("port occupé".into()),
            Header::Connected {
                names: "Galaxy".into(),
                activity: Some("Texte reçu de Galaxy · il y a 12 s".into()),
            },
            Header::Connected {
                names: "Galaxy".into(),
                activity: None,
            },
        ];
        for header in headers {
            for rows in [vec![], vec![row(true), row(false)]] {
                let _: Element<'_, Probe> = devices(
                    header.clone(),
                    rows,
                    "Visible sur le réseau local".into(),
                    Some("Icône indisponible".into()),
                    Probe::Pair,
                    Probe::Forget,
                );
            }
        }
        let _: Element<'_, Probe> = devices(
            Header::Waiting,
            vec![row(true)],
            String::new(),
            None,
            Probe::Pair,
            Probe::Forget,
        );
        assert_eq!(Probe::Forget("a".into()), Probe::Forget("a".into()));
    }

    #[test]
    fn styles_follow_button_status() {
        let theme = Theme::Light;
        let statuses = [
            button::Status::Active,
            button::Status::Hovered,
            button::Status::Pressed,
            button::Status::Disabled,
        ];
        for status in statuses {
            assert_eq!(primary(&theme, status).text_color, Color::WHITE);
            assert_eq!(secondary(&theme, status).text_color, INK);
            assert_eq!(quiet_danger(&theme, status).text_color, DANGER);
        }
        assert_eq!(
            primary(&theme, button::Status::Hovered).background,
            Some(Background::Color(ACCENT_HOVER))
        );
        assert_eq!(
            quiet_danger(&theme, button::Status::Active).background,
            None
        );
        assert_eq!(page(&theme).background, Some(GROUND.into()));
        assert_eq!(card(&theme).border.color, LINE);
        assert_eq!(banner(&theme).background, Some(ACCENT.into()));
        assert_eq!(ring(OFFLINE, 5.0, 2.0)(&theme).border.width, 2.0);
        assert_eq!(
            filled(SUCCESS, 4.0)(&theme).background,
            Some(SUCCESS.into())
        );
        assert_eq!(line(DIVIDER)(&theme).color, DIVIDER);
    }

    /// Renders each window to PNG for comparison with the design board:
    /// `ROHY_SNAPSHOT_DIR=/tmp/rohy cargo test -p rohy-desktop -- --ignored renders`.
    #[test]
    #[ignore = "manual visual check, writes PNG files"]
    fn renders_windows_for_review() {
        let dir = std::path::PathBuf::from(
            std::env::var("ROHY_SNAPSHOT_DIR").unwrap_or_else(|_| "target/snapshots".into()),
        );
        let uri = "rohy://pair?v=1&pk=KvX7alX_NLM5jGQ8bQzTHCEX6zl4KAisrSbv1Axo2WI&t=KvX7alX_NLM5jGQ8bQzTHCEX6zl4KAisrSbv1Axo2WI&n=MacBook-Pro&h=192.168.88.3&p=47823";
        let (w, h, pixels) = crate::images::qr_rgba(uri, 8).unwrap();
        let qr = image::Handle::from_rgba(w, h, pixels);
        let connected = Header::Connected {
            names: "SM-S928B".into(),
            activity: Some("Texte reçu de SM-S928B · il y a 12 s".into()),
        };
        let rows = vec![
            DeviceRow {
                id: "a".into(),
                name: "SM-S928B".into(),
                connected: true,
                detail: "Connecté · associé le 18 nov.".into(),
            },
            DeviceRow {
                id: "b".into(),
                name: "Pixel 9".into(),
                connected: false,
                detail: "Hors ligne · associé le 2 nov.".into(),
            },
        ];
        let footer = "Visible sur le réseau local comme « MacBook-Pro » · port 47823";
        let screens: Vec<(&str, Element<'_, Probe>)> = vec![
            ("pairing", pairing(Some(qr), false, Probe::Copy)),
            (
                "devices",
                devices(
                    connected,
                    rows,
                    footer.into(),
                    None,
                    Probe::Pair,
                    Probe::Forget,
                ),
            ),
            (
                "empty",
                devices(
                    Header::Waiting,
                    vec![],
                    footer.into(),
                    None,
                    Probe::Pair,
                    Probe::Forget,
                ),
            ),
        ];
        for (name, element) in screens {
            let settings = iced::Settings {
                fonts: FONTS.iter().map(|bytes| (*bytes).into()).collect(),
                default_font: BODY,
                ..iced::Settings::default()
            };
            let mut simulator = iced_test::Simulator::with_size(settings, WINDOW, element);
            let snapshot = simulator.snapshot(&Theme::Light).unwrap();
            assert!(snapshot.matches_image(dir.join(name)).unwrap());
        }
    }

    #[test]
    fn assets_are_embedded() {
        for bytes in FONTS {
            assert_eq!(&bytes[..4], b"OTTO");
        }
        for glyph in [
            icons::COPY,
            icons::CHECK,
            icons::LOCK,
            icons::PHONE,
            icons::PLUS,
            icons::LINK,
            icons::RECEIVED,
            icons::LOGO,
        ] {
            assert!(glyph.starts_with(b"<svg") && glyph.ends_with(b"</svg>"));
        }
    }
}
