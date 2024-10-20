# Add rules for Retrofit and Gson
-dontwarn com.squareup.okhttp.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# Keep your data classes for serialization with Gson
-keep class com.example.quickmdcapture.** { *; }

# Keep enums (if any)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
