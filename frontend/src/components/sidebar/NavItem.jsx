import Badge from '../shared/Badge';

export default function NavItem({ id, label, icon: Icon, count, active, onClick }) {
  return (
    <button className={`nav-item${active ? ' active' : ''}`} onClick={() => onClick(id)}>
      {Icon && <Icon />}
      {label}
      <Badge count={count} />
    </button>
  );
}
