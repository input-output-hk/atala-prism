//

class CredentialsViewController: ListingBaseViewController {

    var presenterImpl = CredentialsPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    @IBOutlet weak var viewEmpty: InformationView!
    @IBOutlet weak var viewTable: UIView!
    @IBOutlet weak var searchBar: UISearchBar!
    @IBOutlet weak var tableTopMarginCtrt: NSLayoutConstraint!
    @IBOutlet weak var viewTableDivider: UIView!
    @IBOutlet weak var viewDetail: CredentialDetailView!

    var navBar: NavBarCustomStyle = NavBarCustomStyle(hasNavBar: true)
    override func navBarCustomStyle() -> NavBarCustomStyle {
        return navBar
    }

    lazy var shareDialogViewController: ShareDialogViewController = {
        ShareDialogViewController.makeThisView()
    }()

    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        setupButtons()
        setupEmptyView()
        setupSearchBar()

        ViewControllerUtils.addTapToDismissKeyboard(view: self)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        self.presenterImpl.actionPullToRefresh()
    }

    @discardableResult
    override func onBackPressed() -> Bool {
        if !presenterImpl.tappedBackButton() {
            return super.onBackPressed()
        }
        return false
    }

    // MARK: Config

    func setupSearchBar() {

        searchBar.backgroundColor = .appWhite
        searchBar.setBackgroundImage(UIImage(), for: .any, barMetrics: .default)
        searchBar.isTranslucent = true
        searchBar.searchTextField.addRoundCorners(radius: 6, borderWidth: 1, borderColor: UIColor.appGreyMid.cgColor)
        searchBar.searchTextField.backgroundColor = .appWhite
        searchBar.placeholder = "credentials_search".localize()
        searchBar.delegate = presenterImpl
    }

    func setupEmptyView() {

        viewEmpty.config(imageNamed: "img_notifications_tray", title: "connections_empty_title".localize(),
                         subtitle: "connections_empty_subtitle".localize(), buttonText: nil, buttonAction: nil)
    }

    override func config(mode: ListingBasePresenter.ListingBaseState) {

        let credentialsMode = presenterImpl.getMode()
        let isEmpty = !presenterImpl.hasData() && mode == .listing
        let isDetail = credentialsMode == .detail
        let isHistory = credentialsMode == .history

        // Main views
        viewEmpty.isHidden = !isEmpty
        viewTable.isHidden = isEmpty || isDetail
        viewDetail.isHidden = !isDetail

        // Search Bar
        searchBar.isHidden = isHistory
        tableTopMarginCtrt.constant = isHistory ? -56 : 0
        viewTableDivider.isHidden = !isHistory

        // Change the nav bar
        var navTitle = "credentials_nav_title".localize()
        var navAction: SelectorAction?
        var navActionIcon: String?
        var deleteAction: SelectorAction?
        var deleteActionIcon: String?

        var historyAction: SelectorAction?
        var historyActionIcon: String?
        if let detailCredential = presenterImpl.detailCredential {
            if isDetail {
                deleteActionIcon = "ico_delete"
                deleteAction = actionDelete
                navActionIcon = "ico_share"
                navAction = actionShare
                historyActionIcon = "ico_history"
                historyAction = actionHistory
                navTitle = detailCredential.credentialName
                viewDetail.config(credential: detailCredential)
            } else if isHistory {
                navTitle = String(format: "credentials_history_title".localize(), detailCredential.credentialName)
            } else {
                viewDetail.clearWebView()
            }

        }
        navBar = NavBarCustomStyle(hasNavBar: true, title: navTitle, hasBackButton: credentialsMode != .degrees,
                                   rightIconName: navActionIcon, rightIconAction: navAction,
                                   centerIconName: deleteActionIcon, centerIconAction: deleteAction,
                                   leftIconName: historyActionIcon, leftIconAction: historyAction)
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
        case .degree:
            return "common"
        case .noResults:
            return "noResults"
        case .historyHeader:
            return "historyHeader"
        case .historyShared:
            return "historyShared"
        case .historySection:
            return "historySection"
        default:
            return super.getCellIdentifier(for: indexPath)
        }
    }

    override func getCellNib(for indexPath: IndexPath) -> String? {

        switch presenterImpl.getElementType(indexPath: indexPath) {
        case .degree:
            return DegreeViewCell.default_NibName()
        case .noResults:
            return NoResultsViewCell.default_NibName()
        case .historyHeader:
            return CredentialHistoryHeaderViewCell.default_NibName()
        case .historyShared:
            return CredentialSharedViewCellViewCell.default_NibName()
        case .historySection:
            return CredentialSectionViewCell.default_NibName()
        default:
            return super.getCellNib(for: indexPath)
        }
    }

    // MARK: Buttons

    func setupButtons() {}

    lazy var actionShare = SelectorAction(action: { [weak self] in
        self?.presenterImpl.tappedShareButton()
    })

    lazy var actionDelete = SelectorAction(action: { [weak self] in
        self?.presenterImpl.tappedDeleteButton()
    })

    lazy var actionHistory = SelectorAction(action: { [weak self] in
        self?.presenterImpl.tappedHistoryButton()
    })

    // MARK: Share

    func showShareDialog() {

        if !shareDialogViewController.isBeingPresented {
            customPresentViewController(shareDialogViewController.presentr,
                                        viewController: shareDialogViewController, animated: true)
        }
        shareDialogViewController.config(delegate: presenterImpl, parentVc: self)
        configShareDialog(enableButton: false)
    }

    func configShareDialog(enableButton: Bool) {
        shareDialogViewController.config(enableButton: enableButton)
    }

    func changeScreenToPayment(degree: Degree?) {

        let params = CredentialPayViewController.makeSeguedParams(degree: degree)
        ViewControllerUtils.changeScreenSegued(caller: self, segue: "CredentialPaySegue", params: params)
    }

    // MARK: Delete

    func showDeleteCredentialConfirmation() {
        let confirmation = DeleteCredentialViewController.makeThisView()
        confirmation.config(credential: presenterImpl.detailCredential) {
            self.presenterImpl.deleteCredential()
        }
        customPresentViewController(confirmation.presentr, viewController: confirmation, animated: true)

    }
}
