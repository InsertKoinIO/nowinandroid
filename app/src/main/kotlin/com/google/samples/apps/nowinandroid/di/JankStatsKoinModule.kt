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

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.metrics.performance.JankStats
import androidx.metrics.performance.JankStats.OnFrameListener
import org.koin.android.annotation.ActivityScope
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module

@Module
@Configuration
class JankStatsKoinModule {

    @ActivityScope
    fun jankStats(activity : ComponentActivity) : JankStats = JankStats.createAndTrack(activity.window, providesOnFrameListener())
}

fun providesOnFrameListener(): OnFrameListener = OnFrameListener { frameData ->
    // Make sure to only log janky frames.
    if (frameData.isJank) {
        // We're currently logging this but would better report it to a backend.
        Log.v("NiA Jank", frameData.toString())
//        KotzillaSDK.log("NiA Jank - $frameData")
    }
}