import type { ComponentType } from 'react';
import Badge from '../shared/Badge';
import type { IconProps } from '../shared/Icons';

export type NavId = 'dashboard' | 'market' | 'transactions';

interface NavItemProps {
  id: NavId;
  label: string;
  icon?: ComponentType<IconProps>;
  count?: number;
  active: boolean;
  onClick: (id: NavId) => void;
}

export default function NavItem({ id, label, icon: Icon, count, active, onClick }: NavItemProps) {
  return (
    <button className={`nav-item${active ? ' active' : ''}`} onClick={() => onClick(id)}>
      {Icon && <Icon />}
      {label}
      <Badge count={count} />
    </button>
  );
}
