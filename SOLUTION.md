# ✅ РЕШЕНИЕ ПРОБЛЕМЫ: ExceptionInInitializerError

> Актуально: логины/пароли приложения больше не хранятся в markdown и properties по умолчанию — используются переменные из `.env`.

## Проблема

```
java: java.lang.ExceptionInInitializerError
com.sun.tools.javac.code.TypeTag :: UNKNOWN
```

Это означает, что **Lombok не генерирует методы getter/setter**.

## 🎯 РЕШЕНИЕ (Выбери один способ)

### Способ 1: Используй spring-boot:run (РЕКОМЕНДУЕТСЯ) ⭐⭐⭐

Это обходит проблему с обработкой аннотаций в Maven:

```bash
cd C:\JavaProject\RemByte
mvn spring-boot:run
```

**Это работает быстро и надежно!**

Затем откройте: http://localhost:9087

---

### Способ 2: Используй IntelliJ IDEA

IntelliJ автоматически обрабатывает Lombok аннотации если установлен плагин:

1. **File** → **Settings** → **Plugins**
2. Поиск и установка: "**Lombok**"
3. **File** → **Settings** → **Build, Execution, Deployment** → **Compiler** → **Annotation Processors**
4. ✅ Включить "**Enable annotation processing**"
5. **Build** → **Rebuild Project**
6. Запустить приложение (Shift + F10)

---

### Способ 3: Добавить Lombok annotation processor в pom.xml

Если нужна полная сборка через Maven, добавь:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${project.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

Затем:
```bash
mvn clean install
java -jar target/rembyte-crm-1.0.0.jar
```

---

## ⚡ БЫСТРЫЙ СТАРТ (Работает прямо сейчас!)

```bash
cd C:\JavaProject\RemByte
mvn spring-boot:run
```

Откройте браузер: **http://localhost:9087**

---

## 📝 Почему это происходит?

- Lombok требует обработки аннотаций на этапе компиляции
- Maven не всегда правильно обрабатывает аннотации
- IntelliJ IDEA и spring-boot:run обрабатывают их правильно

---

## ✅ ПРОВЕРКА

После запуска в логах должно быть:

```
🔧 RemByte CRM запущен!
📱 Веб-интерфейс доступен по адресу: http://localhost:9087
✅ Услуги инициализированы успешно!
```

---

**Используй Способ 1 - это самое быстрое решение! 🚀**

