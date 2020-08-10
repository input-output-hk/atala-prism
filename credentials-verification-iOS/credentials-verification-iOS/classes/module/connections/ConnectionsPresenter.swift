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

    var contacts: [Contact] = []

    var detailProofRequestMessageId: String?

    var connectionsWorker = ConnectionsWorker()

    var reachability: Reachability!

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

    func tappedAddNewnButton() {
        viewImpl?.showManualInput()
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
        contacts = []
    }

    func fetchData() {

        state = .fetching
        fetchingQueue = 1

        fetchElements()
    }

    func hasData() -> Bool {
        return contacts.size() > 0
    }

    func getElementCount() -> Int {
        if let baseValue = super.getBaseElementCount() {
            return baseValue
        }
        return contacts.size()
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

        let contactsDao = ContactDAO()
        let contacts = contactsDao.listContacts()
        let credentialsDao = CredentialDAO()

        // Call the service
        ApiService.call(async: {
            do {
                let responses = try ApiService.global.getCredentials(contacts: contacts)
                Logger.d("getCredentials responses: \(responses)")

                var proofRequest: Io_Iohk_Prism_Protos_ProofRequest?
                // Parse the messages
                for response in responses {
                    for message in response.messages {
                        if let atalaMssg = try? Io_Iohk_Prism_Protos_AtalaMessage(serializedData: message.message) {
                            if !atalaMssg.issuerSentCredential.credential.typeID.isEmpty,
                                let credential = credentialsDao.createCredential(sentCredential:
                                    atalaMssg.issuerSentCredential.credential, viewed: false,
                                                                               messageId: message.id) {
                                contactsDao.updateMessageId(did: credential.issuerId, messageId: message.id)
                            } else if !atalaMssg.proofRequest.connectionToken.isEmpty {
                                proofRequest = atalaMssg.proofRequest
                                self.detailProofRequestMessageId = message.id
                            }       
                        }
                    }
                }
                if proofRequest != nil {
                    self.askProofRequest(proofRequest: proofRequest!)
                }
            } catch {
                return error
            }
            return nil
        }, success: {
            // Already asked for proof rquest do nothing
        }, error: { _ in
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
            self.observeReachability()
        })
    }

    func askProofRequest(proofRequest: Io_Iohk_Prism_Protos_ProofRequest) {
        let credentialsDao = CredentialDAO()
        let credentials = credentialsDao.listCredentials() ?? []
        let filteredCredentials = credentials.filter {
            proofRequest.typeIds.contains($0.type)
        }
        let shareContact = self.contacts.first(where: {
            $0.token == proofRequest.connectionToken
        })
        if !filteredCredentials.isEmpty && shareContact != nil {
            DispatchQueue.main.async {
                self.viewImpl?.showNewProofRequestMessage(credentials: filteredCredentials,
                                                          requiered: proofRequest.typeIds,
                                                          contact: shareContact!,
                                                          logoData: shareContact?.logo)
            }
        }
    }

    private func shareCredentials(contact: Contact, credentials: [Credential]) {

        viewImpl?.config(isLoading: true)

        // Call the service
        ApiService.call(async: {
            do {

                let responses = try ApiService.global.shareCredentials(contact: contact,
                                                                       credentials: credentials)
                Logger.d("shareCredential response: \(responses)")

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

    func observeReachability() {
        self.reachability = try? Reachability()
        NotificationCenter.default.addObserver(self, selector: #selector(self.reachabilityChanged),
                                               name: NSNotification.Name.reachabilityChanged, object: nil)
        do {
            try self.reachability.startNotifier()
        } catch let error {
            print("Error occured while starting reachability notifications : \(error.localizedDescription)")
        }
    }

    @objc func reachabilityChanged(note: Notification) {
        guard let reachability = note.object as? Reachability else { return }
        switch reachability.connection {
        case .cellular, .wifi:
            NotificationCenter.default.removeObserver(self, name: NSNotification.Name.reachabilityChanged, object: nil)
            reachability.stopNotifier()
            fetchCredentials()
        default:
            break
        }
    }

    // MARK: Table

    func setup(for cell: ConnectionMainViewCell) {

        let contact = contacts[cell.indexPath!.row]
        cell.config(title: contact.name, logoData: contact.logo)

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
        self.connectionsWorker.validateQrCode(str, contacts: self.contacts)
    }

    func tappedDeclineAction(for: ConnectionConfirmViewController) {

        Tracker.global.trackConnectionDecline()
    }

    func tappedConfirmAction(for: ConnectionConfirmViewController) {

        Tracker.global.trackConnectionAccept()
        self.connectionsWorker.confirmQrCode()
    }

    // MARK: ConnectionProofRequestPresenterDelegate

    func tappedDeclineAction(for viewController: ConnectionProofRequestViewController) {
        let contactDao = ContactDAO()
        contactDao.updateMessageId(did: viewController.contact?.did ?? "", messageId: detailProofRequestMessageId!)
    }

    func tappedConfirmAction(for viewController: ConnectionProofRequestViewController) {
        let contactDao = ContactDAO()
        contactDao.updateMessageId(did: viewController.contact?.did ?? "", messageId: detailProofRequestMessageId!)
        shareCredentials(contact: viewController.contact!, credentials: viewController.selectedCredentials)
    }

    // MARK: ConnectionsWorkerDelegate

    func contactsFetched(contacts: [Contact]) {

        let sortedContacts = contacts.sorted { $0.name < $1.name}
        self.cleanData()
        self.contacts.append(sortedContacts)
        self.startListing()
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            self.fetchCredentials()
        }
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
