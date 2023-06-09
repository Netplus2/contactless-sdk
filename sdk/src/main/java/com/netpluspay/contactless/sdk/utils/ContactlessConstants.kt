package com.netpluspay.contactless.sdk.utils

internal object ContactlessConstants {
    const val INTENT_PIN_KEY = "intent_pin_key"
    const val INTENT_EXTRA_AMOUNT = "intent_extra_amount"
    const val INTENT_EXTRA_CASHBACK_AMOUNT = "intent_extra_cashback_amount"
}

object ContactlessReaderResult {
    const val RESULT_ERROR = 11
    const val RESULT_OK = 10
}