//

class LoginPresenter: ListingBasePresenter, ListingBaseTableUtilsPresenterDelegate {

    var viewImpl: LoginViewController? {
        return view as? LoginViewController
    }

    enum LoginSpecialState {
        case none
        case validatingWords
    }

    var stateSpecial: LoginSpecialState = .none
    var data: [Int] = []

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
        startValidatingWords()
    }

    private func updateContinueButtonState() {

        // Only enable button if all text fields have text
        for fieldText in viewImpl?.getTextFieldsTexts() ?? [] {
            if fieldText.count == 0 {
                viewImpl?.changeButtonState(isEnabled: false)
                return
            }
        }
        viewImpl?.changeButtonState(isEnabled: true)
    }

    // MARK: TextFields

    func textFieldTextChanged() {
        updateContinueButtonState()
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

            // sleep(1)

            self.cleanData()

            // Fake data
            let indexes = FakeData.loginWords()
            self.data = indexes

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
            self.viewImpl?.config(mode: self.state, specialMode: self.stateSpecial)
            self.viewImpl?.configFields(numbers: self.data)
            if self.lastError != nil {
                self.viewImpl?.showErrorMessage(doShow: true, message: self.lastError!.localizedDescription)
                self.lastError = nil
            }
        }
    }

    func startValidatingWords() {

        state = .special
        stateSpecial = .validatingWords
        updateViewToState()

        let words = viewImpl!.getTextFieldsTexts()

        // TODO: Delete me when services are ready
        DispatchQueue.global(qos: .background).async {
            print("This is run on the background queue")

            // sleep(1)

            self.cleanData()

            // Fake data
            var user: LoggedUser?
            if FakeData.loginIsValid(words: words) {
                // user = FakeData.fakeProfile()
                user = LoggedUser()
                user?.apiUrl = Common.URL_API
            }

            DispatchQueue.main.async {
                if user == nil {
                    self.startValidationFail()
                } else {
                    self.startValidationSucess(user: user!)
                }
            }
        }
    }

    func startValidationFail() {

        Tracker.global.trackRecoveryFail()
        self.startShowError(error: SimpleLocalizedError("login_phrase_invalid_error".localize()))
    }

    func startValidationSucess(user: LoggedUser) {

        Tracker.global.trackRecoverySuccess()
        self.sharedMemory.loggedUser = user
        self.sharedMemory.imageBank = ImageBank()
        viewImpl?.changeScreenToSuccess(action: actionSuccessContinue)
    }

    lazy var actionSuccessContinue = SelectorAction(action: { [weak self] in
        self?.viewImpl?.goToMainScreen()
    })
}
