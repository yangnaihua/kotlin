import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.uast.UFile
import org.jetbrains.uast.kotlin.KotlinUClass
import org.jetbrains.uast.kotlin.declarations.KotlinUMethod
import org.jetbrains.uast.test.kotlin.AbstractKotlinUastTest
import org.jetbrains.uast.toUElement
import org.junit.Test

/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class KotlinUastForDetachedElementsTest : AbstractKotlinUastTest() {

    @Test
    fun testClass() {
        val factory = KtPsiFactory(project)
        val ktClass = factory.createClass("class A")
        KtUsefulTestCase.assertInstanceOf(ktClass.toUElement(), KotlinUClass::class.java)
    }

    @Test
    fun testFunction() {
        val factory = KtPsiFactory(project)
        val function = factory.createFunction("fun foo(){}")
        KtUsefulTestCase.assertInstanceOf(function.toUElement(), KotlinUMethod::class.java)
    }


    override fun check(testName: String, file: UFile) {
        error("should not be called")
    }

    override fun setUp() {
        super.setUp()
        getVirtualFile("Elvis") //Configuring by any file, required to setup
    }

}