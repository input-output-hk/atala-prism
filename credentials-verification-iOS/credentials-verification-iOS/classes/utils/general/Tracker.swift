//
import FirebaseAnalytics

class Tracker {

    static let global = Tracker()

    // MARK: Sign-Up/Registration
    
    func trackCreateAccountTapped() {
        Analytics.logEvent("create_account", parameters: nil)
    }

    func trackContinuedAfterAcceptTerms() {
        Analytics.logEvent("user_continue_post_accept", parameters: nil)
    }

    func trackContinuedAfterAcceptRecovery() {
        Analytics.logEvent("user_accept_recovery_phrase_continue", parameters: nil)
    }

    func trackRecoverySuccess() {
        Analytics.logEvent("verify_recovery_phrase_success", parameters: nil)
    }

    func trackRecoveryFail() {
        Analytics.logEvent("verify_recovery_phrase_fail", parameters: nil)
    }
    
    // MARK: App security

    func trackSecureAppFingerprint() {
        Analytics.logEvent("secure_app_fingerprint", parameters: nil)
    }

    func trackSecureAppFacial() {
        Analytics.logEvent("secure_app_facial_recognition", parameters: nil)
    }

    func trackSecureAppPasscode() {
        Analytics.logEvent("secure_app_fingerprint_passcode", parameters: nil)
    }
    
    // MARK: Accept Connection

    func trackConnectionAccept() {
        Analytics.logEvent("new_connection_confirm", parameters: nil)
    }

    func trackConnectionDecline() {
        Analytics.logEvent("new_connection_decline", parameters: nil)
    }

    func trackConnectionRepeat() {
        Analytics.logEvent("repeat_connection_notification", parameters: nil)
    }
    
    // MARK: Accept Credentials

    func trackCredentialNewConfirm() {
        Analytics.logEvent("new_credential_view", parameters: nil)
    }

}
