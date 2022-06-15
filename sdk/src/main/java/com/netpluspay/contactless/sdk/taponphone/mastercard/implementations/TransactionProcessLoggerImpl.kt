package com.netpluspay.contactless.sdk.taponphone.mastercard.implementations

import com.mastercard.terminalsdk.listeners.TransactionProcessLogger

class TransactionProcessLoggerImpl(val builder: StringBuilder): TransactionProcessLogger {
    override fun logCryptoOperations(p0: String?) {
        builder.append("CRYPTO OPERATIONS: ").append(p0).append("\n")
    }

    override fun logInternalOperation(p0: String?) {
        builder.append("INTERNAL OPERATION: ").append(p0).append("\n")
    }

    override fun logVerbose(p0: String?) {
        builder.append("VERBOSE: ").append(p0).append("\n")
    }

    override fun logDebug(p0: String?) {
        builder.append("DEBUG: ").append(p0).append("\n")
    }

    override fun logInfo(p0: String?) {
        builder.append("INFO: ").append(p0).append("\n")
    }

    override fun logApduExchange(p0: String?) {
        builder.append("APDU EXCHANGE: ").append(p0).append("\n")
    }

    override fun logTlvParsing(p0: String?) {
        builder.append("TLV PARSING: ").append(p0).append("\n")
    }

    override fun logStage(p0: String?) {
        builder.append("STAGE: ").append(p0).append("\n")
    }

    override fun logWarning(p0: String?) {
        builder.append("WARNING: ").append(p0).append("\n")
    }

    override fun logError(p0: String?) {
        builder.append("ERROR: ").append(p0).append("\n")
    }
}