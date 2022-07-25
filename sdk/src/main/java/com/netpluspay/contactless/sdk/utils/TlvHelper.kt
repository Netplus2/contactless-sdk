package com.netpluspay.contactless.sdk.utils

import com.netpluspay.contactless.sdk.taponphone.tlv.BerTag
import com.netpluspay.contactless.sdk.taponphone.tlv.BerTlvParser
import com.netpluspay.contactless.sdk.taponphone.tlv.HexUtil

class TlvHelper(private val iccData: String) {
    fun getWithTag(tag: String): String? {
        return try {
            val tlvList = BerTlvParser().parse(HexUtil.parseHex(iccData))
            tlvList.find(BerTag(tag)).hexValue
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}