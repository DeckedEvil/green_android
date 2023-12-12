package com.blockstream.common.models.lightning

import breez_sdk.RecommendedFees
import com.blockstream.common.data.Denomination
import com.blockstream.common.data.ErrorReport
import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Event
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.ifConnectedSuspend
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.data.AccountAsset
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffects
import com.blockstream.common.utils.Loggable
import com.blockstream.common.utils.feeRateKBWithUnit
import com.blockstream.common.utils.toAmountLook
import com.rickclephas.kmm.viewmodel.coroutineScope
import com.rickclephas.kmm.viewmodel.stateIn
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue


abstract class RecoverFundsViewModelAbstract(greenWallet: GreenWallet, val onChainAddress: String?) :
    GreenViewModel(greenWalletOrNull = greenWallet) {

    val isRefund by lazy {
        onChainAddress != null
    }

    val isSweep by lazy {
        onChainAddress == null
    }

    override fun screenName(): String = if(isRefund) "OnChainRefund" else "LightningSweep"

    @NativeCoroutinesState
    abstract val address: MutableStateFlow<String>

    @NativeCoroutinesState
    abstract val amount: StateFlow<String>

    @NativeCoroutinesState
    abstract val amountToBeRefunded: StateFlow<String?>
    @NativeCoroutinesState
    abstract val amountToBeRefundedFiat: StateFlow<String?>
    @NativeCoroutinesState
    abstract val error: StateFlow<String?>
    @NativeCoroutinesState
    abstract val fee: StateFlow<String?>
    @NativeCoroutinesState
    abstract val feeFiat: StateFlow<String?>

    @NativeCoroutinesState
    abstract val feeSlider: MutableStateFlow<Int>

    @NativeCoroutinesState
    abstract val feeAmountRate: StateFlow<String>

    @NativeCoroutinesState
    abstract val hasBitcoinAccount: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val showManualAddress: MutableStateFlow<Boolean>

    @NativeCoroutinesState
    abstract val recommendedFees: StateFlow<RecommendedFees?>

    @NativeCoroutinesState
    abstract val customFee: StateFlow<Long?>
}

class RecoverFundsViewModel(
    greenWallet: GreenWallet,
    onChainAddress: String?,
    val satoshi: Long,
) : RecoverFundsViewModelAbstract(greenWallet = greenWallet, onChainAddress = onChainAddress) {
    private val accountAddress = MutableStateFlow("") // cached account address

    override val address: MutableStateFlow<String> = MutableStateFlow("")

    override val amountToBeRefunded: MutableStateFlow<String?> = MutableStateFlow(null)
    override val amountToBeRefundedFiat: MutableStateFlow<String?> = MutableStateFlow(null)
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val fee: MutableStateFlow<String?> = MutableStateFlow(null)
    override val feeFiat: MutableStateFlow<String?> = MutableStateFlow(null)

    override val customFee: MutableStateFlow<Long> = MutableStateFlow(1L)

    override val amount: StateFlow<String> = flow {
        session.ifConnectedSuspend {
            emit(
                satoshi.absoluteValue.toAmountLook(
                    session = session,
                    withUnit = true,
                ) ?: ""
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

    override val recommendedFees: StateFlow<RecommendedFees?> = flow {
        session.ifConnectedSuspend {
            emit(session.lightningSdk.recommendedFees().also {
                customFee.value = it.minimumFee.toLong()
            })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    override val feeSlider: MutableStateFlow<Int> = MutableStateFlow(2)

    private val _feeAmountRate: MutableStateFlow<String> = MutableStateFlow("")
    override val feeAmountRate: StateFlow<String> = _feeAmountRate.asStateFlow()

    private val bitcoinAccounts = session.accounts.map { accounts ->
        accounts.filter { it.isBitcoin && !it.isLightning }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), session.accounts.value.filter { it.isBitcoin && !it.isLightning })

    override val hasBitcoinAccount: StateFlow<Boolean> = session.accounts.map { accounts ->
        accounts.any { it.isBitcoin && !it.isLightning }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), bitcoinAccounts.value.isNotEmpty())

    override val showManualAddress: MutableStateFlow<Boolean> = MutableStateFlow(hasBitcoinAccount.value)

    class LocalEvents{
        class SetCustomFee(val fee: Long?): Event
    }

    override fun handleEvent(event: Event) {
        super.handleEvent(event)

        if (event is Events.Continue) {
            recoverFunds()
        } else if (event is LocalEvents.SetCustomFee){
            val fee = event.fee
            val minimum = recommendedFees.value?.minimumFee?.toLong() ?: 1L
            if (fee == null) {
                customFee.value = minimum
            } else if (event.fee < minimum) {
                postSideEffect(SideEffects.Snackbar("id_fee_rate_must_be_at_least_s|$minimum"))
                customFee.value = minimum
            } else {
                customFee.value = fee
            }
            feeSlider.value = 0
        }
    }

    init {
        bootstrap()

        accountAsset.value = bitcoinAccounts.value.firstOrNull()?.let { AccountAsset.fromAccount(it) }
        showManualAddress.value = accountAsset.value == null

        // Cache account address so that switching between manual address, account address is the same
        accountAsset.filterNotNull().onEach {
            accountAddress.value = withContext(context = Dispatchers.IO) {
                session.getReceiveAddress(it.account).address
            }
        }.launchIn(viewModelScope.coroutineScope)

        combine(
            accountAddress,
            showManualAddress
        ) { accountAddress, showManualAddress ->
            accountAddress to showManualAddress
        }.onEach {
            if (!it.second) {
                address.value = it.first
            }
        }.launchIn(viewModelScope.coroutineScope)

        combine(
            feeSlider,
            customFee,
            recommendedFees.filterNotNull()
        ) { feeSlider, customFee, recommendedFees ->
            feeSlider to recommendedFees
        }.onEach {
            _feeAmountRate.value = ((getFee()?.toLong() ?: 0) * 1000).feeRateKBWithUnit()
            prepare()
        }.launchIn(viewModelScope.coroutineScope)

    }

    private fun prepare(){
        doAsync({
            if (onChainAddress != null) {
                // Refund from OnChain address
                session.lightningSdk.prepareRefund(
                    swapAddress = onChainAddress,
                    toAddress = address.value,
                    satPerVbyte = getFee()?.toUInt()
                ).also {
                    if(satoshi - it.refundTxFeeSat.toLong() < 0){
                        throw Exception("id_insufficient_funds")
                    }

                    amountToBeRefunded.value = (satoshi - it.refundTxFeeSat.toLong()).toAmountLook(session = session, withUnit = true)
                    amountToBeRefundedFiat.value = (satoshi - it.refundTxFeeSat.toLong()).toAmountLook(session = session, withUnit = true, denomination = Denomination.fiat(session))

                    fee.value = it.refundTxFeeSat.toLong().toAmountLook(session = session, withUnit = true)
                    feeFiat.value = it.refundTxFeeSat.toLong().toAmountLook(session = session, withUnit = true, denomination = Denomination.fiat(session))
                }
            } else {
                // Sweep from Lightning node
                session.lightningSdk.prepareSweep(
                    toAddress = address.value,
                    satPerVbyte = getFee()?.toUInt()
                ).also {
                    if(satoshi - it.sweepTxFeeSat.toLong() < 0){
                        throw Exception("id_insufficient_funds")
                    }

                    amountToBeRefunded.value = (satoshi - it.sweepTxFeeSat.toLong()).toAmountLook(session = session, withUnit = true)
                    amountToBeRefundedFiat.value = (satoshi - it.sweepTxFeeSat.toLong()).toAmountLook(session = session, withUnit = true, denomination = Denomination.fiat(session))

                    fee.value = it.sweepTxFeeSat.toLong().toAmountLook(session = session, withUnit = true)
                    feeFiat.value = it.sweepTxFeeSat.toLong().toAmountLook(session = session, withUnit = true, denomination = Denomination.fiat(session))
                }
            }
        }, onError = {
            error.value = it.message
            amountToBeRefunded.value = null
        }, onSuccess = {
            error.value = null
        })
    }

    private fun getFee(): ULong? {
        return recommendedFees.value?.let {
            customFee.value.takeIf { feeSlider.value == 0 }?.toULong() ?: when (feeSlider.value) {
                2 -> it.hourFee
                3 -> it.halfHourFee
                4 -> it.fastestFee
                else -> it.economyFee
            }
        }
    }

    private fun recoverFunds() {
        doAsync({
            if (onChainAddress != null) {
                // Refund from OnChain address
                session.lightningSdk.refund(
                    swapAddress = onChainAddress,
                    toAddress = address.value,
                    satPerVbyte = getFee()?.toUInt()
                ).refundTxId
            } else {
                // Sweep from Lightning node
                session.lightningSdk.sweep(
                    toAddress = address.value,
                    satPerVbyte = getFee()?.toUInt()
                ).txid
            }
        }, onSuccess = {
            postSideEffect(SideEffects.Success())
        })
    }

    override fun errorReport(exception: Throwable): ErrorReport {
        return ErrorReport.create(throwable = exception, network = session.lightning, session = session)
    }

    companion object : Loggable()
}

class RecoverFundsViewModelPreview(greenWallet: GreenWallet) :
    RecoverFundsViewModelAbstract(greenWallet = greenWallet, onChainAddress = null) {
    companion object {
        fun preview(): RecoverFundsViewModelPreview {
            return RecoverFundsViewModelPreview(
                greenWallet = previewWallet(isHardware = false)
            )
        }
    }

    override val address: MutableStateFlow<String> = MutableStateFlow("")
    override val amount: StateFlow<String> = MutableStateFlow("")
    override val error: MutableStateFlow<String?> = MutableStateFlow(null)
    override val feeSlider: MutableStateFlow<Int> = MutableStateFlow(1)
    override val feeAmountRate: StateFlow<String> = MutableStateFlow("")
    override val hasBitcoinAccount: StateFlow<Boolean> = MutableStateFlow(false)
    override val showManualAddress: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val recommendedFees: StateFlow<RecommendedFees?> = MutableStateFlow(null)
    override val customFee: StateFlow<Long> = MutableStateFlow(0L)
    override val amountToBeRefunded: MutableStateFlow<String?> = MutableStateFlow(null)
    override val amountToBeRefundedFiat: MutableStateFlow<String?> = MutableStateFlow(null)
    override val fee: MutableStateFlow<String?> = MutableStateFlow(null)
    override val feeFiat: MutableStateFlow<String?> = MutableStateFlow(null)
}