-keep class no.nordicsemi.android.dfu.** { *; }


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
-keep class com.amazonaws.** { *; }
-keep class org.apache.harmony.** { *; }
-keep class com.android.org.conscrypt.** { *; }


#  start http
# Retrofit does reflection on method and parameter annotations.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Ignore annotation used for build tooling.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Ignore JSR 305 annotations for embedding nullability information.
-dontwarn javax.annotation.**

# Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
-dontwarn kotlin.Unit

# Top-level functions that can only be used by Kotlin.
-dontwarn retrofit2.KotlinExtensions

# With R8 full mode, it sees no subtypes of Retrofit interfaces since they are created with a Proxy
# and replaces all potential values with null. Explicitly keeping the interfaces prevents this.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>


-keep class co.candyhouse.sesame.** { *; }
-dontwarn co.candyhouse.sesame.**

-keep class co.candyhouse.server.** { *; }

-keep class org.eclipse.paho.client.mqttv3.** { *; }
-keep class com.amazonaws.** { *; }
-dontwarn co.candyhouse.server.**-keep
-dontwarn co.candyhouse.server.dto.**-keep
-keep class co.candyhouse.server.dto.** { *; }


-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# 避免Log打印输出
-assumenosideeffects class android.util.Log {
        public static *** v(...);
        public static *** d(...);
        public static *** i(...);
        public static *** w(...);
        public static *** e(...);
}

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# 数据模型类
-keep class co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.** { *; }
-keepclassmembers class co.candyhouse.app.tabs.devices.hub3.setting.ir.bean.** { *; }


# AWS相关
-keep class com.amazonaws.services.cognitoidentityprovider.** { *; }
-dontwarn com.amazonaws.mobile.auth.facebook.FacebookButton
-dontwarn com.amazonaws.mobile.auth.facebook.FacebookSignInProvider
-dontwarn com.amazonaws.mobile.auth.google.GoogleButton
-dontwarn com.amazonaws.mobile.auth.google.GoogleSignInProvider
-dontwarn com.amazonaws.mobile.auth.ui.AuthUIConfiguration$Builder
-dontwarn com.amazonaws.mobile.auth.ui.AuthUIConfiguration
-dontwarn com.amazonaws.mobile.auth.ui.SignInUI$LoginBuilder
-dontwarn com.amazonaws.mobile.auth.ui.SignInUI
-dontwarn com.amazonaws.mobile.auth.userpools.CognitoUserPoolsSignInProvider
-dontwarn com.amazonaws.mobileconnectors.cognitoauth.Auth$Builder
-dontwarn com.amazonaws.mobileconnectors.cognitoauth.Auth
-dontwarn com.amazonaws.mobileconnectors.cognitoauth.AuthUserSession
-dontwarn com.amazonaws.mobileconnectors.cognitoauth.activities.CustomTabsManagerActivity
-dontwarn com.amazonaws.mobileconnectors.cognitoauth.exceptions.AuthClientException
-dontwarn com.amazonaws.mobileconnectors.cognitoauth.exceptions.AuthServiceException
-dontwarn com.amazonaws.mobileconnectors.cognitoauth.handlers.AuthHandler
-dontwarn com.amazonaws.mobileconnectors.cognitoauth.tokens.AccessToken
-dontwarn com.amazonaws.mobileconnectors.cognitoauth.tokens.IdToken
-dontwarn com.amazonaws.mobileconnectors.cognitoauth.tokens.RefreshToken
-dontwarn com.amazonaws.mobileconnectors.cognitoauth.util.Pkce
-dontwarn com.android.org.conscrypt.SSLParametersImpl
-dontwarn org.apache.harmony.xnet.provider.jsse.SSLParametersImpl