package com.yash.gify.domain

sealed class DataState<T>{
    data class Data<T>(
        var data:T?=null

    ):DataState<T>()

    data class Error<T>(
        val message:String
    ):DataState<T>()

    data class Loading<T>(
        val loading:LoadingState
    ):DataState<T>(){

        sealed class LoadingState{
            data class Active(
                val progress :Float=0f
            ):LoadingState()

            object Idle:LoadingState()
        }
    }



}
