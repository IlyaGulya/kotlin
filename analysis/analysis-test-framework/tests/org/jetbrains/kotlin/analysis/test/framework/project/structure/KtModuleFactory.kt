/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.project.structure

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtModuleWithFiles
import org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestDirectives
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices

fun interface KtModuleFactory : TestService {
    fun createModule(testModule: TestModule, testServices: TestServices, project: Project): KtModuleWithFiles
}

private val TestServices.ktModuleFactory: KtModuleFactory by TestServices.testServiceAccessor()

/**
 * Returns the appropriate [KtModuleFactory] to build a [KtModule][org.jetbrains.kotlin.analysis.project.structure.KtModule] for the given
 * [testModule].
 *
 * By default, the [KtModuleFactory] registered with these [TestServices] is returned. It may be overruled by the
 * [MODULE_KIND][org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestDirectives.MODULE_KIND] directive for a specific test module.
 */
fun TestServices.getKtModuleFactoryForTestModule(testModule: TestModule): KtModuleFactory {
    val explicitKinds = testModule.directives[AnalysisApiTestDirectives.MODULE_KIND]
    if (explicitKinds.size > 1) {
        throw IllegalArgumentException("A test module may only specify one `${AnalysisApiTestDirectives.MODULE_KIND.name}`.")
    }

    return when (explicitKinds.singleOrNull()) {
        TestModuleKind.Source -> KtSourceModuleFactory
        TestModuleKind.LibraryBinary -> KtLibraryBinaryModuleFactory
        TestModuleKind.LibrarySource -> KtLibrarySourceModuleFactory
        TestModuleKind.ScriptSource -> KtScriptModuleFactory
        else -> ktModuleFactory
    }
}
