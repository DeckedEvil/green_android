package com.blockstream.common.models.settings

import com.blockstream.common.data.GreenWallet
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.views.wallet.WatchOnlyLook
import com.rickclephas.kmm.viewmodel.MutableStateFlow
import com.rickclephas.kmm.viewmodel.stateIn
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map


abstract class WatchOnlyViewModelAbstract(greenWallet: GreenWallet) :
    GreenViewModel(greenWalletOrNull = greenWallet) {
    override fun screenName(): String = "WalletSettingsWatchOnly"

    @NativeCoroutinesState
    abstract val hasMultisig: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val hasSinglesig: StateFlow<Boolean>

    @NativeCoroutinesState
    abstract val multisigWatchOnly: StateFlow<List<WatchOnlyLook>>

    @NativeCoroutinesState
    abstract val extendedPublicKeysAccounts: StateFlow<List<WatchOnlyLook>>

    @NativeCoroutinesState
    abstract val outputDescriptorsAccounts: StateFlow<List<WatchOnlyLook>>
}

class WatchOnlyViewModel(greenWallet: GreenWallet) :
    WatchOnlyViewModelAbstract(greenWallet = greenWallet) {
    override val hasMultisig: StateFlow<Boolean> =
        MutableStateFlow(viewModelScope, session.activeMultisig.isNotEmpty())

    override val hasSinglesig: StateFlow<Boolean> =
        MutableStateFlow(viewModelScope, session.activeSinglesig.isNotEmpty())


    override val multisigWatchOnly: StateFlow<List<WatchOnlyLook>> = combine(session.multisigBitcoinWatchOnly, session.multisigLiquidWatchOnly) { bitcoin, liquid ->
        listOfNotNull(
            bitcoin?.let { WatchOnlyLook(network = session.bitcoinMultisig, username = it) },
            liquid?.let { WatchOnlyLook(network = session.liquidMultisig, username = it) }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), listOf())

    private val _accounts = session.accounts.map { accounts ->
        accounts.filter { it.isSinglesig && it.isBitcoin }.map {
            // getAccount() returns a detailed account
            session.getAccount(it)
        }
    }

    override val extendedPublicKeysAccounts: StateFlow<List<WatchOnlyLook>> =
        _accounts.map { accounts ->
            accounts.map {
                WatchOnlyLook(account = it, extendedPubkey = it.extendedPubkey)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), listOf())

    override val outputDescriptorsAccounts: StateFlow<List<WatchOnlyLook>> =
        _accounts.map { accounts ->
            accounts.map {
                WatchOnlyLook(account = it, outputDescriptors = it.outputDescriptors)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), listOf())
}

class WatchOnlyViewModelPreview(greenWallet: GreenWallet) :
    WatchOnlyViewModelAbstract(greenWallet = greenWallet) {
    companion object {
        fun preview() = WatchOnlyViewModelPreview(previewWallet(isHardware = false))
    }

    override val hasMultisig: StateFlow<Boolean> = MutableStateFlow(viewModelScope, false)

    override val hasSinglesig: StateFlow<Boolean> = MutableStateFlow(viewModelScope, false)

    override val multisigWatchOnly: StateFlow<List<WatchOnlyLook>> =
        MutableStateFlow(viewModelScope, listOf())

    override val extendedPublicKeysAccounts: StateFlow<List<WatchOnlyLook>> =
        MutableStateFlow(viewModelScope, listOf())

    override val outputDescriptorsAccounts: StateFlow<List<WatchOnlyLook>> =
        MutableStateFlow(viewModelScope, listOf())
}