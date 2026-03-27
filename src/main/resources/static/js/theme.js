(function () {
  const STORAGE_KEY = 'fixbyte-theme';
  const media = window.matchMedia('(prefers-color-scheme: dark)');

  function resolveTheme(mode) {
    if (mode === 'light' || mode === 'dark') return mode;
    return media.matches ? 'dark' : 'light';
  }

  function getMode() {
    const saved = localStorage.getItem(STORAGE_KEY);
    return saved === 'light' || saved === 'dark' || saved === 'system' ? saved : 'system';
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

