/**
 * Конвертирует logo.png -> build/icon.ico
 * Запуск: node make-icon.js
 */
/**
 * Конвертирует logo.png -> build/icon.ico (256x256)
 * Запуск: node make-icon.js
 */
const sharp = require('sharp');
const toIco = require('to-ico');
const fs = require('fs');
const path = require('path');

const src  = path.join(__dirname, 'build', 'logo.png');
const dest = path.join(__dirname, 'build', 'icon.ico');

if (!fs.existsSync(src)) {
  console.error('❌ Файл не найден:', src);
  process.exit(1);
}

async function convert() {
  // Генерируем PNG нескольких размеров для ICO (16, 32, 48, 64, 128, 256)
  const sizes = [16, 32, 48, 64, 128, 256];
  const pngBuffers = await Promise.all(
    sizes.map(size =>
      sharp(src)
        .resize(size, size, { fit: 'contain', background: { r: 0, g: 0, b: 0, alpha: 0 } })
        .png()
        .toBuffer()
    )
  );

  const icoBuffer = await toIco(pngBuffers);
  fs.writeFileSync(dest, icoBuffer);
  const kb = (icoBuffer.length / 1024).toFixed(1);
  console.log(`✅ icon.ico создан (${kb} KB) из ${sizes.join(',')}px: ${dest}`);
}

convert().catch(err => {
  console.error('❌ Ошибка:', err.message);
  process.exit(1);
});

