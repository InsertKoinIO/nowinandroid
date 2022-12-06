/*
 * Copyright 2022 The Android Open Source Project
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

package com.google.samples.apps.nowinandroid.core.data.di

import com.google.samples.apps.nowinandroid.core.data.repository.AuthorsRepository
import com.google.samples.apps.nowinandroid.core.data.repository.NewsRepository
import com.google.samples.apps.nowinandroid.core.data.repository.OfflineFirstAuthorsRepository
import com.google.samples.apps.nowinandroid.core.data.repository.OfflineFirstNewsRepository
import com.google.samples.apps.nowinandroid.core.data.repository.OfflineFirstTopicsRepository
import com.google.samples.apps.nowinandroid.core.data.repository.OfflineFirstUserDataRepository
import com.google.samples.apps.nowinandroid.core.data.repository.TopicsRepository
import com.google.samples.apps.nowinandroid.core.data.repository.UserDataRepository
import com.google.samples.apps.nowinandroid.core.data.util.ConnectivityManagerNetworkMonitor
import com.google.samples.apps.nowinandroid.core.data.util.NetworkMonitor
import com.google.samples.apps.nowinandroid.core.database.daosKoinModule
import com.google.samples.apps.nowinandroid.core.database.databaseKoinModule
import com.google.samples.apps.nowinandroid.core.datastore.di.dataStoreKoinModule
import com.google.samples.apps.nowinandroid.core.network.di.networkKoinModule
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val dataKoinModule = module {
    includes(daosKoinModule, databaseKoinModule, dataStoreKoinModule, networkKoinModule)

    singleOf(::OfflineFirstTopicsRepository) { bind<TopicsRepository>() }
    singleOf(::OfflineFirstAuthorsRepository) { bind<AuthorsRepository>() }
    singleOf(::OfflineFirstNewsRepository) { bind<NewsRepository>() }
    singleOf(::OfflineFirstUserDataRepository) { bind<UserDataRepository>() }
    singleOf(::ConnectivityManagerNetworkMonitor) { bind<NetworkMonitor>() }
}