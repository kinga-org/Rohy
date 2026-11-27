import { HomePage } from "@/components/HomePage";
import { pageMetadata } from "../metadata";

export const metadata = pageMetadata("en", "home");

export default function Page() {
  return <HomePage lang="en" />;
}
