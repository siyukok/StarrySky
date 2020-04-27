package com.lzx.musiclib

import android.Manifest
import android.app.Application
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Environment
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.danikula.videocache.HttpProxyCacheServer
import com.lzx.musiclib.example.MusicRequest
import com.lzx.musiclib.example.MusicRequest.RequestInfoCallback
import com.lzx.starrysky.MusicService
import com.lzx.starrysky.StarrySky
import com.lzx.starrysky.StarrySkyConfig
import com.lzx.starrysky.common.IMediaConnection
import com.lzx.starrysky.common.PlaybackStage
import com.lzx.starrysky.control.OnPlayerEventListener
import com.lzx.starrysky.control.PlayerControl
import com.lzx.starrysky.control.RepeatMode
import com.lzx.starrysky.imageloader.ImageLoaderCallBack
import com.lzx.starrysky.imageloader.ImageLoaderStrategy
import com.lzx.starrysky.intercept.InterceptorCallback
import com.lzx.starrysky.intercept.StarrySkyInterceptor
import com.lzx.starrysky.notification.INotification
import com.lzx.starrysky.notification.NotificationConfig
import com.lzx.starrysky.notification.StarrySkyNotificationManager
import com.lzx.starrysky.playback.offline.ICache
import com.lzx.starrysky.playback.player.Playback
import com.lzx.starrysky.playback.queue.MediaQueue
import com.lzx.starrysky.provider.IMediaSourceProvider
import com.lzx.starrysky.provider.SongInfo
import com.lzx.starrysky.utils.MainLooper
import com.lzx.starrysky.utils.StarrySkyUtils
import com.qw.soul.permission.SoulPermission
import com.qw.soul.permission.bean.Permission
import com.qw.soul.permission.bean.Permissions
import com.qw.soul.permission.callbcak.CheckRequestPermissionsListener
import com.tencent.bugly.crashreport.CrashReport
import java.io.File

/**
 * create by lzx
 * time:2018/11/9
 */
open class TestApplication : Application() {

    companion object {
        var context: Context? = null
    }

    override fun onCreate() {
        super.onCreate()
        context = this
        val config = StarrySkyConfig().newBuilder()
            .addInterceptor(PermissionInterceptor(this))
            .addInterceptor(RequestSongInfoInterceptor())
            .isOpenNotification(true)
//            .setNotificationFactory(MyNotificationFactory())
//            .setPlayback(MyPlayback())
//            .setPlayerControl(MyPlayerControl())
//            .setMediaQueueProvider(MyMediaQueueProvider())
//            .setMediaQueue(MyMediaQueue())
//            .setImageLoader(MyImageLoader())
//            .setIMediaConnection(MyMediaConnection())
//            .setCache(MyCache(this))
            .build()
        StarrySky.init(this, config)
        StarrySkyUtils.isDebug = true
        CrashReport.initCrashReport(applicationContext, "9e447caa98", false)
    }
}

/**
 * 权限申请拦截器
 */
class PermissionInterceptor internal constructor(private val mContext: Context) :
    StarrySkyInterceptor {
    override fun process(
        songInfo: SongInfo?, mainLooper: MainLooper, callback: InterceptorCallback
    ) {
        if (songInfo == null) {
            callback.onInterrupt(RuntimeException("SongInfo is null"))
            return
        }
        Log.i("TestApplication", "------PermissionInterceptor-----")
        SoulPermission.getInstance().checkAndRequestPermissions(Permissions.build(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE),
            object : CheckRequestPermissionsListener {
                override fun onAllPermissionOk(allPermissions: Array<Permission>) {
                    callback.onContinue(songInfo)
                }

                override fun onPermissionDenied(refusedPermissions: Array<Permission>) {
                    callback.onInterrupt(RuntimeException("没有权限，播放失败"))
                    mainLooper.runOnUiThread(Runnable {
                        Toast.makeText(mContext, "没有权限，播放失败", Toast.LENGTH_SHORT).show()
                    })
                }
            })
    }
}

/**
 * 请求播放url拦截器
 */
class RequestSongInfoInterceptor : StarrySkyInterceptor {
    private val mMusicRequest: MusicRequest = MusicRequest()
    override fun process(
        songInfo: SongInfo?, mainLooper: MainLooper, callback: InterceptorCallback
    ) {
        if (songInfo == null) {
            callback.onInterrupt(RuntimeException("SongInfo is null"))
            return
        }
        Log.i("TestApplication", "------RequestSongInfoInterceptor-----")
        if (songInfo.songUrl.isEmpty()) {
            mMusicRequest.requestSongUrl(songInfo.songId,
                object : RequestInfoCallback {
                    override fun onSuccess(songUrl: String) {
                        songInfo.songUrl = songUrl //给songInfo设置Url
                        callback.onContinue(songInfo)
                    }
                })
        } else {
            callback.onContinue(songInfo)
        }
    }
}

/**
 * 自定义通知栏实例
 */
class MyNotificationFactory : StarrySkyNotificationManager.NotificationFactory {
    override fun build(musicService: MusicService, config: NotificationConfig?): INotification {
        return object : INotification {
            override fun startNotification() {}
            override fun stopNotification() {}
            override fun updateFavoriteUI(isFavorite: Boolean) {}
            override fun updateLyricsUI(isChecked: Boolean) {}
        }
    }
}

/**
 * 自定义播放器示例
 */
class MyPlayback : Playback {
    override val playbackState: Int
        get() = Playback.STATE_NONE
    override val isConnected: Boolean
        get() = true
    override val isPlaying: Boolean
        get() = false
    override val currentStreamPosition: Long
        get() = 0L
    override val bufferedPosition: Long
        get() = 0L
    override val duration: Long
        get() = 0
    override var currentMediaId: String
        get() = ""
        set(value) {}
    override var volume: Float
        get() = 0F
        set(value) {}
    override val currPlayInfo: SongInfo?
        get() = null
    override val audioSessionId: Int
        get() = 0

    override fun stop() {}
    override fun play(songInfo: SongInfo, isPlayWhenReady: Boolean) {}
    override fun pause() {}
    override fun seekTo(position: Long) {}
    override fun onFastForward() {}
    override fun onRewind() {}
    override fun onDerailleur(refer: Boolean, multiple: Float) {}
    override fun setCallback(callback: Playback.Callback) {}
}

/**
 * 自定义播放控制示例
 */
class MyPlayerControl : PlayerControl {
    override fun playMusicById(songId: String) {}
    override fun playMusicByInfo(info: SongInfo) {}
    override fun playMusicByIndex(index: Int) {}
    override fun playMusic(songInfos: MutableList<SongInfo>, index: Int) {}
    override fun pauseMusic() {}
    override fun restoreMusic() {}
    override fun stopMusic() {}
    override fun prepare() {}
    override fun prepareFromSongId(songId: String) {}
    override fun skipToNext() {}
    override fun skipToPrevious() {}
    override fun fastForward() {}
    override fun rewind() {}
    override fun onDerailleur(refer: Boolean, multiple: Float) {}
    override fun seekTo(pos: Long) {}
    override fun setRepeatMode(repeatMode: Int, isLoop: Boolean) {}
    override fun getRepeatMode(): RepeatMode = RepeatMode(RepeatMode.REPEAT_MODE_NONE, true)
    override fun getPlayList(): MutableList<SongInfo> = mutableListOf()
    override fun updatePlayList(songInfos: MutableList<SongInfo>) {}
    override fun addPlayList(infos: MutableList<SongInfo>) {}
    override fun addSongInfo(info: SongInfo) {}
    override fun removeSongInfo(songId: String) {}
    override fun getNowPlayingSongInfo(): SongInfo? = null
    override fun getNowPlayingSongId(): String = ""
    override fun getNowPlayingIndex(): Int = 0
    override fun getBufferedPosition(): Long = 0
    override fun getPlayingPosition(): Long = 0
    override fun isSkipToNextEnabled(): Boolean = false
    override fun isSkipToPreviousEnabled(): Boolean = false
    override fun getPlaybackSpeed(): Float = 0F
    override fun getPlaybackState(): Any? = null
    override fun getErrorMessage(): CharSequence = ""
    override fun getErrorCode(): Int = -1
    override fun getState(): Int = 0
    override fun isPlaying(): Boolean = false
    override fun isPaused(): Boolean = false
    override fun isIdea(): Boolean = false
    override fun isCurrMusicIsPlayingMusic(songId: String): Boolean = false
    override fun isCurrMusicIsPlaying(songId: String): Boolean = false
    override fun isCurrMusicIsPaused(songId: String): Boolean = false
    override fun setVolume(audioVolume: Float) {}
    override fun getVolume(): Float = 0F
    override fun getDuration(): Long = 0
    override fun getAudioSessionId(): Int = 0
    override fun updateFavoriteUI(isFavorite: Boolean) {}
    override fun updateLyricsUI(isChecked: Boolean) {}
    override fun querySongInfoInLocal(): List<SongInfo> = mutableListOf()
    override fun addPlayerEventListener(listener: OnPlayerEventListener?) {}
    override fun removePlayerEventListener(listener: OnPlayerEventListener?) {}
    override fun clearPlayerEventListener() {}
    override fun getPlayerEventListeners(): MutableList<OnPlayerEventListener> = mutableListOf()
    override fun playbackState(): MutableLiveData<PlaybackStage> = MutableLiveData()
}

/**
 * 自定义数据源管理类示例
 */
class MyMediaQueueProvider : IMediaSourceProvider {
    override var songList: MutableList<SongInfo>
        get() = mutableListOf()
        set(value) {}

    override fun updateShuffleSongList() {}
    override fun getShuffleSongList(): MutableList<SongInfo> = mutableListOf()
    override val mediaMetadataCompatList: List<MediaMetadataCompat>
        get() = mutableListOf()

    override fun addSongInfo(info: SongInfo) {}
    override fun addSongInfos(infos: MutableList<SongInfo>) {}
    override fun deleteSongInfoById(songId: String) {}
    override fun hasSongInfo(songId: String): Boolean = false
    override fun getSongInfoById(songId: String): SongInfo? = null
    override fun getSongInfoByIndex(index: Int): SongInfo? = null
    override fun getIndexById(songId: String): Int = 0
    override fun getMediaMetadataById(songId: String?): MediaMetadataCompat? = null
    override fun updateMusicArt(
        songId: String, changeData: MediaMetadataCompat, albumArt: Bitmap, icon: Bitmap
    ) {
    }
}

/**
 * 自定义播放队列管理示例
 */
class MyMediaQueue : MediaQueue {
    override val currentIndex: Int
        get() = 0
    override val currentQueueSize: Int
        get() = 0

    override fun getCurrentSongInfo(isActiveTrigger: Boolean): SongInfo? = null
    override fun setMetadataUpdateListener(listener: IMediaSourceProvider.MetadataUpdateListener) {}
    override fun skipQueuePosition(amount: Int): Boolean = false
    override fun currSongIsFirstSong(): Boolean = false
    override fun currSongIsLastSong(): Boolean = false
    override fun updateIndexBySongId(songId: String): Boolean = false
    override fun updateMediaMetadata(songInfo: SongInfo?) {}
}

/**
 * 自定义图片加载器示例
 */
class MyImageLoader : ImageLoaderStrategy {
    override fun loadImage(context: Context, url: String?, callBack: ImageLoaderCallBack) {
        Glide.with(context).asBitmap().load(url)
            .into(object : CustomTarget<Bitmap?>() {
                override fun onLoadCleared(placeholder: Drawable?) {}
                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    callBack.onBitmapFailed(errorDrawable)
                }

                override fun onResourceReady(
                    resource: Bitmap, transition: Transition<in Bitmap?>?
                ) {
                    callBack.onBitmapLoaded(resource)
                }
            })
    }
}

/**
 * 自定义链接管理类示例
 */
class MyMediaConnection : IMediaConnection {
    override fun subscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {}
    override fun unsubscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {}
    override fun sendCommand(command: String, parameters: Bundle) {}
    override fun getNowPlaying(): MediaMetadataCompat? = null
    override fun getPlaybackStateCompat(): PlaybackStateCompat? = null
    override fun getPlaybackState(): MutableLiveData<PlaybackStage> = MutableLiveData()
    override fun getTransportControls(): MediaControllerCompat.TransportControls? = null
    override fun getMediaController(): MediaControllerCompat? = null
    override fun connect() {}
    override fun disconnect() {}
    override fun setOnConnectListener(listener: IMediaConnection.OnConnectListener?) {}
}

/**
 * 自定义缓存
 */
class MyCache(private val context: Context) : ICache {

    private var proxy: HttpProxyCacheServer? = null
    private var cacheFile: File? = null

    override fun startCache(url: String) {
        //什么都不做
    }

    private fun getProxy(): HttpProxyCacheServer? {
        return if (proxy == null) newProxy().also { proxy = it } else proxy
    }

    private fun newProxy(): HttpProxyCacheServer? {
        return HttpProxyCacheServer.Builder(context)
            .maxCacheSize(1024 * 1024 * 1024)       // 1 Gb for cache
            .cacheDirectory(getCacheDirectory(context, StarrySky.get().config().cacheDestFileDir))
            .build()
    }

    override fun getProxyUrl(url: String): String? {
        return getProxy()?.getProxyUrl(url)
    }

    override fun isOpenCache(): Boolean {
        return super.isOpenCache()
    }

    override fun getCacheDirectory(context: Context, destFileDir: String?): File? {
        var fileDir = destFileDir
        if (fileDir.isNullOrEmpty()) {
            fileDir =
                Environment.getExternalStorageDirectory().absolutePath.toString() + "/111StarrySkyCache/"
        }
        if (cacheFile == null && fileDir.isNotEmpty()) {
            cacheFile = File(destFileDir)
            if (cacheFile?.exists() == false) {
                cacheFile?.mkdirs()
            }
        }
        if (cacheFile == null) {
            cacheFile = context.getExternalFilesDir(null)
            if (cacheFile == null) {
                cacheFile = context.filesDir
            }
        }
        return cacheFile
    }

    override fun isCache(url: String): Boolean {
        return getProxy()?.isCached(url) ?: false
    }
}