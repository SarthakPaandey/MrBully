package com.brutal.accountability.service

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import com.brutal.accountability.data.LocalRepository
import kotlinx.coroutines.flow.first

class TriggerEngine(
    private val context: Context,
    private val repository: LocalRepository
) {

    fun hasHeadphones(): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return devices.any {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        }
    }

    suspend fun strictModeEnabled(): Boolean = repository.strictModeFlow.first()
}
