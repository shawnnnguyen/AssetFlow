import NavItem from './NavItem';
import PortfolioList from './PortfolioList';
import { TrendIcon, BarChartIcon, ReceiptIcon, LogOutIcon } from '../shared/Icons';
import { useAuth } from '../../context/AuthContext';

const NAV_ITEMS = [
  { id: 'dashboard',    label: 'Dashboard',    icon: TrendIcon },
  { id: 'market',       label: 'Market',       icon: BarChartIcon },
  { id: 'transactions', label: 'Transactions', icon: ReceiptIcon },
];

export default function Sidebar({ activeNav, setActiveNav, portfolios, currentPortfolioId, setCurrentPortfolioId, onNewPortfolio }) {
  const { username, logout } = useAuth();
  const initials = username ? username.slice(0, 2).toUpperCase() : '??';

  return (
    <aside className="side">
      <div className="brand">
        <div className="brand-mark">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="3 17 9 11 13 15 21 7" />
            <polyline points="14 7 21 7 21 14" />
          </svg>
        </div>
        <span className="brand-name">AssetFlow</span>
      </div>

      <div className="nav">
        <div className="nav-lbl">Workspace</div>
        {NAV_ITEMS.map(item => (
          <NavItem
            key={item.id}
            {...item}
            active={activeNav === item.id}
            onClick={setActiveNav}
          />
        ))}
      </div>

      <div className="nav">
        <div className="nav-lbl">Portfolios</div>
        <PortfolioList
          portfolios={portfolios}
          currentId={currentPortfolioId}
          onSelect={setCurrentPortfolioId}
          onNew={onNewPortfolio}
        />
      </div>

      <div className="me">
        <div className="av">{initials}</div>
        <div className="who">
          {username}
        </div>
        <button style={{ marginLeft: 'auto', background: 'none', border: 'none', cursor: 'pointer', color: 'var(--ink-3)' }} onClick={logout} title="Sign out">
          <LogOutIcon size={14} />
        </button>
      </div>
    </aside>
  );
}
