# R8 / ProGuard rules for FSS CRM

# --- Data Models (CRITICAL for GSON parsing) ---
-keep class com.fsscrm.network.** { *; }
-keep class com.fsscrm.ui.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# --- Retrofit ---
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# --- OkHttp ---
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-dontwarn okio.**

# --- GSON ---
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.TypeAdapter

# --- Android Support / Jetpack ---
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# --- Firebase ---
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# --- General ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
