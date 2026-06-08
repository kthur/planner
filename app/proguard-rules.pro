-keepclassmembers class * extends androidx.room.RoomDatabase {
    abstract <methods>;
}

-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

-keep class kotlin.reflect.** { *; }

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class com.planner.tracker.data.** { *; }

-keepclassmembers class * {
    @androidx.compose.runtime.Immutable <fields>;
    @androidx.compose.runtime.Stable <fields>;
}

-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

-keep,includedescriptorclasses class com.planner.tracker.** { *; }
