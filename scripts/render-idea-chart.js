const fs = require("fs");
const path = require("path");
const { chromium } = require("playwright");

const inputPath = process.argv[2];
const outputPath = process.argv[3];

if (!inputPath || !outputPath) {
  console.error("Usage: node render-idea-chart.js <input.json> <output.png>");
  process.exit(1);
}

const payload = JSON.parse(fs.readFileSync(inputPath, "utf8"));
const lightweightChartsPackagePath = require.resolve("lightweight-charts/package.json");
const lightweightChartsPath = path.join(
  path.dirname(lightweightChartsPackagePath),
  "dist",
  "lightweight-charts.standalone.production.js"
);
const lightweightChartsScript = fs.readFileSync(lightweightChartsPath, "utf8");

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function buildHtml(data) {
  return `<!doctype html>
<html>
<head>
  <meta charset="utf-8" />
  <script>${lightweightChartsScript}</script>
  <style>
    * { box-sizing: border-box; }
    body {
      margin: 0;
      width: 1280px;
      height: 720px;
      overflow: hidden;
      background: #111827;
      color: #e5e7eb;
      font-family: Inter, Arial, sans-serif;
    }
    #frame {
      position: relative;
      width: 1280px;
      height: 720px;
      background: linear-gradient(180deg, #111827 0%, #0b1120 100%);
    }
    #chart {
      position: absolute;
      left: 0;
      top: 0;
      width: 1280px;
      height: 720px;
    }
    #overlay {
      position: absolute;
      inset: 0;
      pointer-events: none;
      z-index: 2;
    }
    .badge {
      position: absolute;
      z-index: 3;
      padding: 8px 12px;
      border-radius: 8px;
      font-size: 18px;
      font-weight: 700;
      color: white;
      box-shadow: 0 10px 25px rgba(0,0,0,0.35);
    }
    #title {
      left: 18px;
      top: 14px;
      background: rgba(15, 23, 42, 0.82);
      border: 1px solid rgba(148, 163, 184, 0.25);
    }
    #bias {
      right: 26px;
      top: 18px;
      background: rgba(37, 99, 235, 0.86);
    }
    #note {
      left: 28px;
      bottom: 24px;
      max-width: 640px;
      background: rgba(15, 23, 42, 0.82);
      border: 1px solid rgba(148, 163, 184, 0.25);
      color: #cbd5e1;
      font-size: 16px;
      font-weight: 500;
    }
  </style>
</head>
<body>
  <div id="frame">
    <div id="chart"></div>
    <svg id="overlay" width="1280" height="720"></svg>
    <div id="title" class="badge">${escapeHtml(data.symbol)} · ${escapeHtml(data.interval)} · ${escapeHtml(data.title || "Chart")}</div>
    <div id="bias" class="badge">${escapeHtml(data.badge || data.idea.bias)}</div>
    <div id="note" class="badge">${escapeHtml(data.note || "Not financial advice.")}</div>
  </div>
  <script>
    window.__CHART_DATA__ = ${JSON.stringify(data)};
  </script>
  <script>
    const data = window.__CHART_DATA__;
    const chartElement = document.getElementById("chart");
    const chart = LightweightCharts.createChart(chartElement, {
      width: 1280,
      height: 720,
      layout: {
        background: { type: "solid", color: "#111827" },
        textColor: "#cbd5e1",
        fontFamily: "Inter, Arial, sans-serif",
      },
      grid: {
        vertLines: { color: "rgba(148, 163, 184, 0.12)" },
        horzLines: { color: "rgba(148, 163, 184, 0.12)" },
      },
      rightPriceScale: {
        borderColor: "rgba(148, 163, 184, 0.28)",
      },
      timeScale: {
        borderColor: "rgba(148, 163, 184, 0.28)",
        timeVisible: true,
      },
      crosshair: {
        mode: LightweightCharts.CrosshairMode.Normal,
      },
    });

    const candleSeries = chart.addCandlestickSeries({
      upColor: "#10b981",
      downColor: "#ef4444",
      borderUpColor: "#10b981",
      borderDownColor: "#ef4444",
      wickUpColor: "#10b981",
      wickDownColor: "#ef4444",
      priceFormat: { type: "price", precision: 2, minMove: 0.01 },
    });
    candleSeries.setData(data.candles);

    const volumeSeries = chart.addHistogramSeries({
      priceFormat: { type: "volume" },
      priceScaleId: "",
    });
    volumeSeries.priceScale().applyOptions({
      scaleMargins: { top: 0.82, bottom: 0 },
    });
    const baseVolumeData = data.candles.map(candle => ({
      time: candle.time,
      value: Number(candle.volume),
      color: candle.close >= candle.open ? "rgba(16, 185, 129, 0.35)" : "rgba(239, 68, 68, 0.35)",
    }));
    const volumeDeltaData = data.volumeDelta?.points?.map(point => ({
      time: point.time,
      value: Number(point.value),
      color: point.color,
    })) || [];
    volumeSeries.setData(data.chartType === "VOLUME_DELTA" ? volumeDeltaData : baseVolumeData);

    if (data.chartType === "VOLUME_DELTA" && data.volumeDelta?.cumulative) {
      const cumulativeDeltaSeries = chart.addLineSeries({
        color: "#a78bfa",
        lineWidth: 2,
        priceScaleId: "",
        priceLineVisible: false,
        lastValueVisible: true,
      });
      cumulativeDeltaSeries.setData(data.volumeDelta.cumulative);
    }

    const ema20 = chart.addLineSeries({
      color: "#f59e0b",
      lineWidth: 2,
      priceLineVisible: false,
      lastValueVisible: false,
    });
    ema20.setData(data.ema20);

    const ema50 = chart.addLineSeries({
      color: "#60a5fa",
      lineWidth: 2,
      priceLineVisible: false,
      lastValueVisible: false,
    });
    ema50.setData(data.ema50);

    const supportLevel = data.chartType === "BREAKOUT" ? Number(data.breakout.support) : Number(data.idea.support);
    const resistanceLevel = data.chartType === "BREAKOUT" ? Number(data.breakout.resistance) : Number(data.idea.resistance);

    candleSeries.createPriceLine({
      price: supportLevel,
      color: "#22c55e",
      lineWidth: 2,
      lineStyle: LightweightCharts.LineStyle.Solid,
      axisLabelVisible: true,
      title: "Support",
    });

    candleSeries.createPriceLine({
      price: resistanceLevel,
      color: "#f43f5e",
      lineWidth: 2,
      lineStyle: LightweightCharts.LineStyle.Solid,
      axisLabelVisible: true,
      title: "Resistance",
    });

    chart.timeScale().fitContent();

    function formatNumber(value, digits = 2) {
      const number = Number(value);
      if (!Number.isFinite(number)) return "n/a";
      return number.toLocaleString("en-US", { maximumFractionDigits: digits });
    }

    function drawTextPanel(svg, x, y, lines, accentColor, width = 470) {
      const lineHeight = 26;
      const height = 30 + lines.length * lineHeight;
      const text = lines.map((line, index) =>
        \`<text x="\${x + 18}" y="\${y + 34 + index * lineHeight}" fill="#e5e7eb" font-size="18" font-weight="\${index === 0 ? 800 : 600}">\${line}</text>\`
      ).join("");
      return \`
        <rect x="\${x}" y="\${y}" width="\${width}" height="\${height}" fill="rgba(15, 23, 42, 0.88)" stroke="\${accentColor}" stroke-width="2" rx="10"></rect>
        \${text}
      \`;
    }

    function drawVolumeDeltaOverlay(svg) {
      const lastDelta = Number(data.volumeDelta.lastDelta);
      const accent = lastDelta >= 0 ? "#10b981" : "#ef4444";
      svg.innerHTML = drawTextPanel(svg, 32, 78, [
        "Volume Delta",
        \`Buy Vol: \${formatNumber(data.volumeDelta.lastBuyVolume)}\`,
        \`Sell Vol: \${formatNumber(data.volumeDelta.lastSellVolume)}\`,
        \`Last Delta: \${formatNumber(data.volumeDelta.lastDelta)}\`,
        \`Total Delta: \${formatNumber(data.volumeDelta.totalDelta)}\`,
      ], accent, 430);
    }

    function drawBreakoutOverlay(svg, candleSeries) {
      const last = data.candles[data.candles.length - 1];
      const currentY = candleSeries.priceToCoordinate(Number(last.close));
      const referenceY = candleSeries.priceToCoordinate(Number(data.breakout.referenceLevel));
      const isBearish = data.breakout.direction === "Bearish Breakdown";
      const arrowColor = data.breakout.confirmed ? (isBearish ? "#ef4444" : "#10b981") : "#f59e0b";
      const arrowTargetY = isBearish ? Math.max(referenceY + 70, currentY + 20) : Math.min(referenceY - 70, currentY - 20);
      const status = data.breakout.confirmed ? "Confirmed" : "Watch";

      svg.innerHTML = \`
        <defs>
          <marker id="arrowhead" markerWidth="12" markerHeight="12" refX="10" refY="6" orient="auto">
            <path d="M 0 0 L 12 6 L 0 12 z" fill="\${arrowColor}"></path>
          </marker>
        </defs>
        <line x1="720" y1="\${referenceY}" x2="1140" y2="\${referenceY}" stroke="\${arrowColor}" stroke-width="5" stroke-linecap="round" opacity="0.85"></line>
        <line x1="890" y1="\${currentY}" x2="1110" y2="\${arrowTargetY}" stroke="\${arrowColor}" stroke-width="10" stroke-linecap="round" marker-end="url(#arrowhead)" opacity="0.85"></line>
        <text x="760" y="\${Math.min(currentY, arrowTargetY) - 18}" fill="\${arrowColor}" font-size="25" font-weight="900">\${status}: \${data.breakout.direction}</text>
        \${drawTextPanel(svg, 32, 78, [
          "Breakout Confirmation",
          \`Close: \${formatNumber(data.breakout.close)}\`,
          \`Level: \${formatNumber(data.breakout.referenceLevel)}\`,
          \`Volume: x\${formatNumber(data.breakout.volumeRatio, 2)} avg20\`,
          \`Delta: \${formatNumber(data.breakout.volumeDelta)}\`,
        ], arrowColor)}
      \`;
    }

    function coordinateForPoint(point, candleSeries) {
      if (!point || point.time === undefined || point.price === undefined) {
        return null;
      }

      const x = chart.timeScale().timeToCoordinate(Number(point.time));
      const y = candleSeries.priceToCoordinate(Number(point.price));
      if (x === null || y === null) {
        return null;
      }

      return { x, y };
    }

    function drawTrendlinePath(trendline, color, label) {
      if (!trendline || !trendline.first || !trendline.second) {
        return "";
      }

      const start = coordinateForPoint(trendline.first, candleSeries);
      const end = coordinateForPoint({
        time: trendline.extendedTime,
        price: trendline.extendedPrice,
      }, candleSeries);

      if (!start || !end) {
        return "";
      }

      const opacity = trendline.active ? 0.95 : 0.45;
      return \`
        <line x1="\${start.x}" y1="\${start.y}" x2="\${end.x}" y2="\${end.y}" stroke="\${color}" stroke-width="4" stroke-linecap="round" opacity="\${opacity}"></line>
        <circle cx="\${start.x}" cy="\${start.y}" r="6" fill="\${color}" opacity="0.95"></circle>
        <circle cx="\${end.x}" cy="\${end.y}" r="6" fill="\${color}" opacity="0.95"></circle>
        <text x="\${Math.min(1040, end.x + 10)}" y="\${Math.max(72, end.y - 12)}" fill="\${color}" font-size="18" font-weight="800">\${label} · touches \${trendline.touches}</text>
      \`;
    }

    function drawPivotDots(pivots, color) {
      return (pivots || []).slice(-8).map(pivot => {
        const coordinate = coordinateForPoint(pivot, candleSeries);
        if (!coordinate) return "";
        return \`<circle cx="\${coordinate.x}" cy="\${coordinate.y}" r="4" fill="\${color}" opacity="0.85"></circle>\`;
      }).join("");
    }

    function drawTrendlineOverlay(svg) {
      const uptrend = data.trendline?.uptrend;
      const downtrend = data.trendline?.downtrend;
      svg.innerHTML = \`
        \${drawTrendlinePath(uptrend, "#22c55e", "Uptrend")}
        \${drawTrendlinePath(downtrend, "#f43f5e", "Downtrend")}
        \${drawPivotDots(data.trendline?.pivotLows, "#86efac")}
        \${drawPivotDots(data.trendline?.pivotHighs, "#fda4af")}
        \${drawTextPanel(svg, 32, 78, [
          "Real Trendline",
          \`Summary: \${data.trendline?.summary || "n/a"}\`,
          \`Pivot highs: \${data.trendline?.pivotHighs?.length || 0}\`,
          \`Pivot lows: \${data.trendline?.pivotLows?.length || 0}\`,
          \`Up touches: \${uptrend?.touches || 0}\`,
          \`Down touches: \${downtrend?.touches || 0}\`,
        ], "#38bdf8", 470)}
      \`;
    }

    function percent(value) {
      return formatNumber(Number(value) * 100, 2) + "%";
    }

    function drawOrderFlowOverlay(svg) {
      const bidDominance = Number(data.orderFlow?.bidDominance || 0);
      const askDominance = Math.max(0, 1 - bidDominance);
      const bidWidth = Math.max(0, Math.min(1, bidDominance)) * 420;
      const askWidth = Math.max(0, Math.min(1, askDominance)) * 420;
      const accent = bidDominance > 0.6 ? "#22c55e" : bidDominance < 0.4 ? "#ef4444" : "#f59e0b";
      svg.innerHTML = \`
        \${drawTextPanel(svg, 32, 78, [
          "Order Flow",
          \`Bid dominance: \${percent(data.orderFlow?.bidDominance || 0)}\`,
          \`Book: \${data.orderFlow?.bookPressure || "n/a"}\`,
          \`Trades: \${data.orderFlow?.tradePressure || "n/a"}\`,
          \`Buy ratio: \${percent(data.orderFlow?.tradeBuyRatio || 0)}\`,
          \`Open Interest: \${formatNumber(data.orderFlow?.openInterest || 0)}\`,
          \`Funding: \${percent(data.orderFlow?.fundingRate || 0)}\`,
        ], accent, 470)}
        <rect x="760" y="92" width="420" height="34" rx="8" fill="rgba(239,68,68,0.35)" stroke="rgba(148,163,184,0.35)" stroke-width="1"></rect>
        <rect x="760" y="92" width="\${bidWidth}" height="34" rx="8" fill="rgba(34,197,94,0.75)"></rect>
        <text x="776" y="116" fill="#e5e7eb" font-size="18" font-weight="800">Bid \${percent(bidDominance)}</text>
        <text x="1044" y="116" fill="#e5e7eb" font-size="18" font-weight="800">Ask \${percent(askDominance)}</text>
        <rect x="760" y="148" width="420" height="34" rx="8" fill="rgba(239,68,68,0.35)" stroke="rgba(148,163,184,0.35)" stroke-width="1"></rect>
        <rect x="760" y="148" width="\${Math.max(0, Math.min(1, Number(data.orderFlow?.tradeBuyRatio || 0))) * 420}" height="34" rx="8" fill="rgba(59,130,246,0.78)"></rect>
        <text x="776" y="172" fill="#e5e7eb" font-size="18" font-weight="800">Recent trades: \${data.orderFlow?.tradePressure || "n/a"}</text>
      \`;
    }

    function drawIdeaOverlay(svg, candleSeries) {
      const supportY = candleSeries.priceToCoordinate(Number(data.idea.support));
      const resistanceY = candleSeries.priceToCoordinate(Number(data.idea.resistance));
      const last = data.candles[data.candles.length - 1];
      const currentY = candleSeries.priceToCoordinate(Number(last.close));
      const arrowTargetY = data.idea.bias === "Bearish" ? supportY : resistanceY;
      const arrowColor = data.idea.bias === "Bearish" ? "#ef4444" : "#3b82f6";

      const zoneTop = Math.min(supportY, supportY + 26);
      const zoneHeight = 32;
      svg.innerHTML = \`
        <defs>
          <marker id="arrowhead" markerWidth="12" markerHeight="12" refX="10" refY="6" orient="auto">
            <path d="M 0 0 L 12 6 L 0 12 z" fill="\${arrowColor}"></path>
          </marker>
        </defs>
        <rect x="760" y="\${zoneTop}" width="330" height="\${zoneHeight}" fill="rgba(34,197,94,0.18)" stroke="rgba(34,197,94,0.75)" stroke-width="2" rx="8"></rect>
        <text x="776" y="\${zoneTop + 22}" fill="#bbf7d0" font-size="18" font-weight="700">Support area</text>
        <line x1="760" y1="\${resistanceY}" x2="1120" y2="\${resistanceY}" stroke="#f43f5e" stroke-width="4" stroke-linecap="round"></line>
        <line x1="875" y1="\${currentY}" x2="1110" y2="\${arrowTargetY}" stroke="\${arrowColor}" stroke-width="10" stroke-linecap="round" marker-end="url(#arrowhead)" opacity="0.85"></line>
        <text x="900" y="\${Math.min(currentY, arrowTargetY) - 18}" fill="\${arrowColor}" font-size="24" font-weight="800">\${data.idea.bias}</text>
      \`;
    }

    function drawOverlay() {
      const svg = document.getElementById("overlay");
      if (data.chartType === "VOLUME_DELTA") {
        drawVolumeDeltaOverlay(svg);
        return;
      }

      if (data.chartType === "BREAKOUT") {
        drawBreakoutOverlay(svg, candleSeries);
        return;
      }

      if (data.chartType === "TRENDLINE") {
        drawTrendlineOverlay(svg);
        return;
      }

      if (data.chartType === "ORDER_FLOW") {
        drawOrderFlowOverlay(svg);
        return;
      }

      drawIdeaOverlay(svg, candleSeries);
    }

    setTimeout(() => {
      drawOverlay();
      window.__READY__ = true;
    }, 500);
  </script>
</body>
</html>`;
}

(async () => {
  const outputDir = path.dirname(outputPath);
  fs.mkdirSync(outputDir, { recursive: true });

  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1280, height: 720 }, deviceScaleFactor: 1 });
  await page.setContent(buildHtml(payload), { waitUntil: "networkidle" });
  await page.waitForFunction(() => window.__READY__ === true, { timeout: 20000 });
  await page.screenshot({ path: outputPath, type: "png" });
  await browser.close();
})();
