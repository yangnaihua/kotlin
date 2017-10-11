enum class Z(func: () -> Unit) {
    A(getFunc()),
    B({ getFunc() }) // Warning
}

fun getFunc(): () -> Unit = {
    println("whatever")
}