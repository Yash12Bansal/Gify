package com.yash.gify.interactors

import com.yash.gify.domain.DataState
import com.yash.gify.domain.DataState.*
import com.yash.gify.domain.DataState.Loading.LoadingState.*
import com.yash.gify.domain.util.CacheProvider
import com.yash.gify.domain.util.RealCacheProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface ClearGifCache {
    fun execute(): Flow<DataState<Unit>>
}
class ClearGifCacheInteractor constructor(
    var cacheProvider: CacheProvider,

):ClearGifCache{
    override fun execute(): Flow<DataState<Unit>> = flow {
        emit(Loading(Active()))
        try{
            clearGifCache(cacheProvider)
            emit(DataState.Data(Unit))
        }
        catch (e:Exception){
            emit(DataState.Error(e.message?:CLEAR_CACHED_FILES_ERROR))
        }

    }
    companion object{
        var CLEAR_CACHED_FILES_ERROR="Error while clearing the cached gif"

        fun clearGifCache(cacheProvider: CacheProvider){
            var internalStorageDirectory=cacheProvider.gifCache()

            var files=internalStorageDirectory.listFiles()
            for(file in files){
                file.delete()
            }
        }
    }

}