# ⚠️ ИСПРАВЛЕНИЕ ПРОБЛЕМЫ С LOMBOK

> Примечание: учетные данные для входа в систему задаются через `.env` (`FIXBYTE_ADMIN_*`, `FIXBYTE_OPERATOR_*`).

## Проблема

При сборке проекта появляются ошибки типа:
```
cannot find symbol: method setName()
cannot find symbol: method getName()
```

Это означает, что **Lombok не генерирует getter/setter методы**.

## Решение

### Вариант 1: Использование IntelliJ IDEA (Рекомендуется)

1. **Установить Lombok plugin в IntelliJ IDEA:**
   - File → Settings → Plugins
   - Поиск "Lombok"
   - Установить и перезагрузить IDE

2. **Включить Annotation Processing:**
   - File → Settings → Build, Execution, Deployment → Compiler → Annotation Processors
   - ☑️ Enable annotation processing

3. **Пересобрать проект:**
   - Build → Rebuild Project

### Вариант 2: Через командную строку с явной конфигурацией

```bash
# Очистить кэш Maven
mvn clean

# Собрать с явной обработкой аннотаций
mvn -U clean package -DskipTests -X

# Или через spring-boot:run (рекомендуется для разработки)
mvn spring-boot:run
```

### Вариант 3: Редактирование pom.xml

Убедитесь, что в pom.xml правильная конфигурация:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.10.1</version>
    <configuration>
        <source>17</source>
        <target>17</target>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

### Вариант 4: Использование @Data вместо @Getter/@Setter

Замените в моделях:

```java
// Было:
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

// На:
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
```

Аннотация `@Data` объединяет в себе `@Getter`, `@Setter`, `@ToString`, `@EqualsAndHashCode`, `@RequiredArgsConstructor`.

---

## Рекомендуемый способ запуска

Используйте **spring-boot:run** для разработки (не требует полной сборки):

```bash
cd C:\JavaProject\RemByte
mvn spring-boot:run
```

Это автоматически скомпилирует код с правильной обработкой Lombok аннотаций.

---

## Проверка

После исправления все следующие команды должны работать:

```bash
# Полная сборка с тестами
mvn clean package

# Или только компиляция
mvn clean compile

# Запуск приложения
java -jar target/rembyte-crm-1.0.0.jar
```

---

## Если ничего не помогает

1. **Полностью очистить кэш:**
   ```bash
   mvn clean
   rm -rf ~/.m2/repository/org/projectlombok
   mvn compile
   ```

2. **Обновить Lombok:**
   ```bash
   # В pom.xml:
   <version>1.18.30</version>
   
   # Затем:
   mvn -U clean compile
   ```

3. **Использовать альтернативный инструмент:**
   - Рассмотрите переход на **MapStruct** или вручную написать getter/setter

---

**Удачи с развертыванием! 🚀**

