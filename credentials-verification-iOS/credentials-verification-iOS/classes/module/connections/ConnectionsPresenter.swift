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

        Tracker.global.trackScanQrTapped()
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

    func fetchElements() {

        // Call the service
        ApiService.call(async: {
            do {
                let user = self.sharedMemory.loggedUser
                let responses = try ApiService.global.getConnections(userIds: user?.connectionUserIds?.valuesArray)
                Logger.d("getConnections responsed: \(responses)")

                self.cleanData()

                // Parse data
                let parsedResponse = ConnectionMaker.parseResponseList(responses)
                self.universities?.append(contentsOf: parsedResponse.0)
                self.employers?.append(contentsOf: parsedResponse.1)
                // Save logos
                ImageBank.saveLogos(list: self.universities)
                ImageBank.saveLogos(list: self.employers)
            } catch {
                return error
            }
            return nil
        }, success: {
            self.startListing()
        }, error: { error in
            self.viewImpl?.showErrorMessage(doShow: true, message: error.localizedDescription)
        })
    }

    func validateQrCode(_ str: String) {

        self.viewImpl?.config(isLoading: true)

        // Call the service
        ApiService.call(async: {
            do {
                let response = try ApiService.global.getConnectionTokenInfo(token: str)
                Logger.d("getConnectionTokenInfo response: \(response)")

                // Parse data
                guard let connection = ConnectionMaker.build(response.creator) else {
                    return SimpleLocalizedError("Can't parse response")
                }
                let conn = ConnectionRequest()
                conn.info = connection
                conn.token = str
                conn.type = connection as? University != nil ? 0 : 1

                self.connectionRequest = conn
            } catch {
                return error
            }
            return nil
        }, success: {
            self.viewImpl?.config(isLoading: false)
            self.viewImpl?.showNewConnectMessage(type: self.connectionRequest?.type ?? 0, title: self.connectionRequest!.info?.name, logoData: self.connectionRequest?.info?.logoData)
        }, error: { error in
            Tracker.global.trackConnectionFail()
            self.viewImpl?.config(isLoading: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "connections_scan_qr_error".localize(), afterErrorAction: {
                self.tappedBackButton()
            })
        })
    }

    func confirmQrCode() {

        // 1. Get the connection payment token.
        // 2. Call the payment library.
        // 3. On library response, add new connection.

        self.viewImpl?.config(isLoading: true)

        // Call the service
        ApiService.call(async: {
            do {
                let response = try ApiService.global.getPaymentToken()
                Logger.d("getPaymentToken response: \(response)")
                // Save the payment token
                if response.tokenizationKey.isEmpty {
                    throw SimpleLocalizedError("Payment token failed to retrieve")
                }
                self.connectionRequest!.paymentToken = response.tokenizationKey
            } catch {
                return error
            }
            return nil
        }, success: {
            self.viewImpl?.config(isLoading: false)
// FIXME: temporary disabled payment api for demo
//            self.callPaymentLibrary()
            self.connectionRequest!.paymentNonce = ""
            self.sendNewConnectionToServer()
        }, error: { error in
            self.viewImpl?.config(isLoading: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "connections_scan_qr_confirm_error".localize(), afterErrorAction: {
                self.tappedBackButton()
            })
        })
    }

    func callPaymentLibrary() {

        // Call payment library
        self.viewImpl?.config(isLoading: true)

        PaymentUtils.showPaymentView(
            viewImpl,
            token: self.connectionRequest!.paymentToken!,
            success: { nonce in
                Logger.d("showPaymentView success response: \(nonce)")
                self.viewImpl?.config(isLoading: false)
                self.connectionRequest!.paymentNonce = nonce
                self.sendNewConnectionToServer()
            }, error: { isCancelled, error in
                Logger.d("showPaymentView error response: \(error?.localizedDescription ?? "no error")")
                self.viewImpl?.config(isLoading: false)
                self.viewImpl?.showErrorMessage(doShow: true, message: "connections_scan_qr_confirm_payment_error".localize(), afterErrorAction: {
                    self.tappedBackButton()
                })
            }
        )
    }

    func sendNewConnectionToServer() {

        self.viewImpl?.config(isLoading: true)

        // Call the service
        ApiService.call(async: {
            do {
                let response = try ApiService.global.addConnectionToken(token: self.connectionRequest!.token!, nonce: self.connectionRequest!.paymentNonce!)
                Logger.d("addConnectionToken response: \(response)")
                // Save the userId
                self.sharedMemory.loggedUser?.connectionUserIds?[response.connection.connectionID] = response.userID
                self.sharedMemory.loggedUser = self.sharedMemory.loggedUser
            } catch {
                return error
            }
            return nil
        }, success: {
            self.viewImpl?.config(isLoading: false)
            self.tappedBackButton()
        }, error: { error in
            self.viewImpl?.config(isLoading: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "connections_scan_qr_confirm_error".localize(), afterErrorAction: {
                self.tappedBackButton()
            })
        })
    }

    // MARK: Table

    func setup(for cell: ConnectionMainViewCell) {

        if tabMode == .universities {
            let university: University = universities![cell.indexPath!.row]
            cell.config(title: university.name, isUniversity: true, logoData: sharedMemory.imageBank?.logo(for: university.connectionId))
        } else {
            let employer = employers![cell.indexPath!.row]
            cell.config(title: employer.name, isUniversity: false, logoData: sharedMemory.imageBank?.logo(for: employer.connectionId))
        }
    }

    func tappedAction(for cell: ConnectionMainViewCell) {
        // Do nothing
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

        Tracker.global.trackConnectionDecline()
        self.viewImpl?.startQrScan()
    }

    func tappedConfirmAction(for: ConnectionConfirmViewController) {

        Tracker.global.trackConnectionAccept()
        confirmQrCode()
    }
}
