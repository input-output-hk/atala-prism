//

class SettingsViewController: ListingBaseViewController {

    var presenterImpl = SettingsPresenter()
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

        let settingsMode = presenterImpl.getMode()
        let isEmpty = !presenterImpl.hasData() && mode == .listing

        // Main views
        viewEmpty.isHidden = !isEmpty
        viewTable.isHidden = isEmpty

        var navTitle = ""
        var hasBgSpecial = false

        switch settingsMode {
        case .initial:
            hasBgSpecial = true
            navTitle = "settings_nav_title".localize()
        }

        // Navigation bar
        navBar = NavBarCustomStyle(hasNavBar: true, isWhite: false, title: navTitle, hasBackButton: settingsMode != .initial)
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
        default:
            return super.getCellIdentifier(for: indexPath)
        }
    }

    override func getCellNib(for indexPath: IndexPath) -> String? {

        switch presenterImpl.getElementType(indexPath: indexPath) {
        case .initial:
            return CommonViewCell.default_NibName()
        default:
            return super.getCellNib(for: indexPath)
        }
    }

    // MARK: Buttons

    func setupButtons() {}

    // MARK: Screens

    func changeScreenToBrowser(urlStr: String) {

        var params: [Any?] = []
        params.append("settings_support_title".localize())
        params.append(urlStr)
        ViewControllerUtils.changeScreenSegued(caller: self, segue: "BrowserSegue", params: params)
    }
    
    func changeScreenToAbout() {
        ViewControllerUtils.changeScreenSegued(caller: self, segue: "AboutSegue", params: nil)
        
    }
    
    func changeScreenToSecurity() {
        ViewControllerUtils.changeScreenSegued(caller: self, segue: "SecuritySegue", params: nil)
        
    }
}
