# Room & Hilt
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# OpenGL / EGL
-keep class android.opengl.** { *; }
