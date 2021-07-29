package com.yuriy.openradio.shared.coroutines

import kotlin.coroutines.*

fun launch(context: CoroutineContext = EmptyCoroutineContext, block: suspend () -> Unit) =
    block.startCoroutine(Continuation(context) { result ->
        result.onFailure { exception ->
            val currentThread = Thread.currentThread()
            currentThread.uncaughtExceptionHandler?.uncaughtException(currentThread, exception)
        }
    })
