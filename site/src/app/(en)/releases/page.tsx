import { ReleasesPage } from "@/components/ReleasesPage";
import { pageMetadata } from "../../metadata";

export const metadata = pageMetadata("en", "releases");

export default function Page() {
  return <ReleasesPage lang="en" />;
}
