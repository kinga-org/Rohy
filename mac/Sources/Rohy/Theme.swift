import AppKit
import SwiftUI

/// Geometry of the Rohy logo "La corde" (a soft rope linking two points), in the 100 × 100 frame of the
/// design boards, y down. Shared by the QR code mark and the menu bar symbol so both match the other apps.
enum LogoGeometry {
    static let rope: CGPath = {
        let path = CGMutablePath()
        path.move(to: CGPoint(x: 27, y: 67))
        path.addCurve(to: CGPoint(x: 50, y: 50), control1: CGPoint(x: 27, y: 36), control2: CGPoint(x: 50, y: 32))
        path.addCurve(to: CGPoint(x: 73, y: 33), control1: CGPoint(x: 50, y: 68), control2: CGPoint(x: 73, y: 64))
        return path
    }()

    static let ropeWidth: CGFloat = 10

    static let ends: CGPath = {
        let path = CGMutablePath()
        path.addEllipse(in: CGRect(x: 18, y: 60, width: 18, height: 18))
        path.addEllipse(in: CGRect(x: 64, y: 22, width: 18, height: 18))
        return path
    }()

    /// Bounds of the rope and its two ends in the 100 × 100 frame.
    static let bounds = CGRect(x: 18, y: 22, width: 64, height: 56)
}

/// Rohy's mark, the one brand element of the native Mac app (center of the pairing QR code): the rope on the
/// Rohy lagoon, same as the Android and Windows/Linux apps.
struct RohyMark: View {
    static let brand = Color(red: 0x1B / 255, green: 0x6E / 255, blue: 0x85 / 255)
    static let sun = Color(red: 0xF5 / 255, green: 0xB7 / 255, blue: 0x1E / 255)

    let size: CGFloat

    var body: some View {
        Canvas { context, canvas in
            context.scaleBy(x: canvas.width / 100, y: canvas.height / 100)
            context.fill(Path(roundedRect: CGRect(x: 0, y: 0, width: 100, height: 100), cornerRadius: 28),
                         with: .color(Self.brand))
            context.stroke(Path(LogoGeometry.rope), with: .color(.white),
                           style: StrokeStyle(lineWidth: LogoGeometry.ropeWidth, lineCap: .round))
            context.fill(Path(LogoGeometry.ends), with: .color(Self.sun))
        }
        .frame(width: size, height: size)
        .accessibilityHidden(true)
    }
}

enum MenuBarGlyph {
    /// The rope as a template image: macOS paints it black or white like its own menu bar symbols, and dims
    /// it when the status item `appearsDisabled`.
    static func image(height: CGFloat = 15) -> NSImage {
        let bounds = LogoGeometry.bounds
        let scale = height / bounds.height
        let size = NSSize(width: (bounds.width * scale).rounded(.up), height: height)
        let image = NSImage(size: size, flipped: true) { rect in
            guard let context = NSGraphicsContext.current?.cgContext else { return false }
            context.translateBy(x: (rect.width - bounds.width * scale) / 2, y: 0)
            context.scaleBy(x: scale, y: scale)
            context.translateBy(x: -bounds.minX, y: -bounds.minY)
            context.setStrokeColor(NSColor.black.cgColor)
            context.setFillColor(NSColor.black.cgColor)
            context.setLineWidth(LogoGeometry.ropeWidth)
            context.setLineCap(.round)
            context.addPath(LogoGeometry.rope)
            context.strokePath()
            context.addPath(LogoGeometry.ends)
            context.fillPath()
            return true
        }
        image.isTemplate = true
        image.accessibilityDescription = "Rohy"
        return image
    }
}
