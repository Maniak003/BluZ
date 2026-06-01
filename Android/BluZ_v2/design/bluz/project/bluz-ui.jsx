// BluZ shared UI atoms — status bar, nav rails, icons, controls.

// ─── Icons (24px) ──────────────────────────────────────────────
const Ico = {
  spectrum: (s = 22) => (
    <svg width={s} height={s} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 20V8M8 20V4M13 20v-8M18 20v-5"/>
    </svg>
  ),
  history: (s = 22) => (
    <svg width={s} height={s} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 12a9 9 0 1 0 3-6.7"/><path d="M3 4v4h4"/><path d="M12 8v5l3 2"/>
    </svg>
  ),
  dose: (s = 22) => (
    <svg width={s} height={s} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">
      <path d="M2 17l4-4 3 3 5-7 4 6 4-4"/>
      <circle cx="12" cy="12" r="0" />
    </svg>
  ),
  log: (s = 22) => (
    <svg width={s} height={s} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">
      <path d="M4 6h12M4 12h16M4 18h10"/>
      <circle cx="20" cy="6" r="0.6" fill="currentColor"/>
    </svg>
  ),
  map: (s = 22) => (
    <svg width={s} height={s} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 6l6-2 6 2 6-2v14l-6 2-6-2-6 2z"/><path d="M9 4v16M15 6v16"/>
    </svg>
  ),
  settings: (s = 22) => (
    <svg width={s} height={s} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="3"/>
      <path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.9 2.9l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.9-2.9l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.9-2.9l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.9 2.9l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z"/>
    </svg>
  ),
  play: (s = 22) => (
    <svg width={s} height={s} viewBox="0 0 24 24" fill="currentColor"><path d="M7 5l12 7-12 7z"/></svg>
  ),
  stop: (s = 22) => (
    <svg width={s} height={s} viewBox="0 0 24 24" fill="currentColor"><rect x="6" y="6" width="12" height="12" rx="1.5"/></svg>
  ),
  save: (s = 18) => (
    <svg width={s} height={s} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">
      <path d="M5 4h11l3 3v13a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1z"/>
      <path d="M7 4v5h8V4"/><path d="M7 21v-7h10v7"/>
    </svg>
  ),
  zoom: (s = 18) => (
    <svg width={s} height={s} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="11" cy="11" r="6"/><path d="M20 20l-4.5-4.5M9 11h4M11 9v4"/>
    </svg>
  ),
  scale: (s = 18) => (
    <svg width={s} height={s} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 21v-6M3 21h6M3 21l8-8M21 3v6M21 3h-6M21 3l-8 8"/>
    </svg>
  ),
  clear: (s = 18) => (
    <svg width={s} height={s} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">
      <path d="M5 7h14M9 7V4h6v3M7 7l1 13h8l1-13M10 11v6M14 11v6"/>
    </svg>
  ),
  bt: (s = 16) => (
    <svg width={s} height={s} viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" strokeLinecap="round">
      <path d="M5 4l6 6-3 3V2l3 3-6 6"/>
    </svg>
  ),
  warn: (s = 22) => (
    <svg width={s} height={s} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 3l10 17H2z"/><path d="M12 10v5"/><circle cx="12" cy="18" r="0.8" fill="currentColor"/>
    </svg>
  ),
  radiation: (s = 22) => (
    <svg width={s} height={s} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
      <circle cx="12" cy="12" r="2.4" fill="currentColor"/>
      <path d="M12 3a9 9 0 0 1 7.8 4.5L13.5 11A2.4 2.4 0 0 0 12 10z" fill="currentColor"/>
      <path d="M19.8 16.5A9 9 0 0 1 12 21v-7c.55 0 1.05-.18 1.45-.5z" fill="currentColor"/>
      <path d="M4.2 16.5A9 9 0 0 0 12 21v-7c-.55 0-1.05-.18-1.45-.5z" fill="currentColor"/>
    </svg>
  ),
  chevron: (dir = "right", s = 16) => {
    const paths = {
      right: "M6 4l5 5-5 5",
      down:  "M4 6l5 5 5-5",
      up:    "M4 10l5-5 5 5",
      left:  "M11 4l-5 5 5 5",
    };
    return (
      <svg width={s} height={s} viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" strokeLinecap="round">
        <path d={paths[dir]}/>
      </svg>
    );
  },
  thermometer: (s = 14) => (
    <svg width={s} height={s} viewBox="0 0 14 14" fill="none" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round">
      <path d="M7 1.5a1.5 1.5 0 0 0-1.5 1.5v6.2a3 3 0 1 0 3 0V3A1.5 1.5 0 0 0 7 1.5z"/>
      <circle cx="7" cy="11" r="1.2" fill="currentColor"/>
    </svg>
  ),
  clock: (s = 14) => (
    <svg width={s} height={s} viewBox="0 0 14 14" fill="none" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="7" cy="7" r="5.5"/><path d="M7 3.5v3.7l2.3 1.5"/>
    </svg>
  ),
  battery: (s = 14) => (
    <svg width={s} height={s} viewBox="0 0 18 12" fill="none" stroke="currentColor" strokeWidth="1.3">
      <rect x="1" y="2" width="13" height="8" rx="1.5"/><rect x="15" y="4.5" width="2" height="3" rx="0.5" fill="currentColor"/>
      <rect x="2.5" y="3.5" width="6" height="5" rx="0.5" fill="currentColor"/>
    </svg>
  ),
  exit: (s = 22) => (
    <svg width={s} height={s} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">
      <path d="M15 3h5v18h-5"/><path d="M3 12h12M11 8l4 4-4 4"/>
    </svg>
  ),
  check: (s = 14) => (
    <svg width={s} height={s} viewBox="0 0 14 14" fill="none" stroke="currentColor" strokeWidth="2.1" strokeLinecap="round" strokeLinejoin="round">
      <path d="M2.5 7.5l3 3 6-6.5"/>
    </svg>
  ),
  plus: (s = 14) => (
    <svg width={s} height={s} viewBox="0 0 14 14" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round">
      <path d="M7 2.5v9M2.5 7h9"/>
    </svg>
  ),
  target: (s = 22) => (
    <svg width={s} height={s} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="8"/><circle cx="12" cy="12" r="3"/>
      <path d="M12 1v3M12 20v3M1 12h3M20 12h3"/>
    </svg>
  ),
  pin: (s = 22) => (
    <svg width={s} height={s} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 22s7-7.6 7-13a7 7 0 1 0-14 0c0 5.4 7 13 7 13z"/>
      <circle cx="12" cy="9" r="2.5"/>
    </svg>
  ),
  search: (s = 16) => (
    <svg width={s} height={s} viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round">
      <circle cx="7" cy="7" r="4.5"/><path d="M13.5 13.5L10.5 10.5"/>
    </svg>
  ),
};

// ─── BT pill ──────────────────────────────────────────────────
function BtIndicator({ status = "on", label = "BluZ-2A12", compact = false }) {
  const c = status === "on" ? "var(--bz-bt-on)" :
            status === "warn" ? "var(--bz-bt-warn)" :
            "var(--bz-bt-off)";
  if (compact) {
    return (
      <span style={{
        display: "inline-flex", alignItems: "center", gap: 5,
        padding: "5px 9px 5px 8px",
        borderRadius: "var(--bz-r-pill)",
        background: "var(--bz-surface-2)",
        border: "1px solid var(--bz-line-soft)",
        color: c, fontSize: 11, fontWeight: 700,
      }}>
        <span style={{ width:6, height:6, borderRadius:3, background:c }}/>
        {Ico.bt(11)}
      </span>
    );
  }
  return (
    <div style={{
      display: "flex", alignItems: "center", gap: 6,
      padding: "4px 8px 4px 6px",
      borderRadius: "var(--bz-r-pill)",
      background: "var(--bz-surface-2)",
      border: "1px solid var(--bz-line-soft)",
      color: c, fontSize: 12, fontWeight: 600,
      letterSpacing: 0.2,
    }}>
      <span style={{ display: "inline-flex" }}>{Ico.bt(13)}</span>
      <span style={{ fontFamily: "var(--bz-font-mono)", color: "var(--bz-text)" }}>{label}</span>
    </div>
  );
}

// ─── Big-number readout (CPS / dose rate) ─────────────────────
function Readout({ label, value, unit, accent = false, danger = false, large = false }) {
  const color = danger ? "var(--bz-danger)" : accent ? "var(--bz-accent)" : "var(--bz-text)";
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 2, minWidth: 0 }}>
      <span style={{
        fontSize: 10, letterSpacing: 1.2, textTransform: "uppercase",
        color: "var(--bz-text-muted)", fontWeight: 600,
      }}>{label}</span>
      <div style={{ display: "flex", alignItems: "baseline", gap: 4 }}>
        <span className="bz-mono bz-tabular" style={{
          fontSize: large ? 32 : 22, lineHeight: 1, fontWeight: 600,
          color, letterSpacing: -0.5,
        }}>{value}</span>
        {unit && (
          <span style={{
            fontSize: 11, color: "var(--bz-text-dim)",
            fontFamily: "var(--bz-font-mono)", letterSpacing: 0.4,
          }}>{unit}</span>
        )}
      </div>
    </div>
  );
}

// Compact stat for status strips
function Stat({ icon, value, unit, color = "var(--bz-text)" }) {
  return (
    <div style={{
      display: "flex", alignItems: "center", gap: 5,
      color: "var(--bz-text-dim)", fontSize: 12,
    }}>
      {icon && <span style={{ display: "inline-flex", color: "var(--bz-text-muted)" }}>{icon}</span>}
      <span className="bz-mono bz-tabular" style={{ color, fontSize: 12, fontWeight: 600 }}>
        {value}{unit && <span style={{ color: "var(--bz-text-muted)", marginLeft: 2, fontWeight: 500 }}>{unit}</span>}
      </span>
    </div>
  );
}

// ─── Status bar (above-the-fold persistent strip) ─────────────
function StatusStrip({ orientation = "portrait", overload = false, btStatus = "on", elapsed = "07:19:22" }) {
  return (
    <div style={{
      display: "flex", alignItems: "center", gap: 10,
      padding: orientation === "portrait" ? "8px 14px" : "6px 14px",
      borderBottom: "1px solid var(--bz-line-soft)",
      background: overload ? "var(--bz-danger-soft)" : "var(--bz-surface)",
      transition: "background .2s",
      minWidth: 0,
    }}>
      <BtIndicator status={btStatus} compact/>
      <Stat
        icon={Ico.radiation(13)}
        value="14"
        unit="cps"
        color={overload ? "var(--bz-danger)" : "var(--bz-accent)"}
      />
      <Stat icon={Ico.clock(12)} value={elapsed} />
      <div style={{ flex: 1, minWidth: 0 }}/>
      <Stat icon={Ico.thermometer(12)} value="27" unit="°C"/>
      <Stat icon={Ico.battery(13)} value="3.58" unit="V"/>
    </div>
  );
}

// ─── Bottom nav (portrait default) ────────────────────────────
function BottomNav({ active = "spectrum", variant = "bar", onChange }) {
  const tabs = [
    { id: "spectrum", label: "Спектр",   icon: Ico.spectrum },
    { id: "history",  label: "История",  icon: Ico.history },
    { id: "dose",     label: "Доза",     icon: Ico.dose },
    { id: "map",      label: "Карта",    icon: Ico.map },
    { id: "settings", label: "Настр.",   icon: Ico.settings },
  ];

  if (variant === "fab") {
    // Compact FAB row: 4 primary
    const primary = tabs.slice(0, 4);
    return (
      <div style={{
        position: "absolute", left: 16, right: 16, bottom: 18,
        display: "flex", alignItems: "center", justifyContent: "center", gap: 10,
        zIndex: 20,
      }}>
        <div style={{
          display: "flex", alignItems: "center", gap: 4,
          padding: 6,
          borderRadius: 28,
          background: "color-mix(in srgb, var(--bz-surface) 92%, transparent)",
          backdropFilter: "blur(10px)",
          border: "1px solid var(--bz-line)",
          boxShadow: "0 12px 32px -8px rgba(0,0,0,0.6)",
        }}>
          {tabs.map((t) => {
            const isActive = t.id === active;
            return (
              <button
                key={t.id}
                onClick={() => onChange && onChange(t.id)}
                style={{
                  width: 52, height: 52, borderRadius: 22,
                  display: "flex", alignItems: "center", justifyContent: "center",
                  background: isActive ? "var(--bz-accent)" : "transparent",
                  color: isActive ? "var(--bz-on-accent)" : "var(--bz-text-dim)",
                  border: "none", cursor: "pointer", padding: 0,
                  transition: "background .15s",
                }}
                title={t.label}
              >
                {t.icon(22)}
              </button>
            );
          })}
        </div>
      </div>
    );
  }

  return (
    <div style={{
      display: "flex",
      borderTop: "1px solid var(--bz-line-soft)",
      background: "var(--bz-surface)",
      paddingBottom: 8,
      overflow: "hidden",
    }}>
      {tabs.map((t) => {
        const isActive = t.id === active;
        return (
          <button
            key={t.id}
            onClick={() => onChange && onChange(t.id)}
            style={{
              flex: 1, minWidth: 0, height: 64, minHeight: 64,
              display: "flex", flexDirection: "column",
              alignItems: "center", justifyContent: "center",
              gap: 4, padding: "8px 2px",
              background: "transparent", border: "none", cursor: "pointer",
              color: isActive ? "var(--bz-accent)" : "var(--bz-text-dim)",
              position: "relative",
            }}
          >
            {isActive && (
              <span style={{
                position: "absolute", top: 0, left: "50%", transform: "translateX(-50%)",
                width: 32, height: 3, borderRadius: 2,
                background: "var(--bz-accent)",
              }}/>
            )}
            {t.icon(22)}
            <span style={{
              fontSize: 11, fontWeight: isActive ? 600 : 500, letterSpacing: 0.2,
            }}>{t.label}</span>
          </button>
        );
      })}
    </div>
  );
}

// ─── Side rail (landscape) ────────────────────────────────────
function SideRail({ active = "spectrum", side = "right", onChange }) {
  const tabs = [
    { id: "spectrum", label: "Спектр",   icon: Ico.spectrum },
    { id: "history",  label: "История",  icon: Ico.history },
    { id: "dose",     label: "Доза",     icon: Ico.dose },
    { id: "map",      label: "Карта",    icon: Ico.map },
    { id: "settings", label: "Настр.",   icon: Ico.settings },
  ];
  return (
    <div style={{
      width: 76, height: "100%",
      display: "flex", flexDirection: "column", alignItems: "center",
      paddingTop: 8, paddingBottom: 8,
      background: "var(--bz-surface)",
      borderLeft:  side === "right" ? "1px solid var(--bz-line-soft)" : "none",
      borderRight: side === "left"  ? "1px solid var(--bz-line-soft)" : "none",
      gap: 2,
    }}>
      {tabs.map((t) => {
        const isActive = t.id === active;
        return (
          <button key={t.id}
            onClick={() => onChange && onChange(t.id)}
            style={{
              width: 64, minHeight: 58, height: 58,
              display: "flex", flexDirection: "column",
              alignItems: "center", justifyContent: "center", gap: 3,
              background: isActive ? "var(--bz-accent-soft)" : "transparent",
              border: "none", cursor: "pointer", padding: 0,
              borderRadius: 14,
              color: isActive ? "var(--bz-accent)" : "var(--bz-text-dim)",
              position: "relative",
            }}>
            {t.icon(20)}
            <span style={{ fontSize: 10, fontWeight: isActive ? 600 : 500 }}>{t.label}</span>
          </button>
        );
      })}
      <div style={{ flex: 1 }}/>
      <button style={{
        width: 64, height: 50, borderRadius: 14, background: "transparent",
        border: "none", cursor: "pointer", color: "var(--bz-text-muted)",
        display: "flex", alignItems: "center", justifyContent: "center",
      }}>
        {Ico.exit(20)}
      </button>
    </div>
  );
}

// ─── Big circle button (play/stop) ────────────────────────────
function CircleButton({ icon, label, color = "var(--bz-accent)", onClick, danger = false }) {
  const bg = danger ? "var(--bz-danger)" : color;
  return (
    <button onClick={onClick}
      style={{
        width: 64, height: 64, borderRadius: "50%",
        background: bg,
        color: danger ? "#fff" : "var(--bz-on-accent)",
        display: "flex", alignItems: "center", justifyContent: "center",
        border: "none", cursor: "pointer", padding: 0,
        boxShadow: `0 6px 20px -4px ${danger ? "rgba(255,64,64,0.45)" : "rgba(0,212,170,0.4)"}`,
      }}
      title={label}
    >{icon}</button>
  );
}

// ─── Icon button (chart controls) ─────────────────────────────
function IconButton({ icon, label, active = false, onClick, size = 44 }) {
  return (
    <button onClick={onClick}
      style={{
        minWidth: size, height: size,
        padding: label ? "0 14px" : 0,
        borderRadius: 12,
        background: active ? "var(--bz-accent-soft)" : "var(--bz-surface-2)",
        color: active ? "var(--bz-accent)" : "var(--bz-text)",
        border: `1px solid ${active ? "var(--bz-accent)" : "var(--bz-line)"}`,
        display: "flex", alignItems: "center", justifyContent: "center", gap: 6,
        cursor: "pointer",
        fontFamily: "var(--bz-font-ui)", fontSize: 13, fontWeight: 600,
      }}
      title={label}
    >
      {icon}
      {label && <span>{label}</span>}
    </button>
  );
}

// ─── Section header / cards for settings ─────────────────────
function Card({ title, action, children, style }) {
  return (
    <div style={{
      background: "var(--bz-surface)",
      border: "1px solid var(--bz-line-soft)",
      borderRadius: "var(--bz-r)",
      ...style,
    }}>
      {(title || action) && (
        <div style={{
          display: "flex", alignItems: "center", justifyContent: "space-between",
          padding: "14px 16px 10px",
        }}>
          <span style={{
            fontSize: 12, letterSpacing: 1.4, textTransform: "uppercase",
            color: "var(--bz-text-muted)", fontWeight: 600,
          }}>{title}</span>
          {action}
        </div>
      )}
      <div>{children}</div>
    </div>
  );
}

function Row({ label, value, sub, action, divider = true, onClick }) {
  return (
    <div onClick={onClick}
      style={{
        display: "flex", alignItems: "center", gap: 12,
        padding: "13px 16px", minHeight: 52,
        borderTop: divider ? "1px solid var(--bz-divider)" : "none",
        cursor: onClick ? "pointer" : "default",
      }}>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 14, color: "var(--bz-text)", fontWeight: 500 }}>{label}</div>
        {sub && <div style={{ fontSize: 12, color: "var(--bz-text-muted)", marginTop: 2 }}>{sub}</div>}
      </div>
      {value && (
        <div className="bz-mono" style={{ fontSize: 14, color: "var(--bz-text-dim)" }}>{value}</div>
      )}
      {action}
    </div>
  );
}

function Toggle({ on = false, onClick }) {
  return (
    <button onClick={onClick} style={{
      width: 44, height: 26, borderRadius: 13,
      background: on ? "var(--bz-accent)" : "var(--bz-surface-3)",
      border: "1px solid " + (on ? "var(--bz-accent)" : "var(--bz-line)"),
      position: "relative", cursor: "pointer", padding: 0,
      transition: "background .15s",
    }}>
      <span style={{
        position: "absolute", top: 2, left: on ? 20 : 2,
        width: 20, height: 20, borderRadius: "50%",
        background: on ? "var(--bz-on-accent)" : "var(--bz-text-dim)",
        transition: "left .15s",
      }}/>
    </button>
  );
}

function Segmented({ options, value, onChange, fill = false }) {
  return (
    <div style={{
      display: "inline-flex", padding: 3,
      background: "var(--bz-surface-3)",
      borderRadius: 10, border: "1px solid var(--bz-line)",
      width: fill ? "100%" : undefined,
    }}>
      {options.map((o) => {
        const isActive = o.value === value;
        return (
          <button key={o.value}
            onClick={() => onChange && onChange(o.value)}
            style={{
              flex: fill ? 1 : undefined,
              padding: "7px 14px", borderRadius: 7,
              background: isActive ? "var(--bz-accent)" : "transparent",
              color: isActive ? "var(--bz-on-accent)" : "var(--bz-text-dim)",
              border: "none", cursor: "pointer",
              fontSize: 12, fontWeight: 600, letterSpacing: 0.3,
              fontFamily: "var(--bz-font-mono)",
            }}>{o.label}</button>
        );
      })}
    </div>
  );
}

Object.assign(window, {
  Ico, BtIndicator, Readout, Stat, StatusStrip,
  BottomNav, SideRail, CircleButton, IconButton, Card, Row, Toggle, Segmented,
});
