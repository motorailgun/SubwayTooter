package es.ariaontheplanet.quasar.di

import es.ariaontheplanet.quasar.actaccountsetting.AccountSettingViewModel
import es.ariaontheplanet.quasar.actdrawablelist.DrawableListViewModel
import es.ariaontheplanet.quasar.actfavmute.FavMuteViewModel
import es.ariaontheplanet.quasar.acthighlightwordlist.HighlightWordListViewModel
import es.ariaontheplanet.quasar.actkeywordfilter.KeywordFilterViewModel
import es.ariaontheplanet.quasar.actmain.MainViewModel
import es.ariaontheplanet.quasar.actmediaviewer.MediaViewerViewModel
import es.ariaontheplanet.quasar.actmutedapp.MutedAppViewModel
import es.ariaontheplanet.quasar.actmutedpseudoaccount.MutedPseudoAccountViewModel
import es.ariaontheplanet.quasar.actmutedword.MutedWordViewModel
import es.ariaontheplanet.quasar.actnickname.NicknameViewModel
import es.ariaontheplanet.quasar.actpost.PostViewModel
import es.ariaontheplanet.quasar.appsetting.AppSettingViewModel
import es.ariaontheplanet.quasar.emoji.EmojiPickerViewModel
import es.ariaontheplanet.quasar.ui.languageFilter.LanguageFilterViewModel
import es.ariaontheplanet.quasar.ui.ossLicense.ActOSSLicenseViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

// ViewModels bound for `by viewModel()` usage.
// Activities inject via Koin; the manual ViewModelProvider factory is gone.
val viewModelModule = module {
    // Simple ViewModels
    viewModel { FavMuteViewModel() }
    viewModel { KeywordFilterViewModel() }
    viewModel { MutedPseudoAccountViewModel() }
    viewModel { MutedWordViewModel() }

    // AndroidViewModels
    viewModel { AccountSettingViewModel(androidApplication()) }
    viewModel { AppSettingViewModel(androidApplication()) }
    viewModel { DrawableListViewModel(androidApplication()) }
    viewModel { EmojiPickerViewModel(androidApplication()) }
    viewModel { HighlightWordListViewModel(androidApplication()) }
    viewModel { LanguageFilterViewModel(androidApplication()) }
    viewModel { MainViewModel(androidApplication()) }
    viewModel { MediaViewerViewModel(androidApplication()) }
    viewModel { MutedAppViewModel(androidApplication()) }
    viewModel { PostViewModel(androidApplication()) }
    viewModel { ActOSSLicenseViewModel(androidApplication()) }

    // Parameterized
    viewModel { (acctAscii: String) -> NicknameViewModel(acctAscii) }
}
