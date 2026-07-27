/* The live forecast panel that fronts every language page.

   One script drives English, Danish and Persian. Non-English pages load a
   js/i18n/<lang>.js first, which sets window.WXI18N; English needs no file
   because it is what wx.js already speaks.

   The page's own atmosphere is the data: the background gradient follows the
   searched location's day/night, and the accent glow follows its condition. That
   is the whole point of the app made literal — point it anywhere, and this page
   becomes the weather there. */
(function () {
  'use strict';

  var $ = function (id) { return document.getElementById(id); };
  var I = window.WXI18N || {};

  if (I.days && I.months) { WX.setLocale({ days: I.days, months: I.months, digits: I.digits }); }
  if (I.wmo) { WX.setLabels(I.wmo); }

  var EN = {
    feels: 'Feels like', wind: 'Wind', humidity: 'Humidity', rain: 'Rain chance',
    now: 'Now', today: 'Today', localTime: 'local time', updated: 'updated',
    searching: 'Searching…', noMatch: 'No places match that',
    searchFailed: 'Search is unavailable right now',
    offline: 'Offline, showing the last saved forecast',
    failed: 'Could not reach the forecast'
  };
  var t = {};
  for (var k in EN) {
    if (Object.prototype.hasOwnProperty.call(EN, k)) {
      t[k] = (I.ui && I.ui[k]) ? I.ui[k] : EN[k];
    }
  }

  var LANG = document.documentElement.getAttribute('lang') || 'en';

  /* The glow behind the panel is the current condition, as light. */
  var GLOW = {
    clear: '#ffd257', 'clear-night': '#cbd8ff', partly: '#ffd257', 'partly-night': '#8fa8e8',
    cloudy: '#7d93c8', fog: '#7d93c8',
    drizzle: '#7fd6ff', rain: '#7fd6ff', showers: '#7fd6ff', sleet: '#9fd8ff',
    snow: '#dfefff', thunder: '#a78bfa'
  };

  var state = { model: null, tick: null };

  function svg(id, cls) {
    return '<svg class="' + cls + '" viewBox="0 0 64 64" aria-hidden="true">' +
           '<use href="#i-' + id + '"/></svg>';
  }
  function setUse(el, id) {
    var u = el && el.querySelector('use');
    if (u) { u.setAttribute('href', '#i-' + id); }
  }

  function hourIndex(model) {
    var cur = model.current.time;
    if (!cur) { return 0; }
    var key = cur.slice(0, 13);
    var last = 0;
    for (var i = 0; i < model.hourly.length; i++) {
      var h = model.hourly[i].time;
      if (h.slice(0, 13) === key) { return i; }
      if (h < cur) { last = i; }
    }
    return last;
  }

  function placeLine(p) {
    var parts = [p.admin1, p.country].filter(function (s) { return s; });
    return parts.length ? p.name + ' · ' + parts.join(', ') : p.name;
  }

  function tile(label, value) {
    return '<div class="tile"><dt>' + label + '</dt><dd>' + value + '</dd></div>';
  }

  /* The clock at the *searched place*, derived from the API's UTC offset rather
     than the visitor's timezone — the detail that proves the app handles a city
     in another zone correctly. */
  function startClock(model) {
    if (state.tick) { clearInterval(state.tick); }
    function paint() {
      var d = new Date(Date.now() + model.utcOffset * 1000);
      $('wx-clock').textContent =
        WX.tr(WX.pad2(d.getUTCHours()) + ':' + WX.pad2(d.getUTCMinutes()));
    }
    paint();
    state.tick = setInterval(paint, 10000);
  }

  function render(model, stale) {
    state.model = model;
    var c = model.current;
    var d0 = model.daily[0] || {};
    var start = hourIndex(model);
    var iconId = WX.icon(c.weather_code, c.is_day);

    var root = document.documentElement;
    root.style.setProperty('--glow', GLOW[iconId] || '#ffd257');
    root.classList.toggle('is-night', !c.is_day);

    setUse($('wx-icon'), iconId);
    $('wx-temp').innerHTML = WX.temp(c.temperature_2m) + '<span>' + WX.tempUnit() + '</span>';
    $('wx-cond').textContent = WX.label(c.weather_code);
    $('wx-place').textContent = placeLine(model.place);
    startClock(model);

    $('wx-tiles').innerHTML =
      tile(t.feels, WX.tempFull(c.apparent_temperature)) +
      tile(t.wind, WX.wind(c.wind_speed_10m)) +
      tile(t.humidity, WX.percent(c.relative_humidity_2m)) +
      tile(t.rain, WX.percent(d0.precipitation_probability_max));

    var status = $('wx-status');
    status.className = 'wx-status' + (stale ? ' is-stale' : '');
    /* Labelled, because an unadorned time sits right beside the location's own
       clock and the two are otherwise indistinguishable. */
    status.textContent = stale ? t.offline : t.updated + ' ' + WX.clock(c.time);

    var html = '';
    for (var i = start; i < Math.min(start + 12, model.hourly.length); i++) {
      var h = model.hourly[i];
      var pp = h.precipitation_probability;
      var dry = (pp === null || pp === undefined || pp < 5) ? ' is-dry' : '';
      html += '<li class="hour">' +
        '<span class="hour-t">' + (i === start ? t.now : WX.hourLabel(h.time)) + '</span>' +
        svg(WX.icon(h.weather_code, h.is_day), 'hour-i') +
        '<span class="hour-d">' + WX.temp(h.temperature_2m) + '°</span>' +
        '<span class="hour-p' + dry + '">' + WX.percent(pp) + '</span>' +
        '</li>';
    }
    $('wx-hours').innerHTML = html;

    html = '';
    for (var k = 0; k < model.daily.length; k++) {
      var d = model.daily[k];
      html += '<li class="day">' +
        '<span class="day-n">' + (k === 0 ? t.today : WX.weekday(d.time)) +
        '<span class="day-sub">' + WX.dateLabel(d.time) + '</span></span>' +
        svg(WX.icon(d.weather_code, 1), 'day-i') +
        '<span class="day-p">' + WX.percent(d.precipitation_probability_max) + '</span>' +
        '<span class="day-t"><b>' + WX.temp(d.temperature_2m_max) + '°</b>' +
        '<span>' + WX.temp(d.temperature_2m_min) + '°</span></span>' +
        '</li>';
    }
    $('wx-days').innerHTML = html;
  }

  function failed(message) {
    var status = $('wx-status');
    status.className = 'wx-status is-error';
    status.textContent = message;
    $('wx-cond').textContent = t.failed;
  }

  function load(place) {
    $('wx-panel').classList.add('is-loading');
    WX.fetchForecast(place).then(function (model) {
      $('wx-panel').classList.remove('is-loading');
      render(model, false);
    }, function (err) {
      $('wx-panel').classList.remove('is-loading');
      var cached = WX.loadCached();
      if (cached) { render(cached, true); }
      else { failed(String(err && err.message ? err.message : err)); }
    });
  }

  /* ---------- location search ---------- */

  var searchTimer = null;
  var results = [];

  function closeResults() {
    var box = $('wx-results');
    box.hidden = true;
    box.innerHTML = '';
    $('wx-search').setAttribute('aria-expanded', 'false');
  }

  function showMessage(msg) {
    var box = $('wx-results');
    box.innerHTML = '<p class="wx-hint">' + msg + '</p>';
    box.hidden = false;
    $('wx-search').setAttribute('aria-expanded', 'true');
  }

  function showResults(list) {
    results = list;
    if (!list.length) { showMessage(t.noMatch); return; }
    var html = '<ul role="listbox">';
    for (var i = 0; i < list.length; i++) {
      html += '<li><button type="button" role="option" data-i="' + i + '">' +
        '<strong>' + list[i].name + '</strong>' +
        '<span>' + [list[i].admin1, list[i].country].filter(Boolean).join(', ') + '</span>' +
        '</button></li>';
    }
    html += '</ul>';
    var box = $('wx-results');
    box.innerHTML = html;
    box.hidden = false;
    $('wx-search').setAttribute('aria-expanded', 'true');
  }

  function pick(place) {
    closeResults();
    $('wx-search').value = '';
    load(place);
  }

  function onQuery() {
    var q = $('wx-search').value.trim();
    if (searchTimer) { clearTimeout(searchTimer); }
    if (q.length < 2) { closeResults(); return; }
    searchTimer = setTimeout(function () {
      showMessage(t.searching);
      WX.search(q, LANG).then(showResults, function () { showMessage(t.searchFailed); });
    }, 300);
  }

  function init() {
    var input = $('wx-search');
    input.addEventListener('input', onQuery, false);
    input.addEventListener('keydown', function (e) {
      if (e.key === 'Escape') { closeResults(); }
      /* Enter takes the top match — the common case is typing a city and
         expecting the obvious answer, not browsing a list. */
      if (e.key === 'Enter' && results.length) { e.preventDefault(); pick(results[0]); }
    }, false);

    $('wx-results').addEventListener('click', function (e) {
      var btn = e.target.closest('button[data-i]');
      if (btn) { pick(results[+btn.getAttribute('data-i')]); }
    }, false);

    document.addEventListener('click', function (e) {
      if (!e.target.closest('.wx-search')) { closeResults(); }
    }, false);

    var chips = document.querySelectorAll('.chip[data-lat]');
    for (var i = 0; i < chips.length; i++) {
      chips[i].addEventListener('click', function () {
        pick({
          name: this.getAttribute('data-name'),
          country: this.getAttribute('data-country') || '',
          admin1: '',
          latitude: +this.getAttribute('data-lat'),
          longitude: +this.getAttribute('data-lon')
        });
      }, false);
    }

    /* Copenhagen first — where the Tizen app this was ported from was pinned. */
    var first = document.querySelector('.chip[data-lat]');
    pick({
      name: first.getAttribute('data-name'),
      country: first.getAttribute('data-country') || '',
      admin1: '',
      latitude: +first.getAttribute('data-lat'),
      longitude: +first.getAttribute('data-lon')
    });

    /* Open-Meteo publishes hourly; refresh while the tab is open. */
    setInterval(function () {
      if (state.model) { load(state.model.place); }
    }, 10 * 60 * 1000);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init, false);
  } else {
    init();
  }
})();
