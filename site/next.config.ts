import type { NextConfig } from "next";

// Static export: Cloudflare serves the files in out/ and the Worker in worker/ answers /api and /download.
const config: NextConfig = {
  output: "export",
  trailingSlash: true,
  images: { unoptimized: true },
  // Two root layouts (English at /, French at /fr), so the 404 page lives outside both.
  experimental: { globalNotFound: true },
};

export default config;
