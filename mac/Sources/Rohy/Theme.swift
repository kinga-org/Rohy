import AppKit
import SwiftUI

/// Geometry of the Rohy logo "Le presse-papiers" (a smiling clipboard), in the 100 × 100 frame of the design
/// boards, y down. Shared by the QR code mark and the menu bar symbol so both match the Android app.
enum LogoGeometry {
    static let body = CGPath(roundedRect: CGRect(x: 27, y: 27, width: 46, height: 54),
                             cornerWidth: 14, cornerHeight: 14, transform: nil)
    static let clip = CGPath(roundedRect: CGRect(x: 39, y: 20, width: 22, height: 13),
                             cornerWidth: 6.5, cornerHeight: 6.5, transform: nil)

    static let eyes: CGPath = {
        let path = CGMutablePath()
        path.addEllipse(in: CGRect(x: 38.8, y: 47.8, width: 6.4, height: 6.4))
        path.addEllipse(in: CGRect(x: 54.8, y: 47.8, width: 6.4, height: 6.4))
        return path
    }()

    static let smile: CGPath = {
        let path = CGMutablePath()
        path.move(to: CGPoint(x: 43, y: 61))
        path.addQuadCurve(to: CGPoint(x: 57, y: 61), control: CGPoint(x: 50, y: 67))
        return path
    }()

    static let cheeks: CGPath = {
        let path = CGMutablePath()
        path.addEllipse(in: CGRect(x: 31.6, y: 56.8, width: 6.8, height: 4.4))
        path.addEllipse(in: CGRect(x: 61.6, y: 56.8, width: 6.8, height: 4.4))
        return path
    }()

    /// Face cut out of the one-color silhouette (even-odd with the body): eyes and a crescent smile.
    static let faceHoles: CGPath = {
        let path = CGMutablePath()
        // Slightly bolder than the color logo so the face still reads at menu bar size.
        path.addEllipse(in: CGRect(x: 37.5, y: 46.5, width: 9, height: 9))
        path.addEllipse(in: CGRect(x: 53.5, y: 46.5, width: 9, height: 9))
        path.move(to: CGPoint(x: 41.5, y: 60))
        path.addQuadCurve(to: CGPoint(x: 58.5, y: 60), control: CGPoint(x: 50, y: 72))
        path.addQuadCurve(to: CGPoint(x: 41.5, y: 60), control: CGPoint(x: 50, y: 64))
        path.closeSubpath()
        return path
    }()

    /// Bounds of the clipboard (body and clip) in the 100 × 100 frame.
    static let clipboardBounds = CGRect(x: 27, y: 20, width: 46, height: 61)
}

/// Rohy's mark, the one brand element of the native Mac app (center of the pairing QR code): the smiling
/// clipboard on the Rohy blueberry, same as the Android and Windows/Linux apps.
struct RohyMark: View {
    static let brand = Color(red: 0x56 / 255, green: 0x53 / 255, blue: 0xD0 / 255)
    static let peach = Color(red: 0xFF / 255, green: 0x9F / 255, blue: 0x6E / 255)

    let size: CGFloat

    var body: some View {
        Canvas { context, canvas in
            context.scaleBy(x: canvas.width / 100, y: canvas.height / 100)
            context.fill(Path(roundedRect: CGRect(x: 0, y: 0, width: 100, height: 100), cornerRadius: 28),
                         with: .color(Self.brand))
            context.fill(Path(LogoGeometry.body), with: .color(.white))
            context.fill(Path(LogoGeometry.clip), with: .color(Self.peach))
            context.fill(Path(LogoGeometry.eyes), with: .color(Self.brand))
            context.stroke(Path(LogoGeometry.smile), with: .color(Self.brand),
                           style: StrokeStyle(lineWidth: 2.8, lineCap: .round))
            context.fill(Path(LogoGeometry.cheeks), with: .color(Self.peach))
        }
        .frame(width: size, height: size)
        .accessibilityHidden(true)
    }
}

enum MenuBarGlyph {
    /// The clipboard silhouette as a template image: macOS paints it black or white like its own menu bar
    /// symbols, and dims it when the status item `appearsDisabled`.
    static func image(height: CGFloat = 17) -> NSImage {
        let bounds = LogoGeometry.clipboardBounds
        let scale = height / bounds.height
        let size = NSSize(width: (bounds.width * scale).rounded(.up), height: height)
        let image = NSImage(size: size, flipped: true) { rect in
            guard let context = NSGraphicsContext.current?.cgContext else { return false }
            context.translateBy(x: (rect.width - bounds.width * scale) / 2, y: 0)
            context.scaleBy(x: scale, y: scale)
            context.translateBy(x: -bounds.minX, y: -bounds.minY)
            context.setFillColor(NSColor.black.cgColor)
            let silhouette = CGMutablePath()
            silhouette.addPath(LogoGeometry.body)
            silhouette.addPath(LogoGeometry.faceHoles)
            context.addPath(silhouette)
            context.fillPath(using: .evenOdd)
            context.addPath(LogoGeometry.clip)
            context.fillPath()
            return true
        }
        image.isTemplate = true
        image.accessibilityDescription = "Rohy"
        return image
    }
}
