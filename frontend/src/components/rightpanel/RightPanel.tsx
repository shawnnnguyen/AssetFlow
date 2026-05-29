import JustTriggeredBanner from './JustTriggeredBanner';
import ActiveAlerts from './ActiveAlerts';
import RecentTransactions from './RecentTransactions';
import type { TriggeredAlert, NormalizedAlert, DisplayTransaction } from '../../types';

interface RightPanelProps {
  triggeredAlerts: TriggeredAlert[];
  activeAlerts: NormalizedAlert[];
  transactions: DisplayTransaction[];
  onDeleteAlert: (id: number) => void;
  onUpdateAlert?: (updated: NormalizedAlert) => void;
}

export default function RightPanel({
  triggeredAlerts,
  activeAlerts,
  transactions,
  onDeleteAlert,
  onUpdateAlert,
}: RightPanelProps) {
  return (
    <aside className="rail">
      <JustTriggeredBanner alerts={triggeredAlerts} />
      <ActiveAlerts alerts={activeAlerts} onDelete={onDeleteAlert} onUpdate={onUpdateAlert} />
      <RecentTransactions transactions={transactions} />
    </aside>
  );
}
