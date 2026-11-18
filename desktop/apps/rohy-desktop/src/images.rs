//! Pixel buffers for the QR code and the tray icon (RGBA, no platform code).

/// QR code of `text` as RGBA pixels with a 4-module quiet zone; returns (width, height, pixels).
pub fn qr_rgba(text: &str, scale: u32) -> Option<(u32, u32, Vec<u8>)> {
    // Level H tolerates the Rohy mark drawn over the center.
    let code =
        qrcode::QrCode::with_error_correction_level(text.as_bytes(), qrcode::EcLevel::H).ok()?;
    let modules = code.width() as u32;
    let colors = code.to_colors();
    let quiet = 4;
    let side = (modules + 2 * quiet) * scale;
    let mut pixels = Vec::with_capacity((side * side * 4) as usize);
    for y in 0..side {
        for x in 0..side {
            let (mx, my) = (
                (x / scale) as i64 - quiet as i64,
                (y / scale) as i64 - quiet as i64,
            );
            let dark = (0..modules as i64).contains(&mx)
                && (0..modules as i64).contains(&my)
                && colors[(my as u32 * modules + mx as u32) as usize] == qrcode::Color::Dark;
            let value = if dark { 0 } else { 255 };
            pixels.extend_from_slice(&[value, value, value, 255]);
        }
    }
    Some((side, side, pixels))
}

/// Tray icon: a white link (two rounded links and a bar) on a dark teal disc.
pub fn tray_icon_rgba(size: u32) -> Vec<u8> {
    let mut pixels = vec![0u8; (size * size * 4) as usize];
    let s = size as f32;
    let center = s / 2.0;
    let link = |x: f32, y: f32, cx: f32| {
        // Rounded rectangle outline, half-width 0.17·s, half-height 0.11·s, stroke 0.06·s.
        let (hw, hh, r, stroke) = (0.17 * s, 0.11 * s, 0.11 * s, 0.06 * s);
        let qx = ((x - cx).abs() - (hw - r)).max(0.0);
        let qy = ((y - center).abs() - (hh - r)).max(0.0);
        let distance = (qx * qx + qy * qy).sqrt() - r;
        distance.abs() <= stroke / 2.0
    };
    for y in 0..size {
        for x in 0..size {
            let (fx, fy) = (x as f32 + 0.5, y as f32 + 0.5);
            let inside_disc = (fx - center).powi(2) + (fy - center).powi(2) <= (s / 2.0).powi(2);
            let bar = (fy - center).abs() <= 0.03 * s && (fx - center).abs() <= 0.12 * s;
            let white = link(fx, fy, 0.34 * s) || link(fx, fy, 0.66 * s) || bar;
            let index = ((y * size + x) * 4) as usize;
            let color = match (inside_disc, white) {
                (false, _) => [0, 0, 0, 0],
                (true, true) => [255, 255, 255, 255],
                (true, false) => [27, 75, 90, 255],
            };
            pixels[index..index + 4].copy_from_slice(&color);
        }
    }
    pixels
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn qr_code_has_quiet_zone_and_dark_modules() {
        let (width, height, pixels) = qr_rgba("rohy://pair?v=1", 4).unwrap();
        assert_eq!(width, height);
        assert_eq!(pixels.len(), (width * height * 4) as usize);
        assert_eq!(&pixels[..4], &[255, 255, 255, 255]);
        assert!(pixels.chunks(4).any(|p| p[0] == 0));
        assert!(qr_rgba(&"x".repeat(10_000), 1).is_none());
    }

    #[test]
    fn tray_icon_is_transparent_outside_the_disc() {
        let pixels = tray_icon_rgba(32);
        assert_eq!(pixels.len(), 32 * 32 * 4);
        assert_eq!(pixels[3], 0);
        let center = ((16 * 32 + 16) * 4) as usize;
        assert_eq!(&pixels[center..center + 4], &[255, 255, 255, 255]);
        let teal = ((16 * 32 + 3) * 4) as usize;
        assert_eq!(&pixels[teal..teal + 4], &[27, 75, 90, 255]);
    }
}
