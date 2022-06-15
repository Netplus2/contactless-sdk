package com.netpluspay.contactless.sdk.taponphone.mastercard.implementations.nfc;

import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;

class ReaderCallbackImpl implements NfcAdapter.ReaderCallback {

    private TagEventListener mTagEventListener;

    protected ReaderCallbackImpl(final TagEventListener tagEventListener) {
        this.mTagEventListener = tagEventListener;
    }

    @Override
    public void onTagDiscovered(final Tag tag) {
        mTagEventListener.setIsoDep(IsoDep.get(tag));
    }

}
