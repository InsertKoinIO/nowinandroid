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

package com.google.samples.apps.nowinandroid.core.database.di

import com.google.samples.apps.nowinandroid.core.database.NiaDatabase
import org.koin.core.scope.Scope
import org.koin.dsl.module

//@Module
//@InstallIn(SingletonComponent::class)
//internal object DaosModule : KoinComponent {
//
////    // Bridged to Koin
////    @Provides
////    fun providesTopicsDao(): TopicDao = getKoin().get()
////
////    @Provides
////    fun providesNewsResourceDao(): NewsResourceDao = getKoin().get()
//
//}

val daosModule = module {
    includes(databaseModule)

    single { niaDatabase().topicDao() }
    single { niaDatabase().newsResourceDao() }
    single { niaDatabase().topicFtsDao() }
    single { niaDatabase().newsResourceFtsDao() }
    single { niaDatabase().recentSearchQueryDao() }
}

private fun Scope.niaDatabase() = get<NiaDatabase>()
