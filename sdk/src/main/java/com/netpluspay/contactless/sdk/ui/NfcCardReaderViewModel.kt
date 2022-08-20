package com.netpluspay.contactless.sdk.ui

import android.os.Build
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mastercard.terminalsdk.TransactionInterface
import com.mastercard.terminalsdk.listeners.PaymentDataProvider
import com.mastercard.terminalsdk.utility.ByteArrayWrapper
import com.netpluspay.contactless.sdk.card.CardReadResult
import com.netpluspay.contactless.sdk.card.ICCCardHelper
import com.netpluspay.contactless.sdk.taponphone.mastercard.implementations.OutcomeObserver
import com.netpluspay.contactless.sdk.taponphone.mastercard.listener.TransactionListener
import com.netpluspay.contactless.sdk.taponphone.tlv.BerTag
import com.netpluspay.contactless.sdk.taponphone.tlv.BerTlvParser
import com.netpluspay.contactless.sdk.taponphone.tlv.HexUtil
import com.netpluspay.contactless.sdk.taponphone.visa.*
import com.netpluspay.contactless.sdk.taponphone.visa.LiveNfcTransReceiver
import com.netpluspay.contactless.sdk.taponphone.visa.PPSEv21.PPSEManager
import com.netpluspay.contactless.sdk.utils.Event
import com.visa.app.ttpkernel.ContactlessConfiguration
import com.visa.app.ttpkernel.ContactlessKernel
import com.visa.app.ttpkernel.TtpOutcome
import com.visa.vac.tc.emvconverter.Utils
import org.apache.commons.lang.StringUtils
import java.io.IOException

class NfcCardReaderViewModel : ViewModel() {
    private val icc = StringBuilder()
    private var outcomeObserver: OutcomeObserver? = null
    private var transactionsApi: TransactionInterface? = null

    fun setOutcomeObserver(outcomeObserver: OutcomeObserver) {
        this.outcomeObserver = outcomeObserver
    }

    fun setTransactionsApi(transactionInterface: TransactionInterface) {
        this.transactionsApi = transactionInterface
    }

    private val _enableNfcForegroundDispatcher: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData()
    }

    val enableNfcForegroundDispatcher: LiveData<Event<Boolean>>
        get() = _enableNfcForegroundDispatcher

    private val _iccCardHelperLiveData: MutableLiveData<Event<ICCCardHelper>> by lazy {
        MutableLiveData()
    }

    val iccCardHelperLiveData: LiveData<Event<ICCCardHelper>>
        get() = _iccCardHelperLiveData

    var iccCardHelper: ICCCardHelper = ICCCardHelper()

    private val _showPinPadDialog: MutableLiveData<Event<String?>> by lazy {
        MutableLiveData()
    }

    val showPinPadDialog: LiveData<Event<String?>>
        get() = _showPinPadDialog

    private val _showWaitingDialog: MutableLiveData<Event<NfcPaymentType?>> by lazy {
        MutableLiveData()
    }

    val showWaitingDialog: LiveData<Event<NfcPaymentType?>>
        get() = _showWaitingDialog

    private lateinit var amountInBytes: ByteArrayWrapper
    private lateinit var cashBackAmountInBytes: ByteArrayWrapper

    private val _message: MutableLiveData<Event<String?>> by lazy {
        MutableLiveData(null)
    }


    override fun onCleared() {
        super.onCleared()
        icc.clear()
        transactionsApi = null
        outcomeObserver = null
        stopNfcReader()
    }

    fun initiateNfcPayment(amount: Long, cashBackAmount: Long, nfcPaymentType: NfcPaymentType) {
        _showWaitingDialog.postValue(Event(nfcPaymentType))
        amountInBytes = ByteArrayWrapper(StringUtils.leftPad(amount.toString(), 12, '0'))
        cashBackAmountInBytes =
            ByteArrayWrapper(StringUtils.leftPad(cashBackAmount.toString(), 12, '0'))

        when (nfcPaymentType) {
            NfcPaymentType.VISA -> _enableNfcForegroundDispatcher.postValue(Event(true))
            NfcPaymentType.MASTERCARD -> doMasterCardTransaction()
        }
    }

    fun doVisaTransaction(
        nfcTransceiver: LiveNfcTransReceiver,
        contactlessKernel: ContactlessKernel,
    ) {
        iccCardHelper = ICCCardHelper()
        icc.clear()
        val contactlessConfiguration = ContactlessConfiguration.getInstance()

        contactlessConfiguration.terminalData["9F02"] = amountInBytes.bytes

        contactlessConfiguration.terminalData["9F03"] = cashBackAmountInBytes.bytes

        // Select PPSE
        var selectedAid: ByteArray? = null
        val ppseManager =
            PPSEManager()
        try {
            // Set suppported AID
            ppseManager.setSupportedApps(supportedAids)

            // Perform Select PPSE
            selectedAid = ppseManager.selectPPSE(nfcTransceiver)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        var outcome: TtpOutcome = TtpOutcome.ABORTED
        var outComeResponse = ""
        while (selectedAid != null) {
            // Get the ContactlessConfiguration instance
            // Specify the terminal settings for this transaction
            val myData = contactlessConfiguration.terminalData
            myData["4F"] = selectedAid // set the selected aid
            myData["9F4E"] = byteArrayOf(
                0x00.toByte(),
                0x11.toByte(),
                0x22.toByte(),
                0x33.toByte(),
                0x44.toByte(),
                0x55.toByte(),
                0x66.toByte(),
                0x77.toByte(),
                0x88.toByte(),
                0x99.toByte(),
                0xAA.toByte(),
                0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte()
            )

            // Call TTP Kernel performTransaction
            val contactlessResult =
                contactlessKernel.performTransaction(nfcTransceiver, contactlessConfiguration)

            // Check the transaction outcome
            outcome = contactlessResult.finalOutcome
            outComeResponse = when (outcome) {
                TtpOutcome.COMPLETED -> {
                    "Online Approval Requested"
                }
                TtpOutcome.DECLINED -> {
                    "Transaction Declined"
                }
                TtpOutcome.ABORTED -> {
                    "Transaction Terminated"
                }
                TtpOutcome.TRYNEXT -> {
                    "PPSE:Try Next Application"
                }
                TtpOutcome.SELECTAGAIN -> {
                    "GPO Returned 6986. Application Try Again."
                }
                else -> {
                    ""
                }
            }

            // Display the TTP Kernel version
            contactlessKernel.kernelData

            // Display the TTP Kernel data
            val version = contactlessKernel.kernelData
            var key: String
            var value: String
            for ((key1, value1) in version) {
                if (value1 != null) {
                    key = key1 as String
                    value = Utils.getHexString(value1) as String
                    if (key in REQUIRED_TAGS)
                        icc.append(value)
                    //mainLog.text = mainLog.text.toString() + "\n" + key + ":" + value
                }
            }

            // Display the transaction results
            val cardData = contactlessResult.data
            for ((key1, value1) in cardData) {
                key = key1 as String
                if (value1 != null) {
                    value = Utils.getHexString(value1) as String
                    if (key in REQUIRED_TAGS)
                        icc.append(value)
                }
            }
            val internalData = contactlessResult.internalData
            for ((key1, value1) in internalData) {
                if (value1 != null) {
                    key = key1 as String
                    value = Utils.getHexString(value1) as String
                    if (key in REQUIRED_TAGS)
                        icc.append(value)
                }
            }
            selectedAid = if (outcome == TtpOutcome.TRYNEXT) {
                ppseManager.nextCandidate()
            } else {
                null
            }
        }
        _enableNfcForegroundDispatcher.postValue(Event(false))
        if (outcome == TtpOutcome.COMPLETED) {
            iccCardHelper.apply {
                cardScheme = NfcPaymentType.VISA.name
            }
            createVisaCardData(icc)
        } else {
            _showWaitingDialog.postValue(Event(null))
            _iccCardHelperLiveData.postValue(Event(ICCCardHelper(error = Throwable("Error occurred while reading card: $outComeResponse"))))
        }
    }

    private fun createVisaCardData(icc: StringBuilder) {
        icc.append("9F3303E068C8")
        icc.append("9F350122")
        icc.append("9F0902008C")
        icc.append("9F3403420300")
        val buildId =
            HexDump.toHexString(Build.ID.replace("[^a-zA-Z0-9]".toRegex(), "").toByteArray())
        val buildIdHex = "9F1E${HexDump.toHexString("${buildId.length / 2}".toByte())}$buildId"
        icc.append(buildIdHex)
        val iccData = icc.toString()
        val tlvs = BerTlvParser()
            .parse(HexUtil.parseHex(iccData))
        val panSequence = tlvs.find(
            BerTag(
                "5F34"
            )
        ).hexValue
        val track2 = tlvs.find(
            BerTag(
                "57"
            )
        ).hexValue
        val pan = track2.split("D").first()
        val cardReadResult = CardReadResult(
            track2,
            StringUtils.leftPad(panSequence, 3, "0"),
            pan,
            iccData
        )
        iccCardHelper.cardReadResult = cardReadResult
        _showWaitingDialog.postValue(Event(null))
        _showPinPadDialog.postValue(Event(pan))
    }

    fun setPinBlock(encryptedPinBloc: String?) {
        iccCardHelper.cardReadResult?.apply {
            pinBlock = encryptedPinBloc
        }
        finishNfcReading()
    }

    private fun doMasterCardTransaction() {
        outcomeObserver?.resetObserver(object :
            TransactionListener {
            override fun onTransactionSuccessful() {

            }

            override fun onOnlineReferral(cardData: CardReadResult, pan: String) {
                iccCardHelper.apply {
                    this.cardReadResult = cardData
                    cardScheme = NfcPaymentType.MASTERCARD.name
                }
                Looper.prepare()
                _showWaitingDialog.postValue(Event(null))
                _showPinPadDialog.postValue(Event(pan))
            }

            override fun onTransactionDeclined() {
                Looper.prepare()
                _showWaitingDialog.postValue(Event(null))
                _iccCardHelperLiveData.postValue(Event(ICCCardHelper(error = Throwable("Error occurred while reading card: Transaction declined"))))
            }

            override fun onApplicationEnded() {
                Looper.prepare()
                _showWaitingDialog.postValue(Event(null))
                _iccCardHelperLiveData.postValue(Event(ICCCardHelper(error = Throwable("Error occurred while reading card: application ended"))))
            }

            override fun onTransactionCancelled() {
                Looper.prepare()
                _showWaitingDialog.postValue(Event(null))
                _iccCardHelperLiveData.postValue(Event(ICCCardHelper(error = Throwable("Error occurred while reading card: transaction cancelled"))))
            }

            override fun logToScreen(s: String?) {

            }

            override fun onTransactionError(message: String?) {
                _message.value = Event(message)
                _iccCardHelperLiveData.postValue(Event(ICCCardHelper(error = Throwable("Error occurred while reading card: $message"))))
            }
        })

        transactionsApi?.initiatePayment(object :
            PaymentDataProvider {
            override fun getPaymentDataMap(): HashMap<Int, ByteArrayWrapper> {
                val map = HashMap<Int, ByteArrayWrapper>()
                map.apply {
                    put(0x9F02, amountInBytes)
                    put(
                        0x9F03,
                        cashBackAmountInBytes
                    )
                    put(0x5F2A, ByteArrayWrapper("0566"))
                    put(0x9F1A, ByteArrayWrapper("0566"))
                }
                return map
            }

            override fun setPaymentDataEntry(p0: Int?, p1: ByteArrayWrapper?) {

            }

        })
    }

    fun setIccCardHelperLiveData(iccCardHelper: ICCCardHelper) {
        _iccCardHelperLiveData.postValue(Event(iccCardHelper))
    }

    fun stopNfcReader() {
        try {
            transactionsApi?.abortTransaction()
        } catch (e: Exception) {

        }
    }

    private fun finishNfcReading() {
        _enableNfcForegroundDispatcher.postValue(Event(false))
        _iccCardHelperLiveData.postValue(Event(iccCardHelper))
    }


}