//

protocol ListingBaseTableUtilsPresenterDelegate: TableUtilsPresenterDelegate {

    func cleanData()
    func fetchData()
    func hasData() -> Bool
}

class ListingBasePresenter: BasePresenter, ListingErrorRetryViewCellPresenterDelegate,
                            ListingEmptyViewCellPresenterDelegate {

    enum ListingBaseState: Int {
        case fetching
        case listing
        case error
        case special
    }

    enum ListingBaseCellType: Int {
        case loading
        case error
    }

    private var viewImpl: ListingBaseViewController? {
        return view as? ListingBaseViewController
    }

    var state: ListingBaseState = .fetching
    var fetchingQueue: Int = 0
    var lastError: Error?

    func tableDelegate() -> ListingBaseTableUtilsPresenterDelegate? {
        return self as? ListingBaseTableUtilsPresenterDelegate
    }

    // MARK: Lifecycle

    override func viewWillAppear() {
        super.viewWillAppear()

        // Cant continue if there is no logged user
        if !isPublicScreen && !UserUtils.isLogged(sharedMemory) {
            return
        }

        updateViewToState()
        startFetchOrListing()
    }

    func updateViewToState() {

        NSObject.execOnMain(delaySecs: 0) {
            self.viewImpl?.tableUtils?.refreshTable()
            self.viewImpl?.config(mode: self.state)
        }
    }

    func startFetchOrListing(justFetched: Bool = false) {

        if justFetched {
            fetchingQueue -= 1
        }

        Logger.d("startFetchOrListing: Called")
        // If is ready, start listing
        if tableDelegate()?.hasData() ?? false {
            Logger.d("startFetchOrListing: Listing")
            startListing()
            return
        }

        // Otherwise, if its not already fetching, start fetching
        if fetchingQueue == 0 {
            Logger.d("startFetchOrListing: Fetching")
            startFetching()
            return
        }

        Logger.d("startFetchOrListing: Wont start either, is still fetching data")
    }

    func startFetching() {

        state = .fetching
        tableDelegate()?.fetchData()
        updateViewToState()
    }

    func startListing() {

        state = .listing
        updateViewToState()
    }

    func startShowError(error: Error?) {

        lastError = error
        fetchingQueue = -1
        state = .error
        updateViewToState()
    }

    // MARK: Table

    // Override
    func getBaseElementCount() -> Int? {

        let hasData = tableDelegate()?.hasData() ?? false
        if !hasData && state == .listing {
            return 0
        }
        if state == .fetching || state == .error {
            return 1
        }
        return nil
    }

    func getBaseElementType(indexPath: IndexPath) -> ListingBaseCellType? {

        if state == .fetching {
            return .loading
        }
        if state == .error {
            return .error
        }
        return nil
    }

    // MARK: ListingErrorRetryViewCellPresenterDelegate

    func setup(for cell: ListingErrorRetryViewCell) {}

    func errorRetryTapped(for cell: ListingErrorRetryViewCell) {

        tableDelegate()?.cleanData()
        startFetching()
    }

    // MARK: ListingEmptyViewCellPresenterDelegate

    func setup(for cell: ListingEmptyViewCell) {}

    func emptyRetryTapped(for cell: ListingEmptyViewCell) {

        tableDelegate()?.cleanData()
        startFetching()
    }
}
