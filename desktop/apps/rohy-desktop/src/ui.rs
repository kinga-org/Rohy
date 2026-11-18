//! Look of the desktop windows, from the design board "Rohy bureau — refonte":
//! light and dark palettes, Inter font, stroke icons, widget styles and the two window layouts.
//!
//! Colors are picked at draw time from the iced theme, so the windows follow the appearance
//! chosen in the tray menu, or the system mode, without being rebuilt.

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

/// Colors of one appearance.
#[derive(Debug, Clone, Copy, PartialEq)]
pub struct Palette {
    pub ground: Color,
    pub surface: Color,
    pub line: Color,
    pub divider: Color,
    pub control_line: Color,
    pub ink: Color,
    pub muted: Color,
    /// Rohy mark disc.
    pub logo: Color,
    pub primary: Color,
    pub primary_hover: Color,
    /// Accent used for text and icons on the ground or on `accent_soft`.
    pub accent_text: Color,
    pub accent_soft: Color,
    pub accent_pressed_soft: Color,
    pub neutral_soft: Color,
    pub success: Color,
    pub success_text: Color,
    pub offline: Color,
    pub danger: Color,
    pub danger_soft: Color,
}

const fn hex(rgb: u32) -> Color {
    Color::from_rgb8((rgb >> 16) as u8, (rgb >> 8) as u8, rgb as u8)
}

/// Brand teal: the connection banner keeps it in both appearances.
const BRAND: Color = hex(0x1B4B5A);
const ON_BRAND_LABEL: Color = hex(0xCFE3E8);
const ON_BRAND_BODY: Color = hex(0xE3EEF1);

pub const LIGHT: Palette = Palette {
    ground: hex(0xF4F7F7),
    surface: Color::WHITE,
    line: hex(0xDCE4E6),
    divider: hex(0xE6ECEE),
    control_line: hex(0xC5D2D5),
    ink: hex(0x12262C),
    muted: hex(0x4F6369),
    logo: BRAND,
    primary: BRAND,
    primary_hover: hex(0x123540),
    accent_text: BRAND,
    accent_soft: hex(0xE8F0F2),
    accent_pressed_soft: hex(0xDAE7EA),
    neutral_soft: hex(0xEEF1F2),
    success: hex(0x1E8A55),
    success_text: hex(0x1E6B45),
    offline: hex(0x7A8C91),
    danger: hex(0xA1352B),
    danger_soft: hex(0xFBEDEB),
};

pub const DARK: Palette = Palette {
    ground: hex(0x0F1A1D),
    surface: hex(0x172429),
    line: hex(0x26363B),
    divider: hex(0x223136),
    control_line: hex(0x34474D),
    ink: hex(0xE6EEF0),
    muted: hex(0x9AAEB3),
    logo: hex(0x2A6B7E),
    primary: hex(0x2A6B7E),
    primary_hover: hex(0x33809A),
    accent_text: hex(0x8CC7D6),
    accent_soft: hex(0x1E3A42),
    accent_pressed_soft: hex(0x24464F),
    neutral_soft: hex(0x213035),
    success: hex(0x3CB97A),
    success_text: hex(0x6FD39F),
    offline: hex(0x6F858B),
    danger: hex(0xF08A7E),
    danger_soft: hex(0x3A2220),
};

/// Palette matching the theme iced resolved for the window.
pub fn palette(theme: &Theme) -> &'static Palette {
    if theme.extended_palette().is_dark {
        &DARK
    } else {
        &LIGHT
    }
}

/// Chooses one color of the current palette.
type Pick = fn(&Palette) -> Color;

fn white(_: &Palette) -> Color {
    Color::WHITE
}

fn tint(pick: Pick) -> impl Fn(&Theme) -> text::Style {
    move |theme| text::Style {
        color: Some(pick(palette(theme))),
    }
}

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

    /// The two links of the Rohy mark, drawn over its disc (40 × 40).
    pub const MARK: &[u8] = br#"<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40" fill="none" stroke="black" stroke-width="2.4"><rect x="7.2" y="15.6" width="13.6" height="8.8" rx="4.4"/><rect x="19.2" y="15.6" width="13.6" height="8.8" rx="4.4"/></svg>"#;
}

fn icon<'a, M: 'a>(bytes: &'static [u8], size: f32, pick: Pick) -> Element<'a, M> {
    svg(svg::Handle::from_memory(bytes))
        .width(size)
        .height(size)
        .style(move |theme, _| svg::Style {
            color: Some(pick(palette(theme))),
        })
        .into()
}

/// Rohy mark: white links on a disc of the `disc` color.
fn logo<'a, M: 'a>(size: f32, disc: Pick) -> Element<'a, M> {
    container(icon(icons::MARK, size, white))
        .center(size)
        .style(filled(disc, size / 2.0))
        .into()
}

// Container styles.

fn page(theme: &Theme) -> container::Style {
    let p = palette(theme);
    container::Style {
        text_color: Some(p.ink),
        background: Some(p.ground.into()),
        ..container::Style::default()
    }
}

fn card(theme: &Theme) -> container::Style {
    let p = palette(theme);
    container::Style {
        background: Some(p.surface.into()),
        border: Border::default().rounded(16).width(1).color(p.line),
        ..container::Style::default()
    }
}

fn banner(_: &Theme) -> container::Style {
    container::Style {
        text_color: Some(Color::WHITE),
        background: Some(BRAND.into()),
        border: Border::default().rounded(16),
        ..container::Style::default()
    }
}

fn filled(pick: Pick, radius: f32) -> impl Fn(&Theme) -> container::Style {
    move |theme| container::Style {
        background: Some(pick(palette(theme)).into()),
        border: Border::default().rounded(radius),
        ..container::Style::default()
    }
}

fn ring(pick: Pick, radius: f32, width: f32) -> impl Fn(&Theme) -> container::Style {
    move |theme| container::Style {
        border: Border::default()
            .rounded(radius)
            .width(width)
            .color(pick(palette(theme))),
        ..container::Style::default()
    }
}

fn line(pick: Pick) -> impl Fn(&Theme) -> rule::Style {
    move |theme| rule::Style {
        color: pick(palette(theme)),
        radius: 0.0.into(),
        fill_mode: rule::FillMode::Full,
        snap: true,
    }
}

// Button styles.

fn primary(theme: &Theme, status: button::Status) -> button::Style {
    let p = palette(theme);
    let background = match status {
        button::Status::Active => p.primary,
        button::Status::Hovered | button::Status::Pressed => p.primary_hover,
        button::Status::Disabled => p.primary.scale_alpha(0.5),
    };
    button::Style {
        background: Some(Background::Color(background)),
        text_color: Color::WHITE,
        border: Border::default().rounded(10),
        ..button::Style::default()
    }
}

fn secondary(theme: &Theme, status: button::Status) -> button::Style {
    let p = palette(theme);
    let background = match status {
        button::Status::Active | button::Status::Disabled => p.surface,
        button::Status::Hovered => p.accent_soft,
        button::Status::Pressed => p.accent_pressed_soft,
    };
    button::Style {
        background: Some(Background::Color(background)),
        text_color: p.ink,
        border: Border::default().rounded(10).width(1).color(p.control_line),
        ..button::Style::default()
    }
}

fn quiet_danger(theme: &Theme, status: button::Status) -> button::Style {
    let p = palette(theme);
    let background = match status {
        button::Status::Hovered | button::Status::Pressed => Some(p.danger_soft.into()),
        button::Status::Active | button::Status::Disabled => None,
    };
    button::Style {
        background,
        text_color: p.danger,
        border: Border::default().rounded(8),
        ..button::Style::default()
    }
}

/// Full-width 44 px button: icon and label centered.
fn wide_button<'a, M: Clone + 'a>(
    glyph: &'static [u8],
    label: &'a str,
    glyph_color: Pick,
    style: fn(&Theme, button::Status) -> button::Style,
    on_press: M,
) -> Element<'a, M> {
    let content = row![
        icon(glyph, 18.0, glyph_color),
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

fn dot<'a, M: 'a>(size: f32, pick: Pick, hollow: bool) -> Element<'a, M> {
    let shape = container(space()).width(size).height(size);
    if hollow {
        shape.style(ring(pick, size / 2.0, 2.0)).into()
    } else {
        shape.style(filled(pick, size / 2.0)).into()
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
        logo(40.0, |p| p.logo),
        column![
            text("Associer un téléphone").size(20).font(BOLD),
            text("Ouvre Rohy sur ton téléphone, puis scanne ce code.")
                .size(13)
                .style(tint(|p| p.muted)),
        ]
        .spacing(2),
    ]
    .spacing(12)
    .align_y(Alignment::Center);

    // The QR code stays dark on white in both appearances so that every camera reads it.
    let code: Element<'a, M> = match qr {
        Some(handle) => stack![
            image(handle).width(232).height(232),
            container(
                container(logo(36.0, |_| BRAND))
                    .padding(4)
                    .style(filled(white, 12.0))
            )
            .center(232),
        ]
        .into(),
        None => container(
            text("Le serveur démarre…")
                .size(14)
                .style(tint(|p| p.muted)),
        )
        .center(232)
        .into(),
    };
    let waiting = container(
        row![
            dot(8.0, |p| p.accent_text, false),
            text("En attente du scan…")
                .size(13)
                .font(MEDIUM)
                .style(tint(|p| p.accent_text)),
        ]
        .spacing(10)
        .align_y(Alignment::Center),
    )
    .padding([8, 14])
    .style(filled(|p| p.accent_soft, 999.0));
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
        rule::horizontal(1).style(line(|p| p.line)),
        text("Sans caméra").size(12).style(tint(|p| p.muted)),
        rule::horizontal(1).style(line(|p| p.line)),
    ]
    .spacing(12)
    .align_y(Alignment::Center);

    let (glyph, label) = if copied {
        (icons::CHECK, "Code copié")
    } else {
        (icons::COPY, "Copier le code d'association")
    };
    let copy = column![
        wide_button(glyph, label, |p| p.ink, secondary, on_copy),
        text("Colle-le ensuite dans l'app Rohy du téléphone.")
            .size(12)
            .style(tint(|p| p.muted))
            .width(Length::Fill)
            .align_x(Alignment::Center),
    ]
    .spacing(8);

    let note = row![
        icon(icons::LOCK, 16.0, |p| p.muted),
        text(
            "Code à usage unique, valable tant que cette fenêtre est ouverte. \
             Les échanges sont chiffrés de bout en bout."
        )
        .size(12)
        .style(tint(|p| p.muted)),
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
                list = list.push(
                    container(rule::horizontal(1).style(line(|p| p.divider))).padding([0, 16]),
                );
            }
        }
        column![
            text("TÉLÉPHONES ASSOCIÉS")
                .size(13)
                .font(SEMIBOLD)
                .style(tint(|p| p.muted)),
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
            white,
            primary,
            on_pair
        ),
        text(footer)
            .size(12)
            .style(tint(|p| p.muted))
            .width(Length::Fill)
            .align_x(Alignment::Center),
    ]
    .spacing(10);
    if let Some(warning) = warning {
        bottom = bottom.push(
            text(warning)
                .size(12)
                .style(tint(|p| p.danger))
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
            dot(10.0, |p| p.offline, true),
        ),
        Header::Starting => (
            "Démarrage…".to_string(),
            "Rohy prépare le serveur local.".to_string(),
            dot(10.0, |p| p.offline, true),
        ),
        Header::Failed(error) => (
            "Erreur réseau".to_string(),
            error,
            dot(10.0, |p| p.danger, false),
        ),
    };
    container(
        row![
            marker,
            column![
                text(title).size(16).font(SEMIBOLD),
                text(subtitle).size(12).style(tint(|p| p.muted)),
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
    let badge = container(icon(icons::LINK, 22.0, white))
        .center(40)
        .style(filled(|_| Color::WHITE.scale_alpha(0.14), 20.0));
    let mut content = column![
        row![
            badge,
            column![
                text("SYNCHRONISATION ACTIVE")
                    .size(12)
                    .font(MEDIUM)
                    .color(ON_BRAND_LABEL),
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
            .push(rule::horizontal(1).style(line(|_| Color::WHITE.scale_alpha(0.16))))
            .push(
                row![
                    icon(icons::RECEIVED, 16.0, |_| ON_BRAND_BODY),
                    text(activity).size(13).color(ON_BRAND_BODY),
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
    let (tile, glyph, status, marker): (Pick, Pick, Pick, _) = if device.connected {
        (
            |p| p.accent_soft,
            |p| p.accent_text,
            |p| p.success_text,
            dot(7.0, |p| p.success, false),
        )
    } else {
        (
            |p| p.neutral_soft,
            |p| p.muted,
            |p| p.muted,
            dot(7.0, |p| p.offline, true),
        )
    };
    row![
        container(icon(icons::PHONE, 20.0, glyph))
            .center(40)
            .style(filled(tile, 10.0)),
        column![
            text(device.name).size(15).font(SEMIBOLD),
            row![marker, text(device.detail).size(12).style(tint(status))]
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
            container(icon(icons::PHONE, 40.0, |p| p.accent_text))
                .center(88)
                .style(filled(|p| p.accent_soft, 44.0)),
            text("Aucun téléphone associé").size(18).font(BOLD),
            text("Associe ton téléphone Android pour copier d'un côté et coller de l'autre.")
                .size(14)
                .style(tint(|p| p.muted))
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
        assert_eq!(Probe::Forget("a".into()), Probe::Forget("a".into()));
    }

    #[test]
    fn styles_follow_the_theme_and_button_status() {
        assert_eq!(palette(&Theme::Light), &LIGHT);
        assert_eq!(palette(&Theme::Dark), &DARK);
        let statuses = [
            button::Status::Active,
            button::Status::Hovered,
            button::Status::Pressed,
            button::Status::Disabled,
        ];
        for (theme, p) in [(Theme::Light, LIGHT), (Theme::Dark, DARK)] {
            for status in statuses {
                assert_eq!(primary(&theme, status).text_color, Color::WHITE);
                assert_eq!(secondary(&theme, status).text_color, p.ink);
                assert_eq!(quiet_danger(&theme, status).text_color, p.danger);
            }
            assert_eq!(
                primary(&theme, button::Status::Hovered).background,
                Some(Background::Color(p.primary_hover))
            );
            assert_eq!(
                quiet_danger(&theme, button::Status::Active).background,
                None
            );
            assert_eq!(page(&theme).background, Some(p.ground.into()));
            assert_eq!(card(&theme).border.color, p.line);
            assert_eq!(banner(&theme).background, Some(BRAND.into()));
            assert_eq!(
                ring(|p| p.offline, 5.0, 2.0)(&theme).border.color,
                p.offline
            );
            assert_eq!(
                filled(|p| p.success, 4.0)(&theme).background,
                Some(p.success.into())
            );
            assert_eq!(line(|p| p.divider)(&theme).color, p.divider);
            assert_eq!(tint(|p| p.muted)(&theme).color, Some(p.muted));
        }
    }

    /// Renders each window in both appearances to PNG, for comparison with the design board:
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
        let footer = "Visible sur le réseau local comme « MacBook-Pro » · port 47823";
        let screens = || -> Vec<(&str, Element<'_, Probe>)> {
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
            let connected = Header::Connected {
                names: "SM-S928B".into(),
                activity: Some("Texte reçu de SM-S928B · il y a 12 s".into()),
            };
            vec![
                ("pairing", pairing(Some(qr.clone()), false, Probe::Copy)),
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
            ]
        };
        for (suffix, theme) in [("light", Theme::Light), ("dark", Theme::Dark)] {
            for (name, element) in screens() {
                let settings = iced::Settings {
                    fonts: FONTS.iter().map(|bytes| (*bytes).into()).collect(),
                    default_font: BODY,
                    ..iced::Settings::default()
                };
                let mut simulator = iced_test::Simulator::with_size(settings, WINDOW, element);
                let snapshot = simulator.snapshot(&theme).unwrap();
                assert!(
                    snapshot
                        .matches_image(dir.join(format!("{name}-{suffix}")))
                        .unwrap()
                );
            }
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
            icons::MARK,
        ] {
            assert!(glyph.starts_with(b"<svg") && glyph.ends_with(b"</svg>"));
        }
    }
}
