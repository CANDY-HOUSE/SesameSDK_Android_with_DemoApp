-keepattributes Signature
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepattributes InnerClasses
-keepattributes EnclosingMethod

-keep class com.amazonaws.mobileconnectors.apigateway.annotation.** { *; }
-keep class co.candyhouse.sesame.server.** { *; }
-keep class com.amazonaws.services.**.*Handler { *; }

-keep class com.amazonaws.mobile.client.** { *; }
-keep class com.amazonaws.mobile.auth.core.** { *; }

-keep class com.amazonaws.mobileconnectors.cognitoidentityprovider.** { *; }
-keep class com.amazonaws.services.cognitoidentityprovider.** { *; }
-keep class com.amazonaws.services.cognitoidentity.** { *; }

-dontwarn com.amazonaws.mobile.auth.ui.**
-dontwarn com.amazonaws.mobile.auth.facebook.**
-dontwarn com.amazonaws.mobile.auth.google.**
-dontwarn com.amazonaws.mobile.auth.userpools.**
-dontwarn com.amazonaws.mobileconnectors.cognitoauth.**

-dontwarn com.fasterxml.jackson.**
-dontwarn org.apache.commons.logging.**
-dontwarn org.apache.http.**
-dontwarn com.amazonaws.http.**
-dontwarn com.amazonaws.metrics.**

-dontwarn sun.misc.**
-dontwarn com.android.org.conscrypt.SSLParametersImpl
-dontwarn org.apache.harmony.xnet.provider.jsse.SSLParametersImpl

-keep class org.eclipse.paho.client.mqttv3.** { *; }
-dontwarn org.eclipse.paho.client.mqttv3.**

-keep class no.nordicsemi.android.dfu.** { *; }

-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }
-keep class * implements com.google.gson.TypeAdapterFactory { *; }
-keep class * implements com.google.gson.JsonSerializer { *; }
-keep class * implements com.google.gson.JsonDeserializer { *; }

-dontwarn com.google.gson.**
-dontwarn kotlin.**
-dontwarn kotlinx.**
