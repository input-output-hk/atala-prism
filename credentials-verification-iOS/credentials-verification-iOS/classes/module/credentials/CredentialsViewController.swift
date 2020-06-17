//

class CredentialsViewController: ListingBaseViewController {

    var presenterImpl = CredentialsPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    @IBOutlet weak var viewEmpty: InformationView!
    @IBOutlet weak var viewTable: UIView!

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

    func setupEmptyView() {

        viewEmpty.config(imageNamed: "img_notifications_tray", title: "connections_empty_title".localize(),
                         subtitle: "connections_empty_subtitle".localize(), buttonText: nil, buttonAction: nil)
    }

    override func config(mode: ListingBasePresenter.ListingBaseState) {

        let credentialsMode = presenterImpl.getMode()
        let isEmpty = !presenterImpl.hasData() && mode == .listing

        // Main views
        viewEmpty.isHidden = !isEmpty
        viewTable.isHidden = isEmpty

        // Change the nav bar
        var navTitle = credentialsMode == .degrees
            ? "credentials_nav_title".localize()
            : "credentials_document_title".localize()
        var navAction: SelectorAction?
        var navActionIcon: String?
        if credentialsMode == .detail {
            let detailDegree = presenterImpl.detailDegree
            if detailDegree?.isNew ?? false {
                navTitle = "credentials_detail_title_new".localize()
            } else {
                navActionIcon = "ico_share"
                navAction = actionShare
                switch detailDegree!.type {
                case .univerityDegree:
                    navTitle = "credentials_detail_title_type_university".localize()
                case .governmentIssuedId:
                    navTitle = "credentials_detail_title_type_government_id".localize()
                case .certificatOfInsurance:
                    navTitle = "credentials_detail_title_type_insurance".localize()
                case .proofOfEmployment:
                    navTitle = "credentials_detail_title_type_employment".localize()
                default:
                    print("Unrecognized type")
                }
            }

        }
        navBar = NavBarCustomStyle(hasNavBar: true, title: navTitle,
                                   hasBackButton: credentialsMode != .degrees, rightIconName: navActionIcon,
                                   rightIconAction: navAction)
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
        case .newDegreeHeader:
            return "newDegreeHeader"
        case .newDegree:
            return "newDegree"
        case .document:
            return "document"
        case .detailHeader:
            return "detailHeader"
        case .detailProperty:
            return "detailProperty"
        case .detailFooter:
            return "detailFooter"
        default:
            return super.getCellIdentifier(for: indexPath)
        }
    }

    override func getCellNib(for indexPath: IndexPath) -> String? {

        switch presenterImpl.getElementType(indexPath: indexPath) {
        case .degree:
            return DegreeViewCell.default_NibName()
        case .newDegreeHeader:
            return NewDegreeHeaderViewCell.default_NibName()
        case .newDegree:
            return NewDegreeViewCell.default_NibName()
        case .document:
            return DocumentViewCell.default_NibName()
        case .detailHeader:
            return DetailHeaderViewCell.default_NibName()
        case .detailProperty:
            return DetailPropertyViewCell.default_NibName()
        case .detailFooter:
            return DetailFooterViewCell.default_NibName()
        default:
            return super.getCellNib(for: indexPath)
        }
    }

    // MARK: Buttons

    func setupButtons() {}

    lazy var actionShare = SelectorAction(action: { [weak self] in
        self?.presenterImpl.tappedShareButton()
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
}
