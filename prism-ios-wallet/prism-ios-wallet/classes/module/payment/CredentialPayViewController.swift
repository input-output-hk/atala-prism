//

class CredentialPayViewController: ListingBaseViewController {

    var presenterImpl = CredentialPayPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    // Segued params
    var degreeToPay: Degree?
    // Views
    @IBOutlet weak var viewEmpty: InformationView!
    @IBOutlet weak var viewTable: UIView!

    var navBar: NavBarCustomStyle = NavBarCustomStyle(hasNavBar: true)
    override func navBarCustomStyle() -> NavBarCustomStyle {
        return navBar
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        setupButtons()
        ViewControllerUtils.addTapToDismissKeyboard(view: self)
        ViewControllerUtils.addShiftKeyboardListeners(view: self)
    }

    @discardableResult
    override func onBackPressed() -> Bool {
        if !presenterImpl.tappedBackButton() {
            return super.onBackPressed()
        }
        return false
    }

    // MARK: Config

    override func config(mode: ListingBasePresenter.ListingBaseState) {

        let isEmpty = !presenterImpl.hasData() && mode == .listing

        // Main views
        viewEmpty.isHidden = !isEmpty
        viewTable.isHidden = isEmpty

        let navTitle = "credentialpay_nav_title".localize()

        // Navigation bar
        navBar = NavBarCustomStyle(hasNavBar: true, isWhite: false, title: navTitle, hasBackButton: true)
        NavBarCustom.config(view: self)
    }

    // MARK: Table

    override func setupTable() {
        tableUtils = TableUtils(view: self, presenter: presenterImpl, table: table)
    }

    override func getHeaderHeight() -> CGFloat {
        return AppConfigs.TABLE_HEADER_HEIGHT_REGULAR
    }

    override func getCellIdentifier(for indexPath: IndexPath) -> String {

        switch presenterImpl.getElementType(indexPath: indexPath) {
        case .credentialPay:
            return "main"
        case .field:
            return "field"
        case .confirmation:
            return "confirmation"
        default:
            return super.getCellIdentifier(for: indexPath)
        }
    }

    override func getCellNib(for indexPath: IndexPath) -> String? {

        switch presenterImpl.getElementType(indexPath: indexPath) {
        case .credentialPay:
            return CredentialPayViewCell.default_NibName()
        case .field:
            return FieldViewCell.default_NibName()
        case .confirmation:
            return ConfirmationViewCell.default_NibName()
        default:
            return super.getCellNib(for: indexPath)
        }
    }

    // MARK: Buttons

    func setupButtons() {}

    // MARK: TextField

    override func getScrollableMainView() -> UIScrollView? {
        return table
    }

    // MARK: Screens

    func changeScreenToSuccess(action: SelectorAction) {

        let params = SuccessViewController.makeSeguedParams(title: "success_payment_title".localize(),
                                                            subtitle: "success_payment_subtitle".localize(),
                                                            buttonText: "success_payment_button".localize(),
                                                            buttonAction: action)
        ViewControllerUtils.changeScreenSegued(caller: self, segue: "SuccessSegue", params: params)
    }

    func closePaymentFlow() {
        // _ = navigationController?.popViewController(animated: true)
        _ = navigationController?.popToRootViewController(animated: true)
    }
}

extension CredentialPayViewController: SegueableScreen {

    func configScreenFromSegue(params: [Any?]?) {

        degreeToPay = params?[0] as? Degree
    }

    static func makeSeguedParams(degree: Degree?) -> [Any?]? {

        var params: [Any?] = []
        params.append(degree)
        return params
    }
}
