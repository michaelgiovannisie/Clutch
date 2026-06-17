import { NavLink, Outlet } from 'react-router-dom';
import Logo from './Logo.jsx';

const links = [
  { to: '/', label: 'Home', end: true },
  { to: '/teams', label: 'Teams' },
  { to: '/matches', label: 'Matches' },
  { to: '/standings', label: 'Standings' },
  { to: '/venues', label: 'Venues' },
];

export default function Layout() {
  return (
    <div className="app-shell">
      <header className="app-header">
        <Logo size="md" withWordmark />
        <nav className="nav">
          {links.map((l) => (
            <NavLink
              key={l.to}
              to={l.to}
              end={l.end}
              className={({ isActive }) => 'nav-link' + (isActive ? ' is-active' : '')}
            >
              {l.label}
            </NavLink>
          ))}
        </nav>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
      <footer className="app-footer">World Cup Tracker — capstone build</footer>
    </div>
  );
}
