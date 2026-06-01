// SpectrumChart — animated radiation spectrum curve, with optional
// secondary (history) trace, gridlines, log/lin scale, channel cursor,
// and customizable RGB color. Designed to fit any container.

function SpectrumChart({
  width = 380, height = 240,
  log = false,
  style = "line",          // 'line' | 'gisto' | 'fill'
  color = "#00d4aa",
  theme = "dark",
  secondary = null,        // optional second color
  overload = false,
  channels = 2048,
  showGrid = true,
  showPeaks = true,
}) {
  // In light theme, blend user color toward black for readability against
  // a near-white background. Dark theme uses the color as-is.
  const drawColor = theme === "light"
    ? `color-mix(in srgb, ${color} 50%, #06140f)`
    : color;
  const secColor = theme === "light" && secondary
    ? `color-mix(in srgb, ${secondary} 55%, #06140f)`
    : secondary;
  // Deterministic gamma spectrum — Cs-137 (~662 keV peak) + K-40 (~1460)
  // background + Compton continuum
  const data = React.useMemo(() => {
    const n = 220;
    const arr = new Array(n).fill(0);
    for (let i = 0; i < n; i++) {
      const x = i / n;
      // exponentially decaying continuum
      let v = Math.exp(-x * 4) * 920;
      // Cs-137 photopeak
      v += 720 * Math.exp(-Math.pow((i - 38) / 7, 2));
      // Compton edge bump
      v += 90 * Math.exp(-Math.pow((i - 28) / 12, 2));
      // K-40 small bump
      v += 60 * Math.exp(-Math.pow((i - 88) / 5, 2));
      // Tl-208 line
      v += 30 * Math.exp(-Math.pow((i - 152) / 4, 2));
      // pseudo-random noise (seeded)
      const seed = Math.sin(i * 12.9898) * 43758.5453;
      v += (seed - Math.floor(seed)) * v * 0.18;
      arr[i] = Math.max(0.5, v);
    }
    return arr;
  }, []);

  const secData = React.useMemo(() => {
    return data.map((v, i) => v * (0.35 + 0.12 * Math.sin(i * 0.4)));
  }, [data]);

  const max = Math.max(...data) * 1.05;
  const padL = 8, padR = 8, padT = 8, padB = 8;
  const innerW = width - padL - padR;
  const innerH = height - padT - padB;

  const scaleY = (v) => {
    if (log) {
      const lv = Math.log10(Math.max(1, v));
      const lm = Math.log10(max);
      return innerH - (lv / lm) * innerH;
    }
    return innerH - (v / max) * innerH;
  };

  // build path
  const buildPath = (arr) => {
    if (style === "gisto") {
      let d = "";
      const bw = innerW / arr.length;
      arr.forEach((v, i) => {
        const x = i * bw;
        const y = scaleY(v);
        d += `M${x.toFixed(2)} ${innerH} L${x.toFixed(2)} ${y.toFixed(2)} L${(x + bw).toFixed(2)} ${y.toFixed(2)} L${(x + bw).toFixed(2)} ${innerH} `;
      });
      return d;
    } else {
      const pts = arr.map((v, i) => [(i / (arr.length - 1)) * innerW, scaleY(v)]);
      return "M " + pts.map(([x, y]) => `${x.toFixed(2)} ${y.toFixed(2)}`).join(" L ");
    }
  };

  const path = buildPath(data);
  const secPath = buildPath(secData);
  const fillPath = path + ` L ${innerW} ${innerH} L 0 ${innerH} Z`;

  // peak labels (Cs-137 photopeak at index 38)
  const peakIdx = 38;
  const peakX = (peakIdx / (data.length - 1)) * innerW;
  const peakY = scaleY(data[peakIdx]);

  return (
    <svg
      width={width}
      height={height}
      viewBox={`0 0 ${width} ${height}`}
      style={{ display: "block", overflow: "visible", color: "var(--bz-text-muted)" }}
    >
      <defs>
        <linearGradient id="bz-sfill" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor={drawColor} stopOpacity="0.35"/>
          <stop offset="1" stopColor={drawColor} stopOpacity="0"/>
        </linearGradient>
      </defs>

      <g transform={`translate(${padL} ${padT})`}>
        {/* gridlines */}
        {showGrid && (
          <g stroke="currentColor" strokeOpacity="0.22" strokeWidth="1">
            {[0, 0.25, 0.5, 0.75, 1].map((p) => (
              <line key={"h" + p} x1="0" y1={innerH * p} x2={innerW} y2={innerH * p}/>
            ))}
            {[0, 0.2, 0.4, 0.6, 0.8, 1].map((p) => (
              <line key={"v" + p} x1={innerW * p} y1="0" x2={innerW * p} y2={innerH}/>
            ))}
          </g>
        )}

        {/* secondary trace (history) */}
        {secondary && (
          <path d={secPath} fill="none" stroke={secColor} strokeWidth="1" strokeOpacity="0.55"/>
        )}

        {/* main fill (only for fill style) */}
        {style === "fill" && (
          <path d={fillPath} fill={`url(#bz-sfill)`}/>
        )}
        {/* gisto fill */}
        {style === "gisto" && (
          <path d={path} fill={drawColor} fillOpacity="0.85"/>
        )}
        {/* line */}
        {(style === "line" || style === "fill") && (
          <path d={path} fill="none" stroke={drawColor} strokeWidth="1.9" strokeLinejoin="round" strokeLinecap="round"
            style={overload ? { animation: "bz-pulse 0.45s ease-in-out infinite" } : undefined}/>
        )}

        {/* peak callout */}
        {showPeaks && !overload && (
          <g>
            <line x1={peakX} y1={peakY - 2} x2={peakX} y2={peakY - 14} stroke={drawColor} strokeWidth="1" strokeOpacity="0.6"/>
            <circle cx={peakX} cy={peakY} r="2.4" fill={drawColor}/>
            <text x={peakX + 6} y={peakY - 8}
              fill="currentColor" fontSize="10" fontFamily="JetBrains Mono, ui-monospace, monospace" letterSpacing="0.3" fontWeight="600">
              Cs-137  662 keV
            </text>
          </g>
        )}

        {/* edge axis labels */}
        <text x="0" y={innerH + 14} fill="currentColor" fontSize="9" fontWeight="600" fontFamily="JetBrains Mono, ui-monospace, monospace">0</text>
        <text x={innerW - 4} y={innerH + 14} fill="currentColor" fontSize="9" fontWeight="600" fontFamily="JetBrains Mono, ui-monospace, monospace" textAnchor="end">
          {channels}
        </text>
      </g>
    </svg>
  );
}

// CpsOscilloscope — instantaneous + smoothed CPS trace (the dosimeter screen)
function CpsOscilloscope({ width = 380, height = 200, color = "#00d4aa", smoothColor = "#f5b400", theme = "dark" }) {
  const drawColor = theme === "light" ? `color-mix(in srgb, ${color} 50%, #06140f)` : color;
  const drawSmoothColor = theme === "light" ? `color-mix(in srgb, ${smoothColor} 50%, #06140f)` : smoothColor;
  const n = 120;
  const data = React.useMemo(() => {
    const arr = [];
    for (let i = 0; i < n; i++) {
      const seed = Math.sin(i * 91.213) * 43758.5453;
      const r = seed - Math.floor(seed);
      arr.push(12 + r * 12 + (i > 60 && i < 75 ? 14 : 0));
    }
    return arr;
  }, []);
  const sma = React.useMemo(() => {
    const k = 8;
    return data.map((_, i) => {
      let s = 0, c = 0;
      for (let j = Math.max(0, i - k); j <= Math.min(n - 1, i + k); j++) { s += data[j]; c++; }
      return s / c;
    });
  }, [data]);

  const max = Math.max(...data) * 1.15;
  const pad = 10;
  const w = width - pad * 2, h = height - pad * 2;
  const toPts = (arr) => arr.map((v, i) => [(i / (n - 1)) * w, h - (v / max) * h]);
  const ptsA = toPts(data);
  const ptsB = toPts(sma);

  // step-line for instantaneous reading
  const stepPath = "M " + ptsA.map(([x, y], i) => {
    if (i === 0) return `${x} ${y}`;
    const [px] = ptsA[i - 1];
    return `L ${px} ${y} L ${x} ${y}`;
  }).join(" ");

  const smaPath = "M " + ptsB.map(([x, y]) => `${x.toFixed(2)} ${y.toFixed(2)}`).join(" L ");

  return (
    <svg width={width} height={height} viewBox={`0 0 ${width} ${height}`} style={{ display: "block", color: "var(--bz-text-muted)" }}>
      <g transform={`translate(${pad} ${pad})`}>
        {/* gridlines */}
        <g stroke="currentColor" strokeOpacity="0.22" strokeWidth="1">
          {[0.25, 0.5, 0.75].map((p) => (
            <line key={p} x1="0" y1={h * p} x2={w} y2={h * p}/>
          ))}
        </g>
        {/* instantaneous step trace */}
        <path d={stepPath} fill="none" stroke={drawColor} strokeWidth="1.4" strokeOpacity="0.85"/>
        {/* SMA overlay */}
        <path d={smaPath} fill="none" stroke={drawSmoothColor} strokeWidth="1.8" strokeLinejoin="round" strokeLinecap="round"/>
        {/* axis labels */}
        <text x="0" y={h + 12} fill="currentColor" fontSize="10" fontWeight="600" fontFamily="JetBrains Mono, ui-monospace, monospace">cps · 60s</text>
        <text x={w} y={h + 12} fill="currentColor" fontSize="9" fontWeight="600" fontFamily="JetBrains Mono, ui-monospace, monospace" textAnchor="end">now</text>
      </g>
    </svg>
  );
}

Object.assign(window, { SpectrumChart, CpsOscilloscope });
