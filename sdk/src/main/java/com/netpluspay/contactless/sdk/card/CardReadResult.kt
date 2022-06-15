package com.netpluspay.contactless.sdk.card

data class CardReadResult(
    val track2Data: String,
    val applicationPanSequenceNumber: String,
    val pan: String,
    val iccString: String,
){
    var pinBlock: String? = null
}