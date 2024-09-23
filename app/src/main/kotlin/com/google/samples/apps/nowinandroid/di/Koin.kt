/*
 * Copyright 2024 The Android Open Source Project
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
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.scope.Scope
import org.koin.dsl.module

@InstallIn(SingletonComponent::class)
@EntryPoint
interface DaggerBridge {
    fun getUserDataRepository(): UserDataRepository
}

inline fun <reified T> Scope.daggerBridge() : T{
    return EntryPoints.get(androidContext().applicationContext, T::class.java)
}

val jankStatsKoinModule = module {
    viewModelOf(::MainActivityViewModel)
    single { daggerBridge<DaggerBridge>().getUserDataRepository() }
}

val appModule = module {
    includes(jankStatsKoinModule)
}