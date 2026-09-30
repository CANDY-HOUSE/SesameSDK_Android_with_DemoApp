-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*,InnerClasses,EnclosingMethod

# Firebase discovers registrars from manifest metadata and invokes their no-arg constructors.
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    public <init>();
}

-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}
