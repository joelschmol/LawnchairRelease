package app.lawnchair.wallpaper.model

import android.app.WallpaperManager
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lawnchair.wallpaper.WallpaperManagerCompat
import app.lawnchair.wallpaper.service.Wallpaper
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WallpaperViewModel(context: Context) : ViewModel() {
    private val wallpaperManagerCompat = WallpaperManagerCompat.INSTANCE.get(context)

    private val _wallpapers = MutableLiveData<List<Wallpaper>>()
    val wallpapers: LiveData<List<Wallpaper>> = _wallpapers

    private val mutex = Mutex()

    private val listener = object : WallpaperManagerCompat.OnColorsChangedListener {
        override fun onColorsChanged() {
            viewModelScope.launch {
                mutex.withLock {
                    saveWallpaper(wallpaperManagerCompat.wallpaperManager)
                }
            }
        }
    }

    init {
        loadTopWallpapers()
        wallpaperManagerCompat.addOnChangeListener(listener)
    }

    private suspend fun saveWallpaper(wallpaperManager: WallpaperManager) {
        wallpaperManagerCompat.service.saveWallpaper(wallpaperManager)
        refreshWallpapers()
    }

    private suspend fun refreshWallpapers() {
        val topWallpapers = wallpaperManagerCompat.service.dao.getTopWallpapers()
        _wallpapers.postValue(topWallpapers)
    }

    private fun loadTopWallpapers() {
        viewModelScope.launch {
            mutex.withLock {
                refreshWallpapers()
            }
        }
    }

    suspend fun updateWallpaperRank(wallpaper: Wallpaper) {
        wallpaperManagerCompat.service.updateWallpaperRank(wallpaper)
        loadTopWallpapers()
    }
}
