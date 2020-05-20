//
import FirebaseAnalytics

class Tracker {

    static let global = Tracker()

    // MARK: Sign-Up/Registration
    
    func trackCreateAccountTapped() {
        Analytics.logEvent("create_account", parameters: nil)
    }

    func trackAcceptTerms() {
        Analytics.logEvent("user_accept_tcs", parameters: nil)
    }

    func trackAcceptPrivacy() {
        Analytics.logEvent("user_accept_pp", parameters: nil)
    }

    func trackContinuedAfterAcceptTerms() {
        Analytics.logEvent("user_continue_post_accept", parameters: nil)
    }

    func trackAcceptRecovery() {
        Analytics.logEvent("user_accept_recovery_phrase", parameters: nil)
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
    
    // MARK: Accept Credentials

    func trackCredentialNewConfirm() {
        Analytics.logEvent("new_credential_confirm", parameters: nil)
    }

    func trackCredentialNewDecline() {
        Analytics.logEvent("credential_decline", parameters: nil)
    }

}
