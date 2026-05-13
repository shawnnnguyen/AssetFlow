export default function PortfolioStatusCard({ status = 'ACTIVE', createdAt = null }) {
  const isActive = status === 'ACTIVE';
  return (
    <div className="kpi">
      <div className="kpi-lbl">Portfolio status</div>
      <div className={`kpi-val ${isActive ? 'pos' : ''}`} style={{ fontSize: 20 }}>
        {status}
      </div>
      {createdAt && (
        <div className="kpi-sub">
          since {new Date(createdAt).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' })}
        </div>
      )}
    </div>
  );
}
