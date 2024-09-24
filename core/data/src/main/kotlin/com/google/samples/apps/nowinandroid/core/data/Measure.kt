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

package com.google.samples.apps.nowinandroid.core.data

import android.util.Log
import kotlin.time.measureTimedValue

inline fun <reified T> measurePerfs(code : () -> T) : T{
    val result = measureTimedValue(code)
    val timeInMs = result.duration.inWholeMicroseconds / 1000.0
    logMeasure("call: ${T::class.qualifiedName} -> $timeInMs ms")
    return result.value
}

fun logMeasure(msg : String){
    Log.i("[MEASURE]",msg)
}