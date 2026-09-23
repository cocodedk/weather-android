/* The searched place's weather, falling behind the page. live.js calls SKY.set() with the
   current condition, and this draws it on one full-page canvas: rain in three depths slanting
   with the place's real wind and splashing on the forecast panel, drifting snow, fog, and
   lightning. Clear and cloudy skies draw nothing and run no loop. Reduced motion gets one still
   frame; the lightning flash is soft and at least six seconds apart, never a strobe. */
(function () {
  'use strict';

  var RAIN = { drizzle: 1, rain: 1, showers: 1, sleet: 1, thunder: 1 };
  var SNOW = { snow: 1, sleet: 1 };
  var still = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)');

  var cv = null, g = null, W = 0, H = 0, raf = 0, last = 0, panel = null, cfg = {};
  var drops = [], flakes = [], splashes = [], fog = [], bolt = null, flash = 0, nextStrike = 0;

  function rand(a, b) { return a + (b - a) * Math.random(); }
  function active() { return RAIN[cfg.kind] || SNOW[cfg.kind] || cfg.kind === 'fog'; }

  /* Wind comes FROM windDir, so it pushes drops the other way across the screen. */
  function windX() { return -Math.sin(cfg.windDir * Math.PI / 180) * Math.min(cfg.wind, 25); }

  function size() {
    var dpr = Math.min(window.devicePixelRatio || 1, 2);
    /* clientWidth, not innerWidth: the canvas is laid out without the scrollbar. */
    W = document.documentElement.clientWidth; H = document.documentElement.clientHeight;
    cv.width = Math.round(W * dpr); cv.height = Math.round(H * dpr);
    g.setTransform(dpr, 0, 0, dpr, 0, 0);
  }

  /* The panel is shelter: near drops splash on its top edge, and nothing falls inside it. */
  function measure() {
    var el = document.getElementById('wx-panel');
    var r = el && el.getBoundingClientRect();
    panel = r && r.bottom > 0 && r.top < H ? { l: r.left, r: r.right, x0: r.left + 12, x1: r.right - 12, y: r.top, b: r.bottom } : null;
  }

  /* How much is falling. More screen, more drops; more millimetres, more rain. */
  function populate() {
    var area = Math.min(2, (W * H) / (1440 * 900));
    var nRain = { drizzle: 110, rain: 170, showers: 230, sleet: 90, thunder: 300 }[cfg.kind] || 0;
    if (cfg.kind === 'rain' || cfg.kind === 'showers') nRain += Math.min(220, cfg.precip * 70);
    var nSnow = cfg.kind === 'snow' ? 150 : cfg.kind === 'sleet' ? 70 : 0;
    drops = []; flakes = []; splashes = []; fog = [];
    for (var i = 0; i < nRain * area; i++) drops.push(drop({}, true));
    for (i = 0; i < nSnow * area; i++) flakes.push(flake({}, true));
    if (cfg.kind === 'fog') for (i = 0; i < 9; i++) fog.push({ x: rand(0, W), y: rand(0, H), r: rand(180, 420), v: rand(6, 16) });
    nextStrike = cfg.kind === 'thunder' ? performance.now() + rand(1500, 4000) : 0;
  }

  /* Three depths: far drops are thin, faint and slow; near ones are the ones that splash. */
  function drop(d, anywhere) {
    var fine = cfg.kind === 'drizzle';
    d.z = [0.35, 0.65, 1][Math.floor(rand(0, 3))];
    d.vy = (fine ? 380 : 780) * (0.45 + 0.55 * d.z) * rand(0.9, 1.1);
    d.vx = windX() * 26 * d.z;
    d.len = fine ? 5 + 5 * d.z : 10 + 14 * d.z;
    d.x = rand(-W * 0.3, W * 1.3);
    d.y = anywhere ? rand(-H, H) : rand(-H * 0.25, -d.len);
    return d;
  }

  function flake(f, anywhere) {
    f.z = rand(0.3, 1); f.r = 0.8 + 2.2 * f.z; f.vy = 25 + 55 * f.z; f.phase = rand(0, 6.3);
    f.x = rand(0, W); f.y = anywhere ? rand(-H, H) : -f.r * 4;
    return f;
  }

  function splash(x, y) {
    for (var i = 0; i < 3 && splashes.length < 240; i++) splashes.push({ x: x, y: y, vx: rand(-70, 70), vy: rand(-150, -60), life: rand(0.22, 0.38) });
  }

  /* A jagged path by midpoint displacement, halving the roughness each level. */
  function path(x0, y0, x1, y1, rough, depth, out) {
    if (depth === 0) { out.push(x1, y1); return out; }
    var mx = (x0 + x1) / 2 + rand(-rough, rough), my = (y0 + y1) / 2 + rand(-rough, rough) * 0.3;
    path(x0, y0, mx, my, rough / 2, depth - 1, out);
    return path(mx, my, x1, y1, rough / 2, depth - 1, out);
  }

  function strike(now) {
    var x = rand(W * 0.1, W * 0.9);
    bolt = { pts: path(x, -10, x + rand(-120, 120), rand(H * 0.45, H * 0.75), 90, 6, [x, -10]), life: 0.3 };
    flash = 1;
    nextStrike = now + rand(6000, 15000);
  }

  function step(dt, now) {
    var wx = windX(), i, d, f, s;
    for (i = 0; i < drops.length; i++) {
      d = drops[i];
      var py = d.y; d.y += d.vy * dt; d.x += d.vx * dt;
      if (panel && d.z === 1 && py < panel.y && d.y >= panel.y && d.x > panel.x0 && d.x < panel.x1) {
        splash(d.x, panel.y); drop(d, false);
      } else if (d.y - d.len > H) { drop(d, false); }
    }
    for (i = 0; i < flakes.length; i++) {
      f = flakes[i];
      f.y += f.vy * dt;
      f.x += (Math.sin(now / 900 + f.phase) * 18 + wx * 6) * f.z * dt;
      if (f.y - f.r > H) flake(f, false);
      if (f.x < -10) f.x += W + 20; else if (f.x > W + 10) f.x -= W + 20;
    }
    for (i = splashes.length - 1; i >= 0; i--) {
      s = splashes[i];
      s.life -= dt; s.vy += 900 * dt; s.x += s.vx * dt; s.y += s.vy * dt;
      if (s.life <= 0) splashes.splice(i, 1);
    }
    for (i = 0; i < fog.length; i++) {
      fog[i].x += fog[i].v * dt;
      if (fog[i].x - fog[i].r > W) fog[i].x = -fog[i].r;
    }
    if (nextStrike && now >= nextStrike) strike(now);
    if (bolt && (bolt.life -= dt) <= 0) bolt = null;
    flash = Math.max(0, flash - dt * 2.5);
  }

  function draw() {
    var i, d, tone = cfg.day ? '235,244,255' : '190,208,255';
    g.clearRect(0, 0, W, H);
    g.save();
    if (panel) {
      g.beginPath(); g.rect(0, 0, W, H);
      g[g.roundRect ? 'roundRect' : 'rect'](panel.l, panel.y, panel.r - panel.l, panel.b - panel.y, 20);
      g.clip('evenodd');
    }
    for (i = 0; i < fog.length; i++) {
      var f = fog[i], grad = g.createRadialGradient(f.x, f.y, 0, f.x, f.y, f.r);
      grad.addColorStop(0, 'rgba(' + tone + ',0.13)'); grad.addColorStop(1, 'rgba(' + tone + ',0)');
      g.fillStyle = grad; g.fillRect(f.x - f.r, f.y - f.r, f.r * 2, f.r * 2);
    }
    g.lineCap = 'round';
    for (i = 0; i < drops.length; i++) {
      d = drops[i];
      var k = d.len / d.vy;
      g.strokeStyle = 'rgba(' + tone + ',' + (0.12 + 0.33 * d.z) + ')';
      g.lineWidth = 0.6 + 0.9 * d.z;
      g.beginPath(); g.moveTo(d.x, d.y); g.lineTo(d.x - d.vx * k, d.y - d.len); g.stroke();
    }
    g.fillStyle = 'rgba(' + tone + ',0.7)';
    for (i = 0; i < splashes.length; i++) g.fillRect(splashes[i].x, splashes[i].y, 1.6, 1.6);
    for (i = 0; i < flakes.length; i++) {
      g.fillStyle = 'rgba(255,255,255,' + (0.35 + 0.5 * flakes[i].z) + ')';
      g.beginPath(); g.arc(flakes[i].x, flakes[i].y, flakes[i].r, 0, 6.2832); g.fill();
    }
    if (bolt) {
      g.save();
      g.strokeStyle = 'rgba(236,232,255,' + Math.min(1, bolt.life / 0.15) + ')';
      g.lineWidth = 2; g.shadowColor = '#a78bfa'; g.shadowBlur = 18;
      g.beginPath(); g.moveTo(bolt.pts[0], bolt.pts[1]);
      for (i = 2; i < bolt.pts.length; i += 2) g.lineTo(bolt.pts[i], bolt.pts[i + 1]);
      g.stroke(); g.restore();
    }
    g.restore();  /* the flash lights the whole sky, panel included */
    if (flash > 0) { g.fillStyle = 'rgba(200,205,255,' + (0.16 * flash) + ')'; g.fillRect(0, 0, W, H); }
  }

  function frame(now) {
    measure();  /* the panel grows as it fills and as fonts load */
    step(Math.min(0.05, (now - (last || now)) / 1000), now);
    last = now; draw();
    raf = window.requestAnimationFrame(frame);
  }

  function stop() { if (raf) window.cancelAnimationFrame(raf); raf = 0; last = 0; }
  function start() {
    stop();
    if (!active()) { if (cv) { cv.hidden = true; g.clearRect(0, 0, W, H); } return; }
    cv.hidden = false;
    if (still && still.matches) { nextStrike = 0; window.requestAnimationFrame(function () { measure(); draw(); }); return; }
    if (!document.hidden) raf = window.requestAnimationFrame(frame);
  }

  function set(o) {
    if (!cv) {
      cv = document.createElement('canvas');
      cv.className = 'wx-sky';
      cv.setAttribute('aria-hidden', 'true');
      document.body.insertBefore(cv, document.body.firstChild);
      g = cv.getContext('2d');
      size();
      /* Phones resize as the address bar slides in and out; only a new width needs new drops. */
      window.addEventListener('resize', function () {
        var w = W; size();
        if (W !== w) { populate(); start(); } else if (!raf) { start(); }
      });
      /* A running loop re-measures every frame; the still frame has to be redrawn. */
      window.addEventListener('scroll', function () { if (!raf && !cv.hidden) { measure(); draw(); } }, { passive: true });
      document.addEventListener('visibilitychange', start);
      if (still && still.addEventListener) still.addEventListener('change', start);
    }
    var next = { kind: o.kind, day: o.day, wind: o.wind || 0, windDir: o.windDir == null ? 270 : o.windDir, precip: o.precip || 0 };
    /* live.js re-renders every ten minutes; an unchanged sky keeps its drops and its timing. */
    if (JSON.stringify(next) === JSON.stringify(cfg)) return;
    cfg = next;
    populate();
    start();
  }

  window.SKY = { set: set };
})();
