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

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.extension.ExtendWith
import org.junitpioneer.jupiter.TempDirectory
import java.io.File
import java.nio.file.Path
import kotlin.test.*

@ExtendWith(TempDirectory::class)
class BuildLogicFunctionalTest {
    private lateinit var settingsFile: File
    private lateinit var buildFile: File
    private lateinit var testProjectDir: Path
    @BeforeTest
    fun setup(@TempDirectory.TempDir projectDir: Path) {
        settingsFile = projectDir.resolve("settings.gradle").toFile()
        buildFile = projectDir.resolve("build.gradle").toFile()
        this.testProjectDir = projectDir
    }

    @Test
    fun `tasks exist for main and test sourceSets`() {
        // GIVEN
        settingsFile.writeText("""rootProject.name = "hello-world"""")
        buildFile.writeText("""
            plugins {
                id "com.github.fraenkelc.apiscanner.ApiScannerPlugin"
            }
        """)

        // WHEN
        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("scanForApiCandidates", "scanForTestApiCandidates")
                .withPluginClasspath()
                .build()

        // THEN
        listOf("", "Test").forEach {
            val task = assertNotNull(result.task(":scanFor${it}ApiCandidates"), "$it SourceSet task should have run and exist")
            assertEquals(SUCCESS, task.outcome, "$it SourceSet Task should have succeeded")
        }
    }


}