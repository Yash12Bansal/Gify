package com.yash.gify.domain.util

import android.app.Application
import java.io.File

interface CacheProvider {
    fun gifCache():File
}


class RealCacheProvider constructor(var app:Application):CacheProvider{
    override fun gifCache():File {
        var file=File("${app.cacheDir.path}/temp_gifs")
        if(!file.exists()){
            file.mkdirs()
        }
        return file

    }

}