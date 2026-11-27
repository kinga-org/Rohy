import { ReleasesPage } from "@/components/ReleasesPage";
import { pageMetadata } from "../../../metadata";

export const metadata = pageMetadata("fr", "releases");

export default function Page() {
  return <ReleasesPage lang="fr" />;
}
