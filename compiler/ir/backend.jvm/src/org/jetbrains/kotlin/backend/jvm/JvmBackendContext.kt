/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.COROUTINES_INTRINSICS_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ReflectionTypes
import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.jvm.descriptors.JvmSharedVariablesManager
import org.jetbrains.kotlin.backend.jvm.descriptors.SpecialDescriptorsFactory
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.PsiSourceManager
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.lowerIfFlexible

class JvmBackendContext(
        val state: GenerationState,
        psiSourceManager: PsiSourceManager,
        override val irBuiltIns: IrBuiltIns,
        irModuleFragment: IrModuleFragment, symbolTable: SymbolTable
) : CommonBackendContext {

    override val builtIns = state.module.builtIns

    val specialDescriptorsFactory = SpecialDescriptorsFactory(psiSourceManager, builtIns)

    override val sharedVariablesManager = JvmSharedVariablesManager(builtIns)

    override val reflectionTypes: ReflectionTypes by lazy(LazyThreadSafetyMode.PUBLICATION) {
        ReflectionTypes(state.module, FqName("kotlin.reflect.jvm.internal"))
    }

    override val ir = JvmIr(this, irModuleFragment, symbolTable)

    private fun find(memberScope: MemberScope, className: String): ClassDescriptor =
            find(memberScope, Name.identifier(className))

    private fun find(memberScope: MemberScope, name: Name): ClassDescriptor =
            memberScope.getContributedClassifier(name, NoLookupLocation.FROM_BACKEND) as ClassDescriptor

    override fun getInternalClass(name: String): ClassDescriptor =
            find(state.module.getPackage(FqName("kotlin.jvm.internal")).memberScope, name)

    fun getClass(fqName: FqName): ClassDescriptor = find(state.module.getPackage(fqName.parent()).memberScope, fqName.shortName())

    fun getPackage(fqName: FqName): PackageViewDescriptor = state.module.getPackage(fqName)

    override fun getInternalFunctions(name: String): List<FunctionDescriptor> = TODO(name)

    override fun log(message: () -> String) = print(message())

    override val messageCollector: MessageCollector
        get() = state.configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
}

class JvmIr(context: JvmBackendContext, irModule: IrModuleFragment, symbolTable: SymbolTable) :
        Ir<CommonBackendContext>(context, irModule) {

    override val symbols: Symbols<CommonBackendContext> = JvmSymbols(context, symbolTable)

    override fun shouldGenerateHandlerParameterForDefaultBodyFun() = true
}

class JvmSymbols(context: JvmBackendContext, symbolTable: SymbolTable) :
        Symbols<CommonBackendContext>(context, symbolTable) {

    private val intrinsicsClass = context.getInternalClass("Intrinsics")

    override val areEqualByValue: IrFunctionSymbol = symbolTable.referenceSimpleFunction(intrinsicsClass.staticScope.getContributedFunctions(Name.identifier("areEqual"), NoLookupLocation.FROM_BACKEND).single {
        it.valueParameters.all {
            it.type.lowerIfFlexible() == this@JvmSymbols.context.builtIns.anyType
        }
    })

    //TODO
    override val areEqual = areEqualByValue

    override val ThrowNullPointerException = symbolTable.referenceConstructor(
            context.getClass(FqName("kotlin.KotlinNullPointerException")).constructors.single { it.valueParameters.size == 1 })

    override val ThrowNoWhenBranchMatchedException = symbolTable.referenceConstructor(
            context.getClass(FqName("kotlin.NoWhenBranchMatchedException")).constructors.single { it.valueParameters.isEmpty() })

    override val ThrowTypeCastException = symbolTable.referenceConstructor(
            context.getClass(FqName("kotlin.TypeCastException")).constructors.single { it.valueParameters.size == 1 })

    override val ThrowUninitializedPropertyAccessException = symbolTable.referenceSimpleFunction(
            intrinsicsClass.staticScope.
                    getContributedFunctions(Name.identifier("throwUninitializedPropertyAccessException"), NoLookupLocation.FROM_BACKEND).single())

    override val stringBuilder = symbolTable.referenceClass(
            context.getClass(FqName("java.lang.StringBuilder"))
    )

    override val copyRangeTo = arrays.map { symbol ->
        val packageViewDescriptor = context.getClass(FqName("java.util.Arrays"))
        val functionDescriptor = packageViewDescriptor.staticScope
                .getContributedFunctions(Name.identifier("copyOfRange"), NoLookupLocation.FROM_BACKEND)
                .first {
                    it.valueParameters.firstOrNull()?.type?.constructor?.declarationDescriptor == symbol.descriptor
                }
        symbol.descriptor to symbolTable.referenceSimpleFunction(functionDescriptor)
    }.toMap()

    override val coroutineImpl = symbolTable.referenceClass(context.getClass(FqName("kotlin.coroutines.experimental.jvm.internal.CoroutineImpl")))

    override val coroutineSuspendedGetter = symbolTable.referenceSimpleFunction(
            context.getPackage(COROUTINES_INTRINSICS_PACKAGE_FQ_NAME).memberScope
                    .getContributedVariables(Name.identifier("COROUTINE_SUSPENDED"), NoLookupLocation.FROM_BACKEND)
                    .single().getter!!
    )
}