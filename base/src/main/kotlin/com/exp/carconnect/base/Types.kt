package com.exp.carconnect.base

import java.util.concurrent.TimeUnit


sealed class UnAvailableAvailableData<T : Any> {
    class UnAvailable<T : Any> : UnAvailableAvailableData<T>()
    data class Available<T : Any>(val data: T) : UnAvailableAvailableData<T>()
}

sealed class LoadableState<State : Any, Error : Throwable> {
    class NotLoaded<State : Any, Error : Throwable> : LoadableState<State, Error>()
    class Loading<State : Any, Error : Throwable> : LoadableState<State, Error>()
    data class Loaded<State : Any, Error : Throwable>(val savedState: State) : LoadableState<State, Error>()
    data class LoadingError<State : Any, Error : Throwable>(val error: Error) : LoadableState<State, Error>()
}

data class Frequency(val frequency: Long, val unit: TimeUnit)
