//

class ListingBaseViewController: BaseViewController, TableUtilsViewDelegate {

    private var presenterImpl: ListingBasePresenter? {
        return presenter as? ListingBasePresenter
    }

    // Table
    var tableUtils: TableUtils?
    @IBOutlet weak var table: UITableView!

    // MARK: Lifecycle

    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        setupTable()
    }

    // MARK: Configs

    func config(mode: ListingBasePresenter.ListingBaseState) {}

    // MARK: Table

    func setupTable() {

        tableUtils = TableUtils(view: self, presenter: presenterImpl!.tableDelegate(), table: table)
    }

    func getHeaderHeight(for section: Int) -> CGFloat {
        return CGFloat.leastNormalMagnitude
    }

    // Note: Override with correct MainElement
    func getCellIdentifier(for indexPath: IndexPath) -> String {

        switch presenterImpl!.getBaseElementType(indexPath: indexPath)! {
        case .loading:
            return "listing_loading"
        case .error:
            return "error_retry"
        }
    }

    // Note: Override with correct MainElement
    func getCellNib(for indexPath: IndexPath) -> String? {

        switch presenterImpl!.getBaseElementType(indexPath: indexPath)! {
        case .loading:
            return ListingLoadingViewCell.default_NibName()
        case .error:
            return ListingErrorRetryViewCell.default_NibName()
        }
    }
}
