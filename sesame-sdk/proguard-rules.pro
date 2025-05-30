
-keepclassmembers enum  * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class com.google.gson.** { *; }
-keep class com.google.inject.** { *; }
-dontwarn com.google.gson.**

-keepclassmembers enum * {
     public static **[] values();
     public static ** valueOf(java.lang.String);
}
# For using GSON @Expose annotation
-keepattributes *Annotation*

# Class names are needed in reflection
-keepnames class com.amazonaws.**
-keepnames class com.amazon.**

# Request handlers defined in request.handlers
-keep class com.amazonaws.services.**.*Handler

# The following are referenced but aren't required to run
-dontwarn com.fasterxml.jackson.**
-dontwarn org.apache.commons.logging.**

# Android 6.0 release removes support for the Apache HTTP client
-dontwarn org.apache.http.**

# The SDK has several references of Apache HTTP client
-dontwarn com.amazonaws.http.**
-dontwarn com.amazonaws.metrics.**
-dontwarn kotlin.**
-dontwarn kotlinx.**

-keep public class co.candyhouse.sesame.open.** {
    public protected *;
}

-keep public class co.candyhouse.sesame.db.** { *; }
-keep public class co.candyhouse.sesame.server.dto.** { *; }

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
  public static void checkExpressionValueIsNotNull(java.lang.Object, java.lang.String);
  public static void checkFieldIsNotNull(java.lang.Object, java.lang.String);
  public static void checkFieldIsNotNull(java.lang.Object, java.lang.String, java.lang.String);
  public static void checkNotNull(java.lang.Object);
  public static void checkNotNull(java.lang.Object, java.lang.String);
  public static void checkNotNullExpressionValue(java.lang.Object, java.lang.String);
  public static void checkNotNullParameter(java.lang.Object, java.lang.String);
  public static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
  public static void checkReturnedValueIsNotNull(java.lang.Object, java.lang.String);
  public static void checkReturnedValueIsNotNull(java.lang.Object, java.lang.String, java.lang.String);
  public static void throwUninitializedPropertyAccessException(java.lang.String);
}

-keep class co.candyhouse.sesame.ble.CHBaseDevice { *; }
-keep class co.candyhouse.sesame.open.device.CHDeviceStatus { *; }
-keep interface co.candyhouse.sesame.open.device.CHDevices { *; }
-keep class co.candyhouse.sesame.** { *; }