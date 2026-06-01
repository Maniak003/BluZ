/* PhoneFrame — minimal Android device chrome with notch & safe-areas.
   Renders a true rounded-corner screen, draws a punch-hole notch in
   either portrait (top-center) or landscape (left-side or right-side),
   and exposes safe-area variables via inline style. The status content
   the OS would draw lives on a thin OS bar above the cutout. */

function PhoneFrame({
  orientation = "portrait",
  notch = "center",          // 'left' | 'center' | 'right'
  showFrame = true,
  scale = 1,
  os = "10:24",
  battery = 72,
  theme = "dark",            // 'dark' | 'light'
  children,
}) {
  const W = orientation === "portrait" ? 412 : 892;
  const H = orientation === "portrait" ? 892 : 412;
  const bezel = showFrame ? 10 : 0;
  const radius = showFrame ? 44 : 36;

  // notch position
  const notchSize = 22;
  let cutout = null;
  if (orientation === "portrait") {
    const left =
      notch === "left" ? 28 :
      notch === "right" ? W - 28 - notchSize :
      (W - notchSize) / 2;
    cutout = { left, top: 14, w: notchSize, h: notchSize };
  } else {
    const top = (H - notchSize) / 2;
    const left = notch === "right" ? W - 14 - notchSize : 14;
    cutout = { left, top, w: notchSize, h: notchSize };
  }

  // safe area top depends on orientation
  const safeTop = orientation === "portrait" ? 44 : 0;
  const safeBottom = orientation === "portrait" ? 22 : 0;
  const safeLeft  = orientation === "landscape" && notch === "left"  ? 56 : 0;
  const safeRight = orientation === "landscape" && notch === "right" ? 56 : 0;

  return (
    <div
      className="bz-phone"
      style={{
        position: "relative",
        width: W + bezel * 2,
        height: H + bezel * 2,
        padding: bezel,
        borderRadius: radius,
        background: showFrame
          ? "linear-gradient(150deg,#2a2f37 0%,#16191e 50%,#2a2f37 100%)"
          : "transparent",
        boxShadow: showFrame
          ? "0 30px 80px -20px rgba(0,0,0,0.55), 0 0 0 1.5px rgba(0,0,0,0.6), inset 0 0 0 1.5px rgba(255,255,255,0.06)"
          : "none",
        boxSizing: "content-box",
        transform: scale !== 1 ? `scale(${scale})` : undefined,
        transformOrigin: "top left",
      }}
    >
      {/* Screen */}
      <div
        className="bz-screen"
        data-theme={theme}
        style={{
          position: "relative",
          width: W,
          height: H,
          borderRadius: radius - bezel,
          overflow: "hidden",
          background: "var(--bz-bg)",
          "--bz-inset-top": safeTop + "px",
          "--bz-inset-bottom": safeBottom + "px",
          "--bz-inset-left": safeLeft + "px",
          "--bz-inset-right": safeRight + "px",
        }}
      >
        {/* OS status bar (portrait only — landscape collapses it) */}
        {orientation === "portrait" && (
          <div
            style={{
              position: "absolute", top: 0, left: 0, right: 0,
              height: safeTop, zIndex: 30,
              display: "flex", alignItems: "center",
              justifyContent: "space-between",
              padding: "0 22px",
              fontFamily: "var(--bz-font-ui)",
              fontSize: 13, fontWeight: 500,
              color: theme === "light" ? "rgba(0,0,0,0.78)" : "rgba(255,255,255,0.78)",
              pointerEvents: "none",
            }}
          >
            <span style={{
              minWidth: 60,
              textAlign: notch === "left" ? "right" : "left",
              marginLeft: notch === "left" ? 28 : 0,
            }}>{os}</span>
            <span style={{ display: "flex", alignItems: "center", gap: 6 }}>
              <SignalIcon />
              <WifiIcon />
              <BatteryIcon pct={battery} />
            </span>
          </div>
        )}

        {/* Camera punch-hole */}
        <div
          aria-hidden="true"
          style={{
            position: "absolute",
            left: cutout.left, top: cutout.top,
            width: cutout.w, height: cutout.h,
            borderRadius: "50%",
            background: "#000",
            boxShadow: "inset 0 0 0 1px rgba(255,255,255,0.08), 0 0 0 1px rgba(0,0,0,0.8)",
            zIndex: 50,
          }}
        >
          <div style={{
            position: "absolute", inset: 5, borderRadius: "50%",
            background: "radial-gradient(circle at 35% 35%, #1a2233 0%, #060810 70%)",
          }} />
        </div>

        {/* App content */}
        <div
          style={{
            position: "absolute",
            inset: 0,
            display: "flex",
          }}
        >
          {children}
        </div>
      </div>
    </div>
  );
}

function SignalIcon() {
  return (
    <svg width="14" height="11" viewBox="0 0 14 11" fill="currentColor">
      <rect x="0"  y="7" width="2.4" height="4" rx="0.4"/>
      <rect x="3.4" y="5" width="2.4" height="6" rx="0.4"/>
      <rect x="6.8" y="3" width="2.4" height="8" rx="0.4"/>
      <rect x="10.2" y="0" width="2.4" height="11" rx="0.4"/>
    </svg>
  );
}
function WifiIcon() {
  return (
    <svg width="14" height="11" viewBox="0 0 14 11" fill="none" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round">
      <path d="M1 4.2A9 9 0 0 1 13 4.2"/>
      <path d="M3.2 6.5A6 6 0 0 1 10.8 6.5"/>
      <path d="M5.4 8.7A3 3 0 0 1 8.6 8.7"/>
      <circle cx="7" cy="10" r="0.7" fill="currentColor" stroke="none"/>
    </svg>
  );
}
function BatteryIcon({ pct = 72 }) {
  return (
    <span style={{ display: "inline-flex", alignItems: "center", gap: 4 }}>
      <span style={{ fontSize: 11, opacity: 0.75 }}>{pct}%</span>
      <svg width="22" height="11" viewBox="0 0 22 11" fill="none">
        <rect x="0.5" y="0.5" width="18" height="10" rx="2.5" stroke="currentColor" opacity="0.6"/>
        <rect x="19.5" y="3.5" width="2" height="4" rx="0.5" fill="currentColor" opacity="0.6"/>
        <rect x="2" y="2" width={Math.max(0, 15 * pct / 100)} height="7" rx="1" fill="currentColor"/>
      </svg>
    </span>
  );
}

Object.assign(window, { PhoneFrame });
