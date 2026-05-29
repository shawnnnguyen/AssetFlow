import type { ReactNode } from 'react';

interface IcProps {
  d: ReactNode;
  size?: number;
  sw?: number;
  fill?: string;
  className?: string;
}

export type IconProps = Omit<IcProps, 'd'>;

function Ic({ d, size = 16, sw = 1.5, fill = 'none', className = 'ic' }: IcProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill={fill}
      stroke="currentColor"
      strokeWidth={sw}
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
    >
      {typeof d === 'string' ? <path d={d} /> : d}
    </svg>
  );
}

export const TrendIcon     = (p: IconProps) => <Ic {...p} d={<><polyline points="3 17 9 11 13 15 21 7"/><polyline points="14 7 21 7 21 14"/></>} />;
export const BriefcaseIcon = (p: IconProps) => <Ic {...p} d={<><rect x="3" y="7" width="18" height="13" rx="2"/><path d="M9 7V5a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v2"/><path d="M3 12h18"/></>} />;
export const ReceiptIcon   = (p: IconProps) => <Ic {...p} d={<><path d="M4 3h16v18l-3-2-3 2-3-2-3 2-4-2z"/><path d="M8 8h8M8 12h8M8 16h5"/></>} />;
export const BellIcon      = (p: IconProps) => <Ic {...p} d={<><path d="M6 8a6 6 0 1 1 12 0c0 7 3 7 3 9H3c0-2 3-2 3-9z"/><path d="M10 20a2 2 0 0 0 4 0"/></>} />;
export const StarIcon      = (p: IconProps) => <Ic {...p} d="M12 2l3 7 7 .8-5.3 4.7L18 22l-6-4-6 4 1.3-7.5L2 9.8 9 9z" />;
export const SearchIcon    = (p: IconProps) => <Ic {...p} d={<><circle cx="11" cy="11" r="7"/><line x1="21" y1="21" x2="16.5" y2="16.5"/></>} />;
export const PlusIcon      = (p: IconProps) => <Ic {...p} d="M12 5v14M5 12h14" />;
export const ChevronIcon   = (p: IconProps) => <Ic {...p} d="M9 6l6 6-6 6" />;
export const XIcon         = (p: IconProps) => <Ic {...p} d="M18 6L6 18M6 6l12 12" />;
export const LogOutIcon    = (p: IconProps) => <Ic {...p} d={<><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></>} />;
export const BarChartIcon  = (p: IconProps) => <Ic {...p} d={<><rect x="3" y="12" width="4" height="8" rx="1"/><rect x="10" y="8" width="4" height="12" rx="1"/><rect x="17" y="4" width="4" height="16" rx="1"/></>} />;
export const ArrowUpDownIcon = (p: IconProps) => <Ic {...p} d={<><path d="M7 3L7 21M7 21L3 17M7 21L11 17M17 21V3M17 3L13 7M17 3L21 7"/></>} />;
