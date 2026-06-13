import { useNavigate } from 'react-router-dom'
import { clearSession, getEmail } from '../api/auth'

const sports = [
  { emoji: '⚽', name: 'Football' },
  { emoji: '🏀', name: 'Basketball' },
  { emoji: '🎾', name: 'Tennis' },
  { emoji: '🏊', name: 'Swimming' },
  { emoji: '🏋️', name: 'Fitness' },
  { emoji: '🚴', name: 'Cycling' },
]

export default function DashboardPage() {
  const navigate = useNavigate()
  const email = getEmail() ?? 'Athlete'

  const handleLogout = () => {
    clearSession()
    navigate('/login')
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-slate-800 to-gray-900">
      {/* Navbar */}
      <nav className="border-b border-white/10 backdrop-blur-xl bg-white/5 sticky top-0 z-50">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center gap-3">
              <span className="text-2xl">⚽</span>
              <span className="text-white font-extrabold text-xl tracking-tight">SportsApp</span>
            </div>
            <div className="flex items-center gap-4">
              <span className="text-white/50 text-sm hidden sm:block">{email}</span>
              <button
                onClick={handleLogout}
                className="px-4 py-2 rounded-xl bg-white/10 hover:bg-white/20 text-white text-sm font-medium border border-white/10 transition-all duration-200"
              >
                Sign out
              </button>
            </div>
          </div>
        </div>
      </nav>

      <main className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        {/* Hero */}
        <div className="text-center mb-12 animate-fade-in">
          <p className="text-brand-400 font-semibold text-sm uppercase tracking-widest mb-2">Welcome back</p>
          <h1 className="text-4xl sm:text-5xl font-extrabold text-white mb-3">
            Ready to Play? 🏆
          </h1>
          <p className="text-white/50 text-lg max-w-md mx-auto">
            Track your performance, discover sports, and connect with athletes worldwide.
          </p>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-10 animate-slide-up">
          {[
            { label: 'Sessions', value: '24', icon: '📅' },
            { label: 'Calories', value: '12.4k', icon: '🔥' },
            { label: 'Streak', value: '7 days', icon: '⚡' },
            { label: 'Rank', value: '#142', icon: '🏅' },
          ].map((stat) => (
            <div
              key={stat.label}
              className="bg-white/5 backdrop-blur-sm border border-white/10 rounded-2xl p-4 text-center hover:bg-white/10 transition-all duration-200"
            >
              <div className="text-3xl mb-2">{stat.icon}</div>
              <div className="text-2xl font-bold text-white">{stat.value}</div>
              <div className="text-white/40 text-sm">{stat.label}</div>
            </div>
          ))}
        </div>

        {/* Sports Grid */}
        <h2 className="text-xl font-bold text-white mb-4">Explore Sports</h2>
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-4">
          {sports.map((sport) => (
            <button
              key={sport.name}
              className="group bg-white/5 backdrop-blur-sm border border-white/10 rounded-2xl p-6 text-center hover:bg-brand-500/20 hover:border-brand-500/40 transition-all duration-200 active:scale-95"
            >
              <div className="text-4xl mb-3 group-hover:scale-110 transition-transform duration-200">
                {sport.emoji}
              </div>
              <div className="text-white/70 group-hover:text-white text-sm font-medium transition-colors">
                {sport.name}
              </div>
            </button>
          ))}
        </div>
      </main>
    </div>
  )
}

