/** Small line icons and the "rope" logo, drawn inline so the site needs no image files. */

type IconProps = { size?: number };

const line = { fill: "none", stroke: "currentColor", strokeWidth: 2, strokeLinecap: "round", strokeLinejoin: "round" } as const;

export function Logo({ size = 36 }: IconProps) {
  return (
    <svg width={size} height={size} viewBox="0 0 100 100" aria-hidden="true">
      <rect width="100" height="100" rx="28" fill="#1B6E85" />
      <path d="M27 67 C 27 36, 50 32, 50 50 S 73 64, 73 33" fill="none" stroke="#FFFFFF" strokeWidth="10" strokeLinecap="round" />
      <circle cx="27" cy="69" r="9" fill="#F5B71E" />
      <circle cx="73" cy="31" r="9" fill="#F5B71E" />
    </svg>
  );
}

export function DownloadIcon({ size = 20 }: IconProps) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" aria-hidden="true" {...line}>
      <path d="M12 3v12" />
      <polyline points="7 10 12 15 17 10" />
      <path d="M5 21h14" />
    </svg>
  );
}

export function MenuIcon({ open }: { open: boolean }) {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" aria-hidden="true" {...line}>
      {open ? (
        <>
          <line x1="6" y1="6" x2="18" y2="18" />
          <line x1="18" y1="6" x2="6" y2="18" />
        </>
      ) : (
        <>
          <line x1="4" y1="7" x2="20" y2="7" />
          <line x1="4" y1="12" x2="20" y2="12" />
          <line x1="4" y1="17" x2="20" y2="17" />
        </>
      )}
    </svg>
  );
}

export function PlatformIcon({ platform }: { platform: "android" | "macos" | "windows" | "linux" }) {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" aria-hidden="true" {...line}>
      {platform === "android" && (
        <>
          <rect x="6" y="2" width="12" height="20" rx="2.5" />
          <line x1="11" y1="18" x2="13" y2="18" />
        </>
      )}
      {platform === "macos" && (
        <>
          <rect x="3" y="4" width="18" height="12" rx="2" />
          <line x1="2" y1="20" x2="22" y2="20" />
        </>
      )}
      {platform === "windows" && (
        <>
          <rect x="2" y="3" width="20" height="14" rx="2" />
          <line x1="8" y1="21" x2="16" y2="21" />
          <line x1="12" y1="17" x2="12" y2="21" />
        </>
      )}
      {platform === "linux" && (
        <>
          <polyline points="4 17 10 11 4 5" />
          <line x1="12" y1="19" x2="20" y2="19" />
        </>
      )}
    </svg>
  );
}

const STAR = "M12 2.8l2.8 5.9 6.4.8-4.7 4.4 1.2 6.4L12 17.2l-5.7 3.1 1.2-6.4-4.7-4.4 6.4-.8z";

export function Star({ on, size = 18, outlined = false }: { on: boolean; size?: number; outlined?: boolean }) {
  if (outlined) {
    return (
      <svg width={size} height={size} viewBox="0 0 24 24" aria-hidden="true">
        <path d={STAR} fill={on ? "#F5B71E" : "none"} stroke={on ? "#C98F00" : "#9DB3B9"} strokeWidth="1.6" strokeLinejoin="round" />
      </svg>
    );
  }
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" aria-hidden="true">
      <path d={STAR} fill={on ? "#F5B71E" : "#DCE7EA"} />
    </svg>
  );
}

export function CheckIcon() {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" aria-hidden="true" style={{ flex: "none" }} fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="5 12.5 10 17.5 19 7" />
    </svg>
  );
}

/** The phone and the computer smiling at each other (hero picture). */
export function Duo({ label }: { label: string }) {
  return (
    <svg viewBox="0 0 318 110" role="img" aria-label={label}>
      <path d="M100 62 C 134 18, 180 18, 208 54" fill="none" stroke="#1B6E85" strokeWidth="4" strokeLinecap="round" strokeDasharray="1 10" />
      <path d="M154 40 c -7 -5 -13 -9 -13 -15 a 6.5 6.5 0 0 1 13 -2 a 6.5 6.5 0 0 1 13 2 c 0 6 -6 10 -13 15 z" fill="#F5B71E" />
      <rect x="44" y="16" width="54" height="86" rx="15" fill="#FFFFFF" />
      <rect x="62" y="22" width="18" height="4" rx="2" fill="#E0ECEF" />
      <circle cx="62" cy="56" r="3.6" fill="#0E2A33" />
      <circle cx="80" cy="56" r="3.6" fill="#0E2A33" />
      <path d="M64 67 q7 6 14 0" fill="none" stroke="#0E2A33" strokeWidth="2.8" strokeLinecap="round" />
      <ellipse cx="55" cy="65" rx="4" ry="2.6" fill="#F5B71E" opacity="0.7" />
      <ellipse cx="87" cy="65" rx="4" ry="2.6" fill="#F5B71E" opacity="0.7" />
      <rect x="196" y="24" width="94" height="62" rx="13" fill="#FFFFFF" />
      <rect x="184" y="88" width="118" height="12" rx="6" fill="#FFFFFF" />
      <circle cx="231" cy="50" r="3.8" fill="#0E2A33" />
      <circle cx="255" cy="50" r="3.8" fill="#0E2A33" />
      <path d="M233 62 q10 8 20 0" fill="none" stroke="#0E2A33" strokeWidth="2.8" strokeLinecap="round" />
      <ellipse cx="220" cy="60" rx="4.4" ry="2.8" fill="#F5B71E" opacity="0.7" />
      <ellipse cx="266" cy="60" rx="4.4" ry="2.8" fill="#F5B71E" opacity="0.7" />
    </svg>
  );
}
