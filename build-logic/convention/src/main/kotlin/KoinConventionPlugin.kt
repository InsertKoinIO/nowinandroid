/*
 * Copyright 2024 The Android Open Source Project
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

import com.google.samples.apps.nowinandroid.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.koin.compiler.plugin.KoinGradleExtension

/**
 * Convention plugin for Koin dependency injection using the native Koin Compiler Plugin.
 *
 * This plugin applies the Koin Compiler Plugin which:
 * - Processes @Module, @ComponentScan, @Configuration annotations
 * - Processes @Singleton, @Factory, @Scoped, @KoinViewModel annotations
 * - Supports JSR-330 annotations (javax.inject.*, jakarta.inject.*)
 *
 * Usage: Apply this plugin to any module that uses Koin annotations.
 */
class KoinConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // Apply Koin Compiler Plugin
            // This replaces the KSP-based koin-ksp-compiler
            pluginManager.apply("io.insert-koin.compiler.plugin")

            // Configure Koin Compiler Plugin logging
            extensions.configure<KoinGradleExtension> {
                userLogs.set(false)   // Log component detection and DSL interceptions
                debugLogs.set(false)  // Log detailed information about generated code and processing
            }

            // Add koin-annotations dependency for @Module, @Configuration, @ComponentScan, etc.
            dependencies {
                add("implementation", libs.findLibrary("koin.annotations").get())
            }
        }
    }
}
