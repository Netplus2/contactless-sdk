@file:Suppress("DEPRECATION")

package com.netpluspay.contactless.sdk.card

import android.app.Activity
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.netpluspay.contactless.sdk.databinding.DialogSelectCardschemeBinding
import com.netpluspay.contactless.sdk.taponphone.visa.NfcPaymentType
import com.netpluspay.contactless.sdk.utils.Event

data class ICCCardHelper(
    var cardReadResult: CardReadResult? = null,
    var cardScheme: String? = null,
    var error: Throwable? = null
)

fun showCardDialog(
    context: Activity
): LiveData<Event<NfcPaymentType?>> {
    val liveData: MutableLiveData<Event<NfcPaymentType?>> = MutableLiveData()
    showSelectCardDialog(context, liveData)
    return liveData
}

fun showSelectCardDialog(context: Activity, liveData: MutableLiveData<Event<NfcPaymentType?>>) {
    val binding = DialogSelectCardschemeBinding.inflate(LayoutInflater.from(context))
    val selectCardDialog = AlertDialog.Builder(context).apply {
        setCancelable(false)
        setView(binding.root)
    }.create()
    var nfcPaymentType: NfcPaymentType? = null
    binding.mastercardIcon.setOnClickListener {
        binding.mastercardSelector.isVisible = true
        nfcPaymentType = NfcPaymentType.MASTERCARD
        binding.visaSelector.isVisible = false
        binding.proceedButton.isEnabled = true
    }
    binding.visaIcon.setOnClickListener {
        nfcPaymentType = NfcPaymentType.VISA
        binding.mastercardSelector.isVisible = false
        binding.visaSelector.isVisible = true
        binding.proceedButton.isEnabled = true
    }
    binding.proceedButton.setOnClickListener {
        selectCardDialog.cancel()
        liveData.postValue(Event(nfcPaymentType))
    }
    selectCardDialog.show()
    // liveData.postValue(Event(NfcPaymentType.VISA))
}

const val NFC_A_TAG = "TAG: Tech [android.nfc.tech.IsoDep, android.nfc.tech.NfcA]"
const val NFC_B_TAG = "TAG: Tech [android.nfc.tech.IsoDep, android.nfc.tech.NfcB]"
