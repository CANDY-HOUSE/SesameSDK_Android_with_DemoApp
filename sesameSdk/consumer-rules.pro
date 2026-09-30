-keepattributes Signature
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepattributes InnerClasses
-keepattributes EnclosingMethod

-keep class co.candyhouse.sesame.ble.CHDevice { *; }

-dontwarn sun.misc.**
-dontwarn com.android.org.conscrypt.SSLParametersImpl
-dontwarn org.apache.harmony.xnet.provider.jsse.SSLParametersImpl

-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }
-keep class * implements com.google.gson.TypeAdapterFactory { *; }
-keep class * implements com.google.gson.JsonSerializer { *; }
-keep class * implements com.google.gson.JsonDeserializer { *; }

-dontwarn com.google.gson.**
-dontwarn kotlin.**
-dontwarn kotlinx.**

-keep class co.candyhouse.sesame.ble.os3.biometric.parseData.OpenSensorData { *; }
