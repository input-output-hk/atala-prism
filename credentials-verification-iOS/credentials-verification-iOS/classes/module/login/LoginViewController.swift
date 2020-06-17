//

class LoginViewController: BaseViewController {

    var presenterImpl = LoginPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    @IBOutlet weak var buttonContinue: UIButton!
    @IBOutlet weak var textField1: TextFieldTitledView!
    @IBOutlet weak var textField2: TextFieldTitledView!
    @IBOutlet weak var textFieldUrl: TextFieldTitledView!

    override func navBarCustomStyle() -> NavBarCustomStyle {
        return NavBarCustomStyle(hasNavBar: true, title: nil, hasBackButton: true)
    }

    var textFields: [TextFieldTitledView] {
        return [textField1, textField2]
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        setupButtons()
        setupTextFields()
    }

    // MARK: Setup

    func setupButtons() {

        buttonContinue.layer.cornerRadius = AppConfigs.CORNER_RADIUS_BUTTON
    }

    func setupTextFields() {

        setupUrlTextField()

        setupUrlTextField()
        textFields.forEach { textField in
            textField.config(delegate: self)
            textField.textField.autocapitalizationType = .none
            textField.textField.autocorrectionType = .no
        }
        textField1.textField.returnKeyType = .next
        textField2.textField.returnKeyType = .done
        configFields(numbers: [])

        ViewControllerUtils.addTapToDismissKeyboard(view: self)
    }

    // MARK: Buttons

    @IBAction func actionContinueButton(_ sender: Any) {
        presenterImpl.tappedContinueButton()
    }

    // MARK: Config

    func config(mode: ListingBasePresenter.ListingBaseState, specialMode: LoginPresenter.LoginSpecialState) {

        ViewUtils.showLoading(doShow: mode == .fetching || (mode == .special && specialMode == .validatingWords),
                              view: self)
    }

    func changeButtonState(isEnabled: Bool) {

        buttonContinue.isEnabled = isEnabled
        buttonContinue.backgroundColor = isEnabled ? .appRed : .appGreyMid
    }

    func configFields(numbers: [Int]) {

        for index in 0 ..< textFields.count {
            let number = numbers.count > index ? (numbers[index] + 1) : -1
            let text: String = number != -1
                ? String(format: "login_input_title".localize(), number)
                : "login_input_title_empty".localize()
            textFields[index].config(title: text)
        }
    }

    func getTextFieldsTexts() -> [String] {

        var res: [String] = []
        textFields.forEach { textField in
            res.append(textField.textField.text?.trim() ?? "")
        }
        return res
    }

    // MARK: Screens

    func changeScreenToSuccess(action: SelectorAction) {

        let params = SuccessViewController.makeSeguedParams(title: "success_register_title".localize(),
                                                            subtitle: "success_register_subtitle".localize(),
                                                            buttonText: "success_register_button".localize(),
                                                            buttonAction: action)
        ViewControllerUtils.changeScreenSegued(caller: self, segue: "SuccessSegue", params: params)
    }

    func goToMainScreen() {
        MainNavViewController.openThisView()
    }
}

extension LoginViewController: TextFieldTitledViewDelegate {

    func textFieldShouldReturn(_ textField: UITextField) -> Bool {

        if textField == textField1.textField {
            textField2.textField.becomeFirstResponder()
        } else {
            textField.resignFirstResponder()
        }
        return true
    }

    func textFieldDidChange(for view: TextFieldTitledView, textField: UITextField, text: String?) {
        presenterImpl.textFieldTextChanged()
        textFieldUrlChanged(view)
    }
}

// Delete me in the future, URL config server
extension LoginViewController {

    func setupUrlTextField() {

        textFieldUrl.isHidden = Env.isProduction()
        textFieldUrl.config(delegate: self)
        textFieldUrl.textField.autocapitalizationType = .none
        textFieldUrl.textField.autocorrectionType = .no
        textFieldUrl.textField.returnKeyType = .done
        textFieldUrl.config(title: "SERVER URL")
        textFieldUrl.textField.text = Common.URL_API
    }

    func textFieldUrlChanged(_ view: TextFieldTitledView) {

        if view != textFieldUrl {
            return
        }
        Common.URL_API = textFieldUrl.textField.text ?? "develop.atalaprism.io:50051"
        Logger.d("Changed URL to: \(Common.URL_API)")
    }
}
