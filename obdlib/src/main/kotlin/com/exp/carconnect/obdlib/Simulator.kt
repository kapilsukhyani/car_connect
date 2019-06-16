package com.exp.carconnect.obdlib

import java.io.InputStream
import java.io.OutputStream

private class Input : InputStream() {
    override fun read(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

private class Output : OutputStream() {
    override fun write(b: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class SimulatedStreams() {
    val inputStream: InputStream = Input()
    val outputStream: OutputStream = Output()
}