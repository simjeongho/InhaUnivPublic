package kr.ac.inha.android.APP.library

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.nfc.NfcAdapter
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity

class NfcUtil(private val ctx: Context) {
    private val nfcAdapter: NfcAdapter = NfcAdapter.getDefaultAdapter(ctx)
    private val isNfcSupported: Boolean = ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)
    private val isNfcOn: Boolean

    init {
        isNfcOn = isNfcSupported && nfcAdapter.isEnabled
    }

    fun checkNfcFeature(): Boolean {
        return when {
            !isNfcSupported -> {
                showNfcNotSuppportedAlert()
                false
            }
            !isNfcOn -> {
                showNfcOffAlert()
                openNfcSetting()
                false
            }
            else -> true
        }
    }

    private fun openNfcSetting(): Unit {
        when (Build.VERSION.SDK_INT) {
            in Build.VERSION_CODES.O..Int.MAX_VALUE -> goToNfcOnOreo()
            else -> goToNfcBelowOreo()
        }
    }

    private fun goToNfcOnOreo(): Unit {
        var intent: Intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(ctx, intent, null)
    }

    private fun goToNfcBelowOreo(): Unit {
        var intent: Intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
        intent.data = Uri.parse("package:" + ctx.packageName)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(ctx, intent, null)
    }

    private fun showNfcNotSuppportedAlert(): Unit {
        Toast.makeText(ctx, "NFC를 지원하지 않는 기기입니다", Toast.LENGTH_LONG).show()
    }

    private fun showNfcOffAlert(): Unit {
        Toast.makeText(ctx, "NFC를 활성화 한후 다시 시도하여 주시기 바랍니다", Toast.LENGTH_LONG).show()
    }
}