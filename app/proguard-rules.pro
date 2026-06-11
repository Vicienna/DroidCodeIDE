# Kotlin & Coroutines
-keep class kotlin.coroutines.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class ** { @kotlin.Metadata *; }

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp { *; }
-keep class * extends dagger.hilt.android.HiltViewModel { *; }
-dontwarn dagger.hilt.**
-keepclasseswithmembers class * { @dagger.hilt.android.* <init>(...); }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class com.droidcode.ide.data.db.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# OkHttp / Okio / Gson
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-keep class com.google.gson.** { *; }
-keep class com.droidcode.ide.** { *; }
-keepclassmembers class * { @com.google.gson.annotations.SerializedName *; }

# JGit
-keep class org.eclipse.jgit.** { *; }
-dontwarn org.eclipse.jgit.**

# SSHJ / BouncyCastle
-keep class com.hierynomus.** { *; }
-dontwarn com.hierynomus.**
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# QuickJS
-keep class com.arthenica.quickjs.** { *; }
-dontwarn com.arthenica.quickjs.**

# SLF4J / Logback
-keep class org.slf4j.** { *; }
-keep class ch.qos.logback.** { *; }
-dontwarn org.slf4j.**
-dontwarn ch.qos.logback.**

# AndroidX / Compose / Navigation / Lifecycle / Work / Security
-keep class androidx.lifecycle.** { *; }
-keep class androidx.compose.** { *; }
-keep class androidx.navigation.** { *; }
-keep class androidx.hilt.** { *; }
-keep class androidx.work.** { *; }
-keep class androidx.security.** { *; }

# Monaco Bridge JS Interface
-keep class com.droidcode.ide.editor.MonacoBridge { *; }
-keepclassmembers class com.droidcode.ide.editor.MonacoBridge { @android.webkit.JavascriptInterface <methods>; }

# Serialization
-keepclassmembers class * implements java.io.Serializable { static final long serialVersionUID; private static final java.io.ObjectStreamField[] serialPersistentFields; private void writeObject(java.io.ObjectOutputStream); private void readObject(java.io.ObjectInputStream); java.lang.Object writeReplace(); java.lang.Object readResolve(); }

# Attributes
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Remove Logs in Release
-assumenosideeffects class android.util.Log { public static *** d(...); public static *** v(...); public static *** i(...); public static *** w(...); }
