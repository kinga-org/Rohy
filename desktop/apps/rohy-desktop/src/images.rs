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

/// Points along the rope "M27 67 C27 36, 50 32, 50 50 C50 68, 73 64, 73 33" (two cubic curves).
fn rope_points() -> Vec<(f32, f32)> {
    let cubic = |p: [(f32, f32); 4], t: f32| {
        let m = 1.0 - t;
        let (a, b, c, d) = (m * m * m, 3.0 * m * m * t, 3.0 * m * t * t, t * t * t);
        (
            a * p[0].0 + b * p[1].0 + c * p[2].0 + d * p[3].0,
            a * p[0].1 + b * p[1].1 + c * p[2].1 + d * p[3].1,
        )
    };
    let halves = [
        [(27.0, 67.0), (27.0, 36.0), (50.0, 32.0), (50.0, 50.0)],
        [(50.0, 50.0), (50.0, 68.0), (73.0, 64.0), (73.0, 33.0)],
    ];
    halves
        .iter()
        .flat_map(|half| (0..=64).map(move |i| cubic(*half, i as f32 / 64.0)))
        .collect()
}

/// Color of the Rohy logo "La corde" at `(u, v)` in its 100 × 100 frame, or `None` outside its rounded
/// square. `rope` holds points along the rope's center line.
fn logo_color(u: f32, v: f32, rope: &[(f32, f32)]) -> Option<[f32; 3]> {
    if rounded_rect(u, v, (0.0, 0.0, 100.0, 100.0, 28.0)) > 0.0 {
        return None;
    }
    let end = |cx: f32, cy: f32| (u - cx).powi(2) + (v - cy).powi(2) <= 81.0;
    let on_rope = rope
        .iter()
        .any(|&(x, y)| (u - x).powi(2) + (v - y).powi(2) <= 25.0);
    Some(if end(27.0, 69.0) || end(73.0, 31.0) {
        PEACH
    } else if on_rope {
        WHITE
    } else {
        BLUEBERRY
    })
}

/// Tray and window icon: the Rohy logo (rope on blueberry), 4 × 4 supersampled.
pub fn tray_icon_rgba(size: u32) -> Vec<u8> {
    const SAMPLES: u32 = 4;
    let rope = rope_points();
    let mut pixels = vec![0u8; (size * size * 4) as usize];
    let step = 100.0 / (size * SAMPLES) as f32;
    for y in 0..size {
        for x in 0..size {
            let (mut sum, mut covered) = ([0.0f32; 3], 0u32);
            for sy in 0..SAMPLES {
                for sx in 0..SAMPLES {
                    let u = ((x * SAMPLES + sx) as f32 + 0.5) * step;
                    let v = ((y * SAMPLES + sy) as f32 + 0.5) * step;
                    if let Some(color) = logo_color(u, v, &rope) {
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
        // Rounded corner: transparent; side: blueberry; rope middle: white; rope ends: peach.
        assert_eq!(at(0, 0)[3], 0);
        assert_eq!(at(5, 50), &[86, 83, 208, 255]);
        assert_eq!(at(50, 50), &[255, 255, 255, 255]);
        assert_eq!(at(27, 69), &[255, 159, 110, 255]);
        assert_eq!(at(73, 31), &[255, 159, 110, 255]);
        assert_eq!(at(70, 75), &[86, 83, 208, 255]);
    }
}
