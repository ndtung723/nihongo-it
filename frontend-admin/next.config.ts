import type { NextConfig } from 'next'

const nextConfig: NextConfig = {
  // Standalone output: bundles only the required node_modules into .next/standalone
  // for a minimal Docker image. server.js is emitted alongside.
  output: 'standalone',
}

export default nextConfig
