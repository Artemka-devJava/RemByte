(function () {
  const STORAGE_KEY = 'fixbyte-theme';
  const media = window.matchMedia('(prefers-color-scheme: dark)');
  const VALID_THEMES = ['system', 'light', 'dark', 'green', 'purple', 'ocean', 'sunset', 'fixbyte'];

  function resolveTheme(mode) {
    if (mode === 'system') return media.matches ? 'dark' : 'light';
    return VALID_THEMES.includes(mode) ? mode : 'light';
  }

  function getMode() {
    const saved = localStorage.getItem(STORAGE_KEY);
    return VALID_THEMES.includes(saved) ? saved : 'system';
  }

  function applyTheme(mode) {
    const effective = resolveTheme(mode);
    document.documentElement.setAttribute('data-theme', effective);
  }

  function setMode(mode) {
    const normalized = VALID_THEMES.includes(mode) ? mode : 'system';
    localStorage.setItem(STORAGE_KEY, normalized);
    applyTheme(normalized);
  }

  function initAdminThemeControl() {
    const select = document.querySelector('[data-theme-admin-select]');
    if (!select) return;

    const mode = getMode();
    select.value = mode;
    select.style.visibility = 'visible';

  }

  // Public API for optional manual usage.
  window.ThemeManager = {
    getValidThemes: function () {
      return VALID_THEMES.slice();
    },
    getMode,
    setMode,
    applyTheme,
    initAdminThemeControl
  };

  applyTheme(getMode());
  if (media.addEventListener) {
    media.addEventListener('change', function () {
      if (getMode() === 'system') applyTheme('system');
    });
  } else if (media.addListener) {
    media.addListener(function () {
      if (getMode() === 'system') applyTheme('system');
    });
  }

  document.addEventListener('DOMContentLoaded', initAdminThemeControl);
})();

