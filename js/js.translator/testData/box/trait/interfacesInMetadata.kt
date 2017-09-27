// EXPECTED_REACHABLE_NODES: 1058
import kotlin.js.*
import kotlin.reflect.*

interface I

interface J

open class A

class B : A(), I

class C : I

open class D : A()

class E : D(), I, J

private fun check(cls: KClass<*>, vararg interfaces: KClass<*>) {
    val actualInterfaces: Array<JsClass<*>> =  cls.js.asDynamic().`$metadata$`.interfaces
    assertEquals(interfaces.size, actualInterfaces.size, "Class ${cls.simpleName} contains wrong number of items in the list of interfaces")

    for (expectedInterface in interfaces) {
        if (expectedInterface.js !in actualInterfaces) {
            fail("Class ${cls.simpleName} is expected to contain ${expectedInterface.simpleName} in the list of interfaces")
        }
    }
}

fun box(): String {
    check(I::class)
    check(J::class)
    check(A::class)
    check(B::class, I::class)
    check(C::class, I::class)
    check(D::class)
    check(E::class, I::class, J::class)

    return "OK"
}

