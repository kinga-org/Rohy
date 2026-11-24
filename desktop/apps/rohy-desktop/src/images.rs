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

/// Signed distance from `(x, y)` to a rounded rectangle (`x0`, `y0`, width `w`, height `h`, radius `r`).
fn rounded_rect(x: f32, y: f32, (x0, y0, w, h, r): (f32, f32, f32, f32, f32)) -> f32 {
    let (cx, cy) = (x0 + w / 2.0, y0 + h / 2.0);
    let qx = (x - cx).abs() - (w / 2.0 - r);
    let qy = (y - cy).abs() - (h / 2.0 - r);
    let outside = (qx.max(0.0).powi(2) + qy.max(0.0).powi(2)).sqrt();
    outside + qx.max(qy).min(0.0) - r
}

const BLUEBERRY: [f32; 3] = [86.0, 83.0, 208.0];
const PEACH: [f32; 3] = [255.0, 159.0, 110.0];
const WHITE: [f32; 3] = [255.0, 255.0, 255.0];

/// Color of the Rohy logo at `(u, v)` in its 100 × 100 frame (the board's "Le presse-papiers"), or
/// `None` outside its rounded square.
fn logo_color(u: f32, v: f32) -> Option<[f32; 3]> {
    if rounded_rect(u, v, (0.0, 0.0, 100.0, 100.0, 28.0)) > 0.0 {
        return None;
    }
    let eye = |cx: f32| (u - cx).powi(2) + (v - 51.0).powi(2) <= 3.2f32.powi(2);
    // Smile: the curve "M43 61 q7 6 14 0" is the parabola v = 61 + 3·(1 − ((u − 50) / 7)²), 2.8 wide.
    let smile = (43.0..=57.0).contains(&u) && {
        let t = (u - 50.0) / 7.0;
        (v - (61.0 + 3.0 * (1.0 - t * t))).abs() <= 1.4
    };
    let cheek = |cx: f32| ((u - cx) / 3.4).powi(2) + ((v - 59.0) / 2.2).powi(2) <= 1.0;
    Some(
        if rounded_rect(u, v, (39.0, 20.0, 22.0, 13.0, 6.5)) <= 0.0 {
            PEACH
        } else if rounded_rect(u, v, (27.0, 27.0, 46.0, 54.0, 14.0)) <= 0.0 {
            if eye(42.0) || eye(58.0) || smile {
                BLUEBERRY
            } else if cheek(35.0) || cheek(65.0) {
                PEACH
            } else {
                WHITE
            }
        } else {
            BLUEBERRY
        },
    )
}

/// Tray and window icon: the Rohy logo (smiling clipboard on blueberry), 4 × 4 supersampled.
pub fn tray_icon_rgba(size: u32) -> Vec<u8> {
    const SAMPLES: u32 = 4;
    let mut pixels = vec![0u8; (size * size * 4) as usize];
    let step = 100.0 / (size * SAMPLES) as f32;
    for y in 0..size {
        for x in 0..size {
            let (mut sum, mut covered) = ([0.0f32; 3], 0u32);
            for sy in 0..SAMPLES {
                for sx in 0..SAMPLES {
                    let u = ((x * SAMPLES + sx) as f32 + 0.5) * step;
                    let v = ((y * SAMPLES + sy) as f32 + 0.5) * step;
                    if let Some(color) = logo_color(u, v) {
                        for (total, channel) in sum.iter_mut().zip(color) {
                            *total += channel;
                        }
                        covered += 1;
                    }
                }
            }
            let index = ((y * size + x) * 4) as usize;
            if covered > 0 {
                for (channel, total) in sum.iter().enumerate() {
                    pixels[index + channel] = (total / covered as f32).round() as u8;
                }
                pixels[index + 3] = (255 * covered / (SAMPLES * SAMPLES)) as u8;
            }
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
    fn tray_icon_is_the_logo() {
        let pixels = tray_icon_rgba(100);
        assert_eq!(pixels.len(), 100 * 100 * 4);
        let at = |x: usize, y: usize| &pixels[(y * 100 + x) * 4..(y * 100 + x) * 4 + 4];
        // Rounded corner: transparent; side: blueberry; clipboard: white; clip: peach; eye: blueberry.
        assert_eq!(at(0, 0)[3], 0);
        assert_eq!(at(5, 50), &[86, 83, 208, 255]);
        assert_eq!(at(50, 40), &[255, 255, 255, 255]);
        assert_eq!(at(50, 25), &[255, 159, 110, 255]);
        assert_eq!(at(42, 51), &[86, 83, 208, 255]);
        assert_eq!(at(50, 64), &[86, 83, 208, 255]);
    }
}
