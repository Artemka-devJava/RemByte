'use strict';

const { contextBridge, ipcRenderer } = require('electron');

// API для навигации по CRM
contextBridge.exposeInMainWorld('crmApp', {
  navigate: (path) => ipcRenderer.send('navigate', path),
  version: process.env.npm_package_version || '1.0.0',
});

// Добавляем класс electron-app для стилей специфичных для десктопа (если нужно).
// Смещение контента на 36px делается через bounds CRM-вью в main.js —
// CSS-инжекция больше не требуется, прыжок layout исключён.
window.addEventListener('DOMContentLoaded', () => {
  document.documentElement.classList.add('electron-app');
});
