# ============================================================
# DroidCode IDE - ProGuard / R8 Rules
# ============================================================

-keep class kotlin.coroutines.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class ** {
    @kotlin.Metadata *;
}

-keep class dagger.hilt.** { *; }
-keep class com.droidcode.ide.**Hilt* { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp { *; }
-keep class * extends dagger.hilt.android.HiltViewModel { *; }
-dontwarn dagger.hilt.**
-keepclasseswithmembers class * {
    @dagger.hilt.android.* <init>(...);
}

-keep class * extends androidx.room.RoomDatabase { *; }
-keep class com.droidcode.ide.data.db.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

-keep class com.google.gson.** { *; }
-keep class com.droidcode.ide.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName *;
}

-keep class org.eclipse.jgit.** { *; }
-dontwarn org.eclipse.jgit.**
-keep class org.eclipse.jgit.internal.storage.file.FileRepository
-keep class org.eclipse.jgit.transport.Transport
-keep class org.eclipse.jgit.transport.TransportBundleFile
-keep class org.eclipse.jgit.transport.SshTransport
-keep class org.eclipse.jgit.transport.http.HttpTransport
-keep class org.eclipse.jgit.transport.http.apache.HttpClientConnection
-keep class org.eclipse.jgit.util.SystemReader
-keep class org.eclipse.jgit.util.FS

-keep class com.hierynomus.** { *; }
-dontwarn com.hierynomus.**
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

-keep class com.arthenica.quickjs.** { *; }
-dontwarn com.arthenica.quickjs.**

-keep class org.slf4j.** { *; }
-keep class ch.qos.logback.** { *; }
-dontwarn org.slf4j.**
-dontwarn ch.qos.logback.**

-keep class androidx.lifecycle.** { *; }
-keep class androidx.compose.** { *; }
-keep class androidx.navigation.** { *; }
-keep class androidx.hilt.** { *; }
-keep class androidx.work.** { *; }
-keep class androidx.security.** { *; }

-keep class com.droidcode.ide.editor.MonacoBridge { *; }
-keepclassmembers class com.droidcode.ide.editor.MonacoBridge {
    @android.webkit.JavascriptInterface <methods>;
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
} 