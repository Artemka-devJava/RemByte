# 🎯 ТУП К ЗАПУСКУ ЧЕРЕЗ IntelliJ IDEA

## 🆕 Перед запуском (обязательно)

- Проверьте файл `.env` в корне проекта.
- Укажите рабочие значения: `MARIADB_URL`, `MARIADB_USERNAME`, `MARIADB_PASSWORD`, `FIXBYTE_ADMIN_*`, `FIXBYTE_OPERATOR_*`.
- После входа в приложение можно выбрать тему интерфейса (System/Light/Dark).

## Шаг 1: Подготовка IDE

### 1.1 Установить Lombok Plugin

1. Откройте **IntelliJ IDEA**
2. **File** → **Settings** (или **IntelliJ IDEA** → **Preferences** на Mac)
3. Перейти в **Plugins**
4. Поиск: "Lombok"
5. Установить **Lombok** от JetBrains
6. Перезагрузить IDE (нажать "Restart IDE")

### 1.2 Включить Annotation Processing

1. **File** → **Settings** → **Build, Execution, Deployment** → **Compiler** → **Annotation Processors**
2. ✅ Отметить **"Enable annotation processing"**
3. Нажать **OK**

---

## Шаг 2: Открытие проекта

1. **File** → **Open**
2. Выбрать папку: `C:\JavaProject\RemByte`
3. Нажать **Open**
4. IntelliJ предложит загрузить проект как Maven → **Load** или **Open as Project**

---

## Шаг 3: Загрузка зависимостей

1. Подождать, пока IntelliJ загрузит все зависимости Maven
2. В нижней части экрана должно быть написано: "Build successful"
3. Если ошибок - нажать **Build** → **Rebuild Project**

---

## Шаг 4: Запуск приложения

### Способ A: Через запуск главного класса

1. Открить файл: `src/main/java/com/rembyte/RemByteApplication.java`
2. Нажать **зеленую стрелку** рядом с `class RemByteApplication`
3. Выбрать **Run 'RemByteApplication'**
4. В нижней панели (Run) появятся логи запуска

### Способ B: Через меню Run

1. **Run** → **Run 'RemByteApplication'**
2. Или нажать **Shift + F10** (Windows) / **Ctrl + R** (Mac)

### Способ C: Через Maven

1. **View** → **Tool Windows** → **Maven**
2. Развернуть **rembyte-crm** → **Plugins** → **spring-boot**
3. Двойной клик на **spring-boot:run**

---

## Шаг 5: Проверка запуска

### В консоли (Run) должно появиться:

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_|\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.0)

2026-03-27 19:11:00.000 INFO  com.rembyte.RemByteApplication : 
🔧 RemByte CRM запущен!
📱 Веб-интерфейс доступен по адресу: http://localhost:9087

2026-03-27 19:11:02.000 INFO  com.rembyte.config.ApplicationConfiguration : 
🔧 Инициализация стандартных услуг ремонта...
✅ Услуги инициализированы успешно!

2026-03-27 19:11:05.000 INFO  o.s.b.w.e.t.TomcatWebServer : 
Tomcat started on port(s): 8080 (http) with context path ''
```

---

## Шаг 6: Доступ к приложению

Откройте в браузере:

```
http://localhost:9087
```

Должна появиться **главная страница** приложения RemByte CRM с синей кнопкой "Панель управления".

---

## 🔧 Если возникла ошибка

### Ошибка: "cannot find symbol: method setName()"

**Решение:**
1. **Build** → **Rebuild Project**
2. Если не помогло → **File** → **Invalidate Caches** → **Invalidate and Restart**
3. После перезагрузки IDE → **Build** → **Rebuild Project**

### Ошибка: "Spring Boot application not found"

**Решение:**
1. Убедитесь, что класс **RemByteApplication** есть в `src/main/java/com/rembyte/`
2. Класс должен иметь аннотацию `@SpringBootApplication`
3. **Build** → **Rebuild Project**

### Порт 8080 уже занят

**Решение:**
1. Отредактируйте файл `src/main/resources/application.properties`
2. Измените строку:
   ```properties
   server.port=8081
   ```
3. Перезагрузите приложение

---

## 🎓 Структура проекта в IDE

```
RemByte
├── src
│   ├── main
│   │   ├── java/com/rembyte/
│   │   │   ├── config/
│   │   │   │   └── ApplicationConfiguration.java
│   │   │   ├── controller/           ← REST контроллеры
│   │   │   ├── model/                ← JPA сущности
│   │   │   ├── repository/           ← Spring Data репозитории
│   │   │   ├── service/              ← Бизнес-логика
│   │   │   └── RemByteApplication.java ← Главный класс
│   │   └── resources/
│   │       ├── application.properties ← Конфигурация
│   │       ├── static/
│   │       │   ├── css/style.css
│   │       │   └── js/
│   │       └── templates/            ← HTML страницы
│   └── test/
├── target/                           ← Скомпилированные файлы
└── pom.xml                           ← Конфигурация Maven
```

---

## 🚀 Полезные клавиши в IDE

| Команда | Windows | Mac |
|---------|---------|-----|
| Запустить приложение | Shift + F10 | Ctrl + R |
| Остановить приложение | Ctrl + F2 | Cmd + F2 |
| Перестроить проект | Ctrl + Shift + F9 | Cmd + Shift + F9 |
| Переформатировать код | Ctrl + Alt + L | Cmd + Alt + L |
| Синтаксис хелп | Ctrl + P | Cmd + P |
| Открыть класс | Ctrl + N | Cmd + O |
| Найти в классе | Ctrl + F | Cmd + F |

---

## 📝 Редактирование и отладка

### Hot Reload (изменение кода без перезагрузки)

1. **File** → **Settings** → **Build, Execution, Deployment** → **Debugger**
2. Найти **Spring Boot** и включить "On Update action"
3. Выбрать **"Update resources and restart"** или **"Update classes"**

Теперь при сохранении изменений приложение автоматически перезагрузит класс.

### Точки отладки (Breakpoints)

1. Нажмите на номер строки в редакторе (слева появится красная точка)
2. Нажмите **Shift + F9** или **Run** → **Debug 'RemByteApplication'**
3. Приложение остановится на этой строке
4. Используйте Step Over (F10) для пошагового выполнения

---

## 🔍 Просмотр логов

В нижней панели **Run** видны все логи приложения:

- 🟢 **INFO** - информационные сообщения (зеленые)
- 🟡 **WARN** - предупреждения (желтые)
- 🔴 **ERROR** - ошибки (красные)

Фильтруйте логи по классам для быстрого поиска нужной информации.

---

## ✅ Готово!

Теперь приложение **RemByte CRM** запущено в IntelliJ IDEA и доступно по адресу:

```
http://localhost:9087
```

Вы можете начать использовать систему управления сервисом ремонта ПК! 🎉

---

**Полезные ссылки:**
- 📖 README.md - основная документация
- 📖 COMPLETE_README.md - полная документация
- 📖 QUICKSTART.md - быстрый старт
- 📖 FIX_LOMBOK_ISSUE.md - решение проблем

