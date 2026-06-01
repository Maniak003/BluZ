// BluZ screens — full layouts for each artboard.
// All assume they're rendered inside a <PhoneFrame> which sets the
// --bz-inset-* vars on the parent so safe-areas adapt to orientation/notch.

// ════════════════════════════════════════════════════════════════
// 1) SPECTRUM — PORTRAIT
// ════════════════════════════════════════════════════════════════
function _Legacy_ScreenSpectrumPortrait({ overload = false, navVariant = "bar", spectrumColor = "#00d4aa", chartStyle = "line", log = false, theme = "dark" }) {
  return (
    <div className="bz-screen" style={{
      flex: 1, display: "flex", flexDirection: "column",
      paddingTop: "var(--bz-inset-top)",
      paddingBottom: navVariant === "fab" ? 0 : 0,
    }}>
      {/* App title row — overload state appears INLINE so layout doesn't jump */}
      <div style={{
        display: "flex", alignItems: "center", gap: 12,
        padding: "12px 18px 8px",
      }}>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{
            fontSize: 11, letterSpacing: 1.4, textTransform: "uppercase", fontWeight: 700,
            color: overload ? "var(--bz-danger)" : "var(--bz-text-muted)",
            display: "flex", alignItems: "center", gap: 6,
            animation: overload ? "bz-pulse 1.1s ease-in-out infinite" : undefined,
          }}>
            {overload && <span style={{ display:"inline-flex" }}>{Ico.warn(13)}</span>}
            {overload ? "Перегрузка детектора · CPS > 25 000" : "Канал · 2048 · 0 – 3 МэВ"}
          </div>
          <div style={{
            display: "flex", alignItems: "center", gap: 8,
            fontSize: 22, color: "var(--bz-text)", fontWeight: 600, letterSpacing: -0.4,
          }}>
            <span>Гамма-спектр</span>
            {overload && (
              <span style={{
                fontSize: 11, fontFamily: "var(--bz-font-mono)", fontWeight: 700, letterSpacing: 0.6,
                padding: "2px 7px", borderRadius: 5,
                background: "var(--bz-danger)", color: "#fff",
              }}>L3</span>
            )}
          </div>
        </div>
        <IconButton icon={Ico.save(18)} size={40}/>
      </div>

      {/* Status strip */}
      <StatusStrip orientation="portrait" overload={overload} btStatus="on"/>

      {/* Hero readouts */}
      <div style={{
        padding: "14px 18px 10px",
        display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16,
      }}>
        <Readout label="МОЩНОСТЬ ДОЗЫ" value="12.44" unit="мкР/ч" accent={!overload} danger={overload} large/>
        <Readout label="СРЕДНЯЯ" value="13.99" unit="мкР/ч" large/>
      </div>

      {/* Chart */}
      <div style={{
        flex: 1, margin: "4px 14px 12px",
        background: "var(--bz-surface)",
        border: "1px solid var(--bz-line-soft)",
        borderRadius: "var(--bz-r)",
        padding: "12px 12px 8px",
        display: "flex", flexDirection: "column", minHeight: 0,
      }}>
        {/* y-axis labels above */}
        <div style={{
          display: "flex", justifyContent: "space-between",
          fontSize: 10, color: "var(--bz-text-muted)",
          fontFamily: "var(--bz-font-mono)", letterSpacing: 0.5,
          marginBottom: 6,
        }}>
          <span>{log ? "LOG" : "LIN"} · counts</span>
          <span>Total 10 623 119  ·  0.09%</span>
        </div>
        <div style={{ flex: 1, position: "relative", display: "flex" }}>
          <SpectrumChart theme={theme} width={344}
            height={overload ? 320 : 360}
            log={log}
            style={chartStyle}
            color={overload ? "var(--bz-danger)" : spectrumColor}
            overload={overload}
            channels={2048}
            showPeaks={!overload}
          />
        </div>
        {/* Chart toolbar */}
        <div style={{
          display: "flex", gap: 8, marginTop: 8, paddingTop: 10,
          borderTop: "1px solid var(--bz-divider)",
        }}>
          <IconButton icon={Ico.scale(16)} label={log ? "Log" : "Lin"} size={40}/>
          <IconButton icon={Ico.zoom(16)} label="1×" size={40}/>
          <div style={{ flex: 1 }}/>
          <IconButton icon={Ico.clear(16)} label="Clear" size={40}/>
        </div>
      </div>

      {/* Big start/stop affordance */}
      <div style={{
        display: "flex", alignItems: "center", justifyContent: "center",
        padding: "0 18px 14px", gap: 16,
      }}>
        <div style={{ flex: 1, minWidth: 0, fontSize: 11, color: "var(--bz-text-muted)", textTransform: "uppercase", letterSpacing: 1.2, fontWeight: 600 }}>
          <span style={{ display:"inline-flex", alignItems:"center", gap:6 }}>
            <span style={{ width:7, height:7, borderRadius:4, background:"var(--bz-danger)", animation:"bz-pulse 1.2s ease-in-out infinite" }}/>
            Идёт измерение
          </span>
          <div className="bz-mono" style={{ fontSize: 18, color: "var(--bz-text)", marginTop: 4, letterSpacing: -0.3 }}>
            07:19:22<span style={{ color: "var(--bz-text-muted)" }}>:48</span>
          </div>
        </div>
        <CircleButton icon={Ico.stop(22)} label="Стоп" danger/>
      </div>

      {/* Nav */}
      {navVariant === "bar" && <BottomNav active="spectrum"/>}
      {navVariant === "fab" && <BottomNav active="spectrum" variant="fab"/>}
    </div>
  );
}

// Red overload banner (inline at top of content, below OS status area)
function OverloadBanner() {
  return (
    <div className="bz-overload-banner" style={{
      flexShrink: 0,
      display: "flex", alignItems: "center", gap: 10,
      padding: "10px 16px",
      background: "linear-gradient(90deg, var(--bz-danger) 0%, var(--bz-danger-2) 100%)",
      color: "#fff",
      animation: "bz-pulse-bg 1s ease-in-out infinite",
      borderBottom: "1px solid rgba(255,255,255,0.15)",
    }}>
      <span style={{
        display: "inline-flex", padding: 2,
        animation: "bz-pulse 0.45s ease-in-out infinite",
      }}>{Ico.warn(18)}</span>
      <span style={{ fontSize: 13, fontWeight: 700, letterSpacing: 0.3, textTransform: "uppercase" }}>
        Перегрузка детектора
      </span>
      <span style={{ flex: 1, fontSize: 12, opacity: 0.92, fontWeight: 500 }}>
        CPS &gt; 25 000 · уменьшите дозу
      </span>
      <span className="bz-mono" style={{
        fontSize: 13, fontWeight: 700, padding: "3px 8px",
        background: "rgba(0,0,0,0.25)", borderRadius: 4,
      }}>L3</span>
    </div>
  );
}

// ════════════════════════════════════════════════════════════════
// 2) SPECTRUM — LANDSCAPE
// ════════════════════════════════════════════════════════════════
function ScreenSpectrumLandscape({ overload = false, side = "right", spectrumColor = "#00d4aa", chartStyle = "line", log = false, theme = "dark" }) {
  // safe-left/right are already in the screen-level paddings via the parent;
  // here we lay out: rail | content | (rail). side='right' = rail on the right.
  const rail = <SideRail side={side} active="spectrum"/>;

  return (
    <div className="bz-screen" style={{
      flex: 1, display: "flex", flexDirection: "row",
      paddingLeft: "var(--bz-inset-left)", paddingRight: "var(--bz-inset-right)",
    }}>
      {side === "left" && rail}

      <div style={{ flex: 1, display: "flex", flexDirection: "column", minWidth: 0 }}>
        <StatusStrip orientation="landscape" overload={overload} btStatus="on"/>

        {/* Hero readouts row — overload chip inlined; layout stays stable */}
        <div style={{
          display: "flex", alignItems: "center", gap: 18,
          padding: "8px 16px 6px",
          borderBottom: "1px solid var(--bz-line-soft)",
        }}>
          <Readout label="ДОЗА" value="12.44" unit="мкР/ч" accent={!overload} danger={overload} large/>
          <div style={{ width: 1, height: 32, background: "var(--bz-line)" }}/>
          <Readout label="СРЕДНЯЯ" value="13.99" unit="мкР/ч" large/>
          {overload && (
            <span style={{
              display: "inline-flex", alignItems: "center", gap: 6,
              padding: "5px 10px 5px 8px", borderRadius: 6,
              background: "var(--bz-danger)", color: "#fff",
              fontSize: 11, fontWeight: 700, letterSpacing: 0.5, textTransform: "uppercase",
              animation: "bz-pulse 1.1s ease-in-out infinite",
            }}>
              {Ico.warn(14)} Перегрузка · L3
            </span>
          )}
          <div style={{ flex: 1 }}/>
          <div style={{ display: "flex", gap: 6 }}>
            <IconButton icon={Ico.scale(16)} label={log ? "Log" : "Lin"} size={38}/>
            <IconButton icon={Ico.zoom(16)} label="1×" size={38}/>
            <IconButton icon={Ico.save(16)} size={38}/>
            <CircleButton icon={Ico.stop(18)} danger/>
          </div>
        </div>

        {/* Chart — fills remaining */}
        <div style={{ flex: 1, padding: "12px 18px 12px", minHeight: 0, display: "flex" }}>
          <div style={{
            flex: 1,
            background: "var(--bz-surface)",
            border: "1px solid var(--bz-line-soft)",
            borderRadius: "var(--bz-r)",
            padding: "10px 12px",
            display: "flex", flexDirection: "column",
          }}>
            <div style={{
              display: "flex", justifyContent: "space-between",
              fontSize: 10, color: "var(--bz-text-muted)",
              fontFamily: "var(--bz-font-mono)", letterSpacing: 0.5,
            }}>
              <span>{log ? "LOG" : "LIN"} · counts</span>
              <span>2048 каналов · Cs-137 пик @ 662 keV</span>
            </div>
            <div style={{ flex: 1, display: "flex" }}>
              <SpectrumChart theme={theme} width={720}
                height={260}
                log={log}
                style={chartStyle}
                color={overload ? "var(--bz-danger)" : spectrumColor}
                overload={overload}
                channels={2048}
              />
            </div>
          </div>
        </div>
      </div>

      {side === "right" && rail}
    </div>
  );
}

// ════════════════════════════════════════════════════════════════
// 3) SETTINGS — PORTRAIT
// ════════════════════════════════════════════════════════════════
function _Legacy_ScreenSettings({ navVariant = "bar" }) {
  return (
    <div className="bz-screen" style={{
      flex: 1, display: "flex", flexDirection: "column",
      paddingTop: "var(--bz-inset-top)",
    }}>
      <div style={{ padding: "12px 18px 4px" }}>
        <div style={{ fontSize: 11, color: "var(--bz-text-muted)", letterSpacing: 1.4, textTransform: "uppercase", fontWeight: 600 }}>
          BluZ · MAC 00:08:E1:2A:12:34
        </div>
        <div style={{ fontSize: 22, color: "var(--bz-text)", fontWeight: 600, letterSpacing: -0.4 }}>
          Настройки
        </div>
      </div>
      <StatusStrip orientation="portrait"/>

      <div style={{
        flex: 1, overflow: "auto",
        padding: "14px 14px 14px",
        display: "flex", flexDirection: "column", gap: 12,
      }}>

        {/* Search */}
        <div style={{
          display: "flex", alignItems: "center", gap: 8,
          padding: "11px 14px",
          background: "var(--bz-surface-2)",
          border: "1px solid var(--bz-line-soft)",
          borderRadius: 10,
          color: "var(--bz-text-muted)",
        }}>
          {Ico.search(15)}
          <span style={{ fontSize: 13 }}>Поиск по настройкам</span>
        </div>

        {/* Оповещения */}
        <Card title="Оповещения · уровни тревоги" action={
          <button style={{
            display: "inline-flex", alignItems: "center", gap: 4,
            padding: "4px 8px", borderRadius: 8,
            background: "transparent", border: "1px solid var(--bz-line)",
            color: "var(--bz-text-dim)", cursor: "pointer",
            fontSize: 11, fontWeight: 600, letterSpacing: 0.3,
          }}>{Ico.plus(12)} <span>Уровень</span></button>
        }>
          <AlarmLevelRow level={1} color="var(--bz-warn)" name="Жёлтый" hint="информационный"  value={40}    s v/>
          <AlarmLevelRow level={2} color="#ff6b35"        name="Оранжевый" hint="повышенный фон" value={200}   s v/>
          <AlarmLevelRow level={3} color="var(--bz-danger)" name="Красный" hint="опасно"        value={1000}  s v/>
        </Card>

        {/* Оборудование */}
        <Card title="Оборудование">
          <Row label="Звуковой клик" sub="на каждый импульс детектора"
            action={<Segmented options={[{label:"–",value:"none"},{label:"1/1",value:"1"},{label:"1/10",value:"10"}]} value="none"/>}/>
          <Row label="LED-индикатор" sub="вспышка при импульсе"
            action={<Segmented options={[{label:"–",value:"none"},{label:"1/1",value:"1"},{label:"1/10",value:"10"}]} value="1"/>}/>
          <Row label="Вибрация" sub="при тревоге"
            action={<Toggle on/>}/>
          <Row label="Автозапуск спектрометра" sub="при подключении прибора"
            action={<Toggle on/>}/>
          <Row label="Каналов спектра" sub="разрешение АЦП"
            action={<Segmented options={[{label:"1024",value:"1"},{label:"2048",value:"2"},{label:"4096",value:"4"}]} value="2"/>}/>
        </Card>

        {/* Калибровка */}
        <Card title="Калибровка" action={
          <button style={{
            fontSize: 12, color: "var(--bz-accent)", background: "none",
            border: "none", cursor: "pointer", fontWeight: 600, letterSpacing: 0.3,
          }}>Открыть →</button>
        }>
          <Row label="Полиномиальная Ax² + Bx + C"
            sub="A: 0.00012  ·  B: 1.0042  ·  C: -0.18"/>
          <Row label="Высокое напряжение" value="780 В"/>
          <Row label="Компаратор" value="-70 dBm"/>
        </Card>

        {/* Интерфейс */}
        <Card title="Интерфейс">
          <Row label="Стиль спектра"
            action={<Segmented options={[{label:"Линия",value:"line"},{label:"Заливка",value:"fill"},{label:"Бар",value:"gisto"}]} value="line"/>}/>
          <Row label="Цвет спектра"
            action={<ColorPalette/>}/>
          <Row label="Ночной режим" sub="инверсия контрастности под слабый свет"
            action={<Toggle on/>}/>
          <Row label="Сглаживание SMA" value="3"/>
        </Card>

        <Card title="Действия">
          <Row label="Журнал событий" sub="турн-он, тревоги, write to flash"
            onClick={()=>{}}
            action={<span style={{ color: "var(--bz-text-muted)" }}>{Ico.chevron("right")}</span>}/>
          <Row label="Сохранить профиль настроек"
            action={<span style={{ color:"var(--bz-accent)" }}>{Ico.chevron("right")}</span>} onClick={()=>{}}/>
          <Row label="Сбросить устройство" onClick={()=>{}}
            action={<span style={{ color:"var(--bz-text-muted)" }}>{Ico.chevron("right")}</span>}/>
        </Card>

      </div>

      {navVariant === "bar" && <BottomNav active="settings"/>}
      {navVariant === "fab" && <BottomNav active="settings" variant="fab"/>}
    </div>
  );
}

// Editable alarm level — stepper input + S/V indicators
function AlarmLevelRow({ level, color, name, hint, value, s, v }) {
  return (
    <div style={{
      display: "flex", alignItems: "center", gap: 12,
      padding: "12px 16px", minHeight: 60,
      borderTop: "1px solid var(--bz-divider)",
    }}>
      <span style={{
        flexShrink: 0,
        width: 32, height: 32, borderRadius: 10,
        background: "color-mix(in srgb, " + color + " 16%, transparent)",
        color: color,
        display: "flex", alignItems: "center", justifyContent: "center",
        fontWeight: 700, fontSize: 13, fontFamily: "var(--bz-font-mono)",
      }}>L{level}</span>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 14, color: "var(--bz-text)", fontWeight: 500 }}>{name}</div>
        <div style={{ fontSize: 11, color: "var(--bz-text-muted)", marginTop: 2 }}>{hint}</div>
      </div>
      {/* Stepper */}
      <div style={{
        display: "inline-flex", alignItems: "stretch",
        borderRadius: 9, border: "1px solid var(--bz-line)",
        background: "var(--bz-surface-2)",
        overflow: "hidden",
      }}>
        <button style={{
          width: 30, padding: 0, background: "transparent", border: "none",
          color: "var(--bz-text-dim)", cursor: "pointer", fontSize: 16, fontWeight: 600,
          borderRight: "1px solid var(--bz-line)",
        }}>−</button>
        <span className="bz-mono bz-tabular" style={{
          minWidth: 64, padding: "0 10px",
          display: "inline-flex", alignItems: "center", justifyContent: "flex-end",
          fontSize: 14, fontWeight: 600, color: "var(--bz-text)",
        }}>{value.toLocaleString("ru-RU")}<span style={{
          marginLeft: 4, fontSize: 10, color: "var(--bz-text-muted)", letterSpacing: 0.3,
        }}>мкР/ч</span></span>
        <button style={{
          width: 30, padding: 0, background: "transparent", border: "none",
          color: "var(--bz-text-dim)", cursor: "pointer", fontSize: 16, fontWeight: 600,
          borderLeft: "1px solid var(--bz-line)",
        }}>+</button>
      </div>
      <span style={{ display: "inline-flex", flexDirection: "column", gap: 4 }}>
        <AlarmFlag on={s} label="ЗВУК"/>
        <AlarmFlag on={v} label="ВИБРО"/>
      </span>
    </div>
  );
}

function AlarmFlag({ on, label }) {
  return (
    <span style={{
      display: "inline-flex", alignItems: "center", gap: 3,
      padding: "2px 6px", borderRadius: 5,
      background: on ? "var(--bz-accent-soft)" : "var(--bz-surface-3)",
      color: on ? "var(--bz-accent)" : "var(--bz-text-muted)",
      border: "1px solid " + (on ? "color-mix(in srgb, var(--bz-accent) 30%, transparent)" : "var(--bz-line)"),
      fontSize: 9, fontWeight: 700, letterSpacing: 0.4,
    }}>{label}</span>
  );
}

function AlarmFlags({ s, v }) {
  const dot = (on, label) => (
    <span style={{
      display: "inline-flex", alignItems: "center", gap: 4,
      padding: "3px 8px", borderRadius: 6,
      background: on ? "var(--bz-accent-soft)" : "var(--bz-surface-3)",
      border: `1px solid ${on ? "var(--bz-accent)" : "var(--bz-line)"}`,
      color: on ? "var(--bz-accent)" : "var(--bz-text-muted)",
      fontSize: 10, fontWeight: 700, letterSpacing: 0.5,
    }}>{label}</span>
  );
  return (
    <span style={{ display: "inline-flex", gap: 4 }}>
      {dot(s, "ЗВУК")}
      {dot(v, "ВИБРО")}
    </span>
  );
}


function ColorPalette() {
  const colors = ["#00d4aa", "#4ea3ff", "#facc15", "#ff6b35", "#a3e635"];
  return (
    <span style={{ display: "inline-flex", gap: 6 }}>
      {colors.map((c, i) => (
        <span key={c} style={{
          width: 22, height: 22, borderRadius: "50%",
          background: c,
          boxShadow: i === 0 ? "0 0 0 2px var(--bz-bg), 0 0 0 4px var(--bz-accent)" : undefined,
        }}/>
      ))}
    </span>
  );
}

// ════════════════════════════════════════════════════════════════
// 4) OVERLOAD STATE — PORTRAIT (already shown via overload prop above,
//    but this is a dedicated artboard showing the full state)
// ════════════════════════════════════════════════════════════════
function ScreenOverload({ navVariant = "bar" }) {
  return (
    <ScreenSpectrumPortrait overload navVariant={navVariant} spectrumColor="var(--bz-danger)"/>
  );
}

// ════════════════════════════════════════════════════════════════
// 5) HISTORY — accumulated spectrum
// ════════════════════════════════════════════════════════════════
function _Legacy_ScreenHistory({ navVariant = "bar", theme = "dark" }) {
  return (
    <div className="bz-screen" style={{
      flex: 1, display: "flex", flexDirection: "column",
      paddingTop: "var(--bz-inset-top)",
    }}>
      <div style={{ padding: "12px 18px 8px" }}>
        <div style={{ fontSize: 11, color: "var(--bz-text-muted)", letterSpacing: 1.4, textTransform: "uppercase", fontWeight: 600 }}>
          История · накопленный спектр
        </div>
        <div style={{ fontSize: 22, color: "var(--bz-text)", fontWeight: 600, letterSpacing: -0.4 }}>
          7ч 19м 22с
        </div>
      </div>
      <StatusStrip orientation="portrait"/>

      <div style={{
        padding: "14px 18px 8px",
        display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16,
      }}>
        <Readout label="ИНТЕГРАЛ" value="10 623 119" unit="имп" accent large/>
        <Readout label="СР. CPS" value="15.75" unit="имп/с" large/>
      </div>

      <div style={{
        flex: 1, margin: "4px 14px 12px",
        background: "var(--bz-surface)",
        border: "1px solid var(--bz-line-soft)",
        borderRadius: "var(--bz-r)",
        padding: "12px 12px 8px",
        display: "flex", flexDirection: "column",
      }}>
        <div style={{
          display: "flex", justifyContent: "space-between",
          fontSize: 10, color: "var(--bz-text-muted)",
          fontFamily: "var(--bz-font-mono)", letterSpacing: 0.5,
          marginBottom: 6,
        }}>
          <span>LOG · cumulative</span>
          <span>2048 ch · с 23-05 15:31</span>
        </div>
        <div style={{ flex: 1, display: "flex" }}>
          <SpectrumChart theme={theme} width={344}
            height={360}
            log
            style="fill"
            color="#4ea3ff"
            secondary="#00d4aa"
            channels={2048}
          />
        </div>
        {/* Legend */}
        <div style={{
          display: "flex", gap: 18, marginTop: 8, paddingTop: 10,
          borderTop: "1px solid var(--bz-divider)",
          fontSize: 11,
        }}>
          <span style={{ display: "flex", alignItems: "center", gap: 6, color: "var(--bz-text-dim)" }}>
            <span style={{ width: 10, height: 10, borderRadius: 2, background: "#4ea3ff" }}/>
            Накопленный
          </span>
          <span style={{ display: "flex", alignItems: "center", gap: 6, color: "var(--bz-text-dim)" }}>
            <span style={{ width: 10, height: 10, borderRadius: 2, background: "#00d4aa", opacity: 0.6 }}/>
            Фон
          </span>
          <div style={{ flex: 1 }}/>
          <IconButton icon={Ico.save(14)} size={32}/>
        </div>
      </div>

      {navVariant === "bar" && <BottomNav active="history"/>}
      {navVariant === "fab" && <BottomNav active="history" variant="fab"/>}
    </div>
  );
}

// ════════════════════════════════════════════════════════════════
// 6) DOSE — CPS oscilloscope
// ════════════════════════════════════════════════════════════════
function _Legacy_ScreenDose({ navVariant = "bar", theme = "dark" }) {
  return (
    <div className="bz-screen" style={{
      flex: 1, display: "flex", flexDirection: "column",
      paddingTop: "var(--bz-inset-top)",
    }}>
      <div style={{ padding: "12px 18px 8px" }}>
        <div style={{ fontSize: 11, color: "var(--bz-text-muted)", letterSpacing: 1.4, textTransform: "uppercase", fontWeight: 600 }}>
          Дозиметр · CPS мониторинг
        </div>
        <div style={{ fontSize: 22, color: "var(--bz-text)", fontWeight: 600, letterSpacing: -0.4 }}>
          В норме
        </div>
      </div>
      <StatusStrip orientation="portrait"/>

      {/* Hero gauge — big number with level indicator */}
      <div style={{
        margin: "14px 16px 8px",
        padding: "18px 18px 16px",
        background: "var(--bz-surface)",
        border: "1px solid var(--bz-line-soft)",
        borderRadius: "var(--bz-r)",
      }}>
        <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", marginBottom: 14 }}>
          <div>
            <div style={{ fontSize: 11, color: "var(--bz-text-muted)", letterSpacing: 1.4, textTransform: "uppercase", fontWeight: 600 }}>
              Текущая мощность дозы
            </div>
            <div style={{ marginTop: 6, display: "flex", alignItems: "baseline", gap: 6 }}>
              <span className="bz-mono bz-tabular" style={{
                fontSize: 56, lineHeight: 1, fontWeight: 600,
                color: "var(--bz-accent)", letterSpacing: -1.5,
              }}>12.44</span>
              <span style={{ fontSize: 14, color: "var(--bz-text-dim)", fontFamily: "var(--bz-font-mono)" }}>мкР/ч</span>
            </div>
            <div style={{ fontSize: 12, color: "var(--bz-text-muted)", marginTop: 6, fontFamily: "var(--bz-font-mono)" }}>
              Среднее за 60с: <span style={{ color: "var(--bz-text-dim)" }}>13.99</span>
            </div>
          </div>
          <div style={{
            display: "flex", flexDirection: "column", alignItems: "flex-end", gap: 6,
          }}>
            <span style={{
              padding: "4px 10px", borderRadius: 6,
              background: "rgba(0,212,170,0.14)", color: "var(--bz-accent)",
              fontSize: 11, fontWeight: 700, letterSpacing: 0.5,
            }}>NORMAL</span>
            <span style={{ fontSize: 11, color: "var(--bz-text-muted)", fontFamily: "var(--bz-font-mono)" }}>
              31% от L1
            </span>
          </div>
        </div>

        {/* Levels gauge */}
        <LevelGauge value={12.44} l1={40} l2={200} l3={1000}/>
      </div>

      {/* CPS oscilloscope */}
      <div style={{
        flex: 1, margin: "0 16px 12px",
        background: "var(--bz-surface)",
        border: "1px solid var(--bz-line-soft)",
        borderRadius: "var(--bz-r)",
        padding: "14px 14px 10px",
        display: "flex", flexDirection: "column", minHeight: 0,
      }}>
        <div style={{
          display: "flex", justifyContent: "space-between", alignItems: "center",
          fontSize: 10, color: "var(--bz-text-muted)",
          fontFamily: "var(--bz-font-mono)", letterSpacing: 0.5,
          marginBottom: 6,
        }}>
          <span>CPS · последние 60 секунд</span>
          <span style={{ display: "flex", gap: 10 }}>
            <span style={{ color: "var(--bz-accent)" }}>━━ мгновенно</span>
            <span style={{ color: "var(--bz-warn)" }}>━━ среднее</span>
          </span>
        </div>
        <div style={{ flex: 1, display: "flex" }}>
          <CpsOscilloscope theme={theme} width={344} height={210} color="var(--bz-accent)" smoothColor="var(--bz-warn)"/>
        </div>
      </div>

      {navVariant === "bar" && <BottomNav active="dose"/>}
      {navVariant === "fab" && <BottomNav active="dose" variant="fab"/>}
    </div>
  );
}

function LevelGauge({ value, l1, l2, l3 }) {
  // log-scaled bar with 3 thresholds
  const max = l3 * 1.1;
  const lg = (v) => Math.log10(v + 1) / Math.log10(max + 1);
  const pct = (v) => Math.min(1, Math.max(0, lg(v))) * 100;
  return (
    <div>
      <div style={{
        height: 10, borderRadius: 5, overflow: "hidden",
        background: "var(--bz-surface-3)", position: "relative",
        border: "1px solid var(--bz-line)",
      }}>
        {/* segment colors */}
        <div style={{ position: "absolute", left: 0,         width: `${pct(l1)}%`, top: 0, bottom: 0, background: "var(--bz-accent)", opacity: 0.4 }}/>
        <div style={{ position: "absolute", left: `${pct(l1)}%`, width: `${pct(l2) - pct(l1)}%`, top: 0, bottom: 0, background: "var(--bz-warn)", opacity: 0.4 }}/>
        <div style={{ position: "absolute", left: `${pct(l2)}%`, width: `${pct(l3) - pct(l2)}%`, top: 0, bottom: 0, background: "#ff6b35", opacity: 0.4 }}/>
        <div style={{ position: "absolute", left: `${pct(l3)}%`, right: 0, top: 0, bottom: 0, background: "var(--bz-danger)", opacity: 0.4 }}/>

        {/* current */}
        <div style={{
          position: "absolute", left: `calc(${pct(value)}% - 1px)`, top: -3, bottom: -3,
          width: 2, background: "var(--bz-text)",
          boxShadow: "0 0 0 1px var(--bz-bg)",
        }}/>
      </div>
      <div style={{
        display: "flex", justifyContent: "space-between",
        marginTop: 6, fontSize: 10, color: "var(--bz-text-muted)",
        fontFamily: "var(--bz-font-mono)", letterSpacing: 0.4,
      }}>
        <span>0</span>
        <span style={{ color: "var(--bz-accent)" }}>L1 · 40</span>
        <span style={{ color: "var(--bz-warn)" }}>L2 · 200</span>
        <span style={{ color: "#ff6b35" }}>L3 · 1k</span>
        <span style={{ color: "var(--bz-danger)" }}>∞</span>
      </div>
    </div>
  );
}

// ════════════════════════════════════════════════════════════════
// 7) MAP — GPS radiation track
// ════════════════════════════════════════════════════════════════
function _Legacy_ScreenMap({ navVariant = "bar" }) {
  return (
    <div className="bz-screen" style={{
      flex: 1, display: "flex", flexDirection: "column",
      paddingTop: "var(--bz-inset-top)",
      position: "relative",
    }}>
      {/* Map "tiles" — placeholder grid (theme-aware via currentColor) */}
      <div style={{
        position: "absolute", inset: 0,
        background: "var(--bz-surface-2)",
        color: "var(--bz-text-muted)",
      }}>
        <svg width="100%" height="100%" style={{ position: "absolute", inset: 0, color: "currentColor" }}>
          <defs>
            <pattern id="bz-mapgrid" width="32" height="32" patternUnits="userSpaceOnUse">
              <path d="M 32 0 L 0 0 0 32" fill="none" stroke="currentColor" strokeWidth="1" strokeOpacity="0.18"/>
            </pattern>
            <pattern id="bz-mapdots" width="80" height="80" patternUnits="userSpaceOnUse">
              <circle cx="40" cy="40" r="0.8" fill="currentColor" fillOpacity="0.25"/>
            </pattern>
          </defs>
          <rect width="100%" height="100%" fill="url(#bz-mapgrid)"/>
          <rect width="100%" height="100%" fill="url(#bz-mapdots)"/>

          {/* roads */}
          <path d="M -10 600 Q 200 400 412 380" stroke="currentColor" strokeOpacity="0.32" strokeWidth="14" fill="none"/>
          <path d="M -10 600 Q 200 400 412 380" stroke="currentColor" strokeOpacity="0.18" strokeWidth="2" fill="none"/>
          <path d="M 130 -10 L 200 900" stroke="currentColor" strokeOpacity="0.28" strokeWidth="10" fill="none"/>
          <path d="M 350 -10 Q 320 300 200 540 L 80 800" stroke="currentColor" strokeOpacity="0.22" strokeWidth="8" fill="none"/>

          {/* GPS track */}
          <path
            d="M 60 720 L 110 670 L 140 600 L 170 540 L 210 470 L 220 410 L 250 350 L 280 290 L 330 240 L 360 180"
            stroke="var(--bz-accent)" strokeWidth="3" fill="none" strokeLinecap="round" strokeLinejoin="round"
            strokeOpacity="0.95"
          />
          <path
            d="M 60 720 L 110 670 L 140 600 L 170 540 L 210 470 L 220 410 L 250 350 L 280 290 L 330 240 L 360 180"
            stroke="var(--bz-accent)" strokeWidth="10" fill="none" strokeLinecap="round" strokeLinejoin="round"
            strokeOpacity="0.18"
          />

          {/* radiation markers — color by level */}
          {[
            [60, 720, "#00d4aa", "9"],
            [140, 600, "#00d4aa", "12"],
            [210, 470, "#facc15", "48"],
            [250, 350, "#ff6b35", "210"],
            [280, 290, "#ff6b35", "180"],
            [330, 240, "#facc15", "75"],
            [360, 180, "#00d4aa", "15"],
          ].map(([x, y, c, v]) => (
            <g key={x}>
              <circle cx={x} cy={y} r="12" fill={c} fillOpacity="0.18"/>
              <circle cx={x} cy={y} r="6" fill={c} stroke="var(--bz-bg)" strokeWidth="1.5"/>
            </g>
          ))}

          {/* current location */}
          <g>
            <circle cx="360" cy="180" r="20" fill="var(--bz-info)" fillOpacity="0.12"/>
            <circle cx="360" cy="180" r="10" fill="var(--bz-info)" fillOpacity="0.4"/>
            <circle cx="360" cy="180" r="5" fill="var(--bz-info)" stroke="var(--bz-bg)" strokeWidth="2"/>
          </g>
        </svg>
      </div>

      {/* Header — overlay */}
      <div style={{
        position: "relative", zIndex: 2,
        padding: "12px 16px 10px",
        background: "linear-gradient(180deg, color-mix(in srgb, var(--bz-bg) 92%, transparent) 0%, color-mix(in srgb, var(--bz-bg) 60%, transparent) 75%, transparent 100%)",
      }}>
        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 11, color: "var(--bz-text-muted)", letterSpacing: 1.4, textTransform: "uppercase", fontWeight: 600 }}>
              Карта · GPS трек
            </div>
            <div style={{ fontSize: 18, color: "var(--bz-text)", fontWeight: 600, letterSpacing: -0.3 }}>
              Track 23-05-2026
            </div>
          </div>
          <span style={{
            display: "inline-flex", alignItems: "center", gap: 6,
            padding: "5px 9px 5px 8px", borderRadius: "var(--bz-r-pill)",
            background: "rgba(255,64,64,0.16)", color: "var(--bz-danger)",
            fontSize: 11, fontWeight: 700, letterSpacing: 0.5,
          }}>
            <span style={{
              width: 7, height: 7, borderRadius: 4, background: "var(--bz-danger)",
              animation: "bz-pulse 1s ease-in-out infinite",
            }}/>
            REC
          </span>
        </div>
      </div>

      {/* Bottom card — track stats */}
      <div style={{ flex: 1 }}/>
      <div style={{
        position: "relative", zIndex: 2,
        margin: "0 14px 12px",
        padding: "12px 14px",
        background: "color-mix(in srgb, var(--bz-surface) 92%, transparent)",
        backdropFilter: "blur(12px)",
        border: "1px solid var(--bz-line)",
        borderRadius: "var(--bz-r)",
      }}>
        <div style={{
          display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 10,
        }}>
          <Readout label="ДОЗА" value="12.44" unit="мкР/ч" accent/>
          <Readout label="MAX" value="210" unit="мкР/ч"/>
          <Readout label="ПУТЬ" value="1.84" unit="км"/>
        </div>
      </div>

      {/* Floating controls — right */}
      <div style={{
        position: "absolute", right: 14, bottom: 96, zIndex: 3,
        display: "flex", flexDirection: "column", gap: 10,
      }}>
        <button style={{
          width: 48, height: 48, borderRadius: 14,
          background: "color-mix(in srgb, var(--bz-surface) 92%, transparent)",
          backdropFilter: "blur(8px)",
          border: "1px solid var(--bz-line)",
          color: "var(--bz-info)", cursor: "pointer",
          display: "flex", alignItems: "center", justifyContent: "center",
        }}>{Ico.target(22)}</button>
        <button style={{
          width: 48, height: 48, borderRadius: 14,
          background: "color-mix(in srgb, var(--bz-surface) 92%, transparent)",
          backdropFilter: "blur(8px)",
          border: "1px solid var(--bz-line)",
          color: "var(--bz-accent)", cursor: "pointer",
          display: "flex", alignItems: "center", justifyContent: "center",
        }}>{Ico.pin(22)}</button>
      </div>

      {navVariant === "bar" && <BottomNav active="map"/>}
      {navVariant === "fab" && <BottomNav active="map" variant="fab"/>}
    </div>
  );
}

// ════════════════════════════════════════════════════════════════
// 8) CALIBRATION — settings sub-page
// ════════════════════════════════════════════════════════════════
function ScreenCalibration({ navVariant = "bar" }) {
  return (
    <div className="bz-screen" style={{
      flex: 1, display: "flex", flexDirection: "column",
      paddingTop: "var(--bz-inset-top)",
    }}>
      {/* Top: back + title */}
      <div style={{
        display: "flex", alignItems: "center", gap: 10,
        padding: "12px 16px 8px",
      }}>
        <button style={{
          width: 40, height: 40, borderRadius: 12,
          background: "var(--bz-surface-2)", border: "1px solid var(--bz-line)",
          color: "var(--bz-text)", cursor: "pointer",
          display: "flex", alignItems: "center", justifyContent: "center",
        }}>{Ico.chevron("left", 16)}</button>
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 11, color: "var(--bz-text-muted)", letterSpacing: 1.4, textTransform: "uppercase", fontWeight: 600 }}>
            Настройки  /  Калибровка
          </div>
          <div style={{ fontSize: 20, color: "var(--bz-text)", fontWeight: 600, letterSpacing: -0.3 }}>
            Энергетическая шкала
          </div>
        </div>
      </div>
      <StatusStrip orientation="portrait"/>

      <div style={{
        flex: 1, overflow: "auto",
        padding: "14px 14px 14px",
        display: "flex", flexDirection: "column", gap: 12,
      }}>

        {/* Polynomial preview */}
        <div style={{
          padding: "16px 16px 14px",
          background: "var(--bz-surface)",
          border: "1px solid var(--bz-line-soft)",
          borderRadius: "var(--bz-r)",
        }}>
          <div style={{ fontSize: 11, color: "var(--bz-text-muted)", letterSpacing: 1.4, textTransform: "uppercase", fontWeight: 600, marginBottom: 10 }}>
            Полином Ax² + Bx + C
          </div>
          <div className="bz-mono" style={{
            fontSize: 16, color: "var(--bz-text)", letterSpacing: -0.2,
            display: "flex", alignItems: "baseline", gap: 4, flexWrap: "wrap",
          }}>
            E (keV) =
            <span style={{ color: "var(--bz-accent)" }}>0.000 12</span>·N²
            <span style={{ color: "var(--bz-text-muted)" }}>+</span>
            <span style={{ color: "var(--bz-accent)" }}>1.0042</span>·N
            <span style={{ color: "var(--bz-text-muted)" }}>+</span>
            <span style={{ color: "var(--bz-accent)" }}>−0.18</span>
          </div>
          <div style={{ fontSize: 12, color: "var(--bz-text-muted)", marginTop: 8 }}>
            Cs-137 пик 662 keV → канал 658 (ошибка 0.6%)
          </div>
        </div>

        <Card title="Коэффициенты">
          <CalRow letter="A" value="0.000 12" unit="keV/N²" tone="accent"/>
          <CalRow letter="B" value="1.0042" unit="keV/N" tone="accent"/>
          <CalRow letter="C" value="−0.18" unit="keV" tone="accent"/>
        </Card>

        <Card title="Опорные пики">
          <CalRow letter="●" value="662" unit="keV · Cs-137" sub="канал 658"/>
          <CalRow letter="●" value="1 460" unit="keV · K-40" sub="канал 1452"/>
          <CalRow letter="+" value="Добавить пик" sub="ручная привязка" tone="muted"/>
        </Card>

        <Card title="Действия">
          <Row label="Автокалибровка" sub="по обнаруженным пикам"
            action={<button style={{ padding:"6px 12px", borderRadius:8, border:"1px solid var(--bz-accent)", background:"var(--bz-accent-soft)", color:"var(--bz-accent)", fontWeight:600, fontSize:12, cursor:"pointer" }}>Запустить</button>}/>
          <Row label="Записать в прибор" onClick={()=>{}}
            action={<span style={{ color: "var(--bz-accent)" }}>{Ico.chevron("right")}</span>}/>
          <Row label="Считать из прибора" onClick={()=>{}}
            action={<span style={{ color: "var(--bz-text-muted)" }}>{Ico.chevron("right")}</span>}/>
        </Card>
      </div>

      {navVariant === "bar" && <BottomNav active="settings"/>}
      {navVariant === "fab" && <BottomNav active="settings" variant="fab"/>}
    </div>
  );
}

function CalRow({ letter, value, unit, sub, tone = "default" }) {
  const color = tone === "accent" ? "var(--bz-accent)" :
                tone === "muted"  ? "var(--bz-text-muted)" :
                "var(--bz-text)";
  return (
    <div style={{
      display: "flex", alignItems: "center", gap: 14,
      padding: "13px 16px", minHeight: 56,
      borderTop: "1px solid var(--bz-divider)",
    }}>
      <span style={{
        width: 32, height: 32, borderRadius: 10,
        background: tone === "accent" ? "var(--bz-accent-soft)" : "var(--bz-surface-3)",
        color: tone === "accent" ? "var(--bz-accent)" : "var(--bz-text-dim)",
        display: "flex", alignItems: "center", justifyContent: "center",
        fontSize: 16, fontWeight: 700, fontFamily: "var(--bz-font-mono)",
      }}>{letter}</span>
      <div style={{ flex: 1 }}>
        <div className="bz-mono" style={{ fontSize: 15, color, fontWeight: 600 }}>
          {value} {unit && <span style={{ color: "var(--bz-text-muted)", fontWeight: 500, marginLeft: 4 }}>{unit}</span>}
        </div>
        {sub && <div style={{ fontSize: 12, color: "var(--bz-text-muted)", marginTop: 2 }}>{sub}</div>}
      </div>
    </div>
  );
}

// ════════════════════════════════════════════════════════════════
// 9) LOG — diagnostic event journal (accessed from Settings)
// ════════════════════════════════════════════════════════════════
function _Legacy_ScreenLog({ navVariant = "bar" }) {
  const events = [
    { t: "23-05 15:31:34", kind: "info", text: "Start spectrometer" },
    { t: "23-05 15:18:18", kind: "info", text: "Stop spectrometer" },
    { t: "18-05 16:24:12", kind: "info", text: "Start spectrometer" },
    { t: "18-05 14:51:59", kind: "ok",   text: "Normal" },
    { t: "18-05 14:51:58", kind: "warn", text: "Level 1" },
    { t: "18-05 13:32:58", kind: "ok",   text: "Normal" },
    { t: "18-05 13:32:57", kind: "warn", text: "Level 1" },
    { t: "16-05 20:39:33", kind: "muted", text: "Write to flash" },
    { t: "16-05 20:39:12", kind: "muted", text: "Write to flash" },
    { t: "15-05 21:11:35", kind: "muted", text: "Write to flash" },
    { t: "15-05 20:41:27", kind: "muted", text: "Write to flash" },
    { t: "15-05 20:31:49", kind: "muted", text: "Write to flash" },
    { t: "15-05 20:28:29", kind: "info", text: "Stop spectrometer" },
    { t: "15-05 20:28:27", kind: "muted", text: "Write to flash" },
    { t: "15-05 20:28:25", kind: "ok",   text: "Normal" },
    { t: "15-05 20:28:22", kind: "warn", text: "Level 1" },
    { t: "15-05 20:28:20", kind: "ok",   text: "Normal" },
    { t: "15-05 20:26:20", kind: "warn", text: "Level 1" },
    { t: "15-05 20:26:04", kind: "ok",   text: "Normal" },
    { t: "15-05 20:26:03", kind: "info", text: "Dosimeter reset" },
    { t: "15-05 20:26:03", kind: "info", text: "Start spectrometer" },
    { t: "15-05 20:26:03", kind: "info", text: "Turn on" },
  ];
  const colorFor = (k) => ({
    ok:    "var(--bz-accent)",
    warn:  "var(--bz-warn)",
    danger:"var(--bz-danger)",
    info:  "var(--bz-text)",
    muted: "var(--bz-text-muted)",
  }[k] || "var(--bz-text)");

  return (
    <div className="bz-screen" style={{
      flex: 1, display: "flex", flexDirection: "column",
      paddingTop: "var(--bz-inset-top)",
    }}>
      {/* Top: back + title */}
      <div style={{
        display: "flex", alignItems: "center", gap: 10,
        padding: "12px 16px 8px",
      }}>
        <button style={{
          width: 40, height: 40, borderRadius: 12,
          background: "var(--bz-surface-2)", border: "1px solid var(--bz-line)",
          color: "var(--bz-text)", cursor: "pointer",
          display: "flex", alignItems: "center", justifyContent: "center",
        }}>{Ico.chevron("left", 16)}</button>
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 11, color: "var(--bz-text-muted)", letterSpacing: 1.4, textTransform: "uppercase", fontWeight: 600 }}>
            Настройки  /  Журнал
          </div>
          <div style={{ fontSize: 20, color: "var(--bz-text)", fontWeight: 600, letterSpacing: -0.3 }}>
            События прибора
          </div>
        </div>
        <IconButton icon={Ico.save(16)} size={40}/>
        <IconButton icon={Ico.clear(16)} size={40}/>
      </div>
      <StatusStrip orientation="portrait"/>

      {/* Filters */}
      <div style={{ padding: "12px 14px 8px", display: "flex", gap: 6, flexWrap: "wrap" }}>
        {["Все · 22", "Тревоги · 4", "Запись · 6", "Питание · 5"].map((c, i) => (
          <span key={c} style={{
            padding: "5px 10px", borderRadius: 8,
            background: i === 0 ? "var(--bz-accent-soft)" : "var(--bz-surface-2)",
            border: `1px solid ${i === 0 ? "var(--bz-accent)" : "var(--bz-line)"}`,
            color: i === 0 ? "var(--bz-accent)" : "var(--bz-text-dim)",
            fontSize: 11, fontWeight: 600,
          }}>{c}</span>
        ))}
      </div>

      {/* Timeline */}
      <div style={{
        flex: 1, overflow: "auto",
        margin: "4px 14px 12px",
        background: "var(--bz-surface)",
        border: "1px solid var(--bz-line-soft)",
        borderRadius: "var(--bz-r)",
        padding: "4px 0",
      }}>
        {events.map((e, i) => (
          <div key={i} style={{
            display: "flex", alignItems: "center", gap: 12,
            padding: "10px 16px",
            borderTop: i ? "1px solid var(--bz-divider)" : "none",
            fontFamily: "var(--bz-font-mono)",
          }}>
            <span style={{ fontSize: 11, color: "var(--bz-text-muted)", letterSpacing: 0.3, minWidth: 116 }}>
              {e.t}
            </span>
            <span style={{
              width: 6, height: 6, borderRadius: 4, background: colorFor(e.kind), flexShrink: 0,
            }}/>
            <span style={{ fontSize: 13, color: colorFor(e.kind), fontWeight: e.kind === "ok" || e.kind === "warn" ? 600 : 500 }}>
              {e.text}
            </span>
          </div>
        ))}
      </div>

      {navVariant === "bar" && <BottomNav active="settings"/>}
      {navVariant === "fab" && <BottomNav active="settings" variant="fab"/>}
    </div>
  );
}

Object.assign(window, {
   ScreenSpectrumLandscape,
   ScreenOverload,
     ScreenCalibration, 
  OverloadBanner, LevelGauge, AlarmFlags, AlarmFlag, AlarmLevelRow, ColorPalette, CalRow });
