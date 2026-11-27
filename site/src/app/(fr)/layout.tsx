import type { ReactNode } from "react";
import { RootLayout } from "../RootLayout";

export { viewport } from "../RootLayout";

export default function Layout({ children }: { children: ReactNode }) {
  return <RootLayout lang="fr">{children}</RootLayout>;
}
