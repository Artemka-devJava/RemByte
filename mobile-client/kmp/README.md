# FixByte CRM Next — Compose Multiplatform

Переписанный мобильный клиент (Android + iOS из одной кодовой базы, Kotlin
Multiplatform + Compose Multiplatform / Material 3). Тот же REST API, что и у
`mobile-client/android`/`mobile-client/ios` — **те приложения не тронуты** и
продолжают работать; это отдельное приложение для тестового периода
(applicationId/bundle id `ru.fixbyte.crm.next`, чтобы стоять рядом со старым
на одном телефоне).

## Что реализовано (паритет со старыми клиентами)

Вход → Клиенты (поиск, pull-to-refresh, ＋Клиент с опциональной первичной
заявкой, обработка дубля телефона 409) → Карточка клиента (контакты,
звонок/написать по preferredChannel, сводка заказов/оборота/долга, фото:
камера/галерея/шэринг/удаление, список заказов) → Заказ (проблема + оценка,
фото-вложения, акт приёмки — печать/отправка PDF) → Настройки (адрес сервера +
логин/пароль, единообразно на обеих платформах, в отличие от старых
клиентов).

**Не входит в этот rewrite**: напоминания (`/api/reminders`) — сегодня это
web-only функция (дашборд/заказы), не часть паритета со старыми мобильными
клиентами. Возможное следующее направление, не делалось сейчас.

## Сборка — Android

Нужен JDK 17 и Android SDK, включая **платформу 37** (`compileSdk = 37` —
того требуют актуальные артефакты Compose Multiplatform 1.10.3; `targetSdk`
остался 36, как у старого приложения). Android Studio / AGP сама предложит
скачать platform 37 при первой синхронизации. Wrapper-скрипты
(`gradlew`/`gradlew.bat`) в репозитории не хранятся (как и в
`mobile-client/android`) — либо открыть папку в Android Studio (сама
сгенерирует), либо один раз выполнить `gradle wrapper --gradle-version 9.5.0`
при наличии установленного Gradle.

```
./gradlew :composeApp:assembleDebug   # composeApp/build/outputs/apk/debug/
```

## Сборка — iOS

**Не проверялось сборкой в этом окружении — здесь нет macOS/Xcode.** Код в
`composeApp/src/iosMain` и весь `iosApp/` написан по образцу уже работающего
`mobile-client/ios` (UIImagePickerController, UIActivityViewController,
UIPrintInteractionController), но нуждается в ревью и первой сборке на Mac.

1. `brew install xcodegen`
2. В `mobile-client/kmp/iosApp/`: `xcodegen generate`
3. `open FixByteCrmNext.xcodeproj` — сборка сама выполнит
   `:composeApp:embedAndSignAppleFrameworkForXcode` (нужен `./gradlew`, см.
   выше) и подключит Kotlin-фреймворк.

Известное упрощение: пароль на iOS пока хранится в `NSUserDefaults` (как и
сегодня в `mobile-client/ios`), а не в Keychain — сама попытка написать
Keychain-обвязку на CFDictionary без возможности её скомпилировать здесь была
сочтена более рискованной, чем взять заведомо рабочий вариант того же уровня
защищённости, что и в текущих приложениях. На Android пароль **уже** в
`EncryptedSharedPreferences` (Android Keystore).

## Стек

Kotlin Multiplatform + Compose Multiplatform 1.10.3 / Kotlin 2.3.20, Material 3,
Ktor 3.5 (сессионные cookie + авто-релогин — аналог `ReauthInterceptor` из
старого Android-клиента), kotlinx.serialization, Coil3 (картинки тем же
авторизованным Ktor-клиентом), multiplatform-settings (адрес сервера/логин/
cookie), EncryptedSharedPreferences/Keychain (пароль). Навигация — простой
самописный стек экранов (`App.kt`), без отдельной библиотеки.
