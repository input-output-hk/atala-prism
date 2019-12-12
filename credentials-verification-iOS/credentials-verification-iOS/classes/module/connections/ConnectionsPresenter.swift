//

class ConnectionsPresenter: ListingBasePresenter, ListingBaseTableUtilsPresenterDelegate, ConnectionMainViewCellPresenterDelegate, ConnectionConfirmPresenterDelegate {

    var viewImpl: ConnectionsViewController? {
        return view as? ConnectionsViewController
    }

    enum ConnectionsSpecialState {
        case none
        case scanningQr
    }

    enum ConnectionsTabMode {
        case universities
        case employers
    }

    enum ConnectionsCellType {
        case base(value: ListingBaseCellType)
        case main
    }

    var stateSpecial: ConnectionsSpecialState = .none
    var tabMode: ConnectionsTabMode = .universities

    var universities: [University]?
    var employers: [Employer]?

    var connectionRequest: ConnectionRequest?

    // MARK: Modes

    func isScanningQr() -> Bool {
        return self.state == .special && self.stateSpecial == .scanningQr
    }

    func getTabMode() -> ConnectionsTabMode {
        return tabMode
    }

    // MARK: Buttons

    func tappedScanButton() {
        startQrScanning()
    }

    func tappedTabUniversities() {

        tabMode = .universities
        updateViewToState()
        startFetchOrListing()
    }

    func tappedTabEmployers() {

        tabMode = .employers
        updateViewToState()
        startFetchOrListing()
    }

    @discardableResult
    func tappedBackButton() -> Bool {

        if isScanningQr() {
            stopQrScanning()
            return true
        }
        return false
    }

    func startQrScanning() {

        state = .special
        stateSpecial = .scanningQr
        updateViewToState()
        viewImpl?.startQrScan()
    }

    func stopQrScanning() {

        stateSpecial = .none
        viewImpl?.stopQrScan()
        startFetching()
    }

    // MARK: ListingBaseTableUtilsPresenterDelegate

    func cleanData() {
        universities = []
        employers = []
    }

    func fetchData() {

        state = .fetching
        fetchingQueue = 1

        fetchElements()
    }

    func hasData() -> Bool {
        return ((universities?.size() ?? 0) + (employers?.size() ?? 0)) > 0
    }

    func getElementCount() -> Int {
        if let baseValue = super.getBaseElementCount() {
            return baseValue
        }

        switch tabMode {
        case .universities:
            return (universities?.size() ?? 0)
        case .employers:
            return (employers?.size() ?? 0)
        }
    }

    func getElementType(indexPath: IndexPath) -> ConnectionsCellType {
        if let baseValue = super.getBaseElementType(indexPath: indexPath) {
            return .base(value: baseValue)
        }

        return .main
    }

    // MARK: Fetch

    var showAsEmpty_DELETE_ME = false

    func fetchElements() {
        // TODO: Call the services

        // TODO: Delete me when services are ready
        DispatchQueue.global(qos: .background).async {
            print("This is run on the background queue")

            sleep(1)

            self.cleanData()

            // Toggles empty view and loaded view
            self.showAsEmpty_DELETE_ME = !self.showAsEmpty_DELETE_ME
            if self.showAsEmpty_DELETE_ME {
                self.universities?.append(contentsOf: FakeData.universitiesList())
                self.employers?.append(contentsOf: FakeData.employersList())
            }

            DispatchQueue.main.async {
                self.startListing()
            }
        }
    }

    func validateQrCode(_ str: String) {

        self.viewImpl?.config(isLoading: true)

        // TODO: Call the services

        // TODO: Delete me when services are ready
        DispatchQueue.global(qos: .background).async {
            print("This is run on the background queue")

            sleep(1)

            let conn = FakeData.qrIsValid(code: str)

            DispatchQueue.main.async {
                // self.startListing()

                self.viewImpl?.config(isLoading: false)

                if conn != nil {
                    self.connectionRequest = conn!
                    self.viewImpl?.showNewConnectMessage(type: conn!.type!, title: conn!.name, logoUrl: conn!.logoUrl)
                } else {
                    self.viewImpl?.showErrorMessage(doShow: true, message: "connections_scan_qr_error".localize(), afterErrorAction: {
                        self.tappedBackButton()
                    })
                }
            }
        }
    }

    func confirmQrCode() {

        self.viewImpl?.config(isLoading: true)

        // TODO: Call the services

        // TODO: Delete me when services are ready
        DispatchQueue.global(qos: .background).async {
            print("This is run on the background queue")

            sleep(1)

            let result = FakeData.confirmQrCode(conn: self.connectionRequest!)

            DispatchQueue.main.async {

                self.viewImpl?.config(isLoading: false)

                if result {
                    // Success
                    self.tappedBackButton()
                } else {
                    // Fail
                    self.viewImpl?.showErrorMessage(doShow: true, message: "connections_scan_qr_confirm_error".localize(), afterErrorAction: {
                        self.tappedBackButton()
                    })
                }
            }
        }
    }

    // MARK: Table

    func setup(for cell: ConnectionMainViewCell) {

        if tabMode == .universities {
            let university: University = universities![cell.indexPath!.row]
            cell.config(title: university.name, isUniversity: true, logoUrl: university.logoUrl)
        } else {
            let employer = employers![cell.indexPath!.row]
            cell.config(title: employer.name, isUniversity: false, logoUrl: employer.logoUrl)
        }
    }

    func tappedAction(for cell: ConnectionMainViewCell) {

        var urlStr: String?
        if tabMode == .universities {
            let university: University = universities![cell.indexPath!.row]
            urlStr = university.url
        } else {
            let employer = employers![cell.indexPath!.row]
            urlStr = employer.url
        }
        guard let urlStrFn = urlStr, let url = URL(string: urlStrFn) else { return }
        UIApplication.shared.open(url)
    }

    func hasPullToRefresh() -> Bool {
        true
    }

    func actionPullToRefresh() {

        self.fetchData()
        self.updateViewToState()
    }

    // MARK: QR Reader

    func scannedQrCode(_ str: String) {
        Logger.d("Scanned: \(str)")
        validateQrCode(str)
    }

    func tappedDeclineAction(for: ConnectionConfirmViewController) {
        self.viewImpl?.startQrScan()
    }

    func tappedConfirmAction(for: ConnectionConfirmViewController) {
        confirmQrCode()
    }
}
