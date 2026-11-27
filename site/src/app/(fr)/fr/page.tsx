import { HomePage } from "@/components/HomePage";
import { pageMetadata } from "../../metadata";

export const metadata = pageMetadata("fr", "home");

export default function Page() {
  return <HomePage lang="fr" />;
}
