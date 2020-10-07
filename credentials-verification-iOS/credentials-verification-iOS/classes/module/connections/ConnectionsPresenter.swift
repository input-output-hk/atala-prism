//

class ConnectionsPresenter: ListingBasePresenter, ListingBaseTableUtilsPresenterDelegate,
                            ConnectionMainViewCellPresenterDelegate, ConnectionConfirmPresenterDelegate,
                            ConnectionProofRequestPresenterDelegate, ConnectionsWorkerDelegate,
                            UISearchBarDelegate, ContactHistoryHeaderViewCellPresenterDelegate,
                            ContactDetailSectionViewCellPresenterDelegate,
                            ContactDetailSharedViewCellPresenterDelegate {

    var viewImpl: ConnectionsViewController? {
        return view as? ConnectionsViewController
    }

    enum ConnectionsSpecialState {
        case none
        case scanningQr
        case detail
    }

    enum ConnectionsCellType {
        case base(value: ListingBaseCellType)
        case main
        case noResults
        case detailHeader // detail mode
        case detailSection // detail mode
        case detailShared // detail mode
    }

    struct CellRow {
        var type: ConnectionsCellType
        var value: Any?
    }

    var stateSpecial: ConnectionsSpecialState = .none

    var contacts: [Contact] = []
    var filteredContacts: [Contact] = []

    var detailProofRequestMessageId: String?

    var connectionsWorker = ConnectionsWorker()

    var reachability: Reachability!

    var detailRows: [CellRow]?

    var detailContact: Contact?

    override init() {
        super.init()
        connectionsWorker.delegate = self
    }

    // MARK: Modes

    func isScanningQr() -> Bool {
        return self.state == .special && self.stateSpecial == .scanningQr
    }

    func startShowingDetails(contact: Contact) {

        detailContact = contact

        state = .special
        stateSpecial = .detail
        updateViewToState()
    }

    // MARK: Buttons

    func tappedScanButton() {

        startQrScanning()
    }

    func tappedAddNewnButton() {
        viewImpl?.showManualInput()
    }

    func tappedDeleteButton() {
        guard let contact = detailContact else { return }
        let credentialsDao = CredentialDAO()
        let credentials = credentialsDao.listCredentialsForContact(did: contact.connectionId)
        viewImpl?.showDeleteContactConfirmation(contact: contact, credentials: credentials)
    }

    @discardableResult
    func tappedBackButton() -> Bool {

        if isScanningQr() {
            stopQrScanning()
            return true
        }
        if stateSpecial == .detail {
            stateSpecial = .none
            startFetching()
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
        detailRows = []
        filteredContacts = []
    }

    func fetchData() {

        state = .fetching
        fetchingQueue = 1

        fetchElements()
    }

    func hasData() -> Bool {
        return stateSpecial == .detail ? true : contacts.size() > 0
    }

    func getElementCount() -> Int {
        if let baseValue = super.getBaseElementCount() {
            return baseValue
        }

        switch stateSpecial {
        case .none:
            return filteredContacts.count == 0 ? 1 : filteredContacts.count
        case .detail:
            return detailRows?.count ?? 0
        case .scanningQr:
            return 0
        }
    }

    func getElementType(indexPath: IndexPath) -> ConnectionsCellType {
        if let baseValue = super.getBaseElementType(indexPath: indexPath) {
            return .base(value: baseValue)
        }

        if stateSpecial == .detail {
            return detailRows![indexPath.row].type
        } else {
            return filteredContacts.count == 0 ? .noResults : .main
        }
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
        let historyDao = ActivityHistoryDAO()

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
                                                                               messageId: message.id,
                                                                               connectionId: message.connectionID) {
                                contactsDao.updateMessageId(connectionId: credential.0.issuerId, messageId: message.id)
                                if credential.1 {
                                    historyDao.createActivityHistory(timestamp: credential.0.dateReceived,
                                                                     type: .credentialAdded, credential: credential.0,
                                                                     contact: nil)
                                }
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
                let historyDao = ActivityHistoryDAO()
                let timestamp = Date()
                for credential in credentials {
                    historyDao.createActivityHistory(timestamp: timestamp, type: .credentialRequested,
                                                     credential: credential, contact: contact)
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

        let contact = filteredContacts[cell.indexPath!.row]
        cell.config(title: contact.name, logoData: contact.logo)

    }

    func hasPullToRefresh() -> Bool {
        true
    }

    func actionPullToRefresh() {

        stateSpecial = .none
        self.fetchData()
        self.updateViewToState()
    }

    func didSelectRowAt(indexPath: IndexPath) {
        if stateSpecial == .none {
            startShowingDetails(contact: filteredContacts[indexPath.row])
            fetchHistory()
        }
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
        contactDao.updateMessageId(connectionId: viewController.contact?.connectionId ?? "", messageId: detailProofRequestMessageId!)
    }

    func tappedConfirmAction(for viewController: ConnectionProofRequestViewController) {
        let contactDao = ContactDAO()
        contactDao.updateMessageId(connectionId: viewController.contact?.connectionId ?? "", messageId: detailProofRequestMessageId!)
        shareCredentials(contact: viewController.contact!, credentials: viewController.selectedCredentials)
    }

    // MARK: ConnectionsWorkerDelegate

    func contactsFetched(contacts: [Contact]) {

        let sortedContacts = contacts.sorted { $0.name < $1.name}
        self.cleanData()
        self.contacts.append(sortedContacts)
        self.filteredContacts.append(sortedContacts)
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

    // MARK: Search

    func searchBar(_ searchBar: UISearchBar, textDidChange searchText: String) {

        if searchText.isEmpty {
            filteredContacts = contacts
        } else {
            filteredContacts = contacts.filter {
                $0.name.lowercased().contains(searchText.lowercased())
            }
        }
        updateViewToState()
    }

    // MARK: Delete

    func deleteContact(contact: Contact, credentials: [Credential]) {
        let credentialDAO = CredentialDAO()
        let historyDao = ActivityHistoryDAO()
        for credential in credentials {
            historyDao.createActivityHistory(timestamp: Date(), type: .credentialDeleted,
                                             credential: credential, contact: nil)
        }
        if credentialDAO.deleteCredentials(credentials: credentials) {
            historyDao.createActivityHistory(timestamp: Date(), type: .contactDeleted,
                                             credential: nil, contact: contact)
            let contactDao = ContactDAO()
            if contactDao.deleteContact(contact: contact) {
                self.actionPullToRefresh()
                return
            }
        }
        self.viewImpl?.showErrorMessage(doShow: true, message: "credentials_delete_error".localize())
    }

    // MARK: Detail

    func fetchHistory() {
        guard let contactlId = detailContact?.connectionId else { return }
        let historyDao = ActivityHistoryDAO()
        let history = historyDao.listContactActivityHistory(for: contactlId)
        makeDetailRows(history: history)
    }

    private func makeDetailRows(history: [ActivityHistory]?) {

        // Transform data into rows
        self.detailRows?.removeAll()
        self.detailRows?.append(CellRow(type: .detailHeader, value: detailContact))
        var hideDivider = true

        let requested = history?.filter({
            $0.typeEnum == .credentialRequested
        })
        if requested?.count > 0 {
            self.detailRows?.append(CellRow(type: .detailSection,
                                             value: ("contacts_detail_requested".localize(), hideDivider)))
            hideDivider = false
            requested!.forEach { log in
                self.detailRows?.append(CellRow(type: .detailShared, value: log))
            }
        }

        let issued = history?.filter({
            $0.typeEnum == .credentialAdded
        })
        if issued?.count > 0 {
            self.detailRows?.append(CellRow(type: .detailSection,
                                             value: ("contacts_detail_issued".localize(), hideDivider)))
            hideDivider = false
            issued!.forEach { log in
                self.detailRows?.append(CellRow(type: .detailShared, value: log))
            }
        }

        let shared = history?.filter({
            $0.typeEnum == .credentialShared
        })
        if shared?.count > 0 {
            self.detailRows?.append(CellRow(type: .detailSection,
                                             value: ("contacts_detail_shared".localize(), hideDivider)))
            shared!.forEach { log in
                self.detailRows?.append(CellRow(type: .detailShared, value: log))
            }
        }
    }

    func setup(for cell: ContactHistoryHeaderViewCell) {
        guard let contact = detailContact else { return }
        cell.config(title: contact.name, subtitle: String(contact.did.split(separator: ":").last!),
                    date: contact.dateCreated, icon: contact.logo)
    }

    func setup(for cell: ContactDetailSharedViewCell) {
        guard let index = cell.indexPath?.row,
            let log = detailRows?[index].value as? ActivityHistory,
            let credentialName = log.credentialName,
            let timestamp = log.timestamp else { return }
        cell.config(title: credentialName, date: timestamp, type: log.typeEnum)

    }

    func setup(for cell: ContactDetailSectionViewCell) {
        guard let index = cell.indexPath?.row,
            let data = detailRows?[index].value as? (String, Bool) else { return }
        cell.config(title: data.0, hideDivider: data.1)
    }

}
