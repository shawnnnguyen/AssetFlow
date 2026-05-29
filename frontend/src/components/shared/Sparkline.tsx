interface SparklineProps {
  data: number[];
  color?: string;
}

export default function Sparkline({ data, color = 'var(--pos)' }: SparklineProps) {
  if (!data || data.length < 2) return <div className="spark" />;

  const w = 220, h = 36, pad = 2;
  const min = Math.min(...data);
  const max = Math.max(...data);
  const xs = (i: number) => pad + (i * (w - pad * 2)) / (data.length - 1);
  const ys = (v: number) => h - pad - ((v - min) / (max - min || 1)) * (h - pad * 2);
  const line = data.map((v, i) => `${i ? 'L' : 'M'}${xs(i).toFixed(1)},${ys(v).toFixed(1)}`).join(' ');
  const area = `${line} L${xs(data.length - 1)},${h} L${xs(0)},${h} Z`;

  return (
    <svg className="spark" viewBox={`0 0 ${w} ${h}`} preserveAspectRatio="none">
      <path className="area" d={area} style={{ fill: color }} />
      <path className="line" d={line} style={{ stroke: color }} />
    </svg>
  );
}
