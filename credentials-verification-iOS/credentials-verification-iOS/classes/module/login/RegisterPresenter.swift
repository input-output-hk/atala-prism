//

class RegisterPresenter: ListingBasePresenter, ListingBaseTableUtilsPresenterDelegate {

    var viewImpl: RegisterViewController? {
        return view as? RegisterViewController
    }

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

        // TODO: Delete me when services are ready
        DispatchQueue.global(qos: .background).async {
            print("This is run on the background queue")

            sleep(1)

            self.cleanData()

            // Fake data
            let words = FakeData.seedWords()
            self.data = words

            DispatchQueue.main.async {
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
