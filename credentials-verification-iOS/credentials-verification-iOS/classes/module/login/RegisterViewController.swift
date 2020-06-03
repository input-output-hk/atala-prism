//

class RegisterViewController: BaseViewController, RegisterSeedViewDelegate, SwitchCustomDelegate {

    var presenterImpl = RegisterPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    @IBOutlet weak var stack: UIStackView!
    @IBOutlet weak var switchLegal: SwitchCustomView!
    @IBOutlet weak var buttonContinue: UIButton!

    var viewSeeds: [RegisterSeedView] = []

    override func navBarCustomStyle() -> NavBarCustomStyle {
        return NavBarCustomStyle(hasNavBar: true, title: nil, hasBackButton: true)
    }

    static func openThisView(_ caller: UIViewController?) {

        ViewControllerUtils.changeScreenPresented(caller: caller, storyboardName: "Register", viewControllerIdentif: "Register")
        caller?.navigationController?.popViewController(animated: true)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        setupSwitchs()
        setupButtons()
        setupSeeds()
    }

    // MARK: Setup

    func setupSwitchs() {

        switchLegal.delegate = self
    }

    func setupButtons() {

        buttonContinue.layer.cornerRadius = AppConfigs.CORNER_RADIUS_BUTTON
    }

    func setupSeeds() {

        let stackRows = stack.subviews
        let elementsPerRow = CryptoUtils.SEED_COUNT / stackRows.count
        for indexColumn in 0 ..< stackRows.count {
            let stackRow = stackRows[indexColumn] as! UIStackView
            stackRow.arrangedSubviews.forEach { $0.removeFromSuperview() }
            for indexRow in 0 ..< elementsPerRow {
                let seed = RegisterSeedView()
                let index = indexColumn * elementsPerRow + indexRow
                let text = String(format: "register_seed_text".localize(), index + 1, " - ")
                seed.config(delegate: self, index: index, text: text)
                stackRow.addArrangedSubview(seed)
                viewSeeds.append(seed)
            }
        }
    }

    // MARK: Buttons

    @IBAction func actionContinueButton(_ sender: Any) {
        presenterImpl.tappedContinueButton()
    }

    func stateChanged(for view: SwitchCustomView, newState: Bool) {
        presenterImpl.tappedLegalSwitch(newState: newState)
    }

    // MARK: Config

    func configSeed(at index: Int, word: String?) {

        let text = String(format: "register_seed_text".localize(), index + 1, word ?? "")
        viewSeeds[index].config(text: text)
    }

    func changeButtonState(isEnabled: Bool) {

        buttonContinue.isEnabled = isEnabled
        buttonContinue.backgroundColor = isEnabled ? .appRed : .appGreyMid
    }

    func config(mode: ListingBasePresenter.ListingBaseState) {

        ViewUtils.showLoading(doShow: mode == .fetching, view: self)
    }

    // MARK: Screens

    func changeScreenToLogin() {
        _ = app_mayPerformSegue(withIdentifier: "LoginSegue", sender: self)
    }
}
