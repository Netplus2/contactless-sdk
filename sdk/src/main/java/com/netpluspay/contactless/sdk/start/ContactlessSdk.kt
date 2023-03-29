package com.netpluspay.contactless.sdk.start

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.netpluspay.contactless.sdk.ui.NfcActivity

object ContactlessSdk {
    fun readContactlessCard(
        context: Context,
        resultLauncher: ActivityResultLauncher<Intent>,
        pinKey: String,
        amount: Double,
        cashBackAmount: Double = 0.0
    ) {
        NfcActivity.startActivity(context, resultLauncher, pinKey, amount, cashBackAmount)
    }

    @Deprecated(
        message = "use readContactlessCard with result launcher",
        replaceWith = ReplaceWith(
            "ContactlessSdk.readContactlessCard(context, resultLauncher, pinKey, amount, cashBackAmount)",
            "com.netpluspay.contactless.sdk.start.ContactlessSdk"
        )
    )
    fun readContactlessCard(
        context: Activity,
        requestCode: Int,
        pinKey: String,
        amount: Double,
        cashBackAmount: Double = 0.0
    ) {
        NfcActivity.startActivity(context, requestCode, pinKey, amount, cashBackAmount)
    }
}
