package io.iohk.atala.prism.kotlin.crypto

// Annotations for writing common tests, excluding individual platforms

actual typealias IgnoreJvmAndroid = org.junit.jupiter.api.Disabled

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
actual annotation class IgnoreIos

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
actual annotation class IgnoreJs
