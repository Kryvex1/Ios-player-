package dev.anilbeesetti.nextplayer.core.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import dev.anilbeesetti.nextplayer.core.common.Utils
import dev.anilbeesetti.nextplayer.core.common.extensions.mapAsync
import dev.anilbeesetti.nextplayer.core.data.mappers.toAudioStreamInfo
import dev.anilbeesetti.nextplayer.core.data.mappers.toFolder
import dev.anilbeesetti.nextplayer.core.data.mappers.toSubtitleStreamInfo
import dev.anilbeesetti.nextplayer.core.data.mappers.toVideo
import dev.anilbeesetti.nextplayer.core.data.mappers.toVideoState
import dev.anilbeesetti.nextplayer.core.data.mappers.toVideoStreamInfo
import dev.anilbeesetti.nextplayer.core.data.models.VideoState
import dev.anilbeesetti.nextplayer.core.database.converter.UriListConverter
import dev.anilbeesetti.nextplayer.core.database.dao.MediumStateDao
import dev.anilbeesetti.nextplayer.core.database.entities.MediumStateEntity
import dev.anilbeesetti.nextplayer.core.media.services.MediaService
import dev.anilbeesetti.nextplayer.core.model.Folder
import dev.anilbeesetti.nextplayer.core.model.MediaInfo
import dev.anilbeesetti.nextplayer.core.model.Video
import io.github.anilbeesetti.nextlib.mediainfo.MediaInfoBuilder
import java.util.Date
import kotlin.math.absoluteValue
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

@Factory
class LocalMediaRepository(
    private val mediumStateDao: MediumStateDao,
    private val mediaService: MediaService,
    private val context: Context,
) : MediaRepository {

    private val lastSavedPositions = ConcurrentHashMap<String, Long>()

    override fun observeFolders(folderPath: String?): Flow<List<Folder>> {
        return mediaService.observeFolders(folderPath)
            .map { mediaFolders -> mediaFolders.map { it.toFolder() } }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
    }

    override fun observeVideos(folderPath: String?): Flow<List<Video>> {
        return combine(mediaService.observeVideos(folderPath), mediumStateDao.getAll()) { mediaVideos, mediumStates ->
            val statesMap = mediumStates.associateBy { it.uriString }
            mediaVideos.map { mediaVideo ->
                val mediaState = statesMap[mediaVideo.uri.toString()]
                mediaVideo.toVideo(mediaState)
            }
        }.distinctUntilChanged().flowOn(Dispatchers.Default)
    }

    override suspend fun fetchFolders(folderPath: String?): List<Folder> = withContext(Dispatchers.IO) {
        mediaService.fetchFolders(folderPath).map { it.toFolder() }
    }

    override suspend fun fetchVideos(folderPath: String?): List<Video> = withContext(Dispatchers.IO) {
        mediaService.fetchVideos(folderPath).mapAsync { mediaVideo ->
            val mediaState = mediumStateDao.get(mediaVideo.uri.toString())
            mediaVideo.toVideo(mediaState)
        }
    }

    override fun observePlaybackHistory(): Flow<List<Video>> {
        return combine(mediaService.observeVideos(), mediumStateDao.getAll()) { mediaVideos, mediumStates ->
            val videosByUri = mediaVideos.associateBy { it.uri.toString() }
            mediumStates
                .filter { it.lastPlayedTime != null }
                .sortedByDescending { it.lastPlayedTime }
                .map { state ->
                    videosByUri[state.uriString]?.toVideo(state) ?: state.toHistoryVideo()
                }
        }.distinctUntilChanged().flowOn(Dispatchers.Default)
    }

    override fun observeTrashVideos(): Flow<List<Video>> {
        return mediaService.observeTrashVideos()
            .map { videos -> videos.map { it.toVideo() } }
            .distinctUntilChanged()
    }

    override suspend fun getVideoByUri(uri: String): Video? = coroutineScope {
        val mediaVideoDeferred = async { mediaService.findVideo(uri.toUri()) }
        val mediaStateDeferred = async { mediumStateDao.get(uri) }

        val mediaVideo = mediaVideoDeferred.await() ?: return@coroutineScope null
        val mediaState = mediaStateDeferred.await()

        return@coroutineScope mediaVideo.toVideo(mediaState)
    }

    override suspend fun getVideoState(uri: String): VideoState? {
        return mediumStateDao.get(uri)?.toVideoState()
    }

    override suspend fun getMediaInfo(uri: String): MediaInfo? = withContext(Dispatchers.IO) {
        val video = getVideoByUri(uri) ?: return@withContext null
        val cached = dev.anilbeesetti.nextplayer.core.common.cache.MediaMetadataCache.get(context, uri)
        if (cached != null && (cached.fps != null || cached.videoCodec != null || cached.audioTracks.isNotEmpty())) {
            val cachedVideoStream = dev.anilbeesetti.nextplayer.core.model.VideoStreamInfo(
                index = 0,
                title = null,
                codecName = cached.videoCodec.orEmpty(),
                language = null,
                disposition = 0,
                bitRate = cached.videoBitrate,
                frameRate = cached.fps?.toDouble() ?: 0.0,
                frameWidth = video.width,
                frameHeight = video.height,
            )
            return@withContext MediaInfo(
                video = video.copy(format = cached.format ?: video.format),
                videoStream = cachedVideoStream,
                audioStreams = cached.audioTracks.mapIndexed { idx, a ->
                    dev.anilbeesetti.nextplayer.core.model.AudioStreamInfo(
                        index = idx,
                        title = a.title,
                        codecName = a.codec,
                        language = a.language,
                        disposition = 0,
                        bitRate = a.bitRate,
                        sampleFormat = null,
                        sampleRate = a.sampleRate,
                        channels = a.channels,
                        channelLayout = a.channelLayout,
                    )
                },
                subtitleStreams = cached.subtitleTracks.mapIndexed { idx, s ->
                    dev.anilbeesetti.nextplayer.core.model.SubtitleStreamInfo(
                        index = idx,
                        title = s.title,
                        codecName = s.codec,
                        language = s.language,
                        disposition = 0,
                    )
                },
            )
        }
        val mediaInfo = runCatching { MediaInfoBuilder().from(context = context, uri = uri.toUri()).build() }.getOrNull()
        val videoStreamInfo = mediaInfo?.videoStream?.toVideoStreamInfo()
        val audioStreamInfos = mediaInfo?.audioStreams?.map { it.toAudioStreamInfo() } ?: emptyList()
        val subtitleStreamInfos = mediaInfo?.subtitleStreams?.map { it.toSubtitleStreamInfo() } ?: emptyList()
        val result = MediaInfo(
            video = video.copy(format = mediaInfo?.format),
            videoStream = videoStreamInfo,
            audioStreams = audioStreamInfos,
            subtitleStreams = subtitleStreamInfos,
        )
        dev.anilbeesetti.nextplayer.core.common.cache.MediaMetadataCache.put(
            context = context,
            key = uri,
            specs = dev.anilbeesetti.nextplayer.core.common.cache.CachedVideoSpecs(
                fps = videoStreamInfo?.frameRate?.toFloat()?.takeIf { it > 0f },
                videoCodec = videoStreamInfo?.codecName?.takeIf { it.isNotBlank() },
                videoBitrate = videoStreamInfo?.bitRate ?: 0L,
                format = mediaInfo?.format,
                audioTracks = audioStreamInfos.map {
                    dev.anilbeesetti.nextplayer.core.common.cache.CachedAudioTrack(
                        title = it.title,
                        codec = it.codecName,
                        language = it.language,
                        channels = it.channels,
                        channelLayout = it.channelLayout,
                        sampleRate = it.sampleRate,
                        bitRate = it.bitRate,
                    )
                },
                subtitleTracks = subtitleStreamInfos.map {
                    dev.anilbeesetti.nextplayer.core.common.cache.CachedSubtitleTrack(
                        title = it.title,
                        codec = it.codecName,
                        language = it.language,
                    )
                },
            ),
        )
        mediaInfo?.release()
        return@withContext result
    }

    override suspend fun clearPlaybackHistory() {
        lastSavedPositions.clear()
        dev.anilbeesetti.nextplayer.core.common.cache.MediaMetadataCache.clear(context)
        mediumStateDao.clearPlaybackHistory()
    }

    override suspend fun removeVideosFromPlaybackHistory(uris: List<String>) {
        uris.forEach { lastSavedPositions.remove(it) }
        dev.anilbeesetti.nextplayer.core.common.cache.MediaMetadataCache.remove(context, uris)
        mediumStateDao.removeVideosFromPlaybackHistory(uris)
    }

    override suspend fun updateMediumLastPlayedTime(uri: String, lastPlayedTime: Long, duration: Long?) {
        val stateEntity = mediumStateDao.get(uri) ?: MediumStateEntity(uriString = uri)

        mediumStateDao.upsert(
            mediumState = stateEntity.copy(
                lastPlayedTime = lastPlayedTime,
                duration = duration ?: stateEntity.duration,
            ),
        )
    }

    override suspend fun updateMediumPosition(uri: String, position: Long) {
        val lastSaved = lastSavedPositions[uri]
        if (lastSaved != null && position != 0L && (position - lastSaved).absoluteValue < 2000L) {
            return
        }
        lastSavedPositions[uri] = position
        val stateEntity = mediumStateDao.get(uri) ?: MediumStateEntity(uriString = uri)
        if (stateEntity.playbackPosition == position) return
        mediumStateDao.upsert(
            mediumState = stateEntity.copy(
                playbackPosition = position,
            ),
        )
    }

    override suspend fun updateMediumPlaybackSpeed(uri: String, playbackSpeed: Float) {
        val stateEntity = mediumStateDao.get(uri) ?: MediumStateEntity(uriString = uri)

        mediumStateDao.upsert(
            mediumState = stateEntity.copy(
                playbackSpeed = playbackSpeed,
            ),
        )
    }

    override suspend fun updateMediumAudioTrack(uri: String, audioTrackIndex: Int) {
        val stateEntity = mediumStateDao.get(uri) ?: MediumStateEntity(uriString = uri)

        mediumStateDao.upsert(
            mediumState = stateEntity.copy(
                audioTrackIndex = audioTrackIndex,
            ),
        )
    }

    override suspend fun updateMediumSubtitleTrack(uri: String, subtitleTrackIndex: Int) {
        val stateEntity = mediumStateDao.get(uri) ?: MediumStateEntity(uriString = uri)

        mediumStateDao.upsert(
            mediumState = stateEntity.copy(
                subtitleTrackIndex = subtitleTrackIndex,
            ),
        )
    }

    override suspend fun updateMediumZoom(uri: String, zoom: Float) {
        val stateEntity = mediumStateDao.get(uri) ?: MediumStateEntity(uriString = uri)

        mediumStateDao.upsert(
            mediumState = stateEntity.copy(
                videoScale = zoom,
            ),
        )
    }

    override suspend fun addExternalAudioToMedium(uri: String, audioUri: Uri) {
        val stateEntity = mediumStateDao.get(uri) ?: MediumStateEntity(uriString = uri)
        val audio = UriListConverter.fromStringToList(stateEntity.externalAudio)
        if (audioUri in audio) return
        mediumStateDao.upsert(
            stateEntity.copy(externalAudio = UriListConverter.fromListToString(audio + audioUri)),
        )
    }

    override suspend fun addExternalSubtitleToMedium(uri: String, subtitleUri: Uri) {
        val stateEntity = mediumStateDao.get(uri) ?: MediumStateEntity(uriString = uri)
        val currentExternalSubs = UriListConverter.fromStringToList(stateEntity.externalSubs)

        if (currentExternalSubs.contains(subtitleUri)) return
        val newExternalSubs = UriListConverter.fromListToString(urlList = currentExternalSubs + subtitleUri)

        mediumStateDao.upsert(
            mediumState = stateEntity.copy(
                externalSubs = newExternalSubs,
            ),
        )
    }

    override suspend fun updateSubtitleDelay(uri: String, delay: Long) {
        val stateEntity = mediumStateDao.get(uri) ?: MediumStateEntity(uriString = uri)

        mediumStateDao.upsert(
            mediumState = stateEntity.copy(
                subtitleDelayMilliseconds = delay,
            ),
        )
    }

    override suspend fun updateSubtitleSpeed(uri: String, speed: Float) {
        val stateEntity = mediumStateDao.get(uri) ?: MediumStateEntity(uriString = uri)

        mediumStateDao.upsert(
            mediumState = stateEntity.copy(
                subtitleSpeed = speed,
            ),
        )
    }
}

private fun MediumStateEntity.toHistoryVideo(): Video {
    val uri = uriString.toUri()
    val name = uri.lastPathSegment?.substringAfterLast('/')
        ?: uri.host
        ?: uriString
    return Video(
        id = uriString.hashCode().toLong().absoluteValue,
        path = uriString,
        uriString = uriString,
        nameWithExtension = name,
        duration = duration ?: 0,
        width = 0,
        height = 0,
        size = 0,
        formattedDuration = duration?.let(Utils::formatDurationMillis).orEmpty(),
        playbackPosition = playbackPosition,
        lastPlayedAt = lastPlayedTime?.let(::Date),
    )
}
