# ---------------------------------------------------------------------------
# Resonance - regras R8 (release). Mantenha o mínimo: as libs AndroidX já
# distribuem consumer rules (Room, DataStore, Media3, Lifecycle, Compose).
# ---------------------------------------------------------------------------

# Stack traces legíveis no Play Console / retrace (mapping.txt é gerado em build/outputs/mapping).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Anotações/genéricos usados por reflexão em libs (Room, Kotlin).
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# --- Room (2.8 + KSP): o código gerado (_Impl) é instanciado por reflexão ---
# As consumer rules do room-runtime já mantêm RoomDatabase/_Impl; reforço explícito:
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# --- Media3 / ExoPlayer ---
# O serviço é declarado no Manifest (mantido automaticamente pelo AAPT2).
# Media3 usa reflexão opcional em módulos que NÃO usamos (ex.: decoders de extensões); silenciar avisos:
-dontwarn org.checkerframework.**
-dontwarn javax.annotation.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn org.jspecify.annotations.**
-dontwarn org.codehaus.mojo.animal_sniffer.**

# --- Kotlin coroutines / DataStore ---
# (consumer rules próprias já cobrem; preferências usam só chaves de Preferences, sem reflexão)
-dontwarn kotlinx.coroutines.debug.**

# --- Compose ---
# Sem regras próprias: o plugin do compilador + consumer rules do Compose bastam.
# NÃO use -keep class androidx.compose.** { *; } (anula o shrink do app).

# --- Modelos do app ---
# Track/PlaylistUi/etc. são data classes usadas só em memória (sem Gson/Moshi/Serializable por reflexão):
# nenhuma regra necessária. Se um dia serializar por reflexão, adicione -keep no pacote data/ específico.

# --- Qualidade do release ---
# Remove logs de depuração (android.util.Log.v/d) do release.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
