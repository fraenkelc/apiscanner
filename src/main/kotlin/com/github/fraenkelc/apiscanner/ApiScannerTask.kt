/*
 * Copyright 2018 Christian Fraenkel
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
package com.github.fraenkelc.apiscanner

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property

open class ApiScannerTask : DefaultTask() {

    @get:Input
    val inputConfiguration = project.objects.property<Configuration>()

    @get:InputFiles
    @get:Classpath
    val classesDirs = project.files()

    init {
        description = "Scans for API dependency candidates in the supplied configuration"
        group = "help"
    }

    @Suppress("unused")
    @TaskAction
    fun scanConfiguration() {
        val cfg = inputConfiguration.get()
        val resolved = cfg.resolvedConfiguration

        val classesResult = ArtifactScanner("Local classes", classesDirs.files).scanArtifact()
        val resultsFromConfiguration = collectModuleDependencies(resolved.firstLevelModuleDependencies)
                .map { it to ArtifactScanner(it.toString(), it.moduleArtifacts.map { ma -> ma.file }) }
                .map { it.first to it.second.scanArtifact() }

        val resolvedRequirements = (classesResult.consumed - classesResult.declared)
                .flatMap { requirement ->
                    resultsFromConfiguration
                            .filter { it.second.declared.contains(requirement) }
                            .map { requirement to it }
                }.groupBy({ it.second.first.toString() }, { it.first })
        val unresolvedRequirements = (classesResult.consumed - classesResult.declared - resolvedRequirements.values.flatten())
                .filterNot { it.startsWith("java") }

        if (unresolvedRequirements.isNotEmpty()) {
            println("Unresolved requirements: \n" + unresolvedRequirements.joinToString("\n") { " *  $it" })
            println()
        }
        if (resolvedRequirements.isNotEmpty()) {
            println("These dependencies should be declared using the API configuration as they are part of the ABI: \n"
                    + resolvedRequirements.toSortedMap()
                    .map { " * ${it.key}\n" + it.value.toSortedSet().joinToString("\n ** ", " ** ") }
                    .joinToString("\n"))
            println()
        }

        val implementationDeps = resolved.firstLevelModuleDependencies.map { it.toString() } - resolvedRequirements.keys
        if (implementationDeps.isNotEmpty()) {
            println("These dependencies can be declared using the Implementation configuration (not part of the ABI): \n"
                    + implementationDeps.joinToString("\n") { " * $it" })
            println()
        }
    }

    private fun collectModuleDependencies(dependencies: Set<ResolvedDependency>): Set<ResolvedDependency> =
            dependencies + dependencies.flatMap { collectModuleDependencies(it.children) }

}