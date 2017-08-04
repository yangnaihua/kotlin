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

import org.jetbrains.jps.builders.JpsBuildTestCase
import org.jetbrains.kotlin.compilerRunner.JpsKotlinCompilerRunner
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.daemon.client.CompileServiceSession
import org.jetbrains.kotlin.daemon.common.COMPILE_DAEMON_ENABLED_PROPERTY
import org.jetbrains.kotlin.daemon.common.isDaemonEnabled
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.util.TraceClassVisitor
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class KotlinJpsBuildTestIncremental : KotlinJpsBuildTest() {
    var isICEnabledBackup: Boolean = false

    override fun setUp() {
        super.setUp()
        isICEnabledBackup = IncrementalCompilation.isEnabled()
        IncrementalCompilation.setIsEnabled(true)
    }

    override fun tearDown() {
        IncrementalCompilation.setIsEnabled(isICEnabledBackup)
        super.tearDown()
    }

    fun testJpsDaemonIC() {
        fun classToString(classFile: File): String {
            val out = StringWriter()
            val traceVisitor = TraceClassVisitor(PrintWriter(out))
            ClassReader(classFile.readBytes()).accept(traceVisitor, 0)
            return out.toString()
        }

        fun checkBytecodeContains(bc: String, substring: String) {
            if (bc.indexOf(substring) < 0) {
                error("Bytecode should contain '$substring':\n$bc")
            }
        }

        fun checkBytecodeNotContains(bc: String, substring: String) {
            if (bc.indexOf(substring) >= 0) {
                error("Bytecode should not contain '$substring':\n$bc")
            }
        }

        fun testImpl() {
            assert(isDaemonEnabled()) { "Daemon was not enabled!" }

            doTest()
            val module = myProject.modules.get(0)
            val mainKtClassFile = findFileInOutputDir(module, "MainKt.class")
            assert(mainKtClassFile.exists()) { "$mainKtClassFile does not exist!" }
            val mainBytecode1 = classToString(mainKtClassFile)
            checkBytecodeContains(mainBytecode1, "Fizz")
            checkBytecodeNotContains(mainBytecode1, "Buzz")

            JpsBuildTestCase.change(File(workDir, "src/utils.kt").absolutePath, """
                package foo

                const val BAR = "Buzz"
            """.trimIndent())
            val buildAllModules = buildAllModules()
            buildAllModules.assertSuccessful()

            assertCompiled(KotlinBuilder.KOTLIN_BUILDER_NAME, "src/main.kt", "src/utils.kt")
            val mainBytecode2 = classToString(mainKtClassFile)
            checkBytecodeContains(mainBytecode2, "Buzz")
            checkBytecodeNotContains(mainBytecode2, "Fizz")

            val session = run {
                val klass = JpsKotlinCompilerRunner::class.java
                val compileServiceField = klass.getDeclaredField("_jpsCompileServiceSession")
                compileServiceField.isAccessible = true
                val session = compileServiceField.get(klass)
                compileServiceField.isAccessible = false
                session as? CompileServiceSession
            } ?: error("Could not connect to daemon!")

            session.compileService.scheduleShutdown(graceful = true)
        }

        withSystemProperty(COMPILE_DAEMON_ENABLED_PROPERTY, "true") {
            withSystemProperty(JpsKotlinCompilerRunner.FAIL_ON_FALLBACK_PROPERTY, "true") {
                testImpl()
            }
        }
    }

    fun testManyFiles() {
        doTest()

        val module = myProject.modules.get(0)
        assertFilesExistInOutput(module, "foo/MainKt.class", "boo/BooKt.class", "foo/Bar.class")

        checkWhen(touch("src/main.kt"), null, packageClasses("kotlinProject", "src/main.kt", "foo.MainKt"))
        checkWhen(touch("src/boo.kt"), null, packageClasses("kotlinProject", "src/boo.kt", "boo.BooKt"))
        checkWhen(touch("src/Bar.kt"), arrayOf("src/Bar.kt"), arrayOf(klass("kotlinProject", "foo.Bar")))

        checkWhen(del("src/main.kt"),
                  pathsToCompile = null,
                  pathsToDelete = packageClasses("kotlinProject", "src/main.kt", "foo.MainKt"))
        assertFilesExistInOutput(module, "boo/BooKt.class", "foo/Bar.class")
        assertFilesNotExistInOutput(module, "foo/MainKt.class")

        checkWhen(touch("src/boo.kt"), null, packageClasses("kotlinProject", "src/boo.kt", "boo.BooKt"))
        checkWhen(touch("src/Bar.kt"), null, arrayOf(klass("kotlinProject", "foo.Bar")))
    }

    fun testManyFilesForPackage() {
        doTest()

        val module = myProject.modules.get(0)
        assertFilesExistInOutput(module, "foo/MainKt.class", "boo/BooKt.class", "foo/Bar.class")

        checkWhen(touch("src/main.kt"), null, packageClasses("kotlinProject", "src/main.kt", "foo.MainKt"))
        checkWhen(touch("src/boo.kt"), null, packageClasses("kotlinProject", "src/boo.kt", "boo.BooKt"))
        checkWhen(touch("src/Bar.kt"),
                  arrayOf("src/Bar.kt"),
                  arrayOf(klass("kotlinProject", "foo.Bar"),
                          packagePartClass("kotlinProject", "src/Bar.kt", "foo.MainKt"),
                          module("kotlinProject")))

        checkWhen(del("src/main.kt"),
                  pathsToCompile = null,
                  pathsToDelete = packageClasses("kotlinProject", "src/main.kt", "foo.MainKt"))
        assertFilesExistInOutput(module, "boo/BooKt.class", "foo/Bar.class")

        checkWhen(touch("src/boo.kt"), null, packageClasses("kotlinProject", "src/boo.kt", "boo.BooKt"))
        checkWhen(touch("src/Bar.kt"), null,
                  arrayOf(klass("kotlinProject", "foo.Bar"),
                          packagePartClass("kotlinProject", "src/Bar.kt", "foo.MainKt"),
                          module("kotlinProject")))
    }
}