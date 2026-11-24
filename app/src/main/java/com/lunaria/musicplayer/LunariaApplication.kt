package com.lunaria.musicplayer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.lunaria.musicplayer.data.coil.AudioArtworkFetcher

class LunariaApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(AudioArtworkFetcher.Factory(this@LunariaApplication))
            }
            .crossfade(true)
            .build()
    }
}

