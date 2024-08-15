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
package com.example.healthconnect.codelab.data

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.mutableStateOf
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.changes.Change
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import java.io.IOException
import java.time.Instant
import java.time.ZonedDateTime
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// The minimum android level that can use Health Connect
const val MIN_SUPPORTED_SDK = Build.VERSION_CODES.O_MR1

/**
 * Demonstrates reading and writing from Health Connect.
 */
class HealthConnectManager(private val context: Context) {
  // 可先取得 healthConnectClient，是 Health Connect API 的進入點
  private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

  var availability = mutableStateOf(HealthConnectAvailability.NOT_SUPPORTED)
    private set

  init {
    checkAvailability()
  }

  fun checkAvailability() {
    availability.value = when {
      HealthConnectClient.isProviderAvailable(context) -> HealthConnectAvailability.INSTALLED
      isSupported() -> HealthConnectAvailability.NOT_INSTALLED
      else -> HealthConnectAvailability.NOT_SUPPORTED
    }
  }

  /**
   * Determines whether all the specified permissions are already granted. It is recommended to
   * call [PermissionController.getGrantedPermissions] first in the permissions flow, as if the
   * permissions are already granted then there is no need to request permissions via
   * [PermissionController.createRequestPermissionResultContract].
   */
  suspend fun hasAllPermissions(permissions: Set<String>): Boolean {
    return healthConnectClient.permissionController.getGrantedPermissions().containsAll(permissions)
  }

  fun requestPermissionsActivityContract(): ActivityResultContract<Set<String>, Set<String>> {
    return PermissionController.createRequestPermissionResultContract()
  }

  /**
   * 插入體重資訊的範例
   */
  suspend fun writeWeightInput(weightInput: Double) {
    // 第一個：取得記錄時間
    val time = ZonedDateTime.now().withNano(0)
    // 第二個：製造記錄
    val weightRecord = WeightRecord(
      weight = Mass.kilograms(weightInput),
      time = time.toInstant(),
      zoneOffset = time.offset
    )
    // 第三個：把記錄變成 List
    val records = listOf(weightRecord)
    // 第四個：插入記錄
    try {
      healthConnectClient.insertRecords(records)
      Toast.makeText(context, "TODO: write weight input", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
      Toast.makeText(context, e.message.toString(), Toast.LENGTH_SHORT).show()
    }
  }

  /**
   * TODO: Reads in existing [WeightRecord]s.
   */
  suspend fun readWeightInputs(start: Instant, end: Instant): List<WeightRecord> {
    // Toast.makeText(context, "TODO: read weight inputs", Toast.LENGTH_SHORT).show()
    return emptyList()
  }

  /**
   * TODO: Returns the weekly average of [WeightRecord]s.
   */
  suspend fun computeWeeklyAverage(start: Instant, end: Instant): Mass? {
    // Toast.makeText(context, "TODO: get average weight", Toast.LENGTH_SHORT).show()
    return null
  }

  /**
   * TODO: Obtains a list of [ExerciseSessionRecord]s in a specified time frame.
   */
  suspend fun readExerciseSessions(start: Instant, end: Instant): List<ExerciseSessionRecord> {
    // Toast.makeText(context, "TODO: read exercise sessions", Toast.LENGTH_SHORT).show()
    return emptyList()
  }

  /**
   * 建立一段運動記錄
   */
  suspend fun writeExerciseSession(start: ZonedDateTime, end: ZonedDateTime) {
    // 同時有什麼記錄，都丟在同個 List 裡
    val exerciseRecord = listOf(
      ExerciseSessionRecord(
        startTime = start.toInstant(),
        startZoneOffset = start.offset,
        endTime = end.toInstant(),
        endZoneOffset = end.offset,
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        title = "My Run #${Random.nextInt(0, 60)}"
      ),
      StepsRecord(
        startTime = start.toInstant(),
        startZoneOffset = start.offset,
        endTime = end.toInstant(),
        endZoneOffset = end.offset,
        count = (1000 + 1000 * Random.nextInt(3)).toLong()
      ),
      TotalCaloriesBurnedRecord(
        startTime = start.toInstant(),
        startZoneOffset = start.offset,
        endTime = end.toInstant(),
        endZoneOffset = end.offset,
        energy = Energy.calories((140 + Random.nextInt(20)) * 0.01)
      )
    ) + buildHeartRateSeries(start, end)
    healthConnectClient.insertRecords(exerciseRecord)
  }

  /**
   * 建立一系列的心跳記錄
   */
  private fun buildHeartRateSeries(
    sessionStartTime: ZonedDateTime,
    sessionEndTime: ZonedDateTime,
  ): HeartRateRecord {
    val samples = mutableListOf<HeartRateRecord.Sample>()
    var time = sessionStartTime
    while (time.isBefore(sessionEndTime)){
      // 第二步：一系列的心跳記錄
      samples.add(
        // 第一步：一個心跳記錄
        HeartRateRecord.Sample(
          time = time.toInstant(),
          beatsPerMinute = (80 + Random.nextInt(80)).toLong()
        )
      )
      time = time.plusSeconds(30)
    }
    // 第三步：建立一系列的心跳記錄
    // 開始時間與結束時間應該要是一樣的
    val record = HeartRateRecord(
      startTime = sessionStartTime.toInstant(),
      startZoneOffset = sessionStartTime.offset,
      endTime = sessionEndTime.toInstant(),
      endZoneOffset = sessionEndTime.offset,
      samples = samples
    )
    return record
  }

  /**
   * TODO: Reads aggregated data and raw data for selected data types, for a given [ExerciseSessionRecord].
   */
  suspend fun readAssociatedSessionData(
      uid: String,
  ): ExerciseSessionData {
    TODO()
  }

  /**
   * TODO: Obtains a changes token for the specified record types.
   */
  suspend fun getChangesToken(): String {
    Toast.makeText(context, "TODO: get changes token", Toast.LENGTH_SHORT).show()
    return String()
  }

  /**
   * TODO: Retrieve changes from a changes token.
   */
  suspend fun getChanges(token: String): Flow<ChangesMessage> = flow {
    Toast.makeText(context, "TODO: get new changes", Toast.LENGTH_SHORT).show()
  }

  /**
   * Convenience function to reuse code for reading data.
   */
  private suspend inline fun <reified T : Record> readData(
      timeRangeFilter: TimeRangeFilter,
      dataOriginFilter: Set<DataOrigin> = setOf(),
  ): List<T> {
    val request = ReadRecordsRequest(
      recordType = T::class,
      dataOriginFilter = dataOriginFilter,
      timeRangeFilter = timeRangeFilter
    )
    return healthConnectClient.readRecords(request).records
  }

  private fun isSupported() = Build.VERSION.SDK_INT >= MIN_SUPPORTED_SDK

  // Represents the two types of messages that can be sent in a Changes flow.
  sealed class ChangesMessage {
    data class NoMoreChanges(val nextChangesToken: String) : ChangesMessage()
    data class ChangeList(val changes: List<Change>) : ChangesMessage()
  }
}

/**
 * Health Connect requires that the underlying Health Connect APK is installed on the device.
 * [HealthConnectAvailability] represents whether this APK is indeed installed, whether it is not
 * installed but supported on the device, or whether the device is not supported (based on Android
 * version).
 */
enum class HealthConnectAvailability {
  INSTALLED,
  NOT_INSTALLED,
  NOT_SUPPORTED
}
