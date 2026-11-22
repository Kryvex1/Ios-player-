package dev.anilbeesetti.nextplayer.core.common.cache

import android.content.Context
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONArray
import org.json.JSONObject

data class CachedVideoSpecs(
    val fps: Float? = null,
    val videoCodec: String? = null,
    val videoBitrate: Long = 0L,
    val format: String? = null,
    val audioTracks: List<CachedAudioTrack> = emptyList(),
    val subtitleTracks: List<CachedSubtitleTrack> = emptyList(),
)

data class CachedAudioTrack(
    val title: String? = null,
    val codec: String = "",
    val language: String? = null,
    val channels: Int = 0,
    val channelLayout: String? = null,
    val sampleRate: Int = 0,
    val bitRate: Long = 0L,
)

data class CachedSubtitleTrack(
    val title: String? = null,
    val codec: String = "",
    val language: String? = null,
)

/**
 * Fast in-memory + SharedPreferences cache for extracted video FPS and track metadata.
 * Cleared automatically whenever playback history is deleted/cleared.
 */
object MediaMetadataCache {
    private const val PREFS_NAME = "afterplayer_media_metadata_cache"
    private const val MAX_ENTRIES = 120

    private val memoryCache = ConcurrentHashMap<String, CachedVideoSpecs>()

    fun get(context: Context, key: String): CachedVideoSpecs? {
        if (key.isBlank()) return null
        memoryCache[key]?.let { return it }
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(key, null) ?: return null
        val parsed = runCatching { decode(raw) }.getOrNull() ?: return null
        memoryCache[key] = parsed
        return parsed
    }

    fun put(context: Context, key: String, specs: CachedVideoSpecs) {
        if (key.isBlank()) return
        val existing = memoryCache[key]
        val merged = if (existing != null) {
            CachedVideoSpecs(
                fps = specs.fps?.takeIf { it > 0f } ?: existing.fps,
                videoCodec = specs.videoCodec?.takeIf { it.isNotBlank() } ?: existing.videoCodec,
                videoBitrate = if (specs.videoBitrate > 0L) specs.videoBitrate else existing.videoBitrate,
                format = specs.format?.takeIf { it.isNotBlank() } ?: existing.format,
                audioTracks = specs.audioTracks.ifEmpty { existing.audioTracks },
                subtitleTracks = specs.subtitleTracks.ifEmpty { existing.subtitleTracks },
            )
        } else {
            specs
        }
        memoryCache[key] = merged
        runCatching {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            if (prefs.all.size > MAX_ENTRIES) {
                // Trim oldest half if cache grows large
                prefs.all.keys.take(MAX_ENTRIES / 2).forEach { editor.remove(it) }
            }
            editor.putString(key, encode(merged)).apply()
        }
    }

    fun remove(context: Context, keys: Collection<String>) {
        if (keys.isEmpty()) return
        keys.forEach { memoryCache.remove(it) }
        runCatching {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            keys.forEach { editor.remove(it) }
            editor.apply()
        }
    }

    fun clear(context: Context) {
        memoryCache.clear()
        runCatching {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        }
    }

    private fun encode(specs: CachedVideoSpecs): String {
        val obj = JSONObject()
        specs.fps?.let { obj.put("fps", it.toDouble()) }
        specs.videoCodec?.let { obj.put("videoCodec", it) }
        if (specs.videoBitrate > 0L) obj.put("videoBitrate", specs.videoBitrate)
        specs.format?.let { obj.put("format", it) }

        val audioArr = JSONArray()
        for (a in specs.audioTracks) {
            audioArr.put(
                JSONObject().apply {
                    a.title?.let { put("title", it) }
                    put("codec", a.codec)
                    a.language?.let { put("language", it) }
                    put("channels", a.channels)
                    a.channelLayout?.let { put("channelLayout", it) }
                    put("sampleRate", a.sampleRate)
                    put("bitRate", a.bitRate)
                },
            )
        }
        obj.put("audio", audioArr)

        val subArr = JSONArray()
        for (s in specs.subtitleTracks) {
            subArr.put(
                JSONObject().apply {
                    s.title?.let { put("title", it) }
                    put("codec", s.codec)
                    s.language?.let { put("language", it) }
                },
            )
        }
        obj.put("subs", subArr)
        return obj.toString()
    }

    private fun decode(raw: String): CachedVideoSpecs {
        val obj = JSONObject(raw)
        val fps = if (obj.has("fps")) obj.optDouble("fps", -1.0).toFloat().takeIf { it > 0f } else null
        val videoCodec = obj.optString("videoCodec", "").takeIf { it.isNotBlank() }
        val videoBitrate = obj.optLong("videoBitrate", 0L)
        val format = obj.optString("format", "").takeIf { it.isNotBlank() }

        val audioList = mutableListOf<CachedAudioTrack>()
        val audioArr = obj.optJSONArray("audio")
        if (audioArr != null) {
            for (i in 0 until audioArr.length()) {
                val item = audioArr.optJSONObject(i) ?: continue
                audioList.add(
                    CachedAudioTrack(
                        title = item.optString("title", "").takeIf { it.isNotBlank() },
                        codec = item.optString("codec", ""),
                        language = item.optString("language", "").takeIf { it.isNotBlank() },
                        channels = item.optInt("channels", 0),
                        channelLayout = item.optString("channelLayout", "").takeIf { it.isNotBlank() },
                        sampleRate = item.optInt("sampleRate", 0),
                        bitRate = item.optLong("bitRate", 0L),
                    ),
                )
            }
        }

        val subList = mutableListOf<CachedSubtitleTrack>()
        val subArr = obj.optJSONArray("subs")
        if (subArr != null) {
            for (i in 0 until subArr.length()) {
                val item = subArr.optJSONObject(i) ?: continue
                subList.add(
                    CachedSubtitleTrack(
                        title = item.optString("title", "").takeIf { it.isNotBlank() },
                        codec = item.optString("codec", ""),
                        language = item.optString("language", "").takeIf { it.isNotBlank() },
                    ),
                )
            }
        }

        return CachedVideoSpecs(
            fps = fps,
            videoCodec = videoCodec,
            videoBitrate = videoBitrate,
            format = format,
            audioTracks = audioList,
            subtitleTracks = subList,
        )
    }
}
