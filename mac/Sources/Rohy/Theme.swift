import SwiftUI

/// Rohy's mark, the one brand element of the native Mac app (center of the pairing QR code): two white links
/// on the Rohy teal, same color as the Android and Windows/Linux apps.
struct RohyMark: View {
    static let brand = Color(red: 0x1B / 255, green: 0x4B / 255, blue: 0x5A / 255)

    let size: CGFloat

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: size * 0.24).fill(Self.brand)
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
