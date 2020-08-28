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

    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        setupEmptyView()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        self.presenterImpl.actionPullToRefresh()
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

        viewEmpty.config(imageNamed: "img_qr_red", title: "connections_empty_title".localize(),
                         subtitle: "connections_empty_subtitle".localize(),
                         buttonText: "connections_empty_button".localize(), buttonAction: actionScan)
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
        let attributes: [NSAttributedString.Key: Any] = [
        .font: UIFont.systemFont(ofSize: 12),
        .foregroundColor: UIColor.appGrey,
        .underlineStyle: NSUnderlineStyle.single.rawValue]
        let attributeString = (!isScanningQr && mode != .fetching && !Env.isProduction())
            ? NSAttributedString(string: "connections_add_new".localize(), attributes: attributes)
            : nil
        let navIconName = (!isEmpty && !isScanningQr && mode != .fetching) ? "ico_qr" : nil
        navBar = NavBarCustomStyle(hasNavBar: true, title: navTitle, hasBackButton: isScanningQr,
                                   rightIconName: navIconName, rightIconAction: actionScan,
                                   textButtonTitle: attributeString, textButtonAction: actionInput)
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

    lazy var actionInput = SelectorAction(action: { [weak self] in
        self?.presenterImpl.tappedAddNewnButton()
    })

    func showManualInput() {

        let confirmation = ConnectionAddManualViewController.makeThisView()
        confirmation.config { token in
            self.presenterImpl.scannedQrCode(token)
        }
        self.customPresentViewController(confirmation.presentr, viewController: confirmation, animated: true)
    }

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

        let confirmMessage = ConnectionConfirmViewController.makeThisView()
        customPresentViewController(confirmMessage.presentr, viewController: confirmMessage, animated: true)
        confirmMessage.config(delegate: presenterImpl, lead: "connections_scan_qr_confirm_title".localize(),
                              title: title, logoData: logoData, placeholderNamed: "ico_placeholder_credential")
    }

    func showNewProofRequestMessage(credentials: [Credential], requiered: [String], contact: Contact,
                                    logoData: Data?) {

        let confirmProofRequest = ConnectionProofRequestViewController.makeThisView()
        customPresentViewController(confirmProofRequest.presentr, viewController: confirmProofRequest,
                                    animated: true)
        confirmProofRequest.config(delegate: presenterImpl, contact: contact, credentials: credentials,
                                   requiered: requiered, logoData: logoData,
                                   placeholderNamed: "ico_placeholder_university")
    }

    // MARK: Delete

    func showDeleteContactConfirmation(contact: Contact, credentials: [Credential]?) {
        let confirmation = DeleteContactViewController.makeThisView()
        confirmation.config(contact: contact, credentials: credentials) {
            self.presenterImpl.deleteContact(contact: contact, credentials: credentials)
        }
        customPresentViewController(confirmation.presentr, viewController: confirmation, animated: true)

    }
}
