/* Weather domain for the landing pages: WMO codes, unit formatting, and the
   Open-Meteo clients.

   This is a deliberate re-implementation of the app's `domain/Wmo.kt`,
   `domain/Units.kt`, `data/ForecastApi.kt` and `data/GeocodingApi.kt`. The Tizen
   site could copy its shared modules verbatim because both sides were JavaScript;
   across Kotlin and JS that is not possible, so the two must be kept in step by
   hand. Labels, rounding and request parameters all mirror the Kotlin — change
   one, change the other. */
(function (global) {
  'use strict';

  /* ---------- WMO weather-interpretation codes ---------- */

  var CODES = {
    0:  ['Clear sky',          'clear'],
    1:  ['Mainly clear',       'clear'],
    2:  ['Partly cloudy',      'partly'],
    3:  ['Overcast',           'cloudy'],
    45: ['Fog',                'fog'],
    48: ['Freezing fog',       'fog'],
    51: ['Light drizzle',      'drizzle'],
    53: ['Drizzle',            'drizzle'],
    55: ['Dense drizzle',      'drizzle'],
    56: ['Freezing drizzle',   'sleet'],
    57: ['Freezing drizzle',   'sleet'],
    61: ['Light rain',         'rain'],
    63: ['Rain',               'rain'],
    65: ['Heavy rain',         'rain'],
    66: ['Freezing rain',      'sleet'],
    67: ['Freezing rain',      'sleet'],
    71: ['Light snow',         'snow'],
    73: ['Snow',               'snow'],
    75: ['Heavy snow',         'snow'],
    77: ['Snow grains',        'snow'],
    80: ['Light showers',      'showers'],
    81: ['Showers',            'showers'],
    82: ['Violent showers',    'showers'],
    85: ['Snow showers',       'snow'],
    86: ['Heavy snow showers', 'snow'],
    95: ['Thunderstorm',       'thunder'],
    96: ['Thunderstorm, hail', 'thunder'],
    99: ['Thunderstorm, hail', 'thunder']
  };

  var NIGHT = { clear: 'clear-night', partly: 'partly-night' };

  var labels = null;  /* set by setLabels() on the Danish and Persian pages */

  function setLabels(map) { labels = map || null; }

  function label(code) {
    if (labels && labels[code]) { return labels[code]; }
    var e = CODES[code];
    return e ? e[0] : 'Unknown';
  }

  function icon(code, isDay) {
    var e = CODES[code];
    var id = e ? e[1] : 'cloudy';
    if (!isDay && NIGHT[id]) { id = NIGHT[id]; }
    return id;
  }

  /* ---------- units and formatting ---------- */

  var DAYS = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
  var MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
                'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  var digits = null;  /* Persian pages map 0-9 to ۰-۹ */

  function setLocale(loc) {
    if (loc && loc.days && loc.days.length === 7) { DAYS = loc.days; }
    if (loc && loc.months && loc.months.length === 12) { MONTHS = loc.months; }
    if (loc && loc.digits && loc.digits.length === 10) { digits = loc.digits; }
  }

  /* Applied at the very end of every formatter, so the arithmetic above stays
     in ASCII and only the rendered string is localised. */
  function tr(s) {
    if (!digits) { return s; }
    return String(s).replace(/[0-9]/g, function (d) { return digits[+d]; });
  }

  function num(v) { return (v === null || v === undefined || isNaN(v)) ? null : v; }

  function temp(c) {
    var v = num(c);
    return v === null ? '--' : tr(Math.round(v));
  }
  function tempUnit() { return '°C'; }
  function tempFull(c) { return temp(c) + tempUnit(); }

  function wind(ms) {
    var v = num(ms);
    if (v === null) { return '--'; }
    return tr(v < 10 ? v.toFixed(1) : Math.round(v)) + ' m/s';
  }

  function precip(mm) {
    var v = num(mm);
    if (v === null) { return '--'; }
    return tr(v < 10 ? v.toFixed(1) : Math.round(v)) + ' mm';
  }

  function percent(p) {
    var v = num(p);
    return v === null ? '--' : tr(Math.round(v)) + '%';
  }

  /* Open-Meteo returns local wall-clock strings already in the requested place's
     timezone. Parse the digits directly rather than via Date, so the visitor's
     own timezone cannot shift a forecast for a city on the other side of the
     world. This is the same rule the Android app follows. */
  function parseLocal(iso) {
    if (!iso) { return null; }
    var m = /^(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2}))?/.exec(iso);
    if (!m) { return null; }
    return {
      y: +m[1], mo: +m[2], d: +m[3],
      h: m[4] === undefined ? 0 : +m[4],
      mi: m[5] === undefined ? 0 : +m[5]
    };
  }

  function pad2(n) { return n < 10 ? '0' + n : String(n); }

  function clock(iso) {
    var t = parseLocal(iso);
    return t ? tr(pad2(t.h) + ':' + pad2(t.mi)) : '--:--';
  }
  function hourLabel(iso) {
    var t = parseLocal(iso);
    return t ? tr(pad2(t.h) + ':00') : '--';
  }

  /* Day of week without Date-parsing pitfalls: Sakamoto's algorithm. */
  function weekday(iso) {
    var t = parseLocal(iso);
    if (!t) { return ''; }
    var tbl = [0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4];
    var y = t.y;
    if (t.mo < 3) { y -= 1; }
    var idx = (y + Math.floor(y / 4) - Math.floor(y / 100) + Math.floor(y / 400) +
               tbl[t.mo - 1] + t.d) % 7;
    return DAYS[idx];
  }

  function dateLabel(iso) {
    var t = parseLocal(iso);
    return t ? tr(t.d) + ' ' + MONTHS[t.mo - 1] : '';
  }

  /* ---------- Open-Meteo ---------- */

  var CURRENT = ['temperature_2m', 'relative_humidity_2m', 'apparent_temperature', 'is_day',
                 'precipitation', 'weather_code', 'wind_speed_10m', 'wind_direction_10m',
                 'surface_pressure'];
  var HOURLY = ['temperature_2m', 'weather_code', 'precipitation_probability', 'is_day'];
  var DAILY = ['weather_code', 'temperature_2m_max', 'temperature_2m_min',
               'precipitation_probability_max', 'sunrise', 'sunset'];

  var CACHE_KEY = 'wx.site.cache.v1';

  function rows(block, fields) {
    var out = [];
    if (!block || !block.time) { return out; }
    for (var i = 0; i < block.time.length; i++) {
      var r = { time: block.time[i] };
      for (var f = 0; f < fields.length; f++) {
        var k = fields[f];
        r[k] = block[k] ? block[k][i] : null;
      }
      out.push(r);
    }
    return out;
  }

  function fetchForecast(place) {
    var url = 'https://api.open-meteo.com/v1/forecast' +
      '?latitude=' + place.latitude.toFixed(4) +
      '&longitude=' + place.longitude.toFixed(4) +
      '&current=' + CURRENT.join(',') +
      '&hourly=' + HOURLY.join(',') +
      '&daily=' + DAILY.join(',') +
      '&timezone=auto&wind_speed_unit=ms&forecast_days=7';

    return fetch(url, { cache: 'no-store' }).then(function (res) {
      if (!res.ok) { throw new Error('HTTP ' + res.status); }
      return res.json();
    }).then(function (raw) {
      var model = {
        place: place,
        utcOffset: typeof raw.utc_offset_seconds === 'number' ? raw.utc_offset_seconds : 0,
        current: raw.current || {},
        hourly: rows(raw.hourly, HOURLY),
        daily: rows(raw.daily, DAILY)
      };
      try { localStorage.setItem(CACHE_KEY, JSON.stringify(model)); } catch (e) { /* full */ }
      return model;
    });
  }

  function loadCached() {
    try {
      var m = JSON.parse(localStorage.getItem(CACHE_KEY) || 'null');
      return (m && m.daily && m.daily.length) ? m : null;
    } catch (e) { return null; }
  }

  function search(query, lang) {
    var q = String(query || '').trim();
    if (q.length < 2) { return Promise.resolve([]); }
    var url = 'https://geocoding-api.open-meteo.com/v1/search?name=' +
      encodeURIComponent(q) + '&count=6&language=' + (lang || 'en') + '&format=json';

    return fetch(url).then(function (res) {
      if (!res.ok) { throw new Error('HTTP ' + res.status); }
      return res.json();
    }).then(function (raw) {
      /* Open-Meteo omits `results` entirely when nothing matches. */
      return (raw.results || []).map(function (o) {
        return {
          name: o.name,
          country: o.country || '',
          admin1: o.admin1 || '',
          latitude: o.latitude,
          longitude: o.longitude
        };
      });
    });
  }

  global.WX = {
    label: label, icon: icon, setLabels: setLabels, setLocale: setLocale,
    temp: temp, tempUnit: tempUnit, tempFull: tempFull,
    wind: wind, precip: precip, percent: percent, tr: tr,
    parseLocal: parseLocal, pad2: pad2, clock: clock, hourLabel: hourLabel,
    weekday: weekday, dateLabel: dateLabel,
    fetchForecast: fetchForecast, loadCached: loadCached, search: search
  };
})(window);
