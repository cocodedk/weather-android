/* The weather icon sprite, shared by all three language pages.
   Same 64x64 geometry the Android app draws to its Canvas — the artwork is
   the app's, not a lookalike. Injected from JS so one copy serves every page;
   an external <use href="sprite.svg#id"> is not reliably supported. */
(function () {
  'use strict';
  var SPRITE = `<svg id="sprite" width="0" height="0" aria-hidden="true" focusable="false"><defs>

  <symbol id="i-clear" viewBox="0 0 64 64">
    <circle cx="32" cy="32" r="12" fill="currentColor"/>
    <g stroke="currentColor" stroke-width="4" stroke-linecap="round" fill="none">
      <line x1="32" y1="4" x2="32" y2="12"/><line x1="32" y1="52" x2="32" y2="60"/>
      <line x1="4" y1="32" x2="12" y2="32"/><line x1="52" y1="32" x2="60" y2="32"/>
      <line x1="12.2" y1="12.2" x2="17.9" y2="17.9"/><line x1="46.1" y1="46.1" x2="51.8" y2="51.8"/>
      <line x1="12.2" y1="51.8" x2="17.9" y2="46.1"/><line x1="46.1" y1="17.9" x2="51.8" y2="12.2"/>
    </g>
  </symbol>

  <symbol id="i-clear-night" viewBox="0 0 64 64">
    <path d="M56 34.1A24 24 0 1 1 29.9 8 18.7 18.7 0 0 0 56 34.1z" fill="currentColor"/>
  </symbol>

  <symbol id="i-cloudy" viewBox="0 0 64 64">
    <g fill="currentColor">
      <circle cx="26" cy="26" r="11"/><circle cx="41" cy="30" r="9"/>
      <rect x="14" y="30" width="36" height="13" rx="6.5"/>
    </g>
  </symbol>

  <symbol id="i-partly" viewBox="0 0 64 64">
    <circle cx="43" cy="20" r="9" fill="currentColor"/>
    <g stroke="currentColor" stroke-width="3" stroke-linecap="round" fill="none">
      <line x1="43" y1="3" x2="43" y2="7"/><line x1="58" y1="20" x2="62" y2="20"/>
      <line x1="54.3" y1="9.7" x2="57" y2="7"/><line x1="31.7" y1="9.7" x2="29" y2="7"/>
    </g>
    <g fill="currentColor">
      <circle cx="23" cy="35" r="10"/><circle cx="36" cy="38" r="8"/>
      <rect x="13" y="38" width="31" height="12" rx="6"/>
    </g>
  </symbol>

  <symbol id="i-partly-night" viewBox="0 0 64 64">
    <g transform="translate(30 2) scale(0.5)">
      <path d="M56 34.1A24 24 0 1 1 29.9 8 18.7 18.7 0 0 0 56 34.1z" fill="currentColor"/>
    </g>
    <g fill="currentColor">
      <circle cx="23" cy="35" r="10"/><circle cx="36" cy="38" r="8"/>
      <rect x="13" y="38" width="31" height="12" rx="6"/>
    </g>
  </symbol>

  <symbol id="i-fog" viewBox="0 0 64 64">
    <g fill="currentColor">
      <circle cx="26" cy="22" r="10"/><circle cx="39" cy="26" r="8"/>
      <rect x="15" y="26" width="33" height="12" rx="6"/>
    </g>
    <g stroke="currentColor" stroke-width="4" stroke-linecap="round" fill="none" opacity="0.75">
      <line x1="14" y1="46" x2="50" y2="46"/><line x1="20" y1="54" x2="44" y2="54"/>
    </g>
  </symbol>

  <symbol id="i-drizzle" viewBox="0 0 64 64">
    <g fill="currentColor">
      <circle cx="26" cy="22" r="10"/><circle cx="39" cy="26" r="8"/>
      <rect x="15" y="26" width="33" height="12" rx="6"/>
    </g>
    <g stroke="currentColor" stroke-width="4" stroke-linecap="round" fill="none">
      <line x1="22" y1="45" x2="20" y2="50"/><line x1="32" y1="45" x2="30" y2="50"/>
      <line x1="42" y1="45" x2="40" y2="50"/>
    </g>
  </symbol>

  <symbol id="i-rain" viewBox="0 0 64 64">
    <g fill="currentColor">
      <circle cx="26" cy="22" r="10"/><circle cx="39" cy="26" r="8"/>
      <rect x="15" y="26" width="33" height="12" rx="6"/>
    </g>
    <g stroke="currentColor" stroke-width="4" stroke-linecap="round" fill="none">
      <line x1="23" y1="44" x2="19" y2="56"/><line x1="33" y1="44" x2="29" y2="56"/>
      <line x1="43" y1="44" x2="39" y2="56"/>
    </g>
  </symbol>

  <symbol id="i-showers" viewBox="0 0 64 64">
    <circle cx="47" cy="14" r="7" fill="currentColor"/>
    <g stroke="currentColor" stroke-width="3" stroke-linecap="round" fill="none">
      <line x1="47" y1="2" x2="47" y2="5"/><line x1="58" y1="14" x2="61" y2="14"/>
      <line x1="55.5" y1="5.5" x2="58" y2="3"/>
    </g>
    <g fill="currentColor">
      <circle cx="24" cy="26" r="10"/><circle cx="37" cy="30" r="8"/>
      <rect x="13" y="30" width="33" height="12" rx="6"/>
    </g>
    <g stroke="currentColor" stroke-width="4" stroke-linecap="round" fill="none">
      <line x1="22" y1="48" x2="18" y2="58"/><line x1="34" y1="48" x2="30" y2="58"/>
    </g>
  </symbol>

  <symbol id="i-sleet" viewBox="0 0 64 64">
    <g fill="currentColor">
      <circle cx="26" cy="22" r="10"/><circle cx="39" cy="26" r="8"/>
      <rect x="15" y="26" width="33" height="12" rx="6"/>
    </g>
    <g stroke="currentColor" stroke-width="4" stroke-linecap="round" fill="none">
      <line x1="24" y1="44" x2="20" y2="56"/><line x1="42" y1="44" x2="38" y2="56"/>
      <line x1="28" y1="50" x2="36" y2="50"/><line x1="32" y1="46" x2="32" y2="54"/>
    </g>
  </symbol>

  <symbol id="i-snow" viewBox="0 0 64 64">
    <g fill="currentColor">
      <circle cx="26" cy="22" r="10"/><circle cx="39" cy="26" r="8"/>
      <rect x="15" y="26" width="33" height="12" rx="6"/>
    </g>
    <g stroke="currentColor" stroke-width="3.5" stroke-linecap="round" fill="none">
      <line x1="18" y1="50" x2="26" y2="50"/><line x1="22" y1="46" x2="22" y2="54"/>
      <line x1="19.2" y1="47.2" x2="24.8" y2="52.8"/><line x1="24.8" y1="47.2" x2="19.2" y2="52.8"/>
      <line x1="38" y1="50" x2="46" y2="50"/><line x1="42" y1="46" x2="42" y2="54"/>
      <line x1="39.2" y1="47.2" x2="44.8" y2="52.8"/><line x1="44.8" y1="47.2" x2="39.2" y2="52.8"/>
    </g>
  </symbol>

  <symbol id="i-thunder" viewBox="0 0 64 64">
    <g fill="currentColor">
      <circle cx="26" cy="22" r="10"/><circle cx="39" cy="26" r="8"/>
      <rect x="15" y="26" width="33" height="12" rx="6"/>
    </g>
    <path d="M34 41h9l-7 9h8L26 62l5-10h-6z" fill="currentColor"/>
  </symbol>

  <symbol id="i-wind" viewBox="0 0 64 64">
    <g stroke="currentColor" stroke-width="4.5" stroke-linecap="round" fill="none">
      <path d="M6 22h30a7 7 0 1 0-7-7"/><path d="M6 34h40a7 7 0 1 1-7 7"/><path d="M6 46h22"/>
    </g>
  </symbol>

  <symbol id="i-drop" viewBox="0 0 64 64">
    <path d="M32 6s16 18 16 28a16 16 0 0 1-32 0C16 24 32 6 32 6z" fill="currentColor"/>
  </symbol>

  <symbol id="i-thermo" viewBox="0 0 64 64">
    <g fill="currentColor">
      <path d="M32 4a9 9 0 0 0-9 9v22a14 14 0 1 0 18 0V13a9 9 0 0 0-9-9zm0 6a3 3 0 0 1 3 3v25.4l1.9 1.3a8 8 0 1 1-9.8 0l1.9-1.3V13a3 3 0 0 1 3-3z"/>
      <circle cx="32" cy="46" r="6"/>
    </g>
  </symbol>

  <symbol id="i-gauge" viewBox="0 0 64 64">
    <g stroke="currentColor" stroke-width="4.5" fill="none" stroke-linecap="round">
      <path d="M8 44a24 24 0 1 1 48 0"/><line x1="32" y1="44" x2="45" y2="28"/>
    </g>
    <circle cx="32" cy="44" r="4" fill="currentColor"/>
  </symbol>

  <symbol id="i-sunrise" viewBox="0 0 64 64">
    <circle cx="32" cy="40" r="10" fill="currentColor"/>
    <g stroke="currentColor" stroke-width="4" stroke-linecap="round" fill="none">
      <line x1="6" y1="54" x2="58" y2="54"/><line x1="32" y1="8" x2="32" y2="18"/>
      <path d="M22 22l-6-6"/><path d="M42 22l6-6"/>
    </g>
  </symbol>

  <symbol id="i-uv" viewBox="0 0 64 64">
    <circle cx="32" cy="32" r="10" fill="currentColor"/>
    <g stroke="currentColor" stroke-width="4" stroke-linecap="round" fill="none">
      <line x1="32" y1="6" x2="32" y2="14"/><line x1="32" y1="50" x2="32" y2="58"/>
      <line x1="6" y1="32" x2="14" y2="32"/><line x1="50" y1="32" x2="58" y2="32"/>
      <line x1="13.6" y1="13.6" x2="19.2" y2="19.2"/><line x1="44.8" y1="44.8" x2="50.4" y2="50.4"/>
      <line x1="13.6" y1="50.4" x2="19.2" y2="44.8"/><line x1="44.8" y1="19.2" x2="50.4" y2="13.6"/>
    </g>
  </symbol>

</defs></svg>`;
  function inject() {
    var host = document.createElement('div');
    host.style.cssText = 'position:absolute;width:0;height:0;overflow:hidden';
    host.setAttribute('aria-hidden', 'true');
    host.innerHTML = SPRITE;
    document.body.insertBefore(host, document.body.firstChild);
  }
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', inject, false);
  } else { inject(); }
})();
