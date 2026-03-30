'use strict';

const { app, BrowserWindow, shell, Menu, Tray, nativeImage, ipcMain, dialog } = require('electron');
const path = require('path');

// ── Конфигурация ─────────────────────────────────────────────────────────────
const CRM_URL = 'https://crm.fix-byte.ru';
const APP_NAME = 'FixByte CRM';
const ICON_PATH = path.join(__dirname, 'build', 'icon.ico');

let mainWindow = null;
let splashWindow = null;
let tray = null;

// ── Splash-экран при запуске ──────────────────────────────────────────────────
function createSplash() {
  splashWindow = new BrowserWindow({
    width: 400,
    height: 300,
    frame: false,
    transparent: true,
    alwaysOnTop: true,
    resizable: false,
    icon: ICON_PATH,
    webPreferences: { contextIsolation: true }
  });
  splashWindow.loadFile('splash.html');
  splashWindow.center();
}

// ── Главное окно ──────────────────────────────────────────────────────────────
function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1440,
    height: 900,
    minWidth: 900,
    minHeight: 600,
    title: APP_NAME,
    icon: ICON_PATH,
    show: false,
    backgroundColor: '#2d3d50',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      webSecurity: true,
      // Сохранять сессию (куки/логин) между запусками
      partition: 'persist:fixbyte-crm',
    },
    autoHideMenuBar: true,
  });

  // Загружаем CRM
  mainWindow.loadURL(CRM_URL);

  // Показываем окно после загрузки, закрываем splash
  mainWindow.webContents.once('did-finish-load', () => {
    if (splashWindow && !splashWindow.isDestroyed()) {
      splashWindow.close();
      splashWindow = null;
    }
    mainWindow.show();
    mainWindow.focus();
  });

  // Если загрузка упала (нет сети)
  mainWindow.webContents.on('did-fail-load', (_e, code, desc) => {
    if (splashWindow && !splashWindow.isDestroyed()) {
      splashWindow.close();
      splashWindow = null;
    }
    mainWindow.show();
    mainWindow.loadURL(`data:text/html;charset=utf-8,${encodeURIComponent(offlinePage(code, desc, CRM_URL))}`);
  });

  // Заголовок = title страницы
  mainWindow.webContents.on('page-title-updated', (_e, title) => {
    mainWindow.setTitle(title ? `${title} — ${APP_NAME}` : APP_NAME);
  });

  // Внешние ссылки → в браузере
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    if (!url.startsWith(CRM_URL) && !url.startsWith('about:')) {
      shell.openExternal(url);
      return { action: 'deny' };
    }
    return { action: 'allow' };
  });

  // При закрытии — сворачиваем в трей
  mainWindow.on('close', (e) => {
    if (!app.isQuiting) {
      e.preventDefault();
      mainWindow.hide();
    }
  });

  mainWindow.on('closed', () => { mainWindow = null; });
}

// ── Страница оффлайн ──────────────────────────────────────────────────────────
function offlinePage(code, desc, url) {
  return `<!DOCTYPE html>
<html lang="ru">
<head>
  <meta charset="UTF-8">
  <title>FixByte CRM — Нет подключения</title>
  <style>
    *{margin:0;padding:0;box-sizing:border-box;}
    body{font-family:system-ui,sans-serif;background:linear-gradient(135deg,#2d3d50 0%,#3699d9 100%);
         color:#fff;min-height:100vh;display:flex;align-items:center;justify-content:center;text-align:center;}
    .card{background:rgba(255,255,255,.1);backdrop-filter:blur(12px);border-radius:16px;
          padding:40px 48px;max-width:480px;}
    .icon{font-size:56px;margin-bottom:16px;}
    h1{font-size:22px;margin-bottom:8px;}
    p{font-size:14px;opacity:.75;margin-bottom:20px;}
    .url{font-size:13px;opacity:.55;margin-bottom:24px;word-break:break-all;}
    button{padding:12px 28px;background:#3699d9;border:none;border-radius:10px;
           color:#fff;font-size:15px;cursor:pointer;transition:.2s;}
    button:hover{background:#2d8bc0;}
  </style>
</head>
<body>
  <div class="card">
    <div class="icon">🔌</div>
    <h1>Нет подключения к CRM</h1>
    <p>Не удалось загрузить сервер. Проверьте интернет-соединение<br>или убедитесь, что сервер запущен.</p>
    <div class="url">${url}</div>
    <p style="font-size:12px;opacity:.45;margin-bottom:16px;">Ошибка: ${code} — ${desc}</p>
    <button onclick="location.href='${url}'">🔄 Повторить попытку</button>
  </div>
</body>
</html>`;
}

// ── Системный трей ────────────────────────────────────────────────────────────
function createTray() {
  try {
    const icon = nativeImage.createFromPath(ICON_PATH);
    tray = new Tray(icon.isEmpty() ? nativeImage.createEmpty() : icon);
  } catch {
    tray = new Tray(nativeImage.createEmpty());
  }

  const menu = Menu.buildFromTemplate([
    {
      label: `${APP_NAME}`,
      enabled: false,
    },
    { type: 'separator' },
    {
      label: '📂 Открыть',
      click: () => {
        if (mainWindow) { mainWindow.show(); mainWindow.focus(); }
        else createWindow();
      }
    },
    {
      label: '🔄 Обновить',
      click: () => { mainWindow?.webContents.reload(); mainWindow?.show(); }
    },
    { type: 'separator' },
    {
      label: '🚪 Выйти',
      click: () => {
        app.isQuiting = true;
        app.quit();
      }
    }
  ]);

  tray.setToolTip(APP_NAME);
  tray.setContextMenu(menu);
  tray.on('click', () => {
    if (mainWindow) { mainWindow.show(); mainWindow.focus(); }
    else createWindow();
  });
}

// ── Меню приложения ───────────────────────────────────────────────────────────
function buildAppMenu() {
  const template = [
    {
      label: 'Приложение',
      submenu: [
        { label: '🏠 Главная', click: () => mainWindow?.loadURL(CRM_URL) },
        { label: '📊 Дашборд', click: () => mainWindow?.loadURL(`${CRM_URL}/dashboard`) },
        { label: '👥 Клиенты', click: () => mainWindow?.loadURL(`${CRM_URL}/clients`) },
        { label: '📋 Заказы', click: () => mainWindow?.loadURL(`${CRM_URL}/orders`) },
        { type: 'separator' },
        { label: '🔄 Обновить', accelerator: 'F5', click: () => mainWindow?.webContents.reload() },
        { type: 'separator' },
        {
          label: '🚪 Выйти', accelerator: 'Alt+F4', click: () => {
            app.isQuiting = true;
            app.quit();
          }
        }
      ]
    },
    {
      label: 'Вид',
      submenu: [
        { label: 'Назад', accelerator: 'Alt+Left', click: () => mainWindow?.webContents.goBack() },
        { label: 'Вперёд', accelerator: 'Alt+Right', click: () => mainWindow?.webContents.goForward() },
        { type: 'separator' },
        { label: 'Увеличить', accelerator: 'CmdOrCtrl+Equal', role: 'zoomIn' },
        { label: 'Уменьшить', accelerator: 'CmdOrCtrl+Minus', role: 'zoomOut' },
        { label: 'Сбросить масштаб', accelerator: 'CmdOrCtrl+0', role: 'resetZoom' },
        { type: 'separator' },
        { label: 'Полный экран', accelerator: 'F11', role: 'togglefullscreen' },
      ]
    },
    {
      label: 'Разработчик',
      submenu: [
        { label: 'Инструменты разработчика', accelerator: 'F12', role: 'toggleDevTools' },
      ]
    }
  ];
  Menu.setApplicationMenu(Menu.buildFromTemplate(template));
}

// ── IPC ───────────────────────────────────────────────────────────────────────
ipcMain.on('navigate', (_e, url) => {
  mainWindow?.loadURL(url.startsWith('http') ? url : `${CRM_URL}${url}`);
});

// ── Одиночный экземпляр ───────────────────────────────────────────────────────
const gotLock = app.requestSingleInstanceLock();
if (!gotLock) {
  app.quit();
} else {
  app.on('second-instance', () => {
    if (mainWindow) {
      if (mainWindow.isMinimized()) mainWindow.restore();
      mainWindow.show();
      mainWindow.focus();
    }
  });
}

// ── Жизненный цикл ───────────────────────────────────────────────────────────
app.whenReady().then(() => {
  buildAppMenu();
  createSplash();
  createWindow();
  createTray();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  // Не выходить — сидеть в трее
});

app.on('before-quit', () => {
  app.isQuiting = true;
});

