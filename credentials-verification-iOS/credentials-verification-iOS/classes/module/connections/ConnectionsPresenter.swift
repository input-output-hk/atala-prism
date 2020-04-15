//

class ConnectionsPresenter: ListingBasePresenter, ListingBaseTableUtilsPresenterDelegate, ConnectionMainViewCellPresenterDelegate, ConnectionConfirmPresenterDelegate, ConnectionProofRequestPresenterDelegate {

    var viewImpl: ConnectionsViewController? {
        return view as? ConnectionsViewController
    }

    enum ConnectionsSpecialState {
        case none
        case scanningQr
    }

    enum ConnectionsCellType {
        case base(value: ListingBaseCellType)
        case main
    }

    var stateSpecial: ConnectionsSpecialState = .none

    var connections: [ConnectionBase]?

    var connectionRequest: ConnectionRequest?
    
    var detailProofRequestMessageId: String?

    // MARK: Modes

    func isScanningQr() -> Bool {
        return self.state == .special && self.stateSpecial == .scanningQr
    }

    // MARK: Buttons

    func tappedScanButton() {

        Tracker.global.trackScanQrTapped()
        startQrScanning()
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
        connections = []
    }

    func fetchData() {

        state = .fetching
        fetchingQueue = 1

        fetchElements()
    }

    func hasData() -> Bool {
        return (connections?.size() ?? 0) > 0
    }

    func getElementCount() -> Int {
        if let baseValue = super.getBaseElementCount() {
            return baseValue
        }
        return (connections?.size() ?? 0)
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
                self.connections?.append(contentsOf: parsedResponse)
                self.connections?.sort(by: { (lhs, rhs) -> Bool in
                    lhs.name < rhs.name
                })
                // Save logos
                ImageBank.saveLogos(list: self.connections)
            } catch {
                return error
            }
            return nil
        }, success: {
            self.startListing()
            self.fetchCredentials()
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
                conn.type = connection.type

                self.connectionRequest = conn
            } catch {
                return error
            }
            return nil
        }, success: {
            self.viewImpl?.config(isLoading: false)
            self.viewImpl?.onBackPressed()
            self.viewImpl?.showNewConnectMessage(type: self.connectionRequest?.type ?? 0, title: self.connectionRequest!.info?.name, logoData: self.connectionRequest?.info?.logoData)
        }, error: { error in
            Tracker.global.trackConnectionFail()
            self.viewImpl?.config(isLoading: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "connections_scan_qr_error".localize())
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
            self.viewImpl?.showErrorMessage(doShow: true, message: "connections_scan_qr_confirm_error".localize())
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
                self.viewImpl?.showErrorMessage(doShow: true, message: "connections_scan_qr_confirm_payment_error".localize())
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
            self.actionPullToRefresh()
        }, error: { error in
            self.viewImpl?.config(isLoading: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "connections_scan_qr_confirm_error".localize())
        })
    }
    
    func fetchCredentials() {

        guard let user = self.sharedMemory.loggedUser else {
            return
        }

        // Call the service
        ApiService.call(async: {
            do {
                let responses = try ApiService.global.getCredentials(userIds: user.connectionUserIds?.valuesArray)
                Logger.d("getCredentials responses: \(responses)")

                var credentials: [Degree] = []
                var proofRequest: Io_Iohk_Prism_Protos_ProofRequest?
                // Parse the messages
                for response in responses {
                    for message in response.messages {
                        let isRejected = user.messagesRejectedIds?.contains(message.id) ?? false
                        let isNew = !(user.messagesAcceptedIds?.contains(message.id) ?? false)
                        if !isRejected {
                            if let atalaMssg = try? Io_Iohk_Prism_Protos_AtalaMessage(serializedData: message.message) {
                                if !atalaMssg.issuerSentCredential.credential.typeID.isEmpty && !isNew {
                                    if let credential = Degree.build(atalaMssg.issuerSentCredential.credential, messageId: message.id, isNew: isNew) {
                                        credentials.append(credential)
                                    }
                                } else if !atalaMssg.proofRequest.connectionToken.isEmpty && isNew {
                                    proofRequest = atalaMssg.proofRequest
                                    self.detailProofRequestMessageId = message.id
                                }
                            }
                        }
                    }
                }
                if proofRequest != nil {
                    self.askProofRequest(credentials: credentials, proofRequest: proofRequest!)
                }

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
    
    func askProofRequest(credentials: [Degree], proofRequest: Io_Iohk_Prism_Protos_ProofRequest) {
        let filteredCredentials = credentials.filter {
            proofRequest.typeIds.contains($0.type!.rawValue)
        }
        let shareConnection = self.connections?.first(where: {
            $0.token == proofRequest.connectionToken
        })
        if !filteredCredentials.isEmpty && shareConnection != nil {
            DispatchQueue.main.async {
                self.viewImpl?.showNewProofRequestMessage(credentials: filteredCredentials, requiered: proofRequest.typeIds, connection: shareConnection!, logoData: shareConnection?.logoData)
            }
        }
    }
    
    private func shareCredentials(connection: ConnectionBase, credentials: [Degree]) {

        Tracker.global.trackCredentialShareCompleted()
        viewImpl?.config(isLoading: true)

        // Call the service
        ApiService.call(async: {
            do {
                if let connId = connection.connectionId, let userId = self.sharedMemory.loggedUser?.connectionUserIds?[connId] {
                
                let responses = try ApiService.global.shareCredentials(userId: userId, connectionId: connId, degrees: credentials)
                Logger.d("shareCredential response: \(responses)")
                } else {
                    return nil
                }

            } catch {
                return error
            }
            return nil
        }, success: {
            self.viewImpl?.config(isLoading: false)
            let actions = [UIAlertAction(title: "ok".localize(), style: .default, handler: { _ in
                self.tappedBackButton()
                self.actionPullToRefresh()
            })]
            self.viewImpl?.showSuccessMessage(doShow: true, message: "home_detail_share_success".localize(), actions: actions)
        }, error: { error in
            self.viewImpl?.config(isLoading: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: error.localizedDescription)
        })
    }

    // MARK: Table

    func setup(for cell: ConnectionMainViewCell) {

        let university: ConnectionBase = connections![cell.indexPath!.row]
        cell.config(title: university.name, isUniversity: university.type != 0, logoData: sharedMemory.imageBank?.logo(for: university.connectionId))

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
    }

    func tappedConfirmAction(for: ConnectionConfirmViewController) {

        Tracker.global.trackConnectionAccept()
        confirmQrCode()
    }

    // MARK: ConnectionProofRequestPresenterDelegate
    
    func tappedDeclineAction(for: ConnectionProofRequestViewController) {
        sharedMemory.loggedUser?.messagesRejectedIds?.append(detailProofRequestMessageId!)
        sharedMemory.loggedUser = sharedMemory.loggedUser
    }
    
    func tappedConfirmAction(for vc: ConnectionProofRequestViewController) {
        sharedMemory.loggedUser?.messagesAcceptedIds?.append(detailProofRequestMessageId!)
        sharedMemory.loggedUser = sharedMemory.loggedUser
        shareCredentials(connection: vc.connection!, credentials: vc.selectedCredentials)
    }
}
