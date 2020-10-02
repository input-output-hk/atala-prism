//
import SwiftGRPC
import SwiftProtobuf
import ObjectMapper

class CredentialsPresenter: ListingBasePresenter, ListingBaseTableUtilsPresenterDelegate,
                            DegreeViewCellPresenterDelegate, ShareDialogPresenterDelegate,
                            CredentialHistoryHeaderViewCellPresenterDelegate,
                            CredentialSharedViewCellPresenterDelegate, CredentialSectionViewCellPresenterDelegate,
                            UISearchBarDelegate {

    var viewImpl: CredentialsViewController? {
        return view as? CredentialsViewController
    }

    enum CredentialsMode {
        case degrees
        case detail
        case history
    }

    enum CredentialsCellType {
        case base(value: ListingBaseCellType)
        case degree // degrees mode
        case noResults // degree mode
        case historyHeader // history mode
        case historySection // history mode
        case historyShared // history mode
    }

    struct CellRow {
        var type: CredentialsCellType
        var value: Any?
    }

    var mode: CredentialsMode = .degrees

    var degreeRows: [CellRow]?
    var historyRows: [CellRow]?

    var detailCredential: Credential?

    var shareEmployers: [Contact]?
    var shareEmployersFiltered: [Contact]?
    var shareSelectedEmployers: [Contact]?

    var credentials: [Credential] = []
    var filteredCredentials: [Credential] = []

    // MARK: Modes

    func getMode() -> CredentialsMode {
        return mode
    }

    func startShowingDegrees() {

        mode = .degrees
        updateViewToState()
    }

    func startShowingDetails(credential: Credential) {

        detailCredential = credential

        mode = .detail
        updateViewToState()
    }

    func startShowingHistory() {

        mode = .history
        updateViewToState()
    }

    // MARK: Buttons

    @discardableResult
    func tappedBackButton() -> Bool {

        if mode == .history, let credential = detailCredential {
            startShowingDetails(credential: credential)
            return true
        }

        if mode != .degrees {
            startShowingDegrees()
            return true
        }
        return false
    }

    // MARK: ListingBaseTableUtilsPresenterDelegate

    func cleanData() {
        detailCredential = nil
        degreeRows = []
        historyRows = []
        credentials = []
        filteredCredentials = []
    }

    func fetchData() {

        state = .fetching
        fetchingQueue = 1

        fetchElements()
    }

    func hasData() -> Bool {
        switch mode {
        case .degrees:
            return credentials.count > 0
        case .detail:
            return true
        case .history:
            return true
        }
    }

    func getElementCount() -> Int {
        if let baseValue = super.getBaseElementCount() {
            return baseValue
        }

        switch mode {
        case .degrees:
            return degreeRows?.count ?? 0
        case .history:
            return historyRows?.count ?? 0
        case .detail:
            return 0
        }
    }

    func getElementType(indexPath: IndexPath) -> CredentialsCellType {
        if let baseValue = super.getBaseElementType(indexPath: indexPath) {
            return .base(value: baseValue)
        }

        if mode == .degrees {
            return degreeRows![indexPath.row].type
        } else {
            return historyRows![indexPath.row].type
        }
    }

    // MARK: Fetch

    func getLoggedUser() -> LoggedUser? {
        return sharedMemory.loggedUser
    }

    func fetchElements() {

        let contactsDao = ContactDAO()
        let contacts = contactsDao.listContacts()

        // Call the service
        ApiService.call(async: {
            do {
                let responses = try ApiService.global.getCredentials(contacts: contacts)
                Logger.d("getCredentials responses: \(responses)")

                self.cleanData()
                let credentialsDao = CredentialDAO()
                let historyDao = ActivityHistoryDAO()

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
                            }
                        }
                    }
                }
                self.credentials = credentialsDao.listCredentials() ?? []
                self.filteredCredentials.append(self.credentials)
                self.makeDegreeRows()

            } catch {
                return error
            }
            return nil
        }, success: {
            self.startListing()
        }, error: { _ in
            self.cleanData()
            let credentialsDao = CredentialDAO()
            self.credentials = credentialsDao.listCredentials() ?? []
            self.filteredCredentials.append(self.credentials)
            self.makeDegreeRows()
            self.startListing()
        })
    }

    private func makeDegreeRows() {

        // Transform data into rows
        self.degreeRows?.removeAll()
        if filteredCredentials.count > 0 {
            filteredCredentials.forEach { credential in
                self.degreeRows?.append(CellRow(type: .degree, value: credential))
            }
        } else {
            self.degreeRows?.append(CellRow(type: .noResults, value: nil))
        }
    }

    private func fetchShareEmployers() {

        viewImpl?.config(isLoading: true)

        // Clean data
        self.shareEmployers = []
        self.shareEmployersFiltered = []
        self.shareSelectedEmployers = []

        let contactsDao = ContactDAO()
        let contacts = contactsDao.listContactsForShare(connectionId: self.detailCredential?.issuerId ?? "") ?? []
        self.shareEmployers?.append(contentsOf: contacts)
        self.shareEmployersFiltered?.append(contentsOf: contacts)
        self.viewImpl?.config(isLoading: false)
        self.viewImpl?.showShareDialog()

    }

    private func shareWithSelectedEmployers() {

        guard let contacts = self.shareSelectedEmployers else { return }

        viewImpl?.config(isLoading: true)

        // Call the service
        ApiService.call(async: {
            do {
                let responses = try ApiService.global.shareCredential(contacts: self.shareSelectedEmployers ?? [],
                                                                      credential: self.detailCredential!)
                Logger.d("shareCredential response: \(responses)")
                let historyDao = ActivityHistoryDAO()
                let timestamp = Date()
                for contact in contacts {
                    historyDao.createActivityHistory(timestamp: timestamp, type: .credentialShared,
                                                     credential: self.detailCredential, contact: contact)
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
            self.viewImpl?.showSuccessMessage(doShow: true,
                                              message: "credentials_detail_share_success".localize(),
                                              actions: actions)
        }, error: { _ in
            self.viewImpl?.config(isLoading: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
        })
    }

    // MARK: Table

    func hasPullToRefresh() -> Bool {
        true
    }

    func actionPullToRefresh() {

        self.startShowingDegrees()
        self.fetchData()
        self.updateViewToState()
    }

    func didSelectRowAt(indexPath: IndexPath) {
        if mode == .degrees {
            let rowIndex = indexPath.row
            guard let cellRow = degreeRows?[rowIndex], let credential = cellRow.value as? Credential else {
                return
            }
            startShowingDetails(credential: credential)
        }
    }

    func setup(for cell: CommonViewCell) {

        let cellRow = degreeRows?[cell.indexPath!.row]
        // Config for a Degree
        if let credential = cellRow?.value as? Credential {
            cell.config(title: credential.credentialName, subtitle: credential.issuerName, logoData: nil,
                        logoPlaceholderNamed: credential.logoPlaceholder)
        }
    }

    func tappedAction(for cell: CommonViewCell) {

        let cellRow = degreeRows?[cell.indexPath!.row]
        // Config for a Degree
        if let credential = cellRow?.value as? Credential {
            startShowingDetails(credential: credential)
        }
    }

    // MARK: Share

    func tappedShareButton() {
        fetchShareEmployers()
    }

    func tappedDeclineAction(for view: ShareDialogViewController) {
        // Do nothing
    }

    func tappedConfirmAction(for view: ShareDialogViewController) {
        shareWithSelectedEmployers()
    }

    func shareItem(for view: ShareDialogViewController, at index: Int) -> Any? {
        return shareEmployersFiltered?[index]
    }

    func shareItemCount(for view: ShareDialogViewController) -> Int {
        return shareEmployersFiltered?.count ?? 0
    }

    func shareItemTapped(for cell: ShareDialogItemCollectionViewCell?, at index: Int, item: Any?) {

        let employer = shareEmployersFiltered![index]
        if employerIsSelected(employer: employer) {
            shareSelectedEmployers?.remove(employer)
        } else {
            shareSelectedEmployers?.append(employer)
        }
        viewImpl?.configShareDialog(enableButton: (shareSelectedEmployers?.count ?? 0) > 0)
        shareItemConfig(for: cell, at: index, item: item)
        cell?.refreshView()
    }

    func employerIsSelected(employer: Contact) -> Bool {
        return shareSelectedEmployers?.contains(where: { $0.connectionId == employer.connectionId }) ?? false
    }

    func shareItemConfig(for cell: ShareDialogItemCollectionViewCell?, at index: Int, item: Any?) {

        let employer = shareEmployersFiltered![index]
        let isSelected = employerIsSelected(employer: employer)
        cell?.config(name: employer.name,
                     logoData: employer.logo,
                     placeholderNamed: "ico_placeholder_employer", isSelected: isSelected)
    }

    func shareItemFilter(for view: ShareDialogViewController, searchText: String) {
        if searchText.isEmpty {
            shareEmployersFiltered = shareEmployers
        } else {
            shareEmployersFiltered = shareEmployers?.filter({
                $0.name.lowercased().contains(searchText.lowercased())
            })
        }
    }

    // MARK: Search

    func searchBar(_ searchBar: UISearchBar, textDidChange searchText: String) {

        if searchText.isEmpty {
            filteredCredentials = credentials
        } else {
            filteredCredentials = credentials.filter {
                $0.credentialName.lowercased().contains(searchText.lowercased())
                    || $0.issuerName.lowercased().contains(searchText.lowercased())
            }
        }
        makeDegreeRows()
        updateViewToState()
    }

    // MARK: Delete

    func tappedDeleteButton() {
        viewImpl?.showDeleteCredentialConfirmation()
    }

    func deleteCredential() {
        let dao = CredentialDAO()
        let historyDao = ActivityHistoryDAO()
        historyDao.createActivityHistory(timestamp: Date(), type: .credentialDeleted,
                                         credential: self.detailCredential!, contact: nil)
        if dao.deleteCredential(credential: self.detailCredential!) {
            self.tappedBackButton()
            self.actionPullToRefresh()
        } else {
            self.viewImpl?.showErrorMessage(doShow: true, message: "credentials_delete_error".localize())
        }
    }

    // MARK: History

    func tappedHistoryButton() {
        startShowingHistory()
        fetchHistory()
    }

    func fetchHistory() {
        guard let credentialId = detailCredential?.credentialId else { return }
        let historyDao = ActivityHistoryDAO()
        let history = historyDao.listCredentialActivityHistory(for: credentialId)
        makeHistoryRows(history: history)
    }

    private func makeHistoryRows(history: [ActivityHistory]?) {

        // Transform data into rows
        self.historyRows?.removeAll()
        self.historyRows?.append(CellRow(type: .historyHeader, value: detailCredential))
        var hideDivider = true
        let shared = history?.filter({
            $0.typeEnum == .credentialShared
        })
        if shared?.count > 0 {
            self.historyRows?.append(CellRow(type: .historySection,
                                             value: ("credentials_history_shared".localize(), hideDivider)))
            hideDivider = false
            shared!.forEach { log in
                self.historyRows?.append(CellRow(type: .historyShared, value: log))
            }
        }
        let requested = history?.filter({
            $0.typeEnum == .credentialRequested
        })
        if requested?.count > 0 {
            self.historyRows?.append(CellRow(type: .historySection,
                                             value: ("credentials_history_requested".localize(), hideDivider)))
            requested!.forEach { log in
                self.historyRows?.append(CellRow(type: .historyShared, value: log))
            }
        }
    }

    func setup(for cell: CredentialHistoryHeaderViewCell) {
        guard let credential = detailCredential else { return }
        cell.config(title: credential.issuerName, date: credential.dateReceived,
                    icon: credential.logoPlaceholder)
    }

    func setup(for cell: CredentialSharedViewCellViewCell) {
        guard let index = cell.indexPath?.row,
            let log = historyRows?[index].value as? ActivityHistory,
            let contactName = log.contactName,
            let timestamp = log.timestamp else { return }
        cell.config(title: contactName, date: timestamp, logoData: log.contactLogo)

    }

    func setup(for cell: CredentialSectionViewCell) {
        guard let index = cell.indexPath?.row,
            let data = historyRows?[index].value as? (String, Bool) else { return }
        cell.config(title: data.0, hideDivider: data.1)
    }
}
