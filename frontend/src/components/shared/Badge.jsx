export default function Badge({ count }) {
  if (!count) return null;
  return <span className="nav-count">{count}</span>;
}
