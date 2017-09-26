// EXPECTED_REACHABLE_NODES: 1039
open class A {
    fun foo(): Char = 'X'
}

interface I {
    fun foo(): Any
}

class B : A(), I

fun typeOf(x: dynamic): String = js("typeof x")

fun box(): String {
    val a = B()
    val b: I = B()
    val c: A = B()

    val r1 = typeOf(a.asDynamic().foo())
    if (r1 != "object") return "fail1: $r1"

    val r2 = typeOf(b.asDynamic().foo())
    if (r2 != "object") return "fail2: $r2"

    val r3 = typeOf(c.asDynamic().foo())
    if (r3 != "object") return "fail3: $r3"

    val x4 = a.foo()
    val r4 = typeOf(x4.asDynamic())
    if (r4 != "number") return "fail4: $r4"

    val x5 = b.foo()
    val r5 = typeOf(x5.asDynamic())
    if (r5 != "object") return "fail5: $r5"

    val x6 = c.foo()
    val r6 = typeOf(x6.asDynamic())
    if (r6 != "number") return "fail6: $r6"

    return "OK"
}