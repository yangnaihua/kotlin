// TARGET_BACKEND: JVM
// WITH_RUNTIME
// LANGUAGE_VERSION: 1.2

// FILE: foo.kt

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@file:JvmPackageName("baz.foo.quux.bar")
package foo.bar

fun f() {}

// FILE: bar.kt

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

fun box(): String {
    val klass = Class.forName("baz.foo.quux.bar.FooKt")
    val metadata = klass.getAnnotation(Metadata::class.java)
    return if (metadata.pn == "foo.bar") "OK" else "Fail: ${metadata.pn}"
}
