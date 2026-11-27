import { ReviewsPage } from "@/components/ReviewsPage";
import { pageMetadata } from "../../../metadata";

export const metadata = pageMetadata("fr", "reviews");

export default function Page() {
  return <ReviewsPage lang="fr" />;
}
