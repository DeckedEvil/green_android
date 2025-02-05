package com.blockstream.compose.screens.onboarding.phone

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.arkivanov.essenty.statekeeper.stateKeeper
import com.blockstream.common.data.ScanResult
import com.blockstream.common.data.SetupArgs
import com.blockstream.common.events.Events
import com.blockstream.common.extensions.isNotBlank
import com.blockstream.common.models.onboarding.phone.EnterRecoveryPhraseViewModel
import com.blockstream.common.models.onboarding.phone.EnterRecoveryPhraseViewModelAbstract
import com.blockstream.common.models.onboarding.phone.EnterRecoveryPhraseViewModelPreview
import com.blockstream.compose.GreenPreview
import com.blockstream.compose.LocalSnackbar
import com.blockstream.compose.R
import com.blockstream.compose.components.GreenButton
import com.blockstream.compose.components.GreenButtonSize
import com.blockstream.compose.components.GreenColumn
import com.blockstream.compose.components.GreenRow
import com.blockstream.compose.components.PasteButton
import com.blockstream.compose.components.ScanQrButton
import com.blockstream.compose.dialogs.TextDialog
import com.blockstream.compose.navigation.getNavigationResult
import com.blockstream.compose.navigation.resultKey
import com.blockstream.compose.sheets.CameraBottomSheet
import com.blockstream.compose.sheets.LocalBottomSheetNavigatorM3
import com.blockstream.compose.sheets.RecoveryHelpBottomSheet
import com.blockstream.compose.theme.GreenTheme
import com.blockstream.compose.theme.GreenThemePreview
import com.blockstream.compose.theme.bodyLarge
import com.blockstream.compose.theme.bodyMedium
import com.blockstream.compose.theme.bodySmall
import com.blockstream.compose.theme.displayMedium
import com.blockstream.compose.theme.green
import com.blockstream.compose.theme.labelLarge
import com.blockstream.compose.theme.md_theme_brandSurface
import com.blockstream.compose.theme.whiteLow
import com.blockstream.compose.theme.whiteMedium
import com.blockstream.compose.utils.AppBar
import com.blockstream.compose.utils.HandleSideEffect
import com.blockstream.compose.utils.getClipboard
import com.blockstream.compose.utils.stringResourceId
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

@Parcelize
data class EnterRecoveryPhraseScreen(val setupArgs: SetupArgs) : Screen, Parcelable {
    @Composable
    override fun Content() {
        val savedStateRegistryOwner = LocalSavedStateRegistryOwner.current
        val viewModel = getScreenModel<EnterRecoveryPhraseViewModel>() {
            parametersOf(setupArgs, savedStateRegistryOwner.stateKeeper())
        }

        val navData by viewModel.navData.collectAsStateWithLifecycle()

        AppBar(navData)

        EnterRecoveryPhraseScreen(viewModel = viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterRecoveryPhraseScreen(
    viewModel: EnterRecoveryPhraseViewModelAbstract
) {
    getNavigationResult<ScanResult>(CameraBottomSheet::class.resultKey).value?.also {
        viewModel.postEvent(EnterRecoveryPhraseViewModel.LocalEvents.SetRecoveryPhrase(it.result))
    }

    var showPassphraseEncryptionDialog by remember { mutableStateOf(false) }

    val bottomSheetNavigator = LocalBottomSheetNavigatorM3.current
    HandleSideEffect(viewModel = viewModel) {
        if (it is EnterRecoveryPhraseViewModel.LocalSideEffects.LaunchHelp) {
            bottomSheetNavigator.show(RecoveryHelpBottomSheet)
        } else if (it is EnterRecoveryPhraseViewModel.LocalSideEffects.RequestMnemonicPassword){
            showPassphraseEncryptionDialog = true
        }
    }

    if (showPassphraseEncryptionDialog) {
        TextDialog(
            title = stringResource(id = R.string.id_encryption_passphrase),
            label = stringResource(id = R.string.id_passphrase),
            isPassword = true,
        ) { password ->
            showPassphraseEncryptionDialog = false

            if (password != null) {
                viewModel.postEvent(
                    EnterRecoveryPhraseViewModel.LocalEvents.MnemonicEncryptionPassword(
                        password
                    )
                )
            }
        }
    }

    GreenColumn(
        padding = 0,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Text(
            stringResource(R.string.id_enter_your_recovery_phrase),
            style = displayMedium,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        )

        GreenColumn(
            padding = 4,
            space = 6,
            modifier = Modifier
                .padding(end = 8.dp)
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            val recoveryPhrase by viewModel.recoveryPhrase.collectAsStateWithLifecycle()
            val rows by viewModel.rows.collectAsStateWithLifecycle()
            val activeWord by viewModel.activeWord.collectAsStateWithLifecycle()

            (0..<rows).forEach { row ->
                GreenRow(space = 0, padding = 0) {
                    (0..<3).forEach { column ->
                        val index = (row * 3) + column
                        PhraseWord(
                            index = index + 1,
                            word = recoveryPhrase.getOrNull(index) ?: "",
                            isChecked = index == activeWord,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.postEvent(
                                    EnterRecoveryPhraseViewModel.LocalEvents.SetActiveWord(
                                        index
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }


        val options = listOf(12, 24, 27)
        val recoveryPhraseSize by viewModel.recoveryPhraseSize.collectAsStateWithLifecycle()
        SingleChoiceSegmentedButtonRow(modifier = Modifier.align(Alignment.End)) {
            options.forEachIndexed { index, label ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index, count = options.size
                    ),
                    onClick = { viewModel.recoveryPhraseSize.value = options[index] },
                    selected = recoveryPhraseSize == options[index]
                ) {
                    Text(label.toString())
                }
            }
        }

        Box(
            modifier = Modifier
                .background(md_theme_brandSurface)
        ) {

            val context = LocalContext.current
            val snackbar = LocalSnackbar.current
            val scope = rememberCoroutineScope()

            Column {
                val hintMessage by viewModel.hintMessage.collectAsStateWithLifecycle()

                GreenRow(padding = 0, space = 8, modifier = Modifier.clickable {
                    if (hintMessage.isNotBlank()) {
                        scope.launch {
                            snackbar.showSnackbar(stringResourceId(context, hintMessage))
                        }
                    }
                }) {
                    Text(
                        text = stringResourceId(hintMessage),
                        style = bodyMedium,
                        color = whiteMedium,
                        fontStyle = FontStyle.Italic,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(horizontal = 6.dp, vertical = 8.dp)
                            .weight(1f)
                    )

                    if (hintMessage.isNotBlank()) {
                        Icon(
                            painter = painterResource(id = R.drawable.info),
                            contentDescription = null,
                            tint = whiteLow,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 6.dp)
                        )
                    }
                }


                val gap = 6.dp

                Column(
                    modifier = Modifier
                        .padding(horizontal = gap)
                        .padding(bottom = gap),
                    verticalArrangement = Arrangement.spacedBy(gap)
                ) {

                    val enabledKeys by viewModel.enabledKeys.collectAsStateWithLifecycle()

                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        "q w e r t y u i o p".split(" ").forEach {
                            Key(
                                key = it,
                                enabled = enabledKeys.contains(it),
                                modifier = Modifier.weight(1f)
                            ) {
                                viewModel.postEvent(
                                    EnterRecoveryPhraseViewModel.LocalEvents.KeyAction(
                                        it
                                    )
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        Spacer(modifier = Modifier.weight(1f))
                        "a s d f g h j k l".split(" ").forEach {
                            Key(
                                key = it,
                                enabled = enabledKeys.contains(it),
                                modifier = Modifier.weight(2f)
                            ) {
                                viewModel.postEvent(
                                    EnterRecoveryPhraseViewModel.LocalEvents.KeyAction(
                                        it
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        Spacer(modifier = Modifier.weight(2f))
                        "z x c v b n m".split(" ").forEach {
                            Key(
                                key = it,
                                enabled = enabledKeys.contains(it),
                                modifier = Modifier.weight(2f)
                            ) {
                                viewModel.postEvent(
                                    EnterRecoveryPhraseViewModel.LocalEvents.KeyAction(
                                        it
                                    )
                                )
                            }
                        }
                        Key(key = " ", modifier = Modifier.weight(3f)) {
                            viewModel.postEvent(EnterRecoveryPhraseViewModel.LocalEvents.KeyAction(" "))
                        }
                    }

                    val matchedWords by viewModel.matchedWords.collectAsStateWithLifecycle()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            gap,
                            Alignment.CenterHorizontally
                        )
                    ) {
                        matchedWords.forEach {
                            Key(key = it, isWord = true) {
                                viewModel.postEvent(
                                    EnterRecoveryPhraseViewModel.LocalEvents.KeyAction(
                                        it
                                    )
                                )
                            }
                        }

                        if (matchedWords.isEmpty()) {
                            Key(
                                key = "",
                                isWord = true,
                                enabled = true,
                                modifier = Modifier.alpha(0f)
                            ) { }
                        }
                    }


                }
            }

            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .padding(bottom = 4.dp)
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {

                val isRecoveryPhraseValid by viewModel.isRecoveryPhraseValid.collectAsStateWithLifecycle()
                androidx.compose.animation.AnimatedVisibility(
                    enter = fadeIn(),
                    exit = fadeOut(targetAlpha = 1f),
                    visible = isRecoveryPhraseValid, modifier = Modifier.align(
                        Alignment.BottomCenter
                    )
                ) {
                    GreenButton(
                        stringResource(R.string.id_continue),
                        size = GreenButtonSize.BIG,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        viewModel.postEvent(Events.Continue)
                    }
                }

                val showTypeNextWordHint by viewModel.showTypeNextWordHint.collectAsStateWithLifecycle()
                androidx.compose.animation.AnimatedVisibility(
                    enter = fadeIn(),
                    exit = fadeOut(targetAlpha = 1f),
                    visible = showTypeNextWordHint, modifier = Modifier.align(
                        Alignment.BottomCenter
                    )
                ) {
                    Text(
                        stringResource(R.string.id_type_the_next_word),
                        style = bodySmall,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                    )
                }

                val showInvalidMnemonicError by viewModel.showInvalidMnemonicError.collectAsStateWithLifecycle()
                if (showInvalidMnemonicError) {
                    GreenRow(
                        padding = 0,
                        space = 6,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.info),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .size(18.dp)
                        )

                        Text(
                            text = stringResource(R.string.id_invalid_mnemonic),
                            style = bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                        )
                    }
                }

                val showInputButtons by viewModel.showInputButtons.collectAsStateWithLifecycle()
                if (showInputButtons) {
                    Row(modifier = Modifier.align(Alignment.BottomEnd)) {
                        PasteButton {
                            viewModel.postEvent(
                                EnterRecoveryPhraseViewModel.LocalEvents.SetRecoveryPhrase(
                                    getClipboard(context) ?: ""
                                )
                            )
                        }

                        val bottomSheetNavigator = LocalBottomSheetNavigatorM3.current
                        ScanQrButton() {
                            bottomSheetNavigator.show(
                                CameraBottomSheet(
                                    isDecodeContinuous = false,
                                    parentScreenName = viewModel.screenName(),
                                    setupArgs = viewModel.setupArgs
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhraseWord(
    index: Int,
    word: String,
    isChecked: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    GreenRow(
        space = 4,
        padding = 0,
        modifier = Modifier.then(modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$index",
            color = green,
            modifier = Modifier.width(24.dp),
            textAlign = TextAlign.End,
            style = labelLarge
        )


        Card(
            colors = CardDefaults.elevatedCardColors(containerColor = md_theme_brandSurface),
            border = if (isChecked) BorderStroke(1.dp, green) else null,
            modifier = Modifier
                .weight(1f)
                .clickable {
                    onClick()
                }) {
            Text(
                text = word,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .background(md_theme_brandSurface)
            )
        }
    }

}

@Preview
@Composable
fun KeysPreview() {
    GreenThemePreview {
        GreenRow {
            Key(key = "about", isWord = true) { }
            Key(key = "thanks", isWord = true) { }
            Key(key = "rib", isWord = true) { }
        }
    }
}

@Preview
@Composable
fun PhraseW1ordPreview() {
    GreenThemePreview {
        GreenColumn {
            PhraseWord(1, "ribbon", true)
            PhraseWord(2, "ribbon", false)
            PhraseWord(3, "about", false)
        }
    }
}


@Composable
fun Key(
    key: String,
    isWord: Boolean = false,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
//            .padding(vertical = 4.dp)
            .then(modifier),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            disabledContainerColor = MaterialTheme.colorScheme.inverseSurface
        ),
        contentPadding = PaddingValues(horizontal = if (isWord) 16.dp else 0.dp, vertical = 14.dp),
        shape = MaterialTheme.shapes.small,
    ) {
        if (key == " ") {
            Icon(
                painter = painterResource(R.drawable.backspace),
                contentDescription = "Expand",
            )
        } else {
            Text(
                text = key,
                fontSize = if (isWord) 16.sp else 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
@Preview
@Preview(widthDp = 300, heightDp = 500)
fun EnterRecoveryPhrasePreview(
) {
    GreenPreview {
        EnterRecoveryPhraseScreen(viewModel = EnterRecoveryPhraseViewModelPreview.preview().also {
            it.recoveryPhrase.value = listOf("about", "thanks", "rib")
//            it.onProgress.value = true
        })
    }
}