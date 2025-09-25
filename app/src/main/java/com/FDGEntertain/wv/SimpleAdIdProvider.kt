package com.FDGEntertain.wv

import android.content.Context
import androidx.core.content.edit
import com.FDGEntertain.repositoryWebView.AdIdProvider
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import java.util.UUID

class SimpleAdIdProvider(
    private val context: Context
) : AdIdProvider {

    private val prefs by lazy {
        context.getSharedPreferences("${context.packageName}.adid", Context.MODE_PRIVATE)
    }

    override fun getAdId(): String {
        prefs.getString(KEY, null)?.let { return it }

        val fresh = try {
            AdvertisingIdClient.getAdvertisingIdInfo(context).id
        } catch (_: Exception) {
            null
        }
        val value = when {
            fresh.isNullOrBlank() || fresh == ZERO -> generateFallback()
            else -> fresh
        }

        // 4) сохранить и вернуть
        prefs.edit { putString(KEY, value) }
        return value
    }

    private fun generateFallback(): String =
        "${UUID.randomUUID()}${UUID.randomUUID()}"

    private companion object {
        const val KEY = "adId"
        const val ZERO = "00000000-0000-0000-0000-000000000000"
    }
}