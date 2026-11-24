// Draws the macOS app icon (Resources/Assets.xcassets/AppIcon.appiconset) from the Rohy logo
// "La corde": lagoon rounded square on the standard macOS icon grid, white rope with sun-yellow ends.
// Run from mac/: swift scripts/make-icon.swift
import AppKit

let folder = URL(fileURLWithPath: "Resources/Assets.xcassets/AppIcon.appiconset")

func rounded(_ rect: CGRect, _ radius: CGFloat) -> CGPath {
    CGPath(roundedRect: rect, cornerWidth: radius, cornerHeight: radius, transform: nil)
}

func color(_ rgb: UInt32) -> CGColor {
    CGColor(srgbRed: CGFloat((rgb >> 16) & 0xFF) / 255, green: CGFloat((rgb >> 8) & 0xFF) / 255,
            blue: CGFloat(rgb & 0xFF) / 255, alpha: 1)
}

func draw(pixels: Int) -> Data {
    let space = CGColorSpace(name: CGColorSpace.sRGB)!
    let context = CGContext(data: nil, width: pixels, height: pixels, bitsPerComponent: 8, bytesPerRow: 0,
                            space: space, bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
    let k = CGFloat(pixels) / 1024
    // 1024 grid, y down like the design board.
    context.translateBy(x: 0, y: CGFloat(pixels))
    context.scaleBy(x: k, y: -k)

    // Rounded square 824 × 824 at 100, with the soft shadow of macOS icons.
    let tile = rounded(CGRect(x: 100, y: 100, width: 824, height: 824), 185)
    context.saveGState()
    context.setShadow(offset: CGSize(width: 0, height: -10), blur: 24, color: CGColor(gray: 0, alpha: 0.28))
    context.addPath(tile)
    context.setFillColor(color(0x1B6E85))
    context.fillPath()
    context.restoreGState()
    context.saveGState()
    context.addPath(tile)
    context.clip()
    let gradient = CGGradient(colorsSpace: space, colors: [color(0x2683A0), color(0x155C70)] as CFArray,
                              locations: [0, 1])!
    context.drawLinearGradient(gradient, start: CGPoint(x: 512, y: 100), end: CGPoint(x: 512, y: 924), options: [])
    context.restoreGState()

    // Rope: the board's 100-unit logo, scaled 6 and centered.
    context.translateBy(x: 212, y: 212)
    context.scaleBy(x: 6, y: 6)
    context.setStrokeColor(.white)
    context.setLineWidth(10)
    context.setLineCap(.round)
    context.move(to: CGPoint(x: 27, y: 67))
    context.addCurve(to: CGPoint(x: 50, y: 50), control1: CGPoint(x: 27, y: 36), control2: CGPoint(x: 50, y: 32))
    context.addCurve(to: CGPoint(x: 73, y: 33), control1: CGPoint(x: 50, y: 68), control2: CGPoint(x: 73, y: 64))
    context.strokePath()
    context.setFillColor(color(0xF5B71E))
    context.addEllipse(in: CGRect(x: 18, y: 60, width: 18, height: 18))
    context.addEllipse(in: CGRect(x: 64, y: 22, width: 18, height: 18))
    context.fillPath()

    let rep = NSBitmapImageRep(cgImage: context.makeImage()!)
    return rep.representation(using: .png, properties: [:])!
}

var images: [[String: String]] = []
for points in [16, 32, 128, 256, 512] {
    for scale in [1, 2] {
        let name = "icon_\(points)x\(points)\(scale == 2 ? "@2x" : "").png"
        try! draw(pixels: points * scale).write(to: folder.appendingPathComponent(name))
        images.append(["idiom": "mac", "size": "\(points)x\(points)", "scale": "\(scale)x", "filename": name])
    }
}
let contents: [String: Any] = ["images": images, "info": ["author": "xcode", "version": 1]]
let json = try! JSONSerialization.data(withJSONObject: contents, options: [.prettyPrinted, .sortedKeys])
try! json.write(to: folder.appendingPathComponent("Contents.json"))
print("Wrote \(images.count) icons")
