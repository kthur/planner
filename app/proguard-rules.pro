# Planner ProGuard Rules

# Firebase
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# Wearable
-keep class com.google.android.gms.wearable.** { *; }
-dontwarn com.google.android.gms.wearable.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# Compose
-dontwarn androidx.compose.**

# Keep data classes for JSON serialization
-keep class com.planner.tracker.data.** { *; }
-keep class com.planner.tracker.widget.** { *; }

# Keep Service classes
-keep class com.planner.tracker.TrackerService { *; }
-keep class com.planner.tracker.PlannerFirebaseMessagingService { *; }
-keep class com.planner.tracker.WearService { *; }
