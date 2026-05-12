import JustTriggeredBanner from './JustTriggeredBanner';
import ActiveAlerts from './ActiveAlerts';
import RecentTransactions from './RecentTransactions';

export default function RightPanel({ triggeredAlerts, activeAlerts, transactions, onDeleteAlert }) {
  return (
    <aside className="rail">
      <JustTriggeredBanner alerts={triggeredAlerts} />
      <ActiveAlerts alerts={activeAlerts} onDelete={onDeleteAlert} />
      <RecentTransactions transactions={transactions} />
    </aside>
  );
}
