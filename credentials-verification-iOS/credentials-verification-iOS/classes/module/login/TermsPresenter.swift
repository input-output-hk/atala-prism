//

class TermsPresenter: BasePresenter {

    var viewImpl: TermsViewController? {
        return view as? TermsViewController
    }

    var acceptedTerms = false
    var acceptedPrivacy = false

    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        updateContinueButtonState()
    }

    func tappedContinueButton() {
        viewImpl?.changeScreenToRegister()
    }

    func tappedLegalCloseButton() {
        viewImpl?.showLegalView(doShow: false, urlStr: nil)
    }

    func tappedTermsSwitch(newState: Bool) {
        acceptedTerms = newState
        updateContinueButtonState()
    }

    func tappedPrivacySwitch(newState: Bool) {
        acceptedPrivacy = newState
        updateContinueButtonState()
    }

    private func updateContinueButtonState() {
        viewImpl?.changeButtonState(isEnabled: acceptedTerms && acceptedPrivacy)
    }

    func tappedOpenTerms() {
        viewImpl?.showLegalView(doShow: true, urlStr: Common.URL_TERMS)
    }

    func tappedOpenPrivacy() {
        viewImpl?.showLegalView(doShow: true, urlStr: Common.URL_PRIVACY)
    }
}
