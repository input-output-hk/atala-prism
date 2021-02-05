//

class RegisterPresenter: ListingBasePresenter, ListingBaseTableUtilsPresenterDelegate {

    var viewImpl: RegisterViewController? {
        return view as? RegisterViewController
    }

    let cryptoUtils = CryptoUtils.global
    var acceptedLegal = false
    var data: [String] = []

    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        updateContinueButtonState()
    }

    override var isPublicScreen: Bool {
        return true
    }

    // MARK: Buttons

    func tappedContinueButton() {

        Tracker.global.trackContinuedAfterAcceptRecovery()
        viewImpl?.changeScreenToLogin()
    }

    func tappedLegalSwitch(newState: Bool) {

        acceptedLegal = newState
        updateContinueButtonState()
    }

    private func updateContinueButtonState() {
        viewImpl?.changeButtonState(isEnabled: acceptedLegal)
    }

    // MARK: Fetch data

    func hasData() -> Bool {
        return data.count > 0
    }

    func cleanData() {
        data = []
    }

    func getElementCount() -> Int {
        return data.count
    }

    func fetchData() {

        DispatchQueue.global(qos: .background).async {

            self.cleanData()
            // Generate the mnemonics and get the used ones
            self.cryptoUtils.setupMnemonics()
            DispatchQueue.main.async {
                self.data = self.cryptoUtils.usedMnemonics!
                self.startListing()
            }
        }
    }

    func hasPullToRefresh() -> Bool {
        false
    }

    func actionPullToRefresh() {}

    override func updateViewToState() {

        NSObject.execOnMain(delaySecs: 0) {
            self.viewImpl?.config(mode: self.state)
            for index in 0 ..< self.data.count {
                self.viewImpl?.configSeed(at: index, word: self.data[index])
            }
        }
    }
}
