package com.netpluspay.contactless.sdk.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.mastercard.terminalsdk.ConfigurationInterface
import com.mastercard.terminalsdk.TerminalSdk
import com.mastercard.terminalsdk.TransactionInterface
import com.netpluspay.contactless.sdk.R
import com.netpluspay.contactless.sdk.card.ICCCardHelper
import com.netpluspay.contactless.sdk.card.NFC_A_TAG
import com.netpluspay.contactless.sdk.card.NFC_B_TAG
import com.netpluspay.contactless.sdk.card.showCardDialog
import com.netpluspay.contactless.sdk.databinding.DialogContatclessReaderBinding
import com.netpluspay.contactless.sdk.taponphone.mastercard.implementations.*
import com.netpluspay.contactless.sdk.taponphone.mastercard.implementations.nfc.NFCManager.READER_FLAGS
import com.netpluspay.contactless.sdk.taponphone.mastercard.implementations.nfc.NfcProvider
import com.netpluspay.contactless.sdk.taponphone.visa.LiveNfcTransReceiver
import com.netpluspay.contactless.sdk.taponphone.visa.NfcPaymentType
import com.netpluspay.contactless.sdk.utils.dismissIfShowing
import com.netpluspay.contactless.sdk.utils.highlightTexts
import com.netpluspay.contactless.sdk.taponphone.mastercard.implementations.TransactionProcessLoggerImpl
import com.netpluspay.contactless.sdk.ui.dialog.PasswordDialog
import com.netpluspay.contactless.sdk.utils.ContactlessConstants.INTENT_EXTRA_AMOUNT
import com.netpluspay.contactless.sdk.utils.ContactlessConstants.INTENT_EXTRA_CASHBACK_AMOUNT
import com.netpluspay.contactless.sdk.utils.ContactlessConstants.INTENT_PIN_KEY
import com.netpluspay.contactless.sdk.utils.ContactlessReaderResult
import com.visa.app.ttpkernel.ContactlessConfiguration
import com.visa.app.ttpkernel.ContactlessKernel


class NfcActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {
    private lateinit var transactionsApi: TransactionInterface
    private lateinit var outcomeObserver: OutcomeObserver
    private lateinit var configuration: ConfigurationInterface
    private lateinit var builder: StringBuilder
    private lateinit var nfcProvider: NfcProvider
    private lateinit var viewModel: NfcCardReaderViewModel
    private val terminalSdk: TerminalSdk = TerminalSdk.getInstance()
    private var nfcAdapter: NfcAdapter? = null
    private val contactlessKernel: ContactlessKernel by lazy {
        ContactlessKernel.getInstance(applicationContext)
    }
    private lateinit var waitingDialog: AlertDialog
    private lateinit var dialogContactlessReaderBinding: DialogContatclessReaderBinding
    private var pinKey: String? = null
    private var amount: Double = 0.0
    private var cashBackAmount: Double = 0.0

    companion object {
        internal fun startActivity(
            context: Context,
            resultLauncher: ActivityResultLauncher<Intent>,
            pinKey: String,
            amount: Double,
            cashBackAmount: Double = 0.0,
        ) {
            val intent = Intent(context, NfcActivity::class.java)
            resultLauncher.launch(packIntent(intent, pinKey, amount, cashBackAmount))
        }

        @Deprecated(message = "use startActivity with result launcher")
        internal fun startActivity(
            context: Activity, requestCode: Int, pinKey: String,
            amount: Double,
            cashBackAmount: Double = 0.0,
        ) {
            val intent = Intent(context, NfcActivity::class.java)
            context.startActivityForResult(packIntent(intent, pinKey, amount, cashBackAmount),
                requestCode)
        }

        private fun packIntent(
            intent: Intent,
            pinKey: String,
            amount: Double,
            cashBackAmount: Double,
        ) = intent.apply {
            putExtra(INTENT_PIN_KEY, pinKey)
            putExtra(INTENT_EXTRA_AMOUNT, amount)
            putExtra(INTENT_EXTRA_CASHBACK_AMOUNT, cashBackAmount)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(NfcCardReaderViewModel::class.java)
        pinKey = intent.getStringExtra(INTENT_PIN_KEY)
        amount = intent.getDoubleExtra(INTENT_EXTRA_AMOUNT, 0.0)
        cashBackAmount = intent.getDoubleExtra(INTENT_EXTRA_CASHBACK_AMOUNT, 0.0)
        if (pinKey == null) {
            setResult(ContactlessReaderResult.RESULT_ERROR, intent.apply {
                putExtra("message", "pin key not supplied")
            })
            finish()
            return
        }
        if (amount == 0.0) {
            setResult(ContactlessReaderResult.RESULT_ERROR, intent.apply {
                putExtra("message", "amount not set")
            })
            finish()
            return
        }
        Log.e("tag", "got here")
        setupUi()
        initVisaLib()
        initMposLibrary()
        checkNfcAvailable()
        observeViewModels()
    }


    private fun observeViewModels() {
        viewModel.enableNfcForegroundDispatcher.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                if (it)
                    startNfcPayment()
                else
                    stopNfcPayment()
            }
        }
        viewModel.showPinPadDialog.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                showPinDialog(it)
            }
        }
        viewModel.showWaitingDialog.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                dialogContactlessReaderBinding.contactlessHeader.text =
                    getString(R.string.nfc_message, it.cardScheme)
                dialogContactlessReaderBinding.contactlessHeader.highlightTexts(
                    NfcPaymentType.MASTERCARD.cardScheme,
                    NfcPaymentType.VISA.cardScheme
                )
                dialogContactlessReaderBinding.cardScheme.setImageResource(it.icon)
                waitingDialog.show()
                return@observe
            }
            waitingDialog.dismissIfShowing()
        }
        viewModel.iccCardHelperLiveData.observe(this) { event ->
            event.getContentIfNotHandled()?.let { cardHelper ->
                setResult(ContactlessReaderResult.RESULT_OK, Intent().apply {
                    putExtra("data", Gson().toJson(cardHelper))
                })
                finish()
                return@observe
            }
        }
    }

    private fun setupUi() {
        dialogContactlessReaderBinding =
            DialogContatclessReaderBinding.inflate(layoutInflater).apply {

            }
        waitingDialog = AlertDialog.Builder(this)
            .apply {
                setView(dialogContactlessReaderBinding.root)
                setCancelable(false)
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.cancel()
                        viewModel.stopNfcReader()
                        finish()
                    }
            }.create()
    }

    private fun initVisaLib() {
        val contactlessConfiguration = ContactlessConfiguration.getInstance()
        val myData = contactlessConfiguration.terminalData
        myData["9F1A"] = byteArrayOf(0x05, 0x66) // set terminal country code
        myData["5F2A"] = byteArrayOf(0x05, 0x66) // set currency code
        myData["9F35"] = byteArrayOf(0x22) //Terminal Type
        myData["009C"] = byteArrayOf(0x00) //Transaction Type 00 - Purchase; 20 - Refund
        myData["9F09"] = byteArrayOf(0x00, 0x8C.toByte())
        myData["9F66"] =
            byteArrayOf(0xE6.toByte(), 0x00.toByte(), 0x40.toByte(), 0x00.toByte()) //TTQ E6004000
        myData["9F33"] =
            byteArrayOf(0xE0.toByte(), 0xF8.toByte(), 0xC8.toByte()) //Terminal Capabilities
        myData["9F40"] = byteArrayOf(
            0x60.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x50.toByte(), 0x01.toByte()
        ) //Additional Terminal Capabilities
        val mercahnt = "NetPOS Contactless"
        val merchantByte = mercahnt.toByteArray()
        myData["9F4E"] = merchantByte //Merchant Name and location
        myData["9F1B"] = byteArrayOf(0x00, 0x00, 0x00, 0x00) //terminal floor limit
        myData["DF01"] = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x01) //Reader CVM Required Limit
        contactlessConfiguration.terminalData = myData
    }

    private fun initMposLibrary() {
        outcomeObserver =
            OutcomeObserver()
        configuration = terminalSdk.configuration
        builder = StringBuilder()
        nfcProvider =
            NfcProvider(
                this
            )
        nfcAdapter = nfcProvider.mNFCManager?.mNfcAdapter
        val cardCommProviderStub =
            CardCommProviderStub()
        val logger = TransactionProcessLoggerImpl(builder)
        configuration
            .withResourceProvider(
                ResourceProviderImplementation(
                    this.applicationContext
                )
            )
            .withLogger(logger)
            .withCardCommunication(nfcProvider, cardCommProviderStub)
            .withTransactionObserver(outcomeObserver)
            .withUnpredictableNumberProvider(UnpredictableNumberImplementation())
            .withMessageDisplayProvider(
                DisplayImplementation(
                    logger
                )
            )
        transactionsApi = configuration.initializeLibrary()
        configuration.selectProfile("MPOS")
        viewModel.setTransactionsApi(transactionsApi)
        viewModel.setOutcomeObserver(outcomeObserver)
    }

    private fun startNfcPayment() {
        nfcAdapter?.enableReaderMode(
            this,
            this,
            READER_FLAGS,
            Bundle()
        )
    }

    private fun stopNfcPayment() {
        nfcAdapter?.disableReaderMode(this)
    }

    private fun checkNfcAvailable() {
        if (nfcAdapter != null) {
            if (nfcAdapter?.isEnabled == false) {
                AlertDialog.Builder(this)
                    .setTitle("NFC Message")
                    .setMessage("NFC is not enabled, goto device settings to enable")
                    .setCancelable(false)
                    .setPositiveButton("Settings") { dialog, _ ->
                        dialog.dismiss()
                        startActivityForResult(
                            Intent(Settings.ACTION_NFC_SETTINGS),
                            0
                        )
                    }.setNegativeButton("Cancel") { dialog, _ ->
                        dialog.cancel()
                        setResult(ContactlessReaderResult.RESULT_ERROR, Intent().apply {
                            putExtra("message", "NFC not enabled")
                        })
                        finish()
                    }.show()
            } else {
                showCardDialog(this).observe(this) { event ->
                    event.getContentIfNotHandled()?.let {
                        viewModel.initiateNfcPayment(amount.times(100).toLong(),
                            cashBackAmount.times(100).toLong(),
                            it)
                    }
                }
            }
        } else {
            AlertDialog.Builder(this)
                .setTitle("NFC Message")
                .setCancelable(false)
                .setMessage("Device dose not have NFC support")
                .setPositiveButton("Close") { dialog, _ ->
                    dialog.dismiss()
                    finish()
                }.show()
        }
    }

    override fun onTagDiscovered(tag: Tag?) {
        tag?.let {
            if (it.toString() == NFC_A_TAG || it.toString() == NFC_B_TAG) {
                runOnUiThread {
                    handleProvider(tag)
                    nfcAdapter?.disableReaderMode(this)
                }
            }
        }
    }

    private fun handleProvider(tag: Tag) {
        val mTagCom = IsoDep.get(tag)
        try {
            mTagCom.connect()
            val logger = StringBuilder()
            val nfcTransReceiver = LiveNfcTransReceiver(logger, mTagCom)
            viewModel.doVisaTransaction(nfcTransReceiver, contactlessKernel)
        } catch (e: Exception) {
            Log.e("tag", e.localizedMessage ?: "exception")
        }
    }

    private fun showPinDialog(pan: String) {
        PasswordDialog(
            this,
            pan,
            pinKey,
            object : PasswordDialog.Listener {
                override fun onConfirm(pinBlock: String?) {
                    viewModel.setPinBlock(pinBlock)
                }

                override fun onError(message: String?) {
                    viewModel.setIccCardHelperLiveData(ICCCardHelper(error = Throwable(message)))
                }

            }).showDialog()
    }
}