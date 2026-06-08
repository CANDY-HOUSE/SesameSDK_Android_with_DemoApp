-dontobfuscate

-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepattributes InnerClasses
-keepattributes EnclosingMethod

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keep class co.candyhouse.**.*Bridge { *; }

-keep class no.nordicsemi.android.dfu.** { *; }

-keep class * implements com.google.firebase.components.ComponentRegistrar { *; }
-keep class com.google.firebase.crashlytics.FirebaseCrashlyticsRegistrar { *; }
-keep class com.google.firebase.crashlytics.** { *; }

-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}
