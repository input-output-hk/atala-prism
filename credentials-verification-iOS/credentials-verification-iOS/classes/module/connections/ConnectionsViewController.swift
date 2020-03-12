//
import Presentr

class ConnectionsViewController: ListingBaseViewController {

    var presenterImpl = ConnectionsPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    // Views
    @IBOutlet weak var viewEmpty: InformationView!
    @IBOutlet weak var viewScanQr: UIView!
    @IBOutlet weak var viewTable: UIView!
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

    lazy var actionScan = SelectorAction(action: { [weak self] in
        self?.presenterImpl.tappedScanButton()
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

        let lead = "connections_scan_qr_confirm_title".localize()
        let placeholder = type != 0 ? "ico_placeholder_university" : "ico_placeholder_credential"

        if !confirmMessageViewController.isBeingPresented {
            customPresentViewController(confirmMessageViewController.presentr, viewController: confirmMessageViewController, animated: true)
        }
        confirmMessageViewController.config(delegate: presenterImpl, lead: lead, title: title, logoData: logoData, placeholderNamed: placeholder)
    }
}
