package io.iohk.atala.prism.app.dagger.modules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.multibindings.IntoMap
import io.iohk.atala.prism.app.dagger.ViewModelFactory
import io.iohk.atala.prism.app.neo.ui.launch.LaunchViewModel
import io.iohk.atala.prism.app.neo.ui.onboarding.phraseverification.PhraseVerificationViewModel
import io.iohk.atala.prism.app.neo.ui.onboarding.restoreaccount.RestoreAccountViewModel
import io.iohk.atala.prism.app.neo.ui.onboarding.walletsetup.WalletSetupViewModel
import io.iohk.atala.prism.app.ui.commondialogs.AcceptConnectionDialogViewModel
import io.iohk.atala.prism.app.ui.commondialogs.ProofRequestDialogViewModel
import io.iohk.atala.prism.app.ui.main.MainViewModel
import io.iohk.atala.prism.app.ui.main.contacts.ContactDetailViewModel
import io.iohk.atala.prism.app.ui.main.contacts.ContactsViewModel
import io.iohk.atala.prism.app.ui.main.contacts.DeleteContactAlertDialogViewModel
import io.iohk.atala.prism.app.ui.main.credentials.*
import io.iohk.atala.prism.app.ui.main.notifications.ActivityLogViewModel
import io.iohk.atala.prism.app.ui.main.notifications.NotificationsViewModel
import io.iohk.atala.prism.app.ui.main.profile.ProfileViewModel
import io.iohk.atala.prism.app.ui.main.settings.SettingsDateFormatViewModel
import io.iohk.atala.prism.app.ui.main.settings.SettingsViewModel
import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@MapKey
internal annotation class ViewModelKey(val value: KClass<out ViewModel>)

@Module
abstract class ViewModelModule {
    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @ViewModelKey(WalletSetupViewModel::class)
    internal abstract fun walletSetupViewModel(viewModel: WalletSetupViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(LaunchViewModel::class)
    internal abstract fun launchViewModel(viewModel: LaunchViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(PhraseVerificationViewModel::class)
    internal abstract fun phraseVerificationViewModel(viewModel: PhraseVerificationViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(RestoreAccountViewModel::class)
    internal abstract fun restoreAccountViewModel(viewModel: RestoreAccountViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CredentialDetailViewModel::class)
    internal abstract fun credentialsViewModel(viewModel: CredentialDetailViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SettingsViewModel::class)
    internal abstract fun connectionsActivityViewModel(viewModel: SettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    internal abstract fun mainViewModel(viewModel: MainViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AcceptConnectionDialogViewModel::class)
    internal abstract fun acceptConnectionDialogViewModel(viewModel: AcceptConnectionDialogViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(DeleteCredentialDialogViewModel::class)
    internal abstract fun deleteCredentialDialogViewModel(viewModel: DeleteCredentialDialogViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(DeleteContactAlertDialogViewModel::class)
    internal abstract fun deleteContactAlertDialogViewModel(viewModel: DeleteContactAlertDialogViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ContactDetailViewModel::class)
    internal abstract fun contactDetailViewModel(viewModel: ContactDetailViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CredentialHistoryViewModel::class)
    internal abstract fun credentialHistoryViewModel(viewModel: CredentialHistoryViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ActivityLogViewModel::class)
    internal abstract fun activityLogViewModel(viewModel: ActivityLogViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SettingsDateFormatViewModel::class)
    internal abstract fun settingsDateFormatViewModel(viewModel: SettingsDateFormatViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ProofRequestDialogViewModel::class)
    internal abstract fun proofRequestDialogViewModel(viewModel: ProofRequestDialogViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(MyCredentialsViewModel::class)
    internal abstract fun myCredentialsViewModel(viewModel: MyCredentialsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ProfileViewModel::class)
    internal abstract fun profileViewModel(viewModel: ProfileViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(NotificationsViewModel::class)
    internal abstract fun notificationsViewModel(viewModel: NotificationsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ShareCredentialDialogViewModel::class)
    internal abstract fun shareCredentialDialogViewModel(viewModel: ShareCredentialDialogViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ContactsViewModel::class)
    internal abstract fun contactsViewModel(viewModel: ContactsViewModel): ViewModel
}