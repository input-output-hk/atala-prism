//

class ProfileViewController: ListingBaseViewController {

    var presenterImpl = ProfilePresenter()
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
        ViewControllerUtils.addTapToDismissKeyboard(view: self)
        ViewControllerUtils.addShiftKeyboardListeners(view: self)
    }

    override func onBackPressed() -> Bool {
        if !presenterImpl.tappedBackButton() {
            return super.onBackPressed()
        }
        return false
    }

    // MARK: Config

    override func config(mode: ListingBasePresenter.ListingBaseState) {

        let profileMode = presenterImpl.getMode()
        let isEmpty = !presenterImpl.hasData() && mode == .listing

        // Main views
        viewEmpty.isHidden = !isEmpty
        viewTable.isHidden = isEmpty

        var navTitle = ""
        var navTitleIcon: String?
        var navTitleIconAction: SelectorAction?
        var hasBgSpecial = false

        switch profileMode {
        case .initial:
            hasBgSpecial = true
            navTitle = "profile_nav_title".localize()
            navTitleIcon = "ico_pencil"
            navTitleIconAction = actionEdit
        case .editing:
            hasBgSpecial = true
            navTitle = "profile_edit_nav_title".localize()
            navTitleIcon = "ico_check"
            navTitleIconAction = actionEditSave
        }

        // Navigation bar
        navBar = NavBarCustomStyle(hasNavBar: true, isWhite: hasBgSpecial, title: navTitle,
                                   hasBackButton: profileMode != .initial, rightIconName: navTitleIcon,
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
        case .profile:
            return "profile"
        case .tabs:
            return "tabs"
        case .field:
            return "field"
        case .footer:
            return "padding"
        default:
            return super.getCellIdentifier(for: indexPath)
        }
    }

    override func getCellNib(for indexPath: IndexPath) -> String? {

        switch presenterImpl.getElementType(indexPath: indexPath) {
        case .profile:
            return ProfileViewCell.default_NibName()
        case .tabs:
            return TabsViewCell.default_NibName()
        case .field:
            return FieldViewCell.default_NibName()
        case .footer:
            return PaddingViewCell.default_NibName()
        default:
            return super.getCellNib(for: indexPath)
        }
    }

    // MARK: Buttons

    func setupButtons() {}

    lazy var actionEdit = SelectorAction(action: { [weak self] in
        self?.presenterImpl.tappedEditButton()
    })

    lazy var actionEditSave = SelectorAction(action: { [weak self] in
        self?.presenterImpl.tappedEditSaveButton()
    })

    // MARK: TextField

    override func getScrollableMainView() -> UIScrollView? {
        return table
    }
}
