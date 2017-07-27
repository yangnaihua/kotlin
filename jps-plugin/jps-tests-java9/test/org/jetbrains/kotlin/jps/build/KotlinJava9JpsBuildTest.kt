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

package org.jetbrains.kotlin.jps.build

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.util.io.URLUtil
import org.jetbrains.jps.incremental.messages.BuildMessage
import org.jetbrains.jps.model.JpsDummyElement
import org.jetbrains.jps.model.JpsElement
import org.jetbrains.jps.model.JpsModel
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.java.JpsJavaSdkType
import org.jetbrains.jps.model.java.LanguageLevel
import org.jetbrains.jps.model.library.JpsOrderRootType
import org.jetbrains.jps.model.library.sdk.JpsSdk
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.kotlin.test.KotlinTestUtils

class KotlinJava9JpsBuildTest : AbstractKotlinJpsBuildTestCase() {
    override fun setUp() {
        super.setUp()
        workDir = KotlinTestUtils.tmpDirForTest(this)
    }

    override fun addJdk(name: String): JpsSdk<JpsDummyElement> = createJdk9(myModel, name)

    override fun <T : JpsElement?> addModule(
            moduleName: String, srcPaths: Array<String>, outputPath: String?, testOutputPath: String?, sdk: JpsSdk<T>
    ): JpsModule = super.addModule(moduleName, srcPaths, outputPath, testOutputPath, sdk).apply(Companion::setLanguageLevel9)

    fun testSimpleNamedModule() {
        createFile(
                "main.kt",
                """
                package p1

                fun main(args: Array<String>) {
                    println("Hello, " + listOf('w', 'o', 'r', 'l', 'd').joinToString(""))
                }
                """.trimIndent()
        )

        createFile(
                "module-info.java",
                """
                module m1 {
                    exports p1;
                    requires kotlin.stdlib;
                }
                """.trimIndent()
        )

        addModule("m1", orCreateProjectDir.path).apply(Companion::setLanguageLevel9)
        addKotlinStdlibDependency()
        buildAllModules().assertSuccessful()
    }

    fun testNamedModuleWithoutRequiresKotlinStdlib() {
        createFile("main.kt", "package p1; fun foo() {}")

        createFile("module-info.java", "module m1 { exports p1; }")

        addModule("m1", orCreateProjectDir.path)
        addKotlinStdlibDependency()
        val result = buildAllModules()
        result.assertFailed()
        assertEquals(listOf(
                "The Kotlin standard library is not found in the module graph. " +
                "Please ensure you have the 'requires kotlin.stdlib' clause in your module definition"
        ), result.getMessages(BuildMessage.Kind.ERROR).map { it.messageText })
    }

    companion object {
        fun createJdk9(model: JpsModel, name: String = "JDK 9"): JpsSdk<JpsDummyElement> {
            val path = KotlinTestUtils.getJdk9HomeIfPossible()?.absolutePath
                       ?: error("Environment variable JDK_9 must be set")
            val jdk = model.global.addSdk(name, path, "9", JpsJavaSdkType.INSTANCE)
            jdk.addRoot(StandardFileSystems.JRT_PROTOCOL_PREFIX + path + URLUtil.JAR_SEPARATOR + "java.base", JpsOrderRootType.COMPILED)
            return jdk.properties
        }

        fun setLanguageLevel9(module: JpsModule) {
            JpsJavaExtensionService.getInstance().getOrCreateModuleExtension(module).languageLevel = LanguageLevel.JDK_1_9
        }
    }
}
