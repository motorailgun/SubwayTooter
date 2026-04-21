package es.ariaontheplanet.quasar.actaccountsetting

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel

// Thin entry point for the AccountSettings route. Constructs (or reuses) the
// AccountSettingViewModel and kicks off its load before handing off to the
// existing AccountSettingScreen composable.
@Composable
fun AccountSettingRoute(accountDbId: Long, onBack: () -> Unit) {
    val viewModel: AccountSettingViewModel = viewModel()
    LaunchedEffect(accountDbId) { viewModel.load(accountDbId) }
    AccountSettingScreen(viewModel = viewModel, onBack = onBack)
}
