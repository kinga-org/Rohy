import { ReviewsPage } from "@/components/ReviewsPage";
import { pageMetadata } from "../../metadata";

export const metadata = pageMetadata("en", "reviews");

export default function Page() {
  return <ReviewsPage lang="en" />;
}
