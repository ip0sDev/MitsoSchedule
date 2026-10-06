# Правила, которые автоматически применяются во всех приложениях, использующих :core.

# Модели сериализуются в JSON (сеть и кэш в DataStore): имена и конструкторы не должны меняться.
-keep class mitsoschedule.core.model.** { *; }
-keepclassmembers class mitsoschedule.core.model.** {
    *** Companion;
}
