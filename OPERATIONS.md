# OPERATIONS — эксплуатация FixByte CRM

Документ для администрирования, деплоя и восстановления системы.

## 1) Конфигурация окружения

Рекомендуется хранить секреты и prod-настройки в `.env`.

Минимальные переменные:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `FIXBYTE_ADMIN_USERNAME`
- `FIXBYTE_ADMIN_PASSWORD`
- `FIXBYTE_OPERATOR_USERNAME`
- `FIXBYTE_OPERATOR_PASSWORD`

Пример URL для локальной MariaDB:

- `jdbc:mariadb://localhost:9092/rembyte?useUnicode=true&characterEncoding=UTF-8`

## 2) Локальный запуск

```powershell
Set-Location "C:\JavaProject\RemByte"
mvn spring-boot:run
```

Проверка:

- Web: `http://localhost:9087`

## 3) Docker запуск

```powershell
Set-Location "C:\JavaProject\RemByte"
docker compose up -d --build
docker compose ps
```

Остановка:

```powershell
docker compose down
```

Полная остановка с удалением volume БД (осторожно, удалит данные):

```powershell
docker compose down -v
```

## 4) Бэкап и восстановление

Встроенный модуль в настройках CRM:

- Создание backup-архива
- Восстановление из backup
- Включает пользовательские данные и вложения

Рекомендации:

- Делайте бэкап перед обновлением версии
- Храните 2-3 последних архива локально + копию вне сервера
- Проверяйте восстановление на тестовой среде

## 5) Обновление приложения

1. Сделать backup из панели администратора
2. Обновить код/образ
3. Перезапустить сервис
4. Проверить: логин, заказы, вложения, канбан, чат, плагины

Для Docker:

```powershell
docker compose up -d --build
docker compose logs --tail=200 app
```

## 6) Частые проблемы и решения

### Порт занят

Симптом: `Port 8080/9087 was already in use`.

Решение:

- Освободить порт
- Или сменить `server.port`

### MariaDB недоступна

Симптом: `Connection refused`, `Socket fail to connect host=localhost port=9092`.

Проверка:

```powershell
docker ps
```

Убедитесь, что контейнер БД запущен и healthy.

### Ошибка загрузки больших файлов через reverse proxy

Симптом: `413 Request Entity Too Large`.

Для Nginx добавьте в server/location:

- `client_max_body_size 100M;` (или больше)

и перезагрузите Nginx.

### Ошибки шаблонов Thymeleaf

Симптом: ошибки `TemplateInputException`.

Решение:

- Проверить синтаксис fragment/`th:*`
- Проверить существование подключаемых fragment-файлов
- Перезапустить после очистки сборки

## 7) Минимальный post-deploy smoke-check

- Вход под админом
- Создание клиента
- Создание заказа
- Добавление вложения
- Перевод статуса заказа
- Открытие чата
- Открытие канбан-доски
- Создание/восстановление тестового backup

