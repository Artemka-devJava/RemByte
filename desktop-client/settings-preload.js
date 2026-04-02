'use strict';

const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('desktopSettings', {
  load: () => ipcRenderer.invoke('desktop-settings:get'),
  save: (payload) => ipcRenderer.invoke('desktop-settings:save', payload),
  close: () => ipcRenderer.invoke('desktop-settings:close')
});

