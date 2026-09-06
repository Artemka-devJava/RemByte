// AGP 9+ включает поддержку Kotlin «из коробки» — отдельный плагин
// org.jetbrains.kotlin.android применять не нужно (иначе конфликт расширения `kotlin`).
plugins {
    id("com.android.application") version "9.2.1" apply false
}
