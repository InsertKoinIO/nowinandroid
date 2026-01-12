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

import com.google.samples.apps.nowinandroid.MainActivityViewModel
import com.google.samples.apps.nowinandroid.core.data.repository.UserDataRepository
import org.koin.core.annotation.KoinViewModel
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module

@Module(includes = [FeaturesModule::class, DomainModule::class])
@ComponentScan("com.google.samples.apps.nowinandroid.util","com.google.samples.apps.nowinandroid.ui")
@Configuration
class AppModule {

    // keep here to avoid ComponentScan scanning too much in other components
    @KoinViewModel
    fun mainActivityViewModel(userDataRepository: UserDataRepository) = MainActivityViewModel(userDataRepository)
}

@Module
@ComponentScan("com.google.samples.apps.nowinandroid.feature")
class FeaturesModule

@Module
@ComponentScan("com.google.samples.apps.nowinandroid.core.domain")
class DomainModule