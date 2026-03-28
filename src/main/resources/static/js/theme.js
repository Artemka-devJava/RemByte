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

  function syncSelectors(mode) {
    document.querySelectorAll('[data-theme-select]').forEach((select) => {
      if (select.value !== mode) select.value = mode;
    });
  }

  function setMode(mode) {
    localStorage.setItem(STORAGE_KEY, mode);
    applyTheme(mode);
    syncSelectors(mode);
  }

  function initSelectors() {
    const mode = getMode();
    document.querySelectorAll('[data-theme-select]').forEach((select) => {
      select.value = mode;
      select.addEventListener('change', function () {
        setMode(this.value);
      });
    });
  }

  // Public API for optional manual usage.
  window.ThemeManager = {
    getMode,
    setMode,
    applyTheme
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

  document.addEventListener('DOMContentLoaded', initSelectors);
})();

