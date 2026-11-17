package com.thelightphone.gospellibrary

import android.util.Log
import com.thelightphone.sdk.EntryPoint
import com.thelightphone.sdk.LightEntryPoint
import com.thelightphone.sdk.shared.LightServerData
import kotlinx.coroutines.flow.StateFlow

@EntryPoint
object ToolEntryPoint : LightEntryPoint {
    override suspend fun onToolCreate(
        serverData: StateFlow<LightServerData?>,
    ) {
        Log.d("GospelLibrary", "Gospel Library tool initialized")
    }

    override suspend fun onPushNotification(
        data: ByteArray,
    ) {
        // Gospel library is an offline reader tool
    }
}
