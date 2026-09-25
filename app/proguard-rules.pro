# kotlinx.serialization and Navigation's type-safe routes ship consumer rules with their libraries.
# Keep the serializers of our own @Serializable classes (backup DTOs live in :core, routes in :app).
-keepclassmembers @kotlinx.serialization.Serializable class com.goalkeeper.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.goalkeeper.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Glance instantiates widget ActionCallbacks from their class name.
-keep class * implements androidx.glance.appwidget.action.ActionCallback { <init>(); }
