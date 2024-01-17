package com.yash.gify.domain

import android.os.Build

interface VersionProvider {
    fun provideVersion():Int
}

class RealVersionProvider constructor():VersionProvider{

    override fun provideVersion(): Int = Build.VERSION.SDK_INT

}