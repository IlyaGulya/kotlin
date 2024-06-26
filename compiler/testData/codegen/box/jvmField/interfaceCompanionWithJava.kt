// LANGUAGE: +JvmFieldInInterface
// TARGET_BACKEND: JVM

// WITH_STDLIB
// FILE: Test.java

public class Test {
    public static String publicField() {
        return Foo.o.getS() + Foo.k.getS();
    }
}

// FILE: simple.kt


public class Bar(public val s: String)

interface Foo {

    companion object {
        @JvmField
        val o = Bar("O")

        @JvmField
        val k = Bar("K")
    }
}


fun box(): String {
    return Test.publicField()
}
