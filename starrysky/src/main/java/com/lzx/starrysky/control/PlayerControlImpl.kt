package com.lzx.starrysky.control

import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import androidx.lifecycle.MutableLiveData
import com.lzx.basecode.FocusInfo
import com.lzx.basecode.data
import com.lzx.basecode.duration
import com.lzx.basecode.md5
import com.lzx.basecode.title
import com.lzx.starrysky.OnPlayerEventListener
import com.lzx.basecode.SongInfo
import com.lzx.starrysky.playback.MediaSourceProvider
import com.lzx.basecode.Playback
import com.lzx.basecode.isRefrain
import com.lzx.starrysky.playback.PlaybackManager
import com.lzx.starrysky.playback.PlaybackStage
import com.lzx.starrysky.utils.StarrySkyConstant
import com.lzx.starrysky.utils.StarrySkyUtils

class PlayerControlImpl(
    private val provider: MediaSourceProvider,
    private val playbackManager: PlaybackManager
) : PlayerControl {

    private val focusChangeState = MutableLiveData<FocusInfo>()
    private val playbackState = MutableLiveData<PlaybackStage>()
    private val playerEventListener = hashMapOf<String, OnPlayerEventListener>()

    override fun playMusicById(songId: String) {
        if (provider.hasSongInfo(songId)) {
            playMusicImpl(songId)
        }
    }

    override fun playMusicByInfo(info: SongInfo) {
        provider.addSongInfo(info)
        playMusicImpl(info.songId)
    }

    override fun playSingleMusicByInfo(info: SongInfo) {
        provider.clearSongInfos()
        provider.addSongInfo(info)
        val bundle = Bundle()
        bundle.putInt("clearSongId", 1)
        info.headData?.put("SongType", "playSingle")
        playMusicImpl(info.songId, bundle)
    }

    override fun playMusic(songInfos: MutableList<SongInfo>, index: Int) {
        updatePlayList(songInfos)
        playMusicImpl(songInfos.getOrNull(index)?.songId, null)
    }

    private fun playMusicImpl(songId: String?, extras: Bundle? = null) {
        playbackManager.onPlayFromMediaId(songId, extras)
    }

    override fun pauseMusic() {
        playbackManager.onPause()
    }

    override fun restoreMusic() {
        playbackManager.onPlay()
    }

    override fun stopMusic() {
        playbackManager.onStop()
    }

    override fun prepare() {
        playbackManager.onPrepare()
    }

    override fun prepareFromSongId(songId: String) {
        if (provider.hasSongInfo(songId)) {
            playbackManager.onPrepareFromSongId(songId)
        }
    }

    override fun playRefrain(info: SongInfo) {
        info.headData?.put("SongType", "Refrain")
        provider.refrain = info
        playbackManager.onPlayRefrain(info)
    }

    override fun stopRefrain() {
        playbackManager.stopRefrain()
    }

    override fun setRefrainVolume(audioVolume: Float) {
        var volume = audioVolume
        if (volume < 0) {
            volume = 0f
        }
        if (volume > 1) {
            volume = 1f
        }
        playbackManager.getRefrainPlayback()?.setVolume(volume)
    }

    override fun getRefrainVolume(): Float = playbackManager.getRefrainPlayback()?.getVolume() ?: -0f

    override fun getRefrainInfo(): SongInfo? = provider.refrain

    override fun isRefrainPlaying(): Boolean {
        return playbackManager.getRefrainPlayback()?.playbackState == Playback.STATE_PLAYING
    }

    override fun isRefrainBuffering(): Boolean {
        return playbackManager.getRefrainPlayback()?.playbackState == Playback.STATE_BUFFERING
    }

    override fun skipToNext() {
        playbackManager.onSkipToNext()
    }

    override fun skipToPrevious() {
        playbackManager.onSkipToPrevious()
    }

    override fun fastForward() {
        playbackManager.onFastForward()
    }

    override fun rewind() {
        playbackManager.onRewind()
    }

    override fun onDerailleur(refer: Boolean, multiple: Float) {
        playbackManager.onDerailleur(refer, multiple)
    }

    override fun seekTo(pos: Long) {
        playbackManager.seekTo(pos)
    }

    override fun setRepeatMode(repeatMode: Int, isLoop: Boolean) {
        StarrySkyUtils.saveRepeatMode(repeatMode, isLoop)
        playbackManager.setRepeatMode(repeatMode, isLoop)
    }

    override fun getRepeatMode(): RepeatMode = StarrySkyUtils.repeatMode

    override fun getPlayList(): MutableList<SongInfo> = provider.songList

    override fun updatePlayList(songInfos: MutableList<SongInfo>) {
        provider.songList = songInfos
    }

    override fun addPlayList(infos: MutableList<SongInfo>) {
        provider.addSongInfos(infos)
    }

    override fun addSongInfo(info: SongInfo) {
        provider.addSongInfo(info)
    }

    override fun addSongInfo(index: Int, info: SongInfo) {
        provider.addSongInfo(index, info)
    }

    override fun removeSongInfo(songId: String) {
        playbackManager.removeSongInfo(songId)
    }

    override fun clearPlayList() {
        provider.clearSongInfos()
    }

    override fun getNowPlayingSongInfo(): SongInfo? {
        val info = playbackManager.playback.currPlayInfo
        return if (info.isRefrain()) null else info
    }

    override fun getNowPlayingSongId(): String = getNowPlayingSongInfo()?.songId ?: ""

    override fun getNowPlayingSongUrl(): String = getNowPlayingSongInfo()?.songUrl ?: ""

    override fun getNowPlayingIndex(): Int {
        val songId = getNowPlayingSongId()
        return provider.getIndexById(songId)
    }

    override fun getBufferedPosition(): Long = playbackManager.playback.bufferedPosition

    override fun getPlayingPosition(): Long = playbackManager.playback.currentStreamPosition

    override fun isSkipToNextEnabled(): Boolean = playbackManager.isSkipToNextEnabled()

    override fun isSkipToPreviousEnabled(): Boolean = playbackManager.isSkipToPreviousEnabled()

    override fun getPlaybackSpeed(): Float = playbackManager.playback.getPlaybackSpeed()

    override fun isPlaying(): Boolean = playbackState.value?.stage == PlaybackStage.PLAYING

    override fun isPaused(): Boolean = playbackState.value?.stage == PlaybackStage.PAUSE

    override fun isIdea(): Boolean = playbackState.value?.stage == PlaybackStage.IDEA

    override fun isBuffering(): Boolean = playbackState.value?.stage == PlaybackStage.BUFFERING

    override fun isCurrMusicIsPlayingMusic(songId: String?): Boolean {
        return if (songId.isNullOrEmpty()) {
            false
        } else {
            val playingMusic = getNowPlayingSongInfo()
            playingMusic != null && songId == playingMusic.songId
        }
    }

    override fun isCurrMusicIsPlaying(songId: String?): Boolean = isCurrMusicIsPlayingMusic(songId) && isPlaying()

    override fun isCurrMusicIsPaused(songId: String?): Boolean = isCurrMusicIsPlayingMusic(songId) && isPaused()

    override fun isCurrMusicIsIdea(songId: String?): Boolean = isCurrMusicIsPlayingMusic(songId) && isIdea()

    override fun isCurrMusicIsBuffering(songId: String?): Boolean = isCurrMusicIsPlayingMusic(songId) && isBuffering()

    override fun setVolume(audioVolume: Float) {
        var volume = audioVolume
        if (volume < 0) {
            volume = 0f
        }
        if (volume > 1) {
            volume = 1f
        }
        playbackManager.playback.setVolume(volume)
    }

    override fun getVolume(): Float = playbackManager.playback.getVolume()

    override fun getDuration(): Long = playbackManager.playback.duration

    override fun getAudioSessionId(): Int = playbackManager.playback.audioSessionId

    override fun querySongInfoInLocal(context: Context): List<SongInfo> {
        val songInfos = mutableListOf<SongInfo>()
        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null,
            null, MediaStore.Audio.AudioColumns.IS_MUSIC) ?: return songInfos
        while (cursor.moveToNext()) {
            val song = SongInfo()
            song.songUrl = cursor.data
            song.songName = cursor.title
            song.duration = cursor.duration
            val songId = if (song.songUrl.isNotEmpty()) song.songUrl.md5() else System.currentTimeMillis().toString().md5()
            song.songId = songId
            songInfos.add(song)
        }
        cursor.close()
        return songInfos
    }

    override fun cacheSwitch(switch: Boolean) {
        StarrySkyConstant.KEY_CACHE_SWITCH = switch
    }

    override fun stopByTimedOff(time: Long, isFinishCurrSong: Boolean) {
        if (time < 0) {
            return
        }
        playbackManager.stopByTimedOff(time, isFinishCurrSong)
    }

    override fun addPlayerEventListener(listener: OnPlayerEventListener?, tag: String) {
        listener?.let {
            if (!playerEventListener.containsKey(tag)) {
                playerEventListener[tag] = it
            }
        }
    }

    override fun removePlayerEventListener(tag: String) {
        playerEventListener.remove(tag)
    }

    override fun clearPlayerEventListener() {
        playerEventListener.clear()
    }

    override fun focusStateChange(): MutableLiveData<FocusInfo> = focusChangeState

    override fun playbackState(): MutableLiveData<PlaybackStage> = playbackState

    override fun onPlaybackStateUpdated(playbackStage: PlaybackStage) {
        playbackState.postValue(playbackStage)
        playerEventListener.forEach {
            it.value.onPlaybackStageChange(playbackStage)
        }
    }

    override fun onFocusStateChange(info: FocusInfo) {
        focusChangeState.postValue(info)
    }
}