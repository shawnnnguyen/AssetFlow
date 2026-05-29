interface BadgeProps {
  count?: number | undefined;
}

export default function Badge({ count }: BadgeProps) {
  if (!count) return null;
  return <span className="nav-count">{count}</span>;
}
