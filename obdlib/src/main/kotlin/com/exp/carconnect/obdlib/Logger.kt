package com.exp.carconnect.obdlib

public class Logger {
    companion object {
        fun log(tag: String, message: String) {
            System.out.println("[Thread: ${Thread.currentThread().name}] [$tag]: $message")
        }

        fun log(tag: String, exception: Throwable) {
            System.out.println("[Thread: ${Thread.currentThread().name}] [$tag]: ${exception.localizedMessage}")
        }

        fun log(tag: String, message: String, exception: Throwable) {
            System.out.println("[Thread: ${Thread.currentThread().name}] [$tag]: [$message] ${exception.localizedMessage}")
        }
    }
}
