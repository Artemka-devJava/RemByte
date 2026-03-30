'use strict';

const { contextBridge, ipcRenderer } = require('electron');

// Открываем безопасное API для renderer-процесса
contextBridge.exposeInMainWorld('crmApp', {
  // Навигация по CRM
  navigate: (path) => ipcRenderer.send('navigate', path),
  // Версия приложения
  version: process.env.npm_package_version || '3.0.0',
});

window.addEventListener('DOMContentLoaded', () => {
  // Добавляем класс, что запущено в Electron
  document.documentElement.classList.add('electron-app');

  // Показываем версию в title если не задана
  if (!document.title) {
    document.title = 'FixByte CRM';
  }
});

