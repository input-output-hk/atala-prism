//

class WalletViewController: ListingBaseViewController {

    var presenterImpl = WalletPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    @IBOutlet weak var viewEmpty: InformationView!
    @IBOutlet weak var viewTable: UIView!
    @IBOutlet weak var viewBgSpecial: UIView!

    var navBar: NavBarCustomStyle = NavBarCustomStyle(hasNavBar: true)
    override func navBarCustomStyle() -> NavBarCustomStyle {
        return navBar
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        setupButtons()
    }

    override func onBackPressed() -> Bool {
        if !presenterImpl.tappedBackButton() {
            return super.onBackPressed()
        }
        return false
    }

    // MARK: Config

    override func config(mode: ListingBasePresenter.ListingBaseState) {

        let walletMode = presenterImpl.getMode()
        let isEmpty = !presenterImpl.hasData() && mode == .listing

        // Main views
        viewEmpty.isHidden = !isEmpty
        viewTable.isHidden = isEmpty

        var navTitle = ""
        var navTitleIcon: String?
        var navTitleIconAction: SelectorAction?
        var hasBgSpecial = false

        switch walletMode {
        case .initial:
            hasBgSpecial = true
            navTitle = "wallet_initial_nav_title".localize()
            navTitleIcon = "ico_history"
            navTitleIconAction = actionHistory
        case .history:
            navTitle = "wallet_history_nav_title".localize()
            viewEmpty.config(imageNamed: "img_credit_card", title: "wallet_history_empty_title".localize(),
                             subtitle: "wallet_history_empty_subtitle".localize(),
                             buttonText: "wallet_history_empty_button".localize(),
                             buttonAction: actionHistoryEmpty)
        case .creditCards:
            navTitle = "wallet_initial_nav_title".localize()
            viewEmpty.config(imageNamed: "img_credit_card", title: "wallet_cards_empty_title".localize(),
                             subtitle: "wallet_cards_empty_subtitle".localize(),
                             buttonText: "wallet_cards_empty_button".localize(),
                             buttonAction: actionCardsEmpty)
        }

        // Navigation bar
        navBar = NavBarCustomStyle(hasNavBar: true, isWhite: hasBgSpecial, title: navTitle,
                                   hasBackButton: walletMode != .initial, rightIconName: navTitleIcon,
                                   rightIconAction: navTitleIconAction)
        NavBarCustom.config(view: self)
        // Special background
        viewBgSpecial.isHidden = !hasBgSpecial
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
        case .initial:
            return "common"
        case .history:
            return "history"
        case .cards:
            return "cards"
        default:
            return super.getCellIdentifier(for: indexPath)
        }
    }

    override func getCellNib(for indexPath: IndexPath) -> String? {

        switch presenterImpl.getElementType(indexPath: indexPath) {
        case .initial:
            return CommonViewCell.default_NibName()
        case .history:
            return HistoryViewCell.default_NibName()
        case .cards:
            return NewDegreeViewCell.default_NibName()
        default:
            return super.getCellNib(for: indexPath)
        }
    }

    // MARK: Buttons

    func setupButtons() {}

    lazy var actionHistory = SelectorAction(action: { [weak self] in
        self?.presenterImpl.tappedHistoryButton()
    })

    lazy var actionHistoryEmpty = SelectorAction(action: { [weak self] in
        self?.presenterImpl.tappedHistoryEmptyButton()
    })

    lazy var actionCardsEmpty = SelectorAction(action: { [weak self] in
        self?.presenterImpl.tappedCardsEmptyButton()
    })
}
