// Draws the macOS app icon (Resources/Assets.xcassets/AppIcon.appiconset) from the Rohy logo
// "Le presse-papiers": blueberry rounded square on the standard macOS icon grid, smiling clipboard.
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
    context.setFillColor(color(0x5653D0))
    context.fillPath()
    context.restoreGState()
    context.saveGState()
    context.addPath(tile)
    context.clip()
    let gradient = CGGradient(colorsSpace: space, colors: [color(0x6865DE), color(0x4C49C2)] as CFArray,
                              locations: [0, 1])!
    context.drawLinearGradient(gradient, start: CGPoint(x: 512, y: 100), end: CGPoint(x: 512, y: 924), options: [])
    context.restoreGState()

    // Clipboard: the board's 100-unit logo, scaled 6 and centered.
    context.translateBy(x: 212, y: 212)
    context.scaleBy(x: 6, y: 6)
    context.setFillColor(.white)
    context.addPath(rounded(CGRect(x: 27, y: 27, width: 46, height: 54), 14))
    context.fillPath()
    context.setFillColor(color(0xFF9F6E))
    context.addPath(rounded(CGRect(x: 39, y: 20, width: 22, height: 13), 6.5))
    context.fillPath()
    context.addEllipse(in: CGRect(x: 31.6, y: 56.8, width: 6.8, height: 4.4))
    context.addEllipse(in: CGRect(x: 61.6, y: 56.8, width: 6.8, height: 4.4))
    context.fillPath()
    context.setFillColor(color(0x5653D0))
    context.addEllipse(in: CGRect(x: 38.8, y: 47.8, width: 6.4, height: 6.4))
    context.addEllipse(in: CGRect(x: 54.8, y: 47.8, width: 6.4, height: 6.4))
    context.fillPath()
    context.setStrokeColor(color(0x5653D0))
    context.setLineWidth(2.8)
    context.setLineCap(.round)
    context.move(to: CGPoint(x: 43, y: 61))
    context.addQuadCurve(to: CGPoint(x: 57, y: 61), control: CGPoint(x: 50, y: 67))
    context.strokePath()

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
