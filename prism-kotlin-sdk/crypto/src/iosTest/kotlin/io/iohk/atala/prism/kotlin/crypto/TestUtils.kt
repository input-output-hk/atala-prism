package io.iohk.atala.prism.kotlin.crypto

// Annotations for writing common tests, excluding individual platforms

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
actual annotation class IgnoreJvmAndroid

actual typealias IgnoreIos = kotlin.test.Ignore

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
actual annotation class IgnoreJs
