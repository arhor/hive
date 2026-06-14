package io.github.arhor.catrecognizer.client.impl

import jakarta.inject.Qualifier
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

@Qualifier
@Retention(RUNTIME)
@Target(CLASS, FIELD, FUNCTION, VALUE_PARAMETER)
annotation class NativeApiCameraClient
