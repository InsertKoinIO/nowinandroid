/*
 * Copyright 2022 The Android Open Source Project
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

package com.google.samples.apps.nowinandroid

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.google.samples.apps.nowinandroid.core.data.logBenchmark
import com.google.samples.apps.nowinandroid.core.data.startLog
import com.google.samples.apps.nowinandroid.di.appModule
import com.google.samples.apps.nowinandroid.sync.initializers.Sync
import com.google.samples.apps.nowinandroid.util.ProfileVerifierLogger
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.androix.startup.KoinStartup
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.KoinAppDeclaration

/**
 * [Application] class for NiA
 */
//@HiltAndroidApp
@OptIn(KoinExperimentalAPI::class)
class NiaApplication : Application(), ImageLoaderFactory, KoinStartup {

    override fun onKoinStartup(): KoinAppDeclaration = {
        androidContext(this@NiaApplication)
        modules(appModule)
        workManagerFactory()
    }

    val imageLoader: ImageLoader by inject()
    val profileVerifierLogger: ProfileVerifierLogger by inject()

    companion object {
        var startTime: Long = 0
    }

    override fun onCreate() {
        super.onCreate()
        startLog()
        startTime = System.currentTimeMillis()

        // Initialize Sync; the system responsible for keeping data in the app up to date.
        Sync.initialize(context = this)
        profileVerifierLogger()
    }

    override fun newImageLoader(): ImageLoader = imageLoader
}
