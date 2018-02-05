package com.exp.carconnect.base.di

import java.lang.annotation.Documented
import javax.inject.Qualifier


@Qualifier
@Documented
@Retention(AnnotationRetention.RUNTIME)
annotation class Io

@Qualifier
@Documented
@Retention(AnnotationRetention.RUNTIME)
annotation class Computation

@Qualifier
@Documented
@Retention(AnnotationRetention.RUNTIME)
annotation class Main