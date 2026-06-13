import { ReactNode } from 'react'

interface Props {
  children: ReactNode
}

/** Full-page sports-themed gradient background */
export default function AuthLayout({ children }: Props) {
  return (
    <div className="min-h-screen flex items-center justify-center relative overflow-hidden bg-gradient-to-br from-gray-900 via-slate-800 to-gray-900">
      {/* Decorative blobs */}
      <div className="absolute -top-40 -left-40 w-96 h-96 bg-brand-600/30 rounded-full blur-3xl animate-pulse" />
      <div className="absolute -bottom-40 -right-40 w-96 h-96 bg-brand-500/20 rounded-full blur-3xl animate-pulse delay-1000" />
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-white/[0.02] rounded-full blur-3xl" />

      {/* Sports emoji watermark */}
      <div className="absolute inset-0 flex items-center justify-center pointer-events-none select-none opacity-[0.03] text-[20rem] leading-none">
        ⚽
      </div>

      <div className="relative z-10 w-full px-4 py-8">{children}</div>
    </div>
  )
}

