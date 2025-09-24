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

import com.google.samples.apps.nowinandroid.core.analytics.AnalyticsHelper
import com.google.samples.apps.nowinandroid.core.analytics.AnalyticsKoinModule
import com.google.samples.apps.nowinandroid.core.database.dao.NewsResourceDao
import com.google.samples.apps.nowinandroid.core.database.dao.NewsResourceFtsDao
import com.google.samples.apps.nowinandroid.core.database.dao.RecentSearchQueryDao
import com.google.samples.apps.nowinandroid.core.database.dao.TopicDao
import com.google.samples.apps.nowinandroid.core.database.dao.TopicFtsDao
import com.google.samples.apps.nowinandroid.core.datastore.NiaPreferencesDataSource
import com.google.samples.apps.nowinandroid.core.network.NiaNetworkDataSource
import com.google.samples.apps.nowinandroid.core.network.di.CoroutineScopesKoinModule
import com.google.samples.apps.nowinandroid.core.notifications.Notifier
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.koin.android.dagger.dagger
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.scope.Scope

// bridge Dagger to Koin for DataKoinModule
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DataModuleBridge {
    fun recentSearchQueryDao(): RecentSearchQueryDao
    fun newsResourceDao(): NewsResourceDao
    fun newsResourceFtsDao(): NewsResourceFtsDao
    fun topicDao(): TopicDao
    fun topicFtsDao(): TopicFtsDao
    fun niaPreferencesDataSource(): NiaPreferencesDataSource
    fun network(): NiaNetworkDataSource
    fun notifier(): Notifier
//    fun analyticsHelper(): AnalyticsHelper
}

@Module(includes = [CoroutineScopesKoinModule::class, AnalyticsKoinModule::class])
@Configuration
@ComponentScan("com.google.samples.apps.nowinandroid.core.data")
class DataKoinModule {

    @Factory
    fun recentSearchQueryDao(scope: Scope): RecentSearchQueryDao =
        scope.dagger<DataModuleBridge>().recentSearchQueryDao()

    @Factory
    fun newsResourceDao(scope: Scope): NewsResourceDao =
        scope.dagger<DataModuleBridge>().newsResourceDao()

    @Factory
    fun newsResourceFtsDao(scope: Scope): NewsResourceFtsDao =
        scope.dagger<DataModuleBridge>().newsResourceFtsDao()

    @Factory
    fun topicDao(scope: Scope): TopicDao =
        scope.dagger<DataModuleBridge>().topicDao()

    @Factory
    fun topicFtsDao(scope: Scope): TopicFtsDao =
        scope.dagger<DataModuleBridge>().topicFtsDao()

    @Factory
    fun niaPreferencesDataSource(scope: Scope): NiaPreferencesDataSource =
        scope.dagger<DataModuleBridge>().niaPreferencesDataSource()

    @Factory
    fun network(scope: Scope): NiaNetworkDataSource =
        scope.dagger<DataModuleBridge>().network()

    @Factory
    fun notifier(scope: Scope): Notifier =
        scope.dagger<DataModuleBridge>().notifier()

//    @Factory
//    fun analyticsHelper(scope: Scope): AnalyticsHelper =
//        scope.dagger<DataModuleBridge>().analyticsHelper()
}