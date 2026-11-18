import AppKit
import SwiftUI

/// Colors of the design board "Rohy bureau — refonte"; same values as `desktop/apps/rohy-desktop/src/ui.rs`.
struct Palette {
    let ground, surface, line, divider, controlLine, ink, muted: Color
    let logo, primary, primaryHover, accentText, accentSoft, accentPressedSoft, neutralSoft: Color
    let success, successText, offline, danger, dangerSoft: Color

    static let brand = Color(hex: 0x1B4B5A)
    static let onBrandLabel = Color(hex: 0xCFE3E8)
    static let onBrandBody = Color(hex: 0xE3EEF1)

    static let light = Palette(
        ground: Color(hex: 0xF4F7F7), surface: .white, line: Color(hex: 0xDCE4E6), divider: Color(hex: 0xE6ECEE),
        controlLine: Color(hex: 0xC5D2D5), ink: Color(hex: 0x12262C), muted: Color(hex: 0x4F6369),
        logo: brand, primary: brand, primaryHover: Color(hex: 0x123540), accentText: brand,
        accentSoft: Color(hex: 0xE8F0F2), accentPressedSoft: Color(hex: 0xDAE7EA), neutralSoft: Color(hex: 0xEEF1F2),
        success: Color(hex: 0x1E8A55), successText: Color(hex: 0x1E6B45), offline: Color(hex: 0x7A8C91),
        danger: Color(hex: 0xA1352B), dangerSoft: Color(hex: 0xFBEDEB)
    )

    static let dark = Palette(
        ground: Color(hex: 0x0F1A1D), surface: Color(hex: 0x172429), line: Color(hex: 0x26363B), divider: Color(hex: 0x223136),
        controlLine: Color(hex: 0x34474D), ink: Color(hex: 0xE6EEF0), muted: Color(hex: 0x9AAEB3),
        logo: Color(hex: 0x2A6B7E), primary: Color(hex: 0x2A6B7E), primaryHover: Color(hex: 0x33809A),
        accentText: Color(hex: 0x8CC7D6), accentSoft: Color(hex: 0x1E3A42), accentPressedSoft: Color(hex: 0x24464F),
        neutralSoft: Color(hex: 0x213035), success: Color(hex: 0x3CB97A), successText: Color(hex: 0x6FD39F),
        offline: Color(hex: 0x6F858B), danger: Color(hex: 0xF08A7E), dangerSoft: Color(hex: 0x3A2220)
    )

    static func of(_ scheme: ColorScheme) -> Palette { scheme == .dark ? dark : light }
}

extension Color {
    init(hex: UInt32) {
        self.init(red: Double((hex >> 16) & 0xFF) / 255, green: Double((hex >> 8) & 0xFF) / 255, blue: Double(hex & 0xFF) / 255)
    }
}

/// Inter, bundled in Rohy.app (ATSApplicationFontsPath); falls back to the system font under `swift run`.
enum Typeface {
    static func inter(_ size: CGFloat, _ weight: Font.Weight = .regular) -> Font {
        Font.custom("Inter", size: size).weight(weight)
    }
}

/// Appearance chosen in the panel, kept between launches (same choices as the Windows/Linux app).
enum Appearance: String, CaseIterable, Identifiable {
    case system, light, dark

    var id: String { rawValue }

    var label: String {
        switch self {
        case .system: return "Système"
        case .light: return "Clair"
        case .dark: return "Sombre"
        }
    }

    var nsAppearance: NSAppearance? {
        switch self {
        case .system: return nil
        case .light: return NSAppearance(named: .aqua)
        case .dark: return NSAppearance(named: .darkAqua)
        }
    }
}

// Buttons.

struct PrimaryButtonStyle: ButtonStyle {
    let palette: Palette
    @State private var hovered = false

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(Typeface.inter(14, .semibold))
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity, minHeight: 44)
            .background(RoundedRectangle(cornerRadius: 10).fill(hovered || configuration.isPressed ? palette.primaryHover : palette.primary))
            .contentShape(Rectangle())
            .onHover { hovered = $0 }
    }
}

struct SecondaryButtonStyle: ButtonStyle {
    let palette: Palette
    @State private var hovered = false

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(Typeface.inter(14, .semibold))
            .foregroundStyle(palette.ink)
            .frame(maxWidth: .infinity, minHeight: 44)
            .background(
                RoundedRectangle(cornerRadius: 10)
                    .fill(configuration.isPressed ? palette.accentPressedSoft : hovered ? palette.accentSoft : palette.surface)
            )
            .overlay(RoundedRectangle(cornerRadius: 10).stroke(palette.controlLine, lineWidth: 1))
            .contentShape(Rectangle())
            .onHover { hovered = $0 }
    }
}

struct QuietButtonStyle: ButtonStyle {
    let color: Color
    let hoverFill: Color
    @State private var hovered = false

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(Typeface.inter(13, .medium))
            .foregroundStyle(color)
            .padding(.horizontal, 12)
            .frame(minHeight: 32)
            .background(RoundedRectangle(cornerRadius: 8).fill(hovered || configuration.isPressed ? hoverFill : .clear))
            .contentShape(Rectangle())
            .onHover { hovered = $0 }
    }
}

// Shapes.

/// Rohy mark: two white links on a disc.
struct RohyMark: View {
    let size: CGFloat
    let disc: Color

    var body: some View {
        ZStack {
            Circle().fill(disc)
            HStack(spacing: -size * 0.06) {
                ForEach(0..<2, id: \.self) { _ in
                    RoundedRectangle(cornerRadius: size * 0.11)
                        .stroke(.white, lineWidth: size * 0.06)
                        .frame(width: size * 0.34, height: size * 0.22)
                }
            }
        }
        .frame(width: size, height: size)
        .accessibilityHidden(true)
    }
}

struct Dot: View {
    let color: Color
    var size: CGFloat = 8
    var hollow = false

    var body: some View {
        Group {
            if hollow { Circle().stroke(color, lineWidth: 2) } else { Circle().fill(color) }
        }
        .frame(width: size, height: size)
        .accessibilityHidden(true)
    }
}

struct CardBackground: ViewModifier {
    let palette: Palette
    var radius: CGFloat = 16

    func body(content: Content) -> some View {
        content
            .background(RoundedRectangle(cornerRadius: radius).fill(palette.surface))
            .overlay(RoundedRectangle(cornerRadius: radius).stroke(palette.line, lineWidth: 1))
    }
}

extension View {
    func card(_ palette: Palette, radius: CGFloat = 16) -> some View { modifier(CardBackground(palette: palette, radius: radius)) }
}
