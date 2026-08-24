# Compose and Kotlin metadata used by the compiler and runtime.
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,AnnotationDefault,InnerClasses,Signature,EnclosingMethod,*Annotation*

# Play Billing (AAR rules are usually enough; these are extra safety).
-keep class com.android.billingclient.** { *; }
-keep class com.google.android.gms.internal.play_billing.** { *; }
-dontwarn com.android.billingclient.**

# Coroutines
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
