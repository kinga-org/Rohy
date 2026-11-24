//! Look of the desktop windows, same identity as the Android app (palette Lagon & Soleil, logo
//! "La corde"): light and dark palettes, Inter font, stroke icons, widget styles and the two window
//! layouts.
//!
//! Colors are picked at draw time from the iced theme, so the windows follow the appearance
//! chosen in the tray menu, or the system mode, without being rebuilt.

use iced::widget::canvas::{self, LineCap, LineDash, Path, Stroke};
use iced::widget::{
    button, column, container, image, row, rule, scrollable, space, stack, svg, text,
};
use iced::{
    Alignment, Background, Border, Color, Element, Font, Length, Point, Rectangle, Renderer, Size,
    Theme, border, font, mouse,
};

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

/// Colors of one appearance (palette Lagon & Soleil).
#[derive(Debug, Clone, Copy, PartialEq)]
pub struct Palette {
    pub ground: Color,
    pub surface: Color,
    pub line: Color,
    pub divider: Color,
    pub ink: Color,
    pub muted: Color,
    pub primary: Color,
    pub primary_hover: Color,
    pub on_primary: Color,
    /// Soft lagoon: icon tiles, tonal buttons, the waiting pill.
    pub accent_soft: Color,
    pub accent_pressed_soft: Color,
    pub on_accent_soft: Color,
    /// Sun yellow: small cheerful details (heart, cheeks, offline phone tiles).
    pub sun: Color,
    pub sun_soft: Color,
    pub on_sun_soft: Color,
    /// Connection card, its text, and the smiling devices drawn on it.
    pub hero: Color,
    pub on_hero: Color,
    pub hero_device: Color,
    pub success: Color,
    pub success_text: Color,
    pub offline: Color,
    pub danger: Color,
    pub danger_soft: Color,
}

const fn hex(rgb: u32) -> Color {
    Color::from_rgb8((rgb >> 16) as u8, (rgb >> 8) as u8, rgb as u8)
}

pub const LIGHT: Palette = Palette {
    ground: hex(0xF4F9FA),
    surface: Color::WHITE,
    line: hex(0xE0ECEF),
    divider: hex(0xE6F0F2),
    ink: hex(0x0E2A33),
    muted: hex(0x47626A),
    primary: hex(0x1B6E85),
    primary_hover: hex(0x155A6D),
    on_primary: Color::WHITE,
    accent_soft: hex(0xD3ECF3),
    accent_pressed_soft: hex(0xC2E2EB),
    on_accent_soft: hex(0x073B49),
    sun: hex(0xF5B71E),
    sun_soft: hex(0xFFF1C7),
    on_sun_soft: hex(0x6A4A00),
    hero: hex(0xD3ECF3),
    on_hero: hex(0x073B49),
    hero_device: Color::WHITE,
    success: hex(0x1E9B5C),
    success_text: hex(0x17663F),
    offline: hex(0x7A8F95),
    danger: hex(0x9A2E24),
    danger_soft: hex(0xFBEDEB),
};

/// Dark: Android's neutral dark grays, lagoon only as an accent.
pub const DARK: Palette = Palette {
    ground: hex(0x191C1D),
    surface: hex(0x262A2C),
    line: hex(0x353A3C),
    divider: hex(0x353A3C),
    ink: hex(0xE1E3E3),
    muted: hex(0xBFC8CA),
    primary: hex(0x7FD0E6),
    primary_hover: hex(0x99DCEE),
    on_primary: hex(0x003543),
    accent_soft: hex(0x1F4B57),
    accent_pressed_soft: hex(0x285B68),
    on_accent_soft: hex(0xCDEFF8),
    sun: hex(0xFFD25E),
    sun_soft: hex(0x4F3D0A),
    on_sun_soft: hex(0xFFEDB8),
    hero: hex(0x262A2C),
    on_hero: hex(0xE1E3E3),
    hero_device: hex(0x353B3E),
    success: hex(0x6FDBA0),
    success_text: hex(0x9BE8BD),
    offline: hex(0x899295),
    danger: hex(0xFF9A8E),
    danger_soft: hex(0x43262A),
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
    pub const RECEIVED: &[u8] = stroke!(
        r#"<polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>"#
    );

    /// Rohy logo "La corde": a soft rope linking two points, on a lagoon rounded square (in color).
    pub const LOGO: &[u8] = br##"<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect width="100" height="100" rx="28" fill="#1B6E85"/><path d="M27 67 C 27 36, 50 32, 50 50 C 50 68, 73 64, 73 33" fill="none" stroke="#FFFFFF" stroke-width="10" stroke-linecap="round"/><circle cx="27" cy="69" r="9" fill="#F5B71E"/><circle cx="73" cy="31" r="9" fill="#F5B71E"/></svg>"##;
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

/// Rohy logo in its own colors, the same in both appearances.
fn logo<'a, M: 'a>(size: f32) -> Element<'a, M> {
    svg(svg::Handle::from_memory(icons::LOGO))
        .width(size)
        .height(size)
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
        border: Border::default().rounded(24),
        ..container::Style::default()
    }
}

fn banner(theme: &Theme) -> container::Style {
    let p = palette(theme);
    container::Style {
        text_color: Some(p.on_hero),
        background: Some(p.hero.into()),
        border: Border::default().rounded(28),
        ..container::Style::default()
    }
}

/// One row of a grouped list (Material 3 Expressive): the ends are rounder than the joints.
fn grouped(index: usize, count: usize) -> impl Fn(&Theme) -> container::Style {
    let (outer, inner) = (22.0, 6.0);
    let top = if index == 0 { outer } else { inner };
    let bottom = if index + 1 == count { outer } else { inner };
    move |theme| container::Style {
        background: Some(palette(theme).surface.into()),
        border: Border {
            radius: border::Radius {
                top_left: top,
                top_right: top,
                bottom_right: bottom,
                bottom_left: bottom,
            },
            ..Border::default()
        },
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
        text_color: p.on_primary,
        border: Border::default().rounded(999),
        ..button::Style::default()
    }
}

/// Tonal button (soft lagoon), as on the board.
fn secondary(theme: &Theme, status: button::Status) -> button::Style {
    let p = palette(theme);
    let background = match status {
        button::Status::Active | button::Status::Disabled => p.accent_soft,
        button::Status::Hovered | button::Status::Pressed => p.accent_pressed_soft,
    };
    button::Style {
        background: Some(Background::Color(background)),
        text_color: p.on_accent_soft,
        border: Border::default().rounded(999),
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
        border: Border::default().rounded(999),
        ..button::Style::default()
    }
}

/// Full-width 48 px pill button: icon and label centered.
fn wide_button<'a, M: Clone + 'a>(
    glyph: &'static [u8],
    label: &'a str,
    glyph_color: Pick,
    style: fn(&Theme, button::Status) -> button::Style,
    on_press: M,
) -> Element<'a, M> {
    let content = row![
        icon(glyph, 18.0, glyph_color),
        text(label).size(15).font(SEMIBOLD)
    ]
    .spacing(8)
    .align_y(Alignment::Center);
    button(container(content).center(Length::Fill))
        .width(Length::Fill)
        .height(48)
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
        .padding(24)
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
        logo(44.0),
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
                container(logo(40.0))
                    .padding(4)
                    .style(filled(|_| Color::WHITE, 13.0))
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
            dot(8.0, |p| p.primary, false),
            text("En attente du scan…")
                .size(13)
                .font(MEDIUM)
                .style(tint(|p| p.on_accent_soft)),
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
        wide_button(glyph, label, |p| p.on_accent_soft, secondary, on_copy),
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
        let mut list = column![].spacing(3);
        let count = rows.len();
        for (index, device) in rows.into_iter().enumerate() {
            let forget = on_forget(device.id.clone());
            list = list.push(container(device_row(device, forget)).style(grouped(index, count)));
        }
        column![
            container(
                text("Téléphones associés")
                    .size(13)
                    .font(SEMIBOLD)
                    .style(tint(|p| p.muted))
            )
            .padding([0, 8]),
            scrollable(list),
            space().height(Length::Fill),
        ]
        .spacing(8)
        .height(Length::Fill)
        .into()
    };

    let mut bottom = column![
        wide_button(
            icons::PLUS,
            "Associer un téléphone",
            |p| p.on_primary,
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

    window_page(column![header_view(header), body, bottom].spacing(18))
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
    let pill = container(
        row![
            dot(8.0, |p| p.success, false),
            text("Synchronisation active")
                .size(12)
                .font(SEMIBOLD)
                .style(tint(|p| p.success_text)),
        ]
        .spacing(6)
        .align_y(Alignment::Center),
    )
    .padding([5, 12])
    .style(filled(|p| p.hero_device, 999.0));
    let mut title = column![
        text(format!("Connecté à {names}"))
            .size(21)
            .font(BOLD)
            .style(tint(|p| p.on_hero))
    ]
    .spacing(3);
    if let Some(activity) = activity {
        title = title.push(
            row![
                icon(icons::RECEIVED, 15.0, |p| p.on_hero),
                text(activity).size(13).style(tint(|p| p.on_hero)),
            ]
            .spacing(6)
            .align_y(Alignment::Center),
        );
    }
    container(column![container(duo()).center_x(Length::Fill), pill, title].spacing(10))
        .padding([16, 20])
        .width(Length::Fill)
        .style(banner)
        .into()
}

/// The phone and the computer smiling at each other (board illustration, 318 × 110 frame).
fn duo<'a, M: 'a>() -> Element<'a, M> {
    canvas::Canvas::new(Duo).width(260).height(90).into()
}

struct Duo;

impl<M> canvas::Program<M> for Duo {
    type State = ();

    fn draw(
        &self,
        _state: &(),
        renderer: &Renderer,
        theme: &Theme,
        bounds: Rectangle,
        _cursor: mouse::Cursor,
    ) -> Vec<canvas::Geometry> {
        let p = palette(theme);
        let k = bounds.width / 318.0;
        let at = |x: f32, y: f32| Point::new(x * k, y * k);
        let rounded = |x: f32, y: f32, w: f32, h: f32, r: f32| {
            Path::rounded_rectangle(at(x, y), Size::new(w * k, h * k), (r * k).into())
        };
        let dot = |x: f32, y: f32, r: f32| Path::circle(at(x, y), r * k);
        let stroke = |color: Color, width: f32| Stroke {
            width: width * k,
            line_cap: LineCap::Round,
            ..Stroke::default().with_color(color)
        };
        let mut frame = canvas::Frame::new(renderer, bounds.size());

        let link = Path::new(|b| {
            b.move_to(at(100.0, 62.0));
            b.bezier_curve_to(at(134.0, 18.0), at(180.0, 18.0), at(208.0, 54.0));
        });
        frame.stroke(
            &link,
            Stroke {
                line_dash: LineDash {
                    segments: &[k, 10.0 * k],
                    offset: 0,
                },
                ..stroke(p.primary, 4.0)
            },
        );
        let heart = Path::new(|b| {
            b.move_to(at(154.0, 40.0));
            b.bezier_curve_to(at(147.0, 35.0), at(141.0, 31.0), at(141.0, 25.0));
            b.bezier_curve_to(at(141.0, 18.0), at(152.0, 16.0), at(154.0, 23.0));
            b.bezier_curve_to(at(156.0, 16.0), at(167.0, 18.0), at(167.0, 25.0));
            b.bezier_curve_to(at(167.0, 31.0), at(161.0, 35.0), at(154.0, 40.0));
            b.close();
        });
        frame.fill(&heart, p.sun);

        let cheek = p.sun.scale_alpha(0.7);
        // Phone.
        frame.fill(&rounded(44.0, 16.0, 54.0, 86.0, 15.0), p.hero_device);
        frame.fill(&rounded(62.0, 22.0, 18.0, 4.0, 2.0), p.line);
        frame.fill(&dot(62.0, 56.0, 3.6), p.ink);
        frame.fill(&dot(80.0, 56.0, 3.6), p.ink);
        let smile = Path::new(|b| {
            b.move_to(at(64.0, 67.0));
            b.quadratic_curve_to(at(71.0, 73.0), at(78.0, 67.0));
        });
        frame.stroke(&smile, stroke(p.ink, 2.8));
        frame.fill(&rounded(51.0, 62.4, 8.0, 5.2, 2.6), cheek);
        frame.fill(&rounded(83.0, 62.4, 8.0, 5.2, 2.6), cheek);
        // Computer.
        frame.fill(&rounded(196.0, 24.0, 94.0, 62.0, 13.0), p.hero_device);
        frame.fill(&rounded(184.0, 88.0, 118.0, 12.0, 6.0), p.hero_device);
        frame.fill(&dot(231.0, 50.0, 3.8), p.ink);
        frame.fill(&dot(255.0, 50.0, 3.8), p.ink);
        let smile = Path::new(|b| {
            b.move_to(at(233.0, 62.0));
            b.quadratic_curve_to(at(243.0, 70.0), at(253.0, 62.0));
        });
        frame.stroke(&smile, stroke(p.ink, 2.8));
        frame.fill(&rounded(215.6, 57.2, 8.8, 5.6, 2.8), cheek);
        frame.fill(&rounded(261.6, 57.2, 8.8, 5.6, 2.8), cheek);

        vec![frame.into_geometry()]
    }
}

fn device_row<'a, M: Clone + 'a>(device: DeviceRow, on_forget: M) -> Element<'a, M> {
    let (tile, glyph, status, marker): (Pick, Pick, Pick, _) = if device.connected {
        (
            |p| p.accent_soft,
            |p| p.on_accent_soft,
            |p| p.success_text,
            dot(7.0, |p| p.success, false),
        )
    } else {
        (
            |p| p.sun_soft,
            |p| p.on_sun_soft,
            |p| p.muted,
            dot(7.0, |p| p.offline, true),
        )
    };
    row![
        container(icon(icons::PHONE, 20.0, glyph))
            .center(40)
            .style(filled(tile, 14.0)),
        column![
            text(device.name).size(15).font(SEMIBOLD),
            row![marker, text(device.detail).size(12).style(tint(status))]
                .spacing(6)
                .align_y(Alignment::Center),
        ]
        .spacing(3)
        .width(Length::Fill),
        button(container(text("Oublier").size(13).font(SEMIBOLD)).center_y(Length::Fill))
            .height(36)
            .padding([0, 12])
            .style(quiet_danger)
            .on_press(on_forget),
    ]
    .spacing(12)
    .align_y(Alignment::Center)
    .padding([12, 14])
    .into()
}

fn empty_state<'a, M: 'a>() -> Element<'a, M> {
    container(
        column![
            container(icon(icons::PHONE, 40.0, |p| p.on_accent_soft))
                .center(88)
                .style(filled(|p| p.accent_soft, 30.0)),
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
                assert_eq!(primary(&theme, status).text_color, p.on_primary);
                assert_eq!(secondary(&theme, status).text_color, p.on_accent_soft);
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
            assert_eq!(card(&theme).background, Some(p.surface.into()));
            assert_eq!(banner(&theme).background, Some(p.hero.into()));
            let first = grouped(0, 2)(&theme).border.radius;
            let last = grouped(1, 2)(&theme).border.radius;
            assert!(first.top_left > first.bottom_left && last.bottom_left > last.top_left);
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
            icons::RECEIVED,
            icons::LOGO,
        ] {
            assert!(glyph.starts_with(b"<svg") && glyph.ends_with(b"</svg>"));
        }
    }
}
