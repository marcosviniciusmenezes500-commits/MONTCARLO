package com.droidscope.app.data

import android.content.Context
import com.droidscope.app.readDeviceSnapshot
import com.droidscope.core.domain.DeviceSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun observeDeviceSnapshot(intervalMs: Long = 2000L): Flow<DeviceSnapshot> = flow {
        while (currentCoroutineContext().isActive) {
            emit(readDeviceSnapshot(context))
            delay(intervalMs)
        }
    }.flowOn(Dispatchers.IO)
}
