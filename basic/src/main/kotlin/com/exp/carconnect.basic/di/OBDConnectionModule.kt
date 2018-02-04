package com.exp.carconnect.basic.di

import com.exp.carconnect.obdlib.OBDEngine
import dagger.Module
import dagger.Provides
import io.reactivex.Scheduler
import java.io.InputStream
import java.io.OutputStream

@Module()
class OBDConnectionModule(private val inputStream: InputStream,
                          private val outputStream: OutputStream) {

    @OBDConnection
    @Provides
    fun provideOBEngine(@Io ioScheduler: Scheduler,
                        @Computation computationScheduler: Scheduler)
            = OBDEngine(inputStream, outputStream, ioScheduler, computationScheduler)
}