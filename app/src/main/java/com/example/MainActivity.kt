package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.example.data.api.ApiSettingsManager
import com.example.data.repository.RestaurantRepository
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setupCoilImageCache()
    ApiSettingsManager.init(this)
    com.example.data.sync.SyncManager.getInstance(this)
    val repo = RestaurantRepository.getInstance()
    repo.initLocalStorage(this)
    lifecycleScope.launch {
        if (repo.isOnline()) {
            repo.syncFloorPlanIfOnline(force = true)
            repo.syncMenuCatalogIfOnline(force = true)
        }
    }
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          AppNavigation()
        }
      }
    }
  }

  private fun setupCoilImageCache() {
    try {
      val imageLoader = ImageLoader.Builder(this)
        .memoryCache {
          MemoryCache.Builder(this)
            .maxSizePercent(0.25)
            .build()
        }
        .diskCache {
          DiskCache.Builder()
            .directory(cacheDir.resolve("image_cache"))
            .maxSizeBytes(250L * 1024 * 1024) // 250 MB disk cache for offline photos
            .build()
        }
        .networkCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .respectCacheHeaders(false)
        .crossfade(true)
        .build()

      Coil.setImageLoader(imageLoader)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}


