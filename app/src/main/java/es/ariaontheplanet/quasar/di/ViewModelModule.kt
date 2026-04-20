package es.ariaontheplanet.quasar.di

import es.ariaontheplanet.quasar.actmutedpseudoaccount.MutedPseudoAccountViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

// ViewModels bound for `by viewModel()` usage.
// Phase 2 extracted these as plain classes — this module swaps construction
// over to Koin so Activities stop touching the manual provideViewModel factory.
val viewModelModule = module {
    viewModel { MutedPseudoAccountViewModel() }
}
