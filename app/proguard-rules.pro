# Gson usa reflexão: mantém os DTOs da API Xtream intactos no build release.
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
-keep class com.example.iptvplayer.data.remote.dto.** { *; }
