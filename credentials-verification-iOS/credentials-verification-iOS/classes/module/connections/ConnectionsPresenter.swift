//

class ConnectionsPresenter: ListingBasePresenter, ListingBaseTableUtilsPresenterDelegate,
                            ConnectionMainViewCellPresenterDelegate, ConnectionConfirmPresenterDelegate,
                            ConnectionProofRequestPresenterDelegate, ConnectionsWorkerDelegate {

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

    var connections: [ConnectionBase] = []

    var detailProofRequestMessageId: String?

    var connectionsWorker = ConnectionsWorker()

    override init() {
        super.init()
        connectionsWorker.delegate = self
    }

    // MARK: Modes

    func isScanningQr() -> Bool {
        return self.state == .special && self.stateSpecial == .scanningQr
    }

    // MARK: Buttons

    func tappedScanButton() {

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
        return connections.size() > 0
    }

    func getElementCount() -> Int {
        if let baseValue = super.getBaseElementCount() {
            return baseValue
        }
        return connections.size()
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
        self.connectionsWorker.fetchConnections()
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
                                if !atalaMssg.issuerSentCredential.credential.typeID.isEmpty {
                                    if let credential = Degree.build(atalaMssg.issuerSentCredential.credential,
                                                                     messageId: message.id, isNew: isNew) {
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
            // Already asked for proof rquest do nothing
        }, error: { _ in
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
        })
    }

    func askProofRequest(credentials: [Degree], proofRequest: Io_Iohk_Prism_Protos_ProofRequest) {
        let filteredCredentials = credentials.filter {
            proofRequest.typeIds.contains($0.type!.rawValue)
        }
        let shareConnection = self.connections.first(where: {
            $0.token == proofRequest.connectionToken
        })
        if !filteredCredentials.isEmpty && shareConnection != nil {
            DispatchQueue.main.async {
                self.viewImpl?.showNewProofRequestMessage(credentials: filteredCredentials,
                                                          requiered: proofRequest.typeIds,
                                                          connection: shareConnection!,
                                                          logoData: shareConnection?.logoData)
            }
        }
    }

    private func shareCredentials(connection: ConnectionBase, credentials: [Degree]) {

        viewImpl?.config(isLoading: true)

        // Call the service
        ApiService.call(async: {
            do {
                if let connId = connection.connectionId,
                    let userId = self.sharedMemory.loggedUser?.connectionUserIds?[connId] {

                let responses = try ApiService.global.shareCredentials(userId: userId,
                                                                       connectionId: connId,
                                                                       degrees: credentials)
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
            self.viewImpl?.showSuccessMessage(doShow: true, message: "",
                                              title: "credentials_detail_share_success".localize(),
                                              actions: actions)
        }, error: { _ in
            self.viewImpl?.config(isLoading: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
        })
    }

    // MARK: Table

    func setup(for cell: ConnectionMainViewCell) {

        let university: ConnectionBase = connections[cell.indexPath!.row]
        cell.config(title: university.name, isUniversity: university.type != 0,
                    logoData: sharedMemory.imageBank?.logo(for: university.connectionId))

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
        self.connectionsWorker.validateQrCode(str, connections: self.connections)
    }

    func tappedDeclineAction(for: ConnectionConfirmViewController) {

        Tracker.global.trackConnectionDecline()
    }

    func tappedConfirmAction(for: ConnectionConfirmViewController) {

        Tracker.global.trackConnectionAccept()
        self.connectionsWorker.confirmQrCode()
    }

    // MARK: ConnectionProofRequestPresenterDelegate

    func tappedDeclineAction(for: ConnectionProofRequestViewController) {
        sharedMemory.loggedUser?.messagesRejectedIds?.append(detailProofRequestMessageId!)
        sharedMemory.loggedUser = sharedMemory.loggedUser
    }

    func tappedConfirmAction(for viewController: ConnectionProofRequestViewController) {
        sharedMemory.loggedUser?.messagesAcceptedIds?.append(detailProofRequestMessageId!)
        sharedMemory.loggedUser = sharedMemory.loggedUser
        shareCredentials(connection: viewController.connection!, credentials: viewController.selectedCredentials)
    }

    // MARK: ConnectionsWorkerDelegate

    func connectionsFetched(connections: [ConnectionBase]) {

        // Save logos
        ImageBank.saveLogos(list: connections)
        let sortedConnections = connections.sorted { $0.name < $1.name}
        self.cleanData()
        self.connections.append(sortedConnections)
        self.startListing()
        self.fetchCredentials()
    }

    func config(isLoading: Bool) {
         self.viewImpl?.config(isLoading: isLoading)
    }

    func showErrorMessage(doShow: Bool, message: String?) {
        self.viewImpl?.showErrorMessage(doShow: doShow, message: message)
    }

    func showNewConnectMessage(type: Int, title: String?, logoData: Data?) {
        self.viewImpl?.onBackPressed()
        self.viewImpl?.showNewConnectMessage(type: type, title: title, logoData: logoData)
    }

    func conectionAccepted() {
        self.actionPullToRefresh()
    }
}
