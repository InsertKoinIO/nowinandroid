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

package com.google.samples.apps.nowinandroid.core.data.di

import com.google.samples.apps.nowinandroid.core.data.repository.CompositeUserNewsResourceRepository
import com.google.samples.apps.nowinandroid.core.data.repository.NewsRepository
import com.google.samples.apps.nowinandroid.core.data.repository.UserDataRepository
import com.google.samples.apps.nowinandroid.core.data.repository.UserNewsResourceRepository
import com.google.samples.apps.nowinandroid.core.network.di.CoroutineScopesKoinModule
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.koin.android.dagger.dagger
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.core.scope.Scope

// bridge Dagger to Koin for DataKoinModule
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DataModuleBridge {
    fun newsRepository(): NewsRepository
    fun UserDataRepository(): UserDataRepository
}

@Module(includes = [CoroutineScopesKoinModule::class])
@Configuration
@ComponentScan("com.google.samples.apps.nowinandroid.core.data.util")
class DataKoinModule {

    @Single
    fun newsRepository(scope : Scope) : NewsRepository = scope.dagger<DataModuleBridge>().newsRepository()

    @Single
    fun userDataRepository(scope : Scope): UserDataRepository = scope.dagger<DataModuleBridge>().UserDataRepository()

    @Single
    fun userNewsResourceRepository(
        newsRepository: NewsRepository,
        userDataRepository: UserDataRepository,
    ): UserNewsResourceRepository = CompositeUserNewsResourceRepository(newsRepository, userDataRepository)
}