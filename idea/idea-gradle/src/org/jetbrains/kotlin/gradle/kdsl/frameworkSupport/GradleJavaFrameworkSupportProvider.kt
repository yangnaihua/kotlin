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
package org.jetbrains.kotlin.gradle.kdsl.frameworkSupport

import com.intellij.framework.FrameworkTypeEx
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModifiableModelsProvider
import com.intellij.openapi.roots.ModifiableRootModel
import org.jetbrains.plugins.gradle.frameworkSupport.BuildScriptDataBuilder
import javax.swing.Icon

class GradleJavaFrameworkSupportProvider : GradleFrameworkSupportProvider() {

    override fun getFrameworkType(): FrameworkTypeEx {
        return object : FrameworkTypeEx(ID) {
            override fun createProvider(): FrameworkSupportInModuleProvider = this@GradleJavaFrameworkSupportProvider
            override fun getPresentableName(): String = "Java"
            override fun getIcon(): Icon = AllIcons.Nodes.Module
        }
    }

    override fun addSupport(module: Module,
                            rootModel: ModifiableRootModel,
                            modifiableModelsProvider: ModifiableModelsProvider,
                            buildScriptData: BuildScriptDataBuilder) {
        buildScriptData
                .addPluginDefinition("plugin(\"java\")")
                // TODO: in gradle > 4.0 it is just 'java { ... }'
                .addPropertyDefinition("configure<JavaPluginConvention> {\n    sourceCompatibility = JavaVersion.VERSION_1_8\n}")
                .addRepositoriesDefinition("mavenCentral()")
                .addDependencyNotation("testCompile(\"junit\", \"junit\", \"4.12\")")
    }

    companion object {
        val ID = "java"
    }
}
