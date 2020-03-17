//
import FirebaseAnalytics

class Tracker {

    static let global = Tracker()

    func trackCreateAccountTapped() {
        Analytics.logEvent("create_account", parameters: nil)
    }

    func trackAcceptTerms() {
        Analytics.logEvent("student_accept_tcs", parameters: nil)
    }

    func trackAcceptPrivacy() {
        Analytics.logEvent("student_accept_pp", parameters: nil)
    }

    func trackContinuedAfterAcceptTerms() {
        Analytics.logEvent("student_continue_post_accept", parameters: nil)
    }

    func trackAcceptRecovery() {
        Analytics.logEvent("student_accept_recovery_phrase", parameters: nil)
    }

    func trackContinuedAfterAcceptRecovery() {
        Analytics.logEvent("student_accept_recovery_phrase_continue", parameters: nil)
    }

    func trackRecoverySuccess() {
        Analytics.logEvent("verify_recovery_phrase_success", parameters: nil)
    }

    func trackRecoveryFail() {
        Analytics.logEvent("verify_recovery_phrase_fail", parameters: nil)
    }

    func trackScanQrTapped() {
        Analytics.logEvent("connect_scan_qr_code", parameters: nil)
    }

    func trackConnectionAccept() {
        Analytics.logEvent("connect_with_issuer_verifier_confirm", parameters: nil)
    }

    func trackConnectionDecline() {
        Analytics.logEvent("connect_with_issuer_verifier_decline", parameters: nil)
    }

    func trackConnectionFail() {
        Analytics.logEvent("connect_with_issuer_fail", parameters: nil)
    }

    func trackCredentialNewTapped() {
        Analytics.logEvent("new_credential_view", parameters: nil)
    }

    func trackCredentialNewConfirm() {
        Analytics.logEvent("new_credential_confirm", parameters: nil)
    }

    func trackCredentialNewDecline() {
        Analytics.logEvent("credential_decline", parameters: nil)
    }

    func trackPaymentConfirmTapped() {
        Analytics.logEvent("credential_pay_cc", parameters: nil)
    }

    func trackPaymentSuccess() {
        Analytics.logEvent("credential_pay_cc_success", parameters: nil)
    }

    func trackPaymentFail() {
        Analytics.logEvent("credential_pay_cc_fail", parameters: nil)
    }

    func trackCredentialShareCompleted() {
        Analytics.logEvent("share_credential", parameters: nil)
    }
}
