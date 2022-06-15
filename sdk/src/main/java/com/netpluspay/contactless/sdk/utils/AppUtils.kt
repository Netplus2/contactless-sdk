package com.netpluspay.contactless.sdk.utils

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.netpluspay.contactless.sdk.R

fun TextView.highlightTexts(vararg words: String?) {
    val spannable: Spannable = SpannableString(text.toString())
    words.forEach { word ->
        word?.let {
            if (text.toString().contains(it)) {
                spannable.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(context, R.color.highlightColor)),
                    text.toString().indexOf(it),
                    text.toString().indexOf(it).plus(it.length),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }
    this.text = spannable
}

fun AlertDialog.dismissIfShowing() {
    if (this.isShowing)
        this.dismiss()
}

fun xorHex(a: String, b: String): String {
    val chars = CharArray(a.length)
    for (i in chars.indices) {
        chars[i] = toHex(fromHex(a[i]) xor fromHex(b[i]))
    }
    return String(chars)
}

private fun fromHex(c: Char): Int {
    if (c in '0'..'9') {
        return c - '0'
    }
    if (c in 'A'..'F') {
        return c - 'A' + 10
    }
    if (c in 'a'..'f') {
        return c - 'a' + 10
    }
    throw IllegalArgumentException()
}

private fun toHex(nybble: Int): Char {
    require(!(nybble < 0 || nybble > 15))
    return "0123456789ABCDEF"[nybble]
}
