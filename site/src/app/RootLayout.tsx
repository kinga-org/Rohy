import type { ReactNode } from "react";
import type { Viewport } from "next";
import type { Lang } from "@/content/i18n";
import { inter } from "./fonts";
import "./globals.css";

export const viewport: Viewport = { themeColor: "#F4F9FA", width: "device-width", initialScale: 1 };

export function RootLayout({ lang, children }: { lang: Lang; children: ReactNode }) {
  return (
    <html lang={lang} className={inter.variable}>
      <body>{children}</body>
    </html>
  );
}
