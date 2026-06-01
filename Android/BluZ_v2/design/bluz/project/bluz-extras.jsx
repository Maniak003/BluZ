// BluZ extras — fully-featured screens preserving 100% of the legacy
// app functionality. These override the earlier Screen* exports.

// ───── DoseHistogram — 512-channel dose distribution bars ─────
function DoseHistogram({ width = 360, height = 200, channels = 512, theme = "dark" }) {
  const data = React.useMemo(() => {
    const arr = new Array(channels).fill(0);
    for (let i = 0; i < channels; i++) {
      const x = i / channels;
      // smooth bell + secondary lobe
      let v = 220 * Math.exp(-Math.pow((i - 110) / 35, 2));
      v +=    90 * Math.exp(-Math.pow((i - 250) / 40, 2));
      v +=    35 * Math.exp(-Math.pow((i - 380) / 55, 2));
      const seed = Math.sin(i * 9.183) * 43758.5453;
      v += (seed - Math.floor(seed)) * v * 0.35;
      arr[i] = Math.max(0.5, v);
    }
    return arr;
  }, [channels]);
  const max = Math.max(...data) * 1.05;
  const pad = 8;
  const w = width - pad * 2, h = height - pad * 2;
  const bw = w / data.length;
  return (
    <svg width={width} height={height} viewBox={`0 0 ${width} ${height}`} style={{ display:"block", color:"var(--bz-text-muted)" }}>
      <g transform={`translate(${pad} ${pad})`}>
        <g stroke="currentColor" strokeOpacity="0.18" strokeWidth="1">
          {[0.25, 0.5, 0.75].map((p) => (
            <line key={p} x1="0" y1={h * p} x2={w} y2={h * p}/>
          ))}
        </g>
        {data.map((v, i) => {
          const x = i * bw;
          const bh = (v / max) * h;
          return <rect key={i} x={x} y={h - bh} width={Math.max(0.6, bw - 0.2)} height={bh}
            fill={theme === "light" ? "#006b54" : "#00d4aa"} fillOpacity="0.85"/>;
        })}
        <text x="0" y={h + 12} fill="currentColor" fontSize="10" fontWeight="600" fontFamily="JetBrains Mono, ui-monospace, monospace">ch 0</text>
        <text x={w} y={h + 12} fill="currentColor" fontSize="10" fontWeight="600" fontFamily="JetBrains Mono, ui-monospace, monospace" textAnchor="end">ch {channels}</text>
      </g>
    </svg>
  );
}

// ───── DOSE SCREEN — histogram + min/max + Clear (matches legacy) ─────
function ScreenDose({ navVariant = "bar", theme = "dark" }) {
  return (
    <div className="bz-screen" style={{
      flex: 1, display: "flex", flexDirection: "column",
      paddingTop: "var(--bz-inset-top)",
    }}>
      <div style={{ padding: "12px 18px 8px", display: "flex", alignItems: "center", gap: 10 }}>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 11, color: "var(--bz-text-muted)", letterSpacing: 1.4, textTransform: "uppercase", fontWeight: 600 }}>
            Дозиметр · 512 каналов
          </div>
          <div style={{ fontSize: 22, color: "var(--bz-text)", fontWeight: 600, letterSpacing: -0.4 }}>
            В норме
          </div>
        </div>
        <IconButton icon={Ico.clear(16)} label="Clear" size={40}/>
      </div>
      <StatusStrip orientation="portrait"/>

      {/* Hero: dose rate + level gauge */}
      <div style={{
        margin: "14px 16px 8px",
        padding: "16px 16px 14px",
        background: "var(--bz-surface)",
        border: "1px solid var(--bz-line-soft)",
        borderRadius: "var(--bz-r)",
      }}>
        <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", marginBottom: 12 }}>
          <div>
            <div style={{ fontSize: 11, color: "var(--bz-text-muted)", letterSpacing: 1.4, textTransform: "uppercase", fontWeight: 600 }}>
              Мощность дозы
            </div>
            <div style={{ marginTop: 4, display: "flex", alignItems: "baseline", gap: 6 }}>
              <span className="bz-mono bz-tabular" style={{
                fontSize: 48, lineHeight: 1, fontWeight: 600,
                color: "var(--bz-accent)", letterSpacing: -1.2,
              }}>12.44</span>
              <span style={{ fontSize: 13, color: "var(--bz-text-dim)", fontFamily: "var(--bz-font-mono)" }}>мкР/ч</span>
            </div>
            <div style={{ fontSize: 12, color: "var(--bz-text-muted)", marginTop: 4, fontFamily: "var(--bz-font-mono)" }}>
              Avg 13.99 · CPS 156 · AvgCPS 423.5
            </div>
          </div>
          <span style={{
            padding: "4px 10px", borderRadius: 6,
            background: "var(--bz-accent-soft)", color: "var(--bz-accent)",
            fontSize: 11, fontWeight: 700, letterSpacing: 0.5,
          }}>NORMAL</span>
        </div>
        <LevelGauge value={12.44} l1={40} l2={200} l3={1000}/>
      </div>

      {/* Histogram */}
      <div style={{
        flex: 1, margin: "0 16px 12px",
        background: "var(--bz-surface)",
        border: "1px solid var(--bz-line-soft)",
        borderRadius: "var(--bz-r)",
        padding: "12px 14px 10px",
        display: "flex", flexDirection: "column", minHeight: 0,
      }}>
        <div style={{
          display: "flex", justifyContent: "space-between", alignItems: "center",
          marginBottom: 8,
        }}>
          <span style={{
            fontSize: 11, color: "var(--bz-text-muted)", letterSpacing: 0.5,
            fontFamily: "var(--bz-font-mono)", textTransform: "uppercase", fontWeight: 600,
          }}>Распределение дозы · 512 ch</span>
          <span style={{ display: "flex", gap: 12, fontSize: 11, fontFamily: "var(--bz-font-mono)" }}>
            <span style={{ color: "var(--bz-text-muted)" }}>MIN <span style={{ color: "var(--bz-text)" }}>0.4</span></span>
            <span style={{ color: "var(--bz-text-muted)" }}>MAX <span style={{ color: "var(--bz-text)" }}>231</span></span>
          </span>
        </div>
        <div style={{ flex: 1, display: "flex", alignItems: "stretch" }}>
          <DoseHistogram width={344} height={220} theme={theme}/>
        </div>
      </div>

      {navVariant === "bar" && <BottomNav active="dose"/>}
      {navVariant === "fab" && <BottomNav active="dose" variant="fab"/>}
    </div>
  );
}

// ───── LOG SCREEN — dual journals (device events + app log) ─────
function ScreenLog({ navVariant = "bar" }) {
  const [tab, setTab] = React.useState("device");
  const deviceEvents = [
    { t: "23-05 15:31:34", kind: "info",    text: "Start spectrometer" },
    { t: "23-05 15:18:18", kind: "info",    text: "Stop spectrometer" },
    { t: "18-05 16:24:12", kind: "info",    text: "Start spectrometer" },
    { t: "18-05 14:51:59", kind: "normal",  text: "Normal" },
    { t: "18-05 14:51:58", kind: "L1",      text: "Level 1" },
    { t: "18-05 13:32:58", kind: "normal",  text: "Normal" },
    { t: "18-05 13:32:57", kind: "L1",      text: "Level 1" },
    { t: "16-05 20:39:33", kind: "flash",   text: "Write to flash" },
    { t: "16-05 20:39:12", kind: "flash",   text: "Write to flash" },
    { t: "15-05 21:11:35", kind: "overload",text: "Overload" },
    { t: "15-05 20:41:27", kind: "L3",      text: "Level 3" },
    { t: "15-05 20:31:49", kind: "L2",      text: "Level 2" },
    { t: "15-05 20:28:29", kind: "info",    text: "Stop spectrometer" },
    { t: "15-05 20:28:27", kind: "flash",   text: "Write to flash" },
    { t: "15-05 20:28:25", kind: "normal",  text: "Normal" },
    { t: "15-05 20:28:22", kind: "L1",      text: "Level 1" },
    { t: "15-05 20:28:20", kind: "normal",  text: "Normal" },
    { t: "15-05 20:26:20", kind: "L1",      text: "Level 1" },
    { t: "15-05 20:26:04", kind: "normal",  text: "Normal" },
    { t: "15-05 20:26:03", kind: "info",    text: "Dosimeter reset" },
    { t: "15-05 20:26:03", kind: "battery", text: "Low battery" },
    { t: "15-05 20:26:03", kind: "info",    text: "Turn on" },
  ];
  const appEvents = [
    { t: "15:31:34.221", lvl: "INFO",  text: "BLE connected · BluZ·2A12" },
    { t: "15:31:34.198", lvl: "OK",    text: "Subscribed to spectrum char" },
    { t: "15:31:34.090", lvl: "INFO",  text: "Sending START packet" },
    { t: "15:31:33.870", lvl: "DBG",   text: "Read cal: A=0.00012 B=1.0042" },
    { t: "15:31:33.510", lvl: "WARN",  text: "Slow notify · 412ms" },
    { t: "15:31:32.110", lvl: "INFO",  text: "Detector profile: SiPM-A" },
    { t: "15:31:30.812", lvl: "ERR",   text: "GATT 0x85 retry 1/3" },
    { t: "15:31:30.300", lvl: "DBG2",  text: "Packet=97 byte 3=0x10" },
    { t: "15:31:30.110", lvl: "INFO",  text: "BLE scan match RSSI=-62" },
    { t: "15:31:29.880", lvl: "OK",    text: "Permissions granted" },
    { t: "15:31:29.605", lvl: "INFO",  text: "App started" },
  ];

  const deviceColor = (k) => ({
    info:    "var(--bz-text)",
    normal:  "var(--bz-accent)",
    L1:      "var(--bz-warn)",
    L2:      "var(--bz-danger)",
    L3:      "#c084ff",
    overload:"#ff4dd0",
    battery: "#ff4dd0",
    flash:   "var(--bz-text-muted)",
    upgrade: "#ff4dd0",
  }[k] || "var(--bz-text)");

  const appColor = (l) => ({
    ERR:  "var(--bz-danger)",
    INFO: "var(--bz-info)",
    WARN: "var(--bz-warn)",
    OK:   "var(--bz-accent)",
    DBG:  "var(--bz-text-muted)",
    DBG2: "#c084ff",
  }[l] || "var(--bz-text)");

  return (
    <div className="bz-screen" style={{
      flex: 1, display: "flex", flexDirection: "column",
      paddingTop: "var(--bz-inset-top)",
    }}>
      <div style={{ display: "flex", alignItems: "center", gap: 10, padding: "12px 16px 8px" }}>
        <button style={{
          width: 40, height: 40, borderRadius: 12,
          background: "var(--bz-surface-2)", border: "1px solid var(--bz-line)",
          color: "var(--bz-text)", cursor: "pointer",
          display: "flex", alignItems: "center", justifyContent: "center",
        }}>{Ico.chevron("left", 16)}</button>
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 11, color: "var(--bz-text-muted)", letterSpacing: 1.4, textTransform: "uppercase", fontWeight: 600 }}>
            Настройки  /  Лог
          </div>
          <div style={{ fontSize: 20, color: "var(--bz-text)", fontWeight: 600, letterSpacing: -0.3 }}>
            Журналы событий
          </div>
        </div>
        <IconButton icon={Ico.save(16)} size={40}/>
        <IconButton icon={Ico.clear(16)} size={40}/>
      </div>
      <StatusStrip orientation="portrait"/>

      {/* Tabs */}
      <div style={{ padding: "12px 14px 0", display: "flex", gap: 6 }}>
        <button onClick={() => setTab("device")}
          style={{
            flex: 1, padding: "10px 12px", borderRadius: 10,
            background: tab === "device" ? "var(--bz-surface)" : "transparent",
            border: "1px solid " + (tab === "device" ? "var(--bz-line)" : "transparent"),
            color: tab === "device" ? "var(--bz-text)" : "var(--bz-text-muted)",
            fontWeight: 600, fontSize: 13, letterSpacing: 0.2, cursor: "pointer",
          }}>
          События прибора <span style={{ marginLeft: 4, color: "var(--bz-text-muted)", fontWeight: 500 }}>· {deviceEvents.length}</span>
        </button>
        <button onClick={() => setTab("app")}
          style={{
            flex: 1, padding: "10px 12px", borderRadius: 10,
            background: tab === "app" ? "var(--bz-surface)" : "transparent",
            border: "1px solid " + (tab === "app" ? "var(--bz-line)" : "transparent"),
            color: tab === "app" ? "var(--bz-text)" : "var(--bz-text-muted)",
            fontWeight: 600, fontSize: 13, letterSpacing: 0.2, cursor: "pointer",
          }}>
          Лог приложения <span style={{ marginLeft: 4, color: "var(--bz-text-muted)", fontWeight: 500 }}>· {appEvents.length}</span>
        </button>
      </div>

      <div style={{
        flex: 1, overflow: "auto",
        margin: "10px 14px 12px",
        background: "var(--bz-surface)",
        border: "1px solid var(--bz-line-soft)",
        borderRadius: "var(--bz-r)",
        padding: "4px 0",
      }}>
        {tab === "device" && deviceEvents.map((e, i) => (
          <div key={i} style={{
            display: "flex", alignItems: "center", gap: 12,
            padding: "10px 16px",
            borderTop: i ? "1px solid var(--bz-divider)" : "none",
            fontFamily: "var(--bz-font-mono)",
          }}>
            <span style={{ fontSize: 11, color: "var(--bz-text-muted)", letterSpacing: 0.3, minWidth: 118 }}>{e.t}</span>
            <span style={{ width: 6, height: 6, borderRadius: 4, background: deviceColor(e.kind), flexShrink: 0 }}/>
            <span style={{ fontSize: 13, color: deviceColor(e.kind), fontWeight: 600 }}>{e.text}</span>
          </div>
        ))}
        {tab === "app" && appEvents.map((e, i) => (
          <div key={i} style={{
            display: "flex", alignItems: "center", gap: 12,
            padding: "10px 16px",
            borderTop: i ? "1px solid var(--bz-divider)" : "none",
            fontFamily: "var(--bz-font-mono)",
          }}>
            <span style={{ fontSize: 11, color: "var(--bz-text-muted)", letterSpacing: 0.3, minWidth: 88 }}>{e.t}</span>
            <span style={{
              fontSize: 10, fontWeight: 700, letterSpacing: 0.5,
              minWidth: 36, textAlign: "center",
              padding: "2px 5px", borderRadius: 4,
              background: "color-mix(in srgb, " + appColor(e.lvl) + " 16%, transparent)",
              color: appColor(e.lvl),
            }}>{e.lvl}</span>
            <span style={{ fontSize: 13, color: "var(--bz-text)", fontWeight: 500, flex: 1, minWidth: 0 }}>{e.text}</span>
          </div>
        ))}
      </div>

      {navVariant === "bar" && <BottomNav active="settings"/>}
      {navVariant === "fab" && <BottomNav active="settings" variant="fab"/>}
    </div>
  );
}

// ───── Helpers for settings rows ─────
function RowNum({ label, sub, value, unit, range, danger = false }) {
  return (
    <div style={{
      display: "flex", alignItems: "center", gap: 12,
      padding: "12px 16px", minHeight: 56,
      borderTop: "1px solid var(--bz-divider)",
    }}>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 14, color: "var(--bz-text)", fontWeight: 500 }}>{label}</div>
        {sub && <div style={{ fontSize: 11, color: "var(--bz-text-muted)", marginTop: 2 }}>{sub}</div>}
      </div>
      <div style={{
        display: "flex", alignItems: "center", gap: 6,
        padding: "6px 10px",
        background: "var(--bz-surface-2)",
        border: "1px solid " + (danger ? "var(--bz-danger)" : "var(--bz-line)"),
        borderRadius: 9,
        minWidth: 84,
      }}>
        <span className="bz-mono bz-tabular" style={{
          fontSize: 14, fontWeight: 600, color: danger ? "var(--bz-danger)" : "var(--bz-text)",
          flex: 1, textAlign: "right",
        }}>{value}</span>
        {unit && <span style={{ fontSize: 10, color: "var(--bz-text-muted)", fontFamily: "var(--bz-font-mono)" }}>{unit}</span>}
      </div>
      {range && <span style={{
        fontSize: 10, color: "var(--bz-text-muted)", fontFamily: "var(--bz-font-mono)",
        whiteSpace: "nowrap",
      }}>{range}</span>}
    </div>
  );
}

function RGBSlider({ label, color, value }) {
  const fillColors = { A: "linear-gradient(90deg, transparent, var(--bz-text))",
                       R: "linear-gradient(90deg,#001,#f33)",
                       G: "linear-gradient(90deg,#001,#3f3)",
                       B: "linear-gradient(90deg,#001,#36f)" };
  return (
    <div style={{ display:"flex", alignItems:"center", gap: 12, padding: "10px 16px", borderTop: "1px solid var(--bz-divider)" }}>
      <span className="bz-mono" style={{ width: 14, fontSize: 12, fontWeight: 700, color: "var(--bz-text-dim)" }}>{label}</span>
      <div style={{ flex: 1, height: 6, borderRadius: 3, background: fillColors[label], position: "relative" }}>
        <span style={{
          position: "absolute", top: -5, left: `calc(${(value/255)*100}% - 8px)`,
          width: 16, height: 16, borderRadius: 8,
          background: "var(--bz-text)", border: "2px solid var(--bz-bg)",
          boxShadow: "0 1px 4px rgba(0,0,0,0.3)",
        }}/>
      </div>
      <span className="bz-mono bz-tabular" style={{ fontSize: 12, color: "var(--bz-text)", minWidth: 28, textAlign: "right" }}>{value}</span>
    </div>
  );
}

// ───── SETTINGS — complete (every legacy parameter) ─────
function ScreenSettings({ navVariant = "bar", spectrumColor = "#00d4aa" }) {
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
        padding: "14px 14px 80px",
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

        {/* ═══ Подключение ═══ */}
        <Card title="Подключение">
          <Row label="MAC прибора"
            sub="00:08:E1:2A:12:34"
            action={<button style={{ padding:"6px 12px", borderRadius:8, border:"1px solid var(--bz-line)", background:"var(--bz-surface-2)", color:"var(--bz-text)", fontSize:12, fontWeight:600, cursor:"pointer" }}>Изменить</button>}/>
          <Row label="Имя устройства" value="BluZ·2A12"/>
          <Row label="RSSI" sub="уровень сигнала BLE" value="−62 dBm"/>
        </Card>

        {/* ═══ Спектр · отображение ═══ */}
        <Card title="Спектр · отображение" action={<TagApp/>}>
          <Row label="Тип графика"
            action={<Segmented options={[{label:"Линия",value:"line"},{label:"Заливка",value:"fill"},{label:"Бар",value:"gisto"}]} value="line"/>}/>
          <Row label="Шкала Y"
            action={<Segmented options={[{label:"Lin",value:"lin"},{label:"Lg",value:"lg"},{label:"+Фон",value:"fl"},{label:"+ФонLg",value:"flg"}]} value="lin"/>}/>
          <Row label="Цвет линии"
            sub="прозрачность · красный · зелёный · синий"
            action={<span style={{ width: 28, height: 28, borderRadius: 8, background: spectrumColor, border: "1px solid var(--bz-line)" }}/>}/>
          <RGBSlider label="A" value={255}/>
          <RGBSlider label="R" value={0}/>
          <RGBSlider label="G" value={212}/>
          <RGBSlider label="B" value={170}/>
          <RowNum label="Окно SMA-сглаживания" sub="кол-во точек" value="5" range="1–50"/>
          <RowNum label="Отбросить каналов от нуля" sub="убирает шум на низких" value="10" range="0–100"/>
          <RowNum label="Padding слева" value="8" unit="px"/>
          <RowNum label="Padding справа" value="8" unit="px"/>
          <RowNum label="Горизонтальный зум" value="1.0" range="0.5–4.0"/>
        </Card>

        {/* ═══ Единицы и форматы ═══ */}
        <Card title="Единицы и форматы" action={<TagApp/>}>
          <Row label="Единицы дозы"
            action={<Segmented options={[{label:"мкР/ч",value:"urh"},{label:"мкЗв/ч",value:"usvh"}]} value="urh"/>}/>
          <Row label="Экспорт спектра"
            action={<Segmented options={[{label:"BqMon",value:"bqm"},{label:"CSV",value:"csv"}]} value="bqm"/>}/>
          <Row label="Экспорт трека"
            action={<Segmented options={[{label:"KML",value:"kml"},{label:"CSV",value:"csv"}]} value="kml"/>}/>
        </Card>

        {/* ═══ Карта и интерфейс ═══ */}
        <Card title="Карта и интерфейс" action={<TagApp/>}>
          <Row label="Ночной режим карты" sub="тёмная тема Yandex MapKit" action={<Toggle on/>}/>
          <Row label="Полноэкранный режим" sub="скрывать системную строку" action={<Toggle/>}/>
          <RowNum label="Уровень лога приложения"
            sub="0 = отключено · 5 = детальная отладка"
            value="2" range="0–5"/>
        </Card>

        {/* ═══ Профиль детектора ═══ */}
        <Card title="Профиль детектора" action={<TagApp/>}>
          <Row label="Активный профиль" value="SiPM·A · 5×5"/>
          <Row label="Создать новый профиль" action={<span style={{ color:"var(--bz-accent)" }}>{Ico.chevron("right")}</span>}/>
          <Row label="Загрузить из файла"  action={<span style={{ color:"var(--bz-accent)" }}>{Ico.chevron("right")}</span>}/>
          <Row label="Выбрать из списка"    action={<span style={{ color:"var(--bz-accent)" }}>{Ico.chevron("right")}</span>}/>
        </Card>

        {/* ═══ Прибор · сигналы ═══ */}
        <Card title="Прибор · сигналы" action={<TagDev/>}>
          <Row label="Звук на импульс"
            action={<Segmented options={[{label:"–",value:"none"},{label:"1/1",value:"1"},{label:"1/10",value:"10"}]} value="none"/>}/>
          <Row label="LED на импульс"
            action={<Segmented options={[{label:"–",value:"none"},{label:"1/1",value:"1"},{label:"1/10",value:"10"}]} value="1"/>}/>
          <Row label="Автозапуск спектрометра" sub="при подключении прибора" action={<Toggle on/>}/>
        </Card>

        {/* ═══ Оповещения (alarms — editable) ═══ */}
        <Card title="Прибор · тревоги" action={<TagDev/>}>
          <AlarmLevelRow level={1} color="var(--bz-warn)"   name="Уровень 1" hint="информационный"  value={40}    s v={false}/>
          <AlarmLevelRow level={2} color="#ff6b35"          name="Уровень 2" hint="повышенный фон"  value={200}   s v/>
          <AlarmLevelRow level={3} color="var(--bz-danger)" name="Уровень 3" hint="критический"     value={1000}  s v/>
        </Card>

        {/* ═══ Калибровка ═══ */}
        <Card title="Калибровка энергии" action={<TagDev/>}>
          <Row label="Разрешение спектрометра" sub="три набора коэффициентов"
            action={<Segmented options={[{label:"1024",value:"1"},{label:"2048",value:"2"},{label:"4096",value:"4"}]} value="2"/>}/>
          <Row label="Полином Ax² + Bx + C"
            sub="A: 0.00012  ·  B: 1.0042  ·  C: −0.18"
            action={<span style={{ color: "var(--bz-accent)", display:"inline-flex", alignItems:"center", gap:4, fontSize:12, fontWeight:600 }}>Открыть {Ico.chevron("right",14)}</span>}
            onClick={()=>{}}/>
        </Card>

        {/* ═══ Аппаратные — Эксперт ═══ */}
        <Card title="Аппаратные параметры" action={
          <span style={{
            display: "inline-flex", alignItems: "center", gap: 4,
            padding: "3px 8px", borderRadius: 6,
            background: "var(--bz-warn-soft)", color: "var(--bz-warn)",
            fontSize: 10, fontWeight: 700, letterSpacing: 0.6,
          }}>⚠ ЭКСПЕРТ</span>
        }>
          <div style={{ padding: "8px 16px 0", fontSize: 11, color: "var(--bz-text-muted)" }}>
            Изменение этих параметров может вывести детектор из строя.
            Изменения применяются на прибор только кнопкой <span style={{ color: "var(--bz-danger)", fontWeight: 700 }}>Write</span>.
          </div>
          <RowNum label="Высокое напряжение HV"     value="780"   unit="ед" range="0–1023" danger/>
          <RowNum label="Порог компаратора"         value="410"   unit="ед" range="0–1023" danger/>
          <RowNum label="Бит на канал"              value="20"    range="16–32"/>
          <RowNum label="Время выборки АЦП"         value="3"     range="0–7"/>
          <RowNum label="Точность АЦП"              value="100"/>
          <RowNum label="CPS → мкР/ч"               value="0.0660" sub="коэффициент пересчёта"/>
        </Card>

        {/* ═══ Действия ═══ */}
        <Card title="Действия">
          <Row label="Журнал событий"
            sub="включения, тревоги, write to flash"
            onClick={()=>{}}
            action={<span style={{ color: "var(--bz-text-muted)" }}>{Ico.chevron("right")}</span>}/>
          <Row label="Сохранить профиль настроек" sub="локально на телефоне"
            action={<span style={{ color:"var(--bz-accent)" }}>{Ico.chevron("right")}</span>} onClick={()=>{}}/>
          <Row label="Сбросить устройство" sub="команда reset на прибор"
            action={<span style={{ color:"var(--bz-danger)" }}>{Ico.chevron("right")}</span>} onClick={()=>{}}/>
        </Card>

      </div>

      {/* Floating action dock — Read/Write/Save/Rest/Scan/Find */}
      <DeviceActionDock/>

      {navVariant === "bar" && <BottomNav active="settings"/>}
      {navVariant === "fab" && <BottomNav active="settings" variant="fab"/>}
    </div>
  );
}

function TagApp() {
  return (
    <span style={{
      padding: "3px 7px", borderRadius: 5,
      background: "var(--bz-surface-3)", color: "var(--bz-text-muted)",
      fontSize: 10, fontWeight: 700, letterSpacing: 0.5, fontFamily: "var(--bz-font-mono)",
    }}>ПРИЛОЖЕНИЕ</span>
  );
}
function TagDev() {
  return (
    <span style={{
      padding: "3px 7px", borderRadius: 5,
      background: "var(--bz-accent-soft)", color: "var(--bz-accent)",
      fontSize: 10, fontWeight: 700, letterSpacing: 0.5, fontFamily: "var(--bz-font-mono)",
    }}>ПРИБОР</span>
  );
}

function DeviceActionDock() {
  return (
    <div style={{
      position: "absolute",
      left: 14, right: 14,
      bottom: 80,
      display: "flex", gap: 6,
      padding: 6,
      background: "color-mix(in srgb, var(--bz-surface) 95%, transparent)",
      backdropFilter: "blur(10px)",
      border: "1px solid var(--bz-line)",
      borderRadius: 14,
      boxShadow: "0 8px 24px -8px rgba(0,0,0,0.4)",
      zIndex: 10,
    }}>
      <DockBtn label="Scan"  icon={Ico.search(14)} tone="neutral"/>
      <DockBtn label="Save"  icon={Ico.save(14)}   tone="neutral"/>
      <DockBtn label="Rest"  icon={Ico.history(14)} tone="neutral"/>
      <DockBtn label="Read"  icon={Ico.chevron("down",14)} tone="info"/>
      <DockBtn label="Write" icon={Ico.chevron("up",14)} tone="danger"/>
      <DockBtn label="Find"  icon={Ico.target(14)} tone="accent"/>
    </div>
  );
}
function DockBtn({ label, icon, tone = "neutral" }) {
  const styles = {
    neutral: { bg: "transparent", color: "var(--bz-text-dim)", border: "var(--bz-line)" },
    info:    { bg: "color-mix(in srgb, var(--bz-info) 14%, transparent)", color: "var(--bz-info)", border: "var(--bz-info)" },
    accent:  { bg: "var(--bz-accent-soft)", color: "var(--bz-accent)", border: "var(--bz-accent)" },
    danger:  { bg: "var(--bz-danger)", color: "#fff", border: "var(--bz-danger)" },
  }[tone];
  return (
    <button style={{
      flex: 1, minHeight: 44,
      display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 2,
      borderRadius: 10,
      background: styles.bg,
      color: styles.color,
      border: `1px solid ${styles.border}`,
      cursor: "pointer",
      fontSize: 11, fontWeight: 700, letterSpacing: 0.4,
      padding: "4px 4px",
    }}>
      {icon}
      <span>{label}</span>
    </button>
  );
}

// ───── SPECTRUM — add full button set (BqMon/Load/Clear/Calibrate/SMA/MED) ─────
function ScreenSpectrumPortrait({ overload = false, navVariant = "bar", spectrumColor = "#00d4aa", chartStyle = "line", log = false, theme = "dark" }) {
  return (
    <div className="bz-screen" style={{
      flex: 1, display: "flex", flexDirection: "column",
      paddingTop: "var(--bz-inset-top)",
      paddingBottom: navVariant === "fab" ? 0 : 0,
    }}>
      <div style={{ display: "flex", alignItems: "center", gap: 12, padding: "12px 18px 8px" }}>
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
          <div style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 22, color: "var(--bz-text)", fontWeight: 600, letterSpacing: -0.4 }}>
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
      </div>

      <StatusStrip orientation="portrait" overload={overload} btStatus="on"/>

      {/* Auxiliary status — Total / AvgCPS — compact mono row */}
      <div style={{
        display: "flex", alignItems: "center", gap: 10,
        padding: "5px 14px",
        borderBottom: "1px solid var(--bz-line-soft)",
        background: "var(--bz-surface)",
        fontSize: 10.5, fontFamily: "var(--bz-font-mono)", color: "var(--bz-text-muted)",
        overflow: "hidden", whiteSpace: "nowrap",
      }}>
        <span><span style={{ letterSpacing: 0.4 }}>Σ</span> <span style={{ color: "var(--bz-text)" }}>10 623 119</span><span style={{ color: "var(--bz-text-faint)" }}> · 85%</span></span>
        <span>Avg <span style={{ color: "var(--bz-text)" }}>423.5</span></span>
        <span>COMP <span style={{ color: "var(--bz-text-dim)" }}>3.4</span></span>
        <span>MED <span style={{ color: "var(--bz-text-dim)" }}>1.2</span></span>
      </div>

      <div style={{
        padding: "12px 18px 8px",
        display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16,
      }}>
        <Readout label="МОЩНОСТЬ ДОЗЫ" value="12.44" unit="мкР/ч" accent={!overload} danger={overload} large/>
        <Readout label="СРЕДНЯЯ" value="13.99" unit="мкР/ч" large/>
      </div>

      {/* Chart with isotope hint and SMA/MED inline */}
      <div style={{
        flex: 1, margin: "4px 14px 8px",
        background: "var(--bz-surface)",
        border: "1px solid var(--bz-line-soft)",
        borderRadius: "var(--bz-r)",
        padding: "10px 12px 8px",
        display: "flex", flexDirection: "column", minHeight: 0,
      }}>
        <div style={{
          display: "flex", justifyContent: "space-between", alignItems: "center",
          fontSize: 10, color: "var(--bz-text-muted)", fontFamily: "var(--bz-font-mono)",
          letterSpacing: 0.5, marginBottom: 6,
        }}>
          <span>{log ? "LOG" : "LIN"} · counts</span>
          <span>Cs-137 photopeak · ch 658 · 662 keV</span>
        </div>
        <div style={{ flex: 1, position: "relative", display: "flex" }}>
          <SpectrumChart theme={theme} width={344} height={overload ? 280 : 320}
            log={log} style={chartStyle}
            color={overload ? "var(--bz-danger)" : spectrumColor}
            overload={overload} channels={2048} showPeaks={!overload}/>
        </div>

        {/* Inline filter chips */}
        <div style={{ display: "flex", gap: 6, marginTop: 6, paddingTop: 8, borderTop: "1px solid var(--bz-divider)" }}>
          <FilterChip label="SMA" on/>
          <FilterChip label="MED"/>
          <div style={{ flex: 1 }}/>
          <IconButton icon={Ico.scale(15)} label={log ? "Log" : "Lin"} size={36}/>
          <IconButton icon={Ico.zoom(15)} label="1×" size={36}/>
        </div>
      </div>

      {/* Action bar — Start/Stop primary; BqMon/Load/Clear/Cal secondary */}
      <div style={{
        display: "flex", alignItems: "center", gap: 5,
        padding: "0 12px 12px",
        minWidth: 0,
      }}>
        <SmallAction icon={Ico.save(14)} label="BqMon"/>
        <SmallAction icon={Ico.chevron("down",13)} label="Load"/>
        <SmallAction icon={Ico.clear(14)} label="Clear"/>
        <SmallAction icon={<span style={{ fontFamily:"var(--bz-font-mono)", fontSize:12, fontWeight:700 }}>1</span>} label="Cal"/>
        <div style={{ flex: 1, minWidth: 0 }}/>
        <CircleButton icon={Ico.stop(20)} danger/>
      </div>

      {navVariant === "bar" && <BottomNav active="spectrum"/>}
      {navVariant === "fab" && <BottomNav active="spectrum" variant="fab"/>}
    </div>
  );
}

function FilterChip({ label, on = false }) {
  return (
    <button style={{
      padding: "6px 11px", borderRadius: 8,
      background: on ? "var(--bz-accent-soft)" : "transparent",
      border: `1px solid ${on ? "var(--bz-accent)" : "var(--bz-line)"}`,
      color: on ? "var(--bz-accent)" : "var(--bz-text-muted)",
      fontSize: 11, fontWeight: 700, letterSpacing: 0.6,
      fontFamily: "var(--bz-font-mono)",
      cursor: "pointer", minHeight: 30,
    }}>{label}</button>
  );
}
function SmallAction({ icon, label }) {
  return (
    <button style={{
      display: "inline-flex", alignItems: "center", gap: 4,
      padding: "8px 9px", minHeight: 38,
      borderRadius: 9, background: "var(--bz-surface-2)",
      border: "1px solid var(--bz-line)",
      color: "var(--bz-text)", cursor: "pointer",
      fontSize: 11.5, fontWeight: 600, letterSpacing: 0.1,
      whiteSpace: "nowrap", flexShrink: 0,
    }}>
      {icon}
      <span>{label}</span>
    </button>
  );
}

// ───── HISTORY — add Load/BqMon/Clear ─────
function ScreenHistory({ navVariant = "bar", theme = "dark" }) {
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
        padding: "12px 18px 6px",
        display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16,
      }}>
        <Readout label="ИНТЕГРАЛ" value="10 623 119" unit="имп" accent large/>
        <Readout label="СР. CPS" value="423.5" unit="имп/с" large/>
      </div>

      <div style={{
        flex: 1, margin: "4px 14px 8px",
        background: "var(--bz-surface)",
        border: "1px solid var(--bz-line-soft)",
        borderRadius: "var(--bz-r)",
        padding: "10px 12px 8px",
        display: "flex", flexDirection: "column",
      }}>
        <div style={{
          display: "flex", justifyContent: "space-between",
          fontSize: 10, color: "var(--bz-text-muted)",
          fontFamily: "var(--bz-font-mono)", letterSpacing: 0.5, marginBottom: 6,
        }}>
          <span>LOG · cumulative</span>
          <span>2048 ch · с 23-05 15:31</span>
        </div>
        <div style={{ flex: 1, display: "flex" }}>
          <SpectrumChart theme={theme} width={344} height={300}
            log style="fill" color="#4ea3ff" secondary="#00d4aa"
            channels={2048}/>
        </div>
        <div style={{ display: "flex", gap: 18, marginTop: 6, paddingTop: 8, borderTop: "1px solid var(--bz-divider)", fontSize: 11 }}>
          <span style={{ display: "flex", alignItems: "center", gap: 6, color: "var(--bz-text-dim)" }}>
            <span style={{ width: 10, height: 10, borderRadius: 2, background: "#4ea3ff" }}/>
            Накопленный
          </span>
          <span style={{ display: "flex", alignItems: "center", gap: 6, color: "var(--bz-text-dim)" }}>
            <span style={{ width: 10, height: 10, borderRadius: 2, background: "#00d4aa", opacity: 0.6 }}/>
            Фон
          </span>
        </div>
      </div>

      {/* Action bar */}
      <div style={{ display: "flex", gap: 6, padding: "0 14px 14px" }}>
        <SmallAction icon={Ico.save(15)} label="BqMon"/>
        <SmallAction icon={Ico.chevron("down",14)} label="Load"/>
        <SmallAction icon={Ico.clear(15)} label="Clear"/>
        <div style={{ flex: 1 }}/>
      </div>

      {navVariant === "bar" && <BottomNav active="history"/>}
      {navVariant === "fab" && <BottomNav active="history" variant="fab"/>}
    </div>
  );
}

// ───── MAP — add Tracks/New/Save/AddPoint ─────
function ScreenMap({ navVariant = "bar", theme = "dark" }) {
  return (
    <div className="bz-screen" style={{
      flex: 1, display: "flex", flexDirection: "column",
      paddingTop: "var(--bz-inset-top)",
      position: "relative",
    }}>
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
          <path d="M -10 600 Q 200 400 412 380" stroke="currentColor" strokeOpacity="0.32" strokeWidth="14" fill="none"/>
          <path d="M 130 -10 L 200 900" stroke="currentColor" strokeOpacity="0.28" strokeWidth="10" fill="none"/>
          <path d="M 350 -10 Q 320 300 200 540 L 80 800" stroke="currentColor" strokeOpacity="0.22" strokeWidth="8" fill="none"/>

          {/* GPS track */}
          <path d="M 60 720 L 110 670 L 140 600 L 170 540 L 210 470 L 220 410 L 250 350 L 280 290 L 330 240 L 360 180"
            stroke="var(--bz-accent)" strokeWidth="3" fill="none" strokeLinecap="round" strokeLinejoin="round" strokeOpacity="0.95"/>
          <path d="M 60 720 L 110 670 L 140 600 L 170 540 L 210 470 L 220 410 L 250 350 L 280 290 L 330 240 L 360 180"
            stroke="var(--bz-accent)" strokeWidth="10" fill="none" strokeLinecap="round" strokeLinejoin="round" strokeOpacity="0.18"/>

          {/* 32-color scale markers (sample) */}
          {[
            [60, 720, 0],   [110, 670, 2],
            [140, 600, 3],  [170, 540, 5],
            [210, 470, 10], [220, 410, 11],
            [250, 350, 18], [280, 290, 24],
            [330, 240, 21], [360, 180, 16],
          ].map(([x, y, lvl]) => {
            // Color from blue → cyan → green → yellow → red gradient
            const hue = (200 - lvl * 6.5) % 360;
            const c = `hsl(${hue}, 85%, 55%)`;
            return (
              <g key={x}>
                <circle cx={x} cy={y} r="12" fill={c} fillOpacity="0.18"/>
                <circle cx={x} cy={y} r="6" fill={c} stroke="var(--bz-bg)" strokeWidth="1.5"/>
              </g>
            );
          })}
          <g>
            <circle cx="360" cy="180" r="20" fill="var(--bz-info)" fillOpacity="0.12"/>
            <circle cx="360" cy="180" r="10" fill="var(--bz-info)" fillOpacity="0.4"/>
            <circle cx="360" cy="180" r="5" fill="var(--bz-info)" stroke="var(--bz-bg)" strokeWidth="2"/>
          </g>
        </svg>
      </div>

      {/* Header */}
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
            <span style={{ width: 7, height: 7, borderRadius: 4, background: "var(--bz-danger)", animation: "bz-pulse 1s ease-in-out infinite" }}/>
            REC
          </span>
        </div>
      </div>

      <div style={{ flex: 1 }}/>

      {/* Track stats + actions */}
      <div style={{
        position: "relative", zIndex: 2,
        margin: "0 14px 12px",
        background: "color-mix(in srgb, var(--bz-surface) 95%, transparent)",
        backdropFilter: "blur(12px)",
        border: "1px solid var(--bz-line)",
        borderRadius: "var(--bz-r)",
        overflow: "hidden",
      }}>
        <div style={{ padding: "12px 14px", display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 10 }}>
          <Readout label="ДОЗА" value="12.44" unit="мкР/ч" accent/>
          <Readout label="MAX" value="210" unit="мкР/ч"/>
          <Readout label="ПУТЬ" value="1.84" unit="км"/>
        </div>
        <div style={{ display: "flex", borderTop: "1px solid var(--bz-divider)" }}>
          <MapAction icon={Ico.history(15)} label="Tracks"/>
          <MapAction icon={Ico.plus(14)} label="New"/>
          <MapAction icon={Ico.save(14)} label="Save"/>
          <MapAction icon={Ico.stop(13)} label="Stop" tone="danger"/>
        </div>
      </div>

      {/* Floating controls */}
      <div style={{
        position: "absolute", right: 14, bottom: 168, zIndex: 3,
        display: "flex", flexDirection: "column", gap: 10,
      }}>
        <FloatBtn icon={Ico.target(22)} color="var(--bz-info)" title="GPS-локация"/>
        <FloatBtn icon={Ico.pin(22)} color="var(--bz-accent)" title="Точка вручную"/>
      </div>

      {navVariant === "bar" && <BottomNav active="map"/>}
      {navVariant === "fab" && <BottomNav active="map" variant="fab"/>}
    </div>
  );
}

function MapAction({ icon, label, tone = "neutral" }) {
  const c = tone === "danger" ? "var(--bz-danger)" : "var(--bz-text)";
  return (
    <button style={{
      flex: 1, padding: "10px 0", border: "none",
      borderRight: "1px solid var(--bz-divider)",
      background: "transparent",
      color: c, cursor: "pointer",
      display: "flex", alignItems: "center", justifyContent: "center", gap: 6,
      fontSize: 12, fontWeight: 600,
    }}>
      {icon}
      <span>{label}</span>
    </button>
  );
}
function FloatBtn({ icon, color, title }) {
  return (
    <button title={title} style={{
      width: 48, height: 48, borderRadius: 14,
      background: "color-mix(in srgb, var(--bz-surface) 95%, transparent)",
      backdropFilter: "blur(8px)",
      border: "1px solid var(--bz-line)",
      color: color, cursor: "pointer",
      display: "flex", alignItems: "center", justifyContent: "center",
    }}>{icon}</button>
  );
}

Object.assign(window, {
  ScreenSpectrumPortrait, ScreenHistory, ScreenDose, ScreenLog,
  ScreenSettings, ScreenMap,
  DoseHistogram, FilterChip, SmallAction, MapAction, FloatBtn,
  DeviceActionDock, DockBtn, RowNum, RGBSlider, TagApp, TagDev,
});
