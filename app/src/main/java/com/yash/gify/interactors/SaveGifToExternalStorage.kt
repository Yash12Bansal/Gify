package com.yash.gify.interactors

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.yash.gify.domain.DataState
import com.yash.gify.domain.DataState.*
import com.yash.gify.domain.DataState.Loading.LoadingState.*
import com.yash.gify.domain.VersionProvider
import com.yash.gify.domain.util.FileNameBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.FileOutputStream

interface SaveGifToExternalStorage {
    fun execute(
        contentResolver: ContentResolver,
        context: Context,
        cachedUri:Uri,
        checkFilesPermission:()->Boolean,
    ):Flow<DataState<Unit>>
}

class SaveGifToExternalStorageInteractor constructor(
    var versionProvider: VersionProvider
):SaveGifToExternalStorage{
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun execute(
        contentResolver: ContentResolver,
        context: Context,
        cachedUri: Uri,
        checkFilesPermission: () -> Boolean
    ): Flow<DataState<Unit>> = flow {
        try{
            emit(Loading(Active()))
            when{
                versionProvider.provideVersion()>=Build.VERSION_CODES.Q->{
                    saveGifToScopedStorage(
                        contentResolver=contentResolver,
                        cachedUri=cachedUri
                    )
                    emit(DataState.Data(Unit))

                }
                checkFilesPermission()->{
                    saveGifToExternalStorage(
                        contentResolver=contentResolver,
                        context=context,
                        cachedUri=cachedUri
                    )
                    emit(DataState.Data(Unit))
                }
                else->{
                    emit(DataState.Error(SAVE_GIF_TO_EXTERNAL_STORAGE_ERROR))
                }

            }
            emit(Loading(Idle))
        }
        catch (e:Exception){
            emit(Error(e.message?:SAVE_GIF_TO_EXTERNAL_STORAGE_ERROR))
        }
    }

    companion object{
        var SAVE_GIF_TO_EXTERNAL_STORAGE_ERROR="Error while saving the gif to external storage"

        fun getBytesFromUri(
            contentResolver: ContentResolver,
            uri: Uri,

            ):ByteArray{
            var inputStream=contentResolver.openInputStream(uri)
            var bytes=inputStream?.readBytes()?: ByteArray(0)
            inputStream?.close()
            return bytes
        }
        fun saveGifToExternalStorage(
            context: Context,
            contentResolver: ContentResolver,
            cachedUri: Uri
        ){
            var bytes=getBytesFromUri(
                contentResolver=contentResolver,
                uri=cachedUri,
            )
            var picturesDir=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)

            var fileName=FileNameBuilder.buildFileName()
            var fileSavedPath= File(picturesDir,"${fileName}.gif")

            picturesDir.mkdirs()

            var fos=FileOutputStream(fileSavedPath)
            fos.write(bytes)
            fos.close()

            MediaScannerConnection.scanFile(
                context,
                arrayOf(fileSavedPath.toString()),
                null
            ){_,_->
            }
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        fun saveGifToScopedStorage(
            contentResolver: ContentResolver,
            cachedUri: Uri,
        ){
            var bytes= getBytesFromUri(
                contentResolver,
                uri=cachedUri
            )
            var fileName="${FileNameBuilder.buildFileName()}.gif"
            var externalUri=MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            var contentValues=ContentValues()

            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME,fileName)
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE,"image/gif")

            var uri=contentResolver.insert(externalUri,contentValues)?:throw Exception("Error inserting")
            return contentResolver.openOutputStream(uri)?.let { os->
                os.write(bytes)
                os.flush()
                os.close()
            }?:throw Exception("Error")

        }

    }


}