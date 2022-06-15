package com.netpluspay.contactless.sdk.taponphone.mastercard.listener;


import com.netpluspay.contactless.sdk.card.CardReadResult;

public interface TransactionListener {

    /**
     * Invoked for offline apprroval
     */
    void onTransactionSuccessful();

    /**
     * Invoked when requires online approval
     */
    void onOnlineReferral(CardReadResult cardReadResult, String pan);

    /**
     * Invoked when transaction is declined
     */
    void onTransactionDeclined();

    /**
     * Invoked when SDK ends due to error
     */
    void onApplicationEnded();

    /**
     * Invoked when transaction is cancelled
     */
    void onTransactionCancelled();

    void logToScreen(String s);

    void onTransactionError(String message);
}
