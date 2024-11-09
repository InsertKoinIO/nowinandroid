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

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import kotlin.time.measureTimedValue

/**
 * To activate benchmarks monitoring
 */

inline fun <reified T> Context.measureTime(tag : String, code : () -> T) : T{
    val result = measureTimedValue(code)
    val timeInMs = result.duration.inWholeMicroseconds / 1000.0
    logBenchmark(tag,timeInMs)
    return result.value
}

inline fun <reified T> Context.measureTimeLazy(tag : String, code : () -> Lazy<T>) : Lazy<T>{
    val result = measureTimedValue(code)
    val timeInMs = result.duration.inWholeMicroseconds / 1000.0
    logBenchmark(tag,timeInMs)
    return result.value
}

fun Context.startLog(){
    Log.i("[Benchmark]","start ...")
    writeToFile("")
}

fun Context.logBenchmark(tag : String, time : Double){
    Log.i("[Benchmark]","$tag : $time ms")
    writeToFile("$tag=$time")
}

private const val fileName = "benchmark_log.txt"
private var logFile: File? = null
private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

private fun Context.writeToFile(content: String) {
    val contextFilesDir = runCatching { applicationContext?.filesDir }.getOrNull()
    if (logFile == null && contextFilesDir != null) {
        logFile = File(contextFilesDir, fileName)
    }
    logFile?.let { file ->
        ioScope.launch {
            try {
                FileOutputStream(file, true).use { it.write((content + "\n").toByteArray()) }
            } catch (e: Exception) {
                Log.e("[Benchmark]", "Error writing to log file: ${e.message}", e)
            }
        }
    }
}