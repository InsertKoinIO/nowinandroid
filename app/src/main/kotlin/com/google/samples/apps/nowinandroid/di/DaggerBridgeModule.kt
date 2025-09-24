/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.nowinandroid.di

import coil.ImageLoader
import com.google.samples.apps.nowinandroid.core.data.repository.RecentSearchRepository
import com.google.samples.apps.nowinandroid.core.data.repository.SearchContentsRepository
import com.google.samples.apps.nowinandroid.core.data.repository.TopicsRepository
import com.google.samples.apps.nowinandroid.core.data.repository.UserDataRepository
import com.google.samples.apps.nowinandroid.core.data.util.SyncManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.koin.android.dagger.dagger
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.scope.Scope

@InstallIn(SingletonComponent::class)
@EntryPoint
interface DaggerBridge {
    fun imageLoader(): ImageLoader
    fun syncManager(): SyncManager
}

// only Factory to not keep instance in Koin of Dagger's instance
@Module
@Configuration
class DaggerBridgeModule {

    @Factory
    fun imageLoader(scope : Scope) = daggerBridge(scope).imageLoader()

    @Factory
    fun syncManager(scope : Scope) = daggerBridge(scope).syncManager()

    private fun daggerBridge(scope: Scope): DaggerBridge = scope.dagger<DaggerBridge>()
}