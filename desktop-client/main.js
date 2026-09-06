'use strict';

const { app, BrowserWindow, shell, Menu, Tray, nativeImage, ipcMain, dialog } = require('electron');
const path = require('path');
const fs = require('fs');

// ── Конфигурация ─────────────────────────────────────────────────────────────
const LOCAL_CRM_URL = 'http://localhost:9087';
const CLOUD_CRM_URL = 'https://crm.fix-byte.ru';
let CRM_URL = process.env.NODE_ENV === 'production' ? CLOUD_CRM_URL : LOCAL_CRM_URL;
const APP_NAME = 'FixByte CRM';
const ICON_PATH = path.join(__dirname, 'build', 'icon.ico');
const DEFAULT_WINDOW_SIZE = { startupWidth: 1440, startupHeight: 900 };

let mainWindow = null;
let splashWindow = null;
let tray = null;
let settingsWindow = null;
let activeDownloads = new Set();
let titleBeforeDownload = APP_NAME;

const CRM_NAV_ITEMS = [
  { label: '🏠 Главная',    route: '/' },
  { label: '📊 Панель',     route: '/dashboard' },
  { label: '👥 Клиенты',    route: '/clients' },
  { label: '📋 Заказы',     route: '/orders' },
  { label: '🔧 Услуги',     route: '/services' },
  { label: '💰 Калькулятор',route: '/calculator' },
  { label: '💬 Чат',        route: '/chat' },
  { label: '🗂️ Канбан',    route: '/kanban' },
  { label: '⚙️ Настройки', route: '/admin' },
];

// ── Вспомогательные функции ───────────────────────────────────────────────────
function normalizeBaseUrl(url) {
  return String(url || '').replace(/\/$/, '');
}

function navigateToRoute(route = '/') {
  if (!mainWindow || mainWindow.isDestroyed()) { createWindow(); return; }
  const base = normalizeBaseUrl(CRM_URL);
  const p = route.startsWith('/') ? route : `/${route}`;
  mainWindow.loadURL(`${base}${p}`);
}

function quitApplication() {
  app.isQuiting = true;
  tray?.destroy(); tray = null;
  if (settingsWindow && !settingsWindow.isDestroyed()) { settingsWindow.destroy(); settingsWindow = null; }
  if (mainWindow && !mainWindow.isDestroyed()) { mainWindow.removeAllListeners('close'); mainWindow.destroy(); mainWindow = null; }
  app.exit(0);
}

function updateDownloadProgressUI() {
  if (!mainWindow || mainWindow.isDestroyed()) return;
  const downloads = Array.from(activeDownloads);
  if (!downloads.length) { mainWindow.setProgressBar(-1); mainWindow.setTitle(titleBeforeDownload || APP_NAME); return; }
  const known = downloads.filter(d => d.totalBytes > 0);
  if (!known.length) { mainWindow.setProgressBar(2); mainWindow.setTitle(`Скачивание… — ${APP_NAME}`); return; }
  const recv  = known.reduce((s, d) => s + d.receivedBytes, 0);
  const total = known.reduce((s, d) => s + d.totalBytes,    0);
  const pct   = total > 0 ? Math.min(recv / total, 1) : 0;
  mainWindow.setProgressBar(pct);
  mainWindow.setTitle(`Скачивание ${Math.round(pct * 100)}% — ${APP_NAME}`);
}

function setupDownloadTracking(windowRef) {
  const ses = windowRef?.webContents?.session;
  if (!ses || ses.__fixbyteDownloadTrackingAttached) return;
  ses.__fixbyteDownloadTrackingAttached = true;
  ses.on('will-download', (_event, item) => {
    if (!mainWindow || mainWindow.isDestroyed()) return;
    titleBeforeDownload = mainWindow.getTitle() || APP_NAME;
    const isBackup = (item.getURL() || '').includes('/admin/backup');
    const dl = { id: `${Date.now()}_${Math.random().toString(36).slice(2,8)}`, isBackup, receivedBytes: item.getReceivedBytes(), totalBytes: item.getTotalBytes() };
    activeDownloads.add(dl);
    updateDownloadProgressUI();
    item.on('updated', (_e, state) => { if (state === 'interrupted') return; dl.receivedBytes = item.getReceivedBytes(); dl.totalBytes = item.getTotalBytes(); updateDownloadProgressUI(); });
    item.once('done', (_e, state) => {
      activeDownloads.delete(dl);
      updateDownloadProgressUI();
      if (dl.isBackup) mainWindow.setTitle(titleBeforeDownload || APP_NAME);
      if (dl.isBackup) dialog.showMessageBox({ type: state === 'completed' ? 'info' : 'warning', title: state === 'completed' ? 'Бэкап скачан' : 'Скачивание прервано', message: state === 'completed' ? 'Резервная копия успешно скачана.' : 'Не удалось скачать резервную копию.', detail: state === 'completed' ? `Файл сохранен: ${item.getSavePath()}` : `Статус: ${state}` });
    });
  });
}

// ── Настройки ─────────────────────────────────────────────────────────────────
function getSettingsPath() {
  return path.join(app.getPath('userData'), 'desktop-settings.json');
}

function normalizeWindowSize(settings) {
  const width  = Number(settings?.startupWidth);
  const height = Number(settings?.startupHeight);
  let customServerUrl = (settings?.customServerUrl || '').trim()
    .replace(/^http:\/\/localhost:8080\/?$/i, LOCAL_CRM_URL)
    .replace(/^http:\/\/127\.0\.0\.1:8080\/?$/i, 'http://127.0.0.1:9087')
    .replace(/^https:\/\/(localhost|127\.0\.0\.1):9087\/?$/i, 'http://$1:9087');
  return {
    startupWidth:  Number.isFinite(width)  ? Math.min(Math.max(Math.round(width),  900), 3840) : DEFAULT_WINDOW_SIZE.startupWidth,
    startupHeight: Number.isFinite(height) ? Math.min(Math.max(Math.round(height), 600), 2160) : DEFAULT_WINDOW_SIZE.startupHeight,
    launchFullscreen: settings?.launchFullscreen === true,
    serverMode:    settings?.serverMode    || 'auto',
    customServerUrl,
  };
}

function loadDesktopSettings() {
  try { return normalizeWindowSize(JSON.parse(fs.readFileSync(getSettingsPath(), 'utf-8'))); }
  catch { return normalizeWindowSize(DEFAULT_WINDOW_SIZE); }
}

function saveDesktopSettings(next) {
  const s = normalizeWindowSize(next);
  fs.writeFileSync(getSettingsPath(), JSON.stringify(s, null, 2), 'utf-8');
  return s;
}

function getServerUrlFromSettings(settings) {
  const mode = settings?.serverMode || 'auto';
  const custom = (settings?.customServerUrl || '').trim();
  if (mode === 'local')  return LOCAL_CRM_URL;
  if (mode === 'cloud')  return CLOUD_CRM_URL;
  if (mode === 'custom' && custom) return custom;
  return process.env.NODE_ENV === 'production' ? CLOUD_CRM_URL : LOCAL_CRM_URL;
}

function resolveReloadUrlForServerSwitch(from, to) {
  if (!mainWindow || mainWindow.isDestroyed()) return normalizeBaseUrl(to);
  const cur = mainWindow.webContents.getURL() || '';
  if (!cur || cur.startsWith('data:')) return to;
  if (cur.startsWith(from)) { const suffix = cur.slice(from.length) || '/'; return `${to}${suffix.startsWith('/') ? suffix : `/${suffix}`}`; }
  return to;
}

function applyRuntimeDesktopSettings(saved, previousServerUrl) {
  if (!mainWindow || mainWindow.isDestroyed()) return;
  if (!mainWindow.isFullScreen() && !saved.launchFullscreen) { mainWindow.setSize(saved.startupWidth, saved.startupHeight); mainWindow.center(); }
  if (mainWindow.isFullScreen() !== saved.launchFullscreen) mainWindow.setFullScreen(saved.launchFullscreen === true);
  const nextServerUrl = getServerUrlFromSettings(saved);
  const prevBase = normalizeBaseUrl(previousServerUrl || CRM_URL);
  const nextBase = normalizeBaseUrl(nextServerUrl);
  CRM_URL = nextBase;
  if (prevBase !== nextBase) mainWindow.loadURL(resolveReloadUrlForServerSwitch(prevBase, nextBase));
}

// ── Splash ────────────────────────────────────────────────────────────────────
function createSplash() {
  splashWindow = new BrowserWindow({ width: 400, height: 300, frame: false, transparent: true, alwaysOnTop: true, resizable: false, icon: ICON_PATH, webPreferences: { contextIsolation: true } });
  splashWindow.loadFile('splash.html');
  splashWindow.center();
}

// ── Главное окно ──────────────────────────────────────────────────────────────
function createWindow() {
  const settings = loadDesktopSettings();
  CRM_URL = getServerUrlFromSettings(settings);
  console.log(`[CRM] Сервер: ${CRM_URL} (режим: ${settings.serverMode || 'auto'})`);

  mainWindow = new BrowserWindow({
    width:     settings.startupWidth,
    height:    settings.startupHeight,
    minWidth:  900,
    minHeight: 600,
    title:     APP_NAME,
    icon:      ICON_PATH,
    show:      false,
    webPreferences: {
      preload:          path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration:  false,
      webSecurity:      true,
      partition:        'persist:fixbyte-crm',
    },
    autoHideMenuBar: true,
    skipTaskbar:     false,
    fullscreen:      settings.launchFullscreen === true,
  });

  setupDownloadTracking(mainWindow);
  mainWindow.loadURL(CRM_URL);

  mainWindow.webContents.once('did-finish-load', () => {
    if (splashWindow && !splashWindow.isDestroyed()) { splashWindow.close(); splashWindow = null; }
    mainWindow.show();
    mainWindow.focus();
  });

  mainWindow.webContents.on('did-fail-load', (_e, code, desc) => {
    if (splashWindow && !splashWindow.isDestroyed()) { splashWindow.close(); splashWindow = null; }
    mainWindow.show();
    mainWindow.loadURL(`data:text/html;charset=utf-8,${encodeURIComponent(offlinePage(code, desc, CRM_URL))}`);
  });

  mainWindow.webContents.on('page-title-updated', (_e, title) => {
    mainWindow.setTitle(title ? `${title} — ${APP_NAME}` : APP_NAME);
  });

  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    if (!url.startsWith(CRM_URL) && !url.startsWith('about:')) { shell.openExternal(url); return { action: 'deny' }; }
    return { action: 'allow' };
  });

  mainWindow.on('close', (e) => {
    if (!app.isQuiting) {
      e.preventDefault();
      const action = dialog.showMessageBoxSync(mainWindow, { type: 'question', buttons: ['Отмена', 'Закрыть'], defaultId: 0, cancelId: 0, title: 'Выход', message: 'Закрыть FixByte CRM?', detail: 'Приложение будет полностью завершено.' });
      if (action === 1) quitApplication();
    }
  });

  mainWindow.on('closed', () => { mainWindow = null; });
}

function openSettingsWindow() {
  if (settingsWindow && !settingsWindow.isDestroyed()) { settingsWindow.show(); settingsWindow.focus(); return; }
  settingsWindow = new BrowserWindow({ width: 620, height: 560, minWidth: 520, minHeight: 460, resizable: true, title: 'Настройки клиента', icon: ICON_PATH, parent: mainWindow || undefined, modal: false, webPreferences: { preload: path.join(__dirname, 'settings-preload.js'), contextIsolation: true, nodeIntegration: false, webSecurity: true } });
  settingsWindow.setMenuBarVisibility(false);
  settingsWindow.loadFile('settings.html');
  settingsWindow.on('closed', () => { settingsWindow = null; });
}

// ── Offline-страница ──────────────────────────────────────────────────────────
function offlinePage(code, desc, url) {
  return `<!DOCTYPE html><html lang="ru"><head><meta charset="UTF-8"><title>FixByte CRM — Нет подключения</title>
  <style>*{margin:0;padding:0;box-sizing:border-box;}body{font-family:system-ui,sans-serif;background:linear-gradient(135deg,#2d3d50 0%,#3699d9 100%);color:#fff;min-height:100vh;display:flex;align-items:center;justify-content:center;text-align:center;}.card{background:rgba(255,255,255,.1);backdrop-filter:blur(12px);border-radius:16px;padding:40px 48px;max-width:480px;}.icon{font-size:56px;margin-bottom:16px;}h1{font-size:22px;margin-bottom:8px;}p{font-size:14px;opacity:.75;margin-bottom:20px;}.url{font-size:13px;opacity:.55;margin-bottom:24px;word-break:break-all;}button{padding:12px 28px;background:#3699d9;border:none;border-radius:10px;color:#fff;font-size:15px;cursor:pointer;}</style>
  </head><body><div class="card"><div class="icon">🔌</div><h1>Нет подключения к CRM</h1><p>Проверьте соединение или убедитесь что сервер запущен.</p><div class="url">${url}</div><p style="font-size:12px;opacity:.45;margin-bottom:16px;">Ошибка: ${code} — ${desc}</p><button onclick="location.href='${url}'">🔄 Повторить</button></div></body></html>`;
}

// ── Трей ──────────────────────────────────────────────────────────────────────
function createTray() {
  try { const icon = nativeImage.createFromPath(ICON_PATH); tray = new Tray(icon.isEmpty() ? nativeImage.createEmpty() : icon); }
  catch { tray = new Tray(nativeImage.createEmpty()); }
  const menu = Menu.buildFromTemplate([
    { label: APP_NAME, enabled: false },
    { type: 'separator' },
    { label: '📂 Открыть',  click: () => { if (mainWindow) { mainWindow.show(); mainWindow.focus(); navigateToRoute('/'); } else createWindow(); } },
    { label: '🔄 Обновить', click: () => { mainWindow?.webContents.reload(); mainWindow?.show(); } },
    { label: '⚙️ Настройки клиента', click: () => openSettingsWindow() },
    { type: 'separator' },
    { label: '🚪 Выйти',   click: () => quitApplication() },
  ]);
  tray.setToolTip(APP_NAME);
  tray.setContextMenu(menu);
  tray.on('click', () => { if (mainWindow) { mainWindow.show(); mainWindow.focus(); } else createWindow(); });
}

// ── Меню приложения ───────────────────────────────────────────────────────────
function buildAppMenu() {
  Menu.setApplicationMenu(Menu.buildFromTemplate([
    { label: 'Приложение', submenu: [
      ...CRM_NAV_ITEMS.map(i => ({ label: i.label, click: () => navigateToRoute(i.route) })),
      { type: 'separator' },
      { label: '⚙️ Настройки клиента', accelerator: 'CmdOrCtrl+,', click: () => openSettingsWindow() },
      { type: 'separator' },
      { label: '🔄 Обновить', accelerator: 'F5', click: () => mainWindow?.webContents.reload() },
      { type: 'separator' },
      { label: '🚪 Выйти', accelerator: 'Alt+F4', click: () => quitApplication() },
    ]},
    { label: 'Вид', submenu: [
      { label: 'Назад',          accelerator: 'Alt+Left',  click: () => mainWindow?.webContents.goBack() },
      { label: 'Вперёд',         accelerator: 'Alt+Right', click: () => mainWindow?.webContents.goForward() },
      { type: 'separator' },
      { label: 'Увеличить',      accelerator: 'CmdOrCtrl+Equal', role: 'zoomIn' },
      { label: 'Уменьшить',      accelerator: 'CmdOrCtrl+Minus', role: 'zoomOut' },
      { label: 'Сбросить масштаб',accelerator: 'CmdOrCtrl+0',   role: 'resetZoom' },
      { type: 'separator' },
      { label: 'Полный экран',   accelerator: 'F11', role: 'togglefullscreen' },
    ]},
    { label: 'Разработчик', submenu: [
      { label: 'Инструменты разработчика', accelerator: 'F12', role: 'toggleDevTools' },
    ]},
  ]));
}

// ── IPC ───────────────────────────────────────────────────────────────────────
ipcMain.on('navigate', (_e, url) => {
  mainWindow?.loadURL(url.startsWith('http') ? url : `${CRM_URL}${url}`);
});

ipcMain.handle('desktop-settings:get', () => loadDesktopSettings());

ipcMain.handle('desktop-settings:save', (_e, settings) => {
  const previousServerUrl = CRM_URL;
  const saved = saveDesktopSettings(settings);
  applyRuntimeDesktopSettings(saved, previousServerUrl);
  const serverInfo = saved.serverMode === 'custom' ? `пользовательский (${saved.customServerUrl})` : saved.serverMode === 'local' ? 'локальный (localhost:9087)' : saved.serverMode === 'cloud' ? 'облачный (crm.fix-byte.ru)' : 'автоматический';
  const windowMode = saved.launchFullscreen ? 'полный экран' : `${saved.startupWidth}×${saved.startupHeight}`;
  dialog.showMessageBox({ type: 'info', title: 'Настройки сохранены', message: 'Настройки клиента обновлены.', detail: `Сервер: ${serverInfo}\nОкно: ${windowMode}` });
  return saved;
});

ipcMain.handle('desktop-settings:close', () => {
  if (settingsWindow && !settingsWindow.isDestroyed()) settingsWindow.close();
});

// ── Одиночный экземпляр ───────────────────────────────────────────────────────
const gotLock = app.requestSingleInstanceLock();
if (!gotLock) { app.quit(); }
else {
  app.on('second-instance', () => {
    if (mainWindow) { if (mainWindow.isMinimized()) mainWindow.restore(); mainWindow.show(); mainWindow.focus(); }
  });
}

// ── Жизненный цикл ───────────────────────────────────────────────────────────
app.whenReady().then(() => {
  buildAppMenu();
  createSplash();
  createWindow();
  createTray();
  app.on('activate', () => { if (BrowserWindow.getAllWindows().length === 0) createWindow(); });
});

app.on('window-all-closed', () => { /* сидим в трее */ });
app.on('before-quit', () => { app.isQuiting = true; });
