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

import org.jetbrains.jps.model.JpsDummyElement
import org.jetbrains.jps.model.JpsElement
import org.jetbrains.jps.model.library.sdk.JpsSdk
import org.jetbrains.jps.model.module.JpsModule

abstract class AbstractIncrementalJava9JpsTest : AbstractIncrementalJpsTest() {
    override fun createJdk(): JpsSdk<JpsDummyElement> = KotlinJava9JpsBuildTest.createJdk9(myModel)

    override fun <T : JpsElement?> addModule(
            moduleName: String, srcPaths: Array<String>, outputPath: String?, testOutputPath: String?, sdk: JpsSdk<T>
    ): JpsModule =
            super.addModule(moduleName, srcPaths, outputPath, testOutputPath, sdk).apply((KotlinJava9JpsBuildTest)::setLanguageLevel9)
}
