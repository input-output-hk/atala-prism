//
import Presentr

class ConnectionsViewController: ListingBaseViewController {

    var presenterImpl = ConnectionsPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    // Views
    @IBOutlet weak var viewEmpty: InformationView!
    @IBOutlet weak var viewScanQr: UIView!
    @IBOutlet weak var viewTable: UIView!
    // Tabs
    @IBOutlet weak var viewTabUniversities: UIView!
    @IBOutlet weak var labelTabUniversities: UILabel!
    @IBOutlet weak var viewTabDecoratorUniversities: UIView!
    @IBOutlet weak var viewTabEmployers: UIView!
    @IBOutlet weak var labelTabEmployers: UILabel!
    @IBOutlet weak var viewTabDecoratorEmployers: UIView!
    // Scan QR
    @IBOutlet weak var viewQrScannerContainer: UIView!
    let scanner = QRCode()

    var navBar: NavBarCustomStyle = NavBarCustomStyle(hasNavBar: true)
    override func navBarCustomStyle() -> NavBarCustomStyle {
        return navBar
    }

    lazy var confirmMessageViewController: ConnectionConfirmViewController = {
        ConnectionConfirmViewController.makeThisView()
    }()

    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        setupButtons()
        setupEmptyView()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()

        // Setup (views thar require others to be resized first)
        setupQrScanner()
    }

    @discardableResult
    override func onBackPressed() -> Bool {
        if !presenterImpl.tappedBackButton() {
            return super.onBackPressed()
        }
        return false
    }

    // MARK: Config

    func setupEmptyView() {

        viewEmpty.config(imageNamed: "img_qr_red", title: "connections_empty_title".localize(), subtitle: "connections_empty_subtitle".localize(), buttonText: "connections_empty_button".localize(), buttonAction: actionScan)
    }

    override func config(mode: ListingBasePresenter.ListingBaseState) {

        let isScanningQr = presenterImpl.isScanningQr()
        let isEmpty = !presenterImpl.hasData() && mode == .listing
        let isUniversitiesMode = presenterImpl.getTabMode() == .universities

        // Tabs
        labelTabUniversities.textColor = isUniversitiesMode ? UIColor.appRed : UIColor.appGreySub
        labelTabEmployers.textColor = !isUniversitiesMode ? UIColor.appRed : UIColor.appGreySub
        viewTabDecoratorUniversities.isHidden = !isUniversitiesMode
        viewTabDecoratorEmployers.isHidden = isUniversitiesMode

        // Main views
        viewEmpty.isHidden = !isEmpty
        viewScanQr.isHidden = !isScanningQr
        viewTable.isHidden = isEmpty || isScanningQr

        // Change the nav bar
        let navTitle = isScanningQr ? "connections_scan_qr_nav_title".localize() : "connections_nav_title".localize()
        let navIconName = (!isEmpty && !isScanningQr) ? "ico_qr" : nil
        navBar = NavBarCustomStyle(hasNavBar: true, title: navTitle, hasBackButton: isScanningQr, rightIconName: navIconName, rightIconAction: actionScan)
        NavBarCustom.config(view: self)
    }

    func config(isLoading: Bool) {

        showLoading(doShow: isLoading)
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
        case .main:
            return "main"
        default:
            return super.getCellIdentifier(for: indexPath)
        }
    }

    override func getCellNib(for indexPath: IndexPath) -> String? {

        switch presenterImpl.getElementType(indexPath: indexPath) {
        case .main:
            return ConnectionMainViewCell.default_NibName()
        default:
            return super.getCellNib(for: indexPath)
        }
    }

    // MARK: Buttons

    func setupButtons() {
        viewTabUniversities.addOnClickListener(action: actionTabUniversities)
        viewTabEmployers.addOnClickListener(action: actionTabEmployers)
        labelTabUniversities.addOnClickListener(action: actionTabUniversities)
        labelTabEmployers.addOnClickListener(action: actionTabEmployers)
    }

    lazy var actionScan = SelectorAction(action: { [weak self] in
        self?.presenterImpl.tappedScanButton()
    })

    lazy var actionTabEmployers = SelectorAction(action: { [weak self] in
        self?.presenterImpl.tappedTabEmployers()
    })

    lazy var actionTabUniversities = SelectorAction(action: { [weak self] in
        self?.presenterImpl.tappedTabUniversities()
    })

    // MARK: Scan QR

    func setupQrScanner() {

        scanner.prepareScan(viewQrScannerContainer) { (stringValue) -> Void in
            self.presenterImpl.scannedQrCode(stringValue)
        }
        scanner.scanFrame = viewQrScannerContainer.frame
        scanner.autoRemoveSubLayers = true
        scanner.lineWidth = 0
        scanner.strokeColor = UIColor.appRed
        scanner.maxDetectedCount = 1
    }

    func startQrScan() {
        scanner.clearDrawLayer()
        scanner.startScan()
    }

    func stopQrScan() {
        scanner.stopScan()
    }

    func showNewConnectMessage(type: Int, title: String?, logoData: Data?) {

        let lead = (type == 0 ? "connections_scan_qr_confirm_title_type_0" : "connections_scan_qr_confirm_title_type_1").localize()
        let placeholder = type == 0 ? "ico_placeholder_university" : "ico_placeholder_employer"

        if !confirmMessageViewController.isBeingPresented {
            customPresentViewController(confirmMessageViewController.presentr, viewController: confirmMessageViewController, animated: true)
        }
        confirmMessageViewController.config(delegate: presenterImpl, lead: lead, title: title, logoData: logoData, placeholderNamed: placeholder)
    }
}
