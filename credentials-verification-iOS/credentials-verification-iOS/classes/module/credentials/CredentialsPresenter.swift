//
import SwiftGRPC
import SwiftProtobuf
import ObjectMapper

class CredentialsPresenter: ListingBasePresenter, ListingBaseTableUtilsPresenterDelegate,
                            DegreeViewCellPresenterDelegate, DocumentViewCellPresenterDelegate,
                            DetailHeaderViewCellPresenterDelegate, DetailFooterViewCellPresenterDelegate,
                            DetailPropertyViewCellPresenterDelegate, ShareDialogPresenterDelegate,
                            UISearchBarDelegate {

    var viewImpl: CredentialsViewController? {
        return view as? CredentialsViewController
    }

    enum CredentialsMode {
        case degrees
        case document
        case detail
    }

    enum CredentialsCellType {
        case base(value: ListingBaseCellType)
        case degree // degrees mode
        case noResults // degree mode
        case document // document mode
        case detailHeader // detail mode
        case detailProperty // detail mode
        case detailFooter // detail mode
    }

    struct CellRow {
        var type: CredentialsCellType
        var value: Any?
    }

    var mode: CredentialsMode = .degrees

    var degreeRows: [CellRow]?
    var detailRows: [CellRow]?

    var detailDegree: Degree?
    var detailCredential: Credential?

    var shareEmployers: [Contact]?
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

    func startShowingDocument() {

        mode = .document
        updateViewToState()
    }

    func startShowingDetails(degree: Degree) {

        // Make the rows
        detailRows = []
        detailDegree = degree
        detailRows?.append(CellRow(type: .detailHeader, value: degree))
        switch degree.type {
        case .univerityDegree:
            detailRows?.append(CellRow(type: .detailProperty,
                                       value: ("credentials_detail_full_name".localize(),
                                               degree.credentialSubject?.name, false, degree.type)))
            detailRows?.append(CellRow(type: .detailProperty,
                                       value: ("credentials_detail_degree_name".localize(),
                                               degree.credentialSubject?.degreeAwarded, false, degree.type)))
            detailRows?.append(CellRow(type: .detailProperty,
                                       value: ("credentials_detail_award".localize(),
                                               degree.credentialSubject?.degreeResult, false, degree.type)))
            detailRows?.append(CellRow(type: .detailProperty,
                                       value: ("credentials_detail_issuance_date".localize(),
                                               degree.issuanceDate, true, degree.type)))
        case .governmentIssuedId:
            detailRows?.append(CellRow(type: .document, value: degree))
        case .certificatOfInsurance:
            detailRows?.append(CellRow(type: .detailProperty,
                                       value: ("credentials_detail_full_name".localize(),
                                               degree.credentialSubject?.name, false, degree.type)))
            detailRows?.append(CellRow(type: .detailProperty,
                                       value: ("credentials_detail_employment_class_insurance".localize(),
                                               degree.productClass, false, degree.type)))
            detailRows?.append(CellRow(type: .detailProperty,
                                       value: ("credentials_detail_employment_policy_number".localize(),
                                               degree.policyNumber, false, degree.type)))
            detailRows?.append(CellRow(type: .detailProperty,
                                       value: ("credentials_detail_employment_policy_end_date".localize(),
                                               degree.expiryDate, true, degree.type)))
        case .proofOfEmployment:
            detailRows?.append(CellRow(type: .detailProperty,
                                       value: ("credentials_detail_employee_name".localize(),
                                               degree.credentialSubject?.name, false, degree.type)))
            detailRows?.append(CellRow(type: .detailProperty,
                                       value: ("credentials_detail_employment_status".localize(),
                                               degree.employmentStatus, false, degree.type)))
            detailRows?.append(CellRow(type: .detailProperty,
                                       value: ("credentials_detail_employment_start_date".localize(),
                                               degree.issuanceDate, true, degree.type)))
        default:
            print("Unrecognized type")
        }
        detailRows?.append(CellRow(type: .detailFooter, value: degree))

        mode = .detail
        updateViewToState()
    }

    // MARK: Buttons

    @discardableResult
    func tappedBackButton() -> Bool {

        if mode != .degrees {
            startShowingDegrees()
            return true
        }
        return false
    }

    // MARK: ListingBaseTableUtilsPresenterDelegate

    func cleanData() {
        detailDegree = nil
        detailCredential = nil
        degreeRows = []
        detailRows = []
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
            return (detailRows?.count ?? 0) > 0
        case .document:
            return true
        }
    }

    func getElementCount() -> Int {
        if let baseValue = super.getBaseElementCount() {
            return baseValue
        }

        switch mode {
        case .degrees:
            return (degreeRows?.count ?? 0)
        case .document:
            return 1
        case .detail:
            return (detailRows?.count ?? 0)
        }
    }

    func getElementType(indexPath: IndexPath) -> CredentialsCellType {
        if let baseValue = super.getBaseElementType(indexPath: indexPath) {
            return .base(value: baseValue)
        }

        switch mode {
        case .degrees:
            return degreeRows![indexPath.row].type
        case .document:
            return .document
        case .detail:
            return detailRows![indexPath.row].type
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
                                                                               messageId: message.id) {
                                contactsDao.updateMessageId(did: credential.0.issuerId, messageId: message.id)
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
        self.shareSelectedEmployers = []

        let contactsDao = ContactDAO()
        let contacts = contactsDao.listContactsForShare(did: self.detailDegree?.issuer?.id ?? "") ?? []
        self.shareEmployers?.append(contentsOf: contacts)
        self.viewImpl?.config(isLoading: false)
        self.viewImpl?.showShareDialog()

    }

    private func shareWithSelectedEmployers() {

        guard let contacts = self.shareSelectedEmployers else { return }

        viewImpl?.config(isLoading: true)

        // Call the service
        ApiService.call(async: {
            do {
                let responses = try ApiService.global.shareCredential(contacts: contacts,
                                                                      degree: self.detailDegree!)
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
            detailCredential = credential
            // FIXME this should be updated for HTML credentials
            if let degree = Mapper<Degree>().map(JSONString: credential.htmlView) {

                degree.intCredential = credential.encoded

                degree.type = CredentialType(rawValue: credential.type)
                detailCredential = credential
                startShowingDetails(degree: degree)
            }
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
        if let degree = cellRow?.value as? Degree {
            startShowingDetails(degree: degree)
        }
        // Config for an Id
        else if cellRow?.value is LoggedUser {
            startShowingDocument()
        }
    }

    func setup(for cell: DocumentViewCell) {
        cell.config(degree: detailDegree, logoData: sharedMemory.imageBank?.logo(for: detailDegree?.connectionId))
    }

    func setup(for cell: DetailHeaderViewCell) {
        switch detailDegree?.type {
        case .univerityDegree:
            cell.config(title: "credentials_detail_university_name".localize(),
                        subtitle: detailDegree?.issuer?.name,
                        logoData: sharedMemory.imageBank?.logo(for: detailDegree?.connectionId),
                        type: detailDegree?.type)
        case .governmentIssuedId:
            cell.config(title: "credentials_detail_national_id_card".localize(),
                        subtitle: detailDegree?.issuer?.name,
                        logoData: sharedMemory.imageBank?.logo(for: detailDegree?.connectionId),
                        type: detailDegree?.type)
        case .certificatOfInsurance:
            cell.config(title: "credentials_detail_provider_name".localize(),
                        subtitle: detailDegree?.issuer?.name,
                        logoData: sharedMemory.imageBank?.logo(for: detailDegree?.connectionId),
                        type: detailDegree?.type)
        case .proofOfEmployment:
            cell.config(title: "credentials_detail_company_name".localize(),
                        subtitle: detailDegree?.issuer?.name,
                        logoData: sharedMemory.imageBank?.logo(for: detailDegree?.connectionId),
                        type: detailDegree?.type)
        default:
            print("Unrecognized type")
        }

    }

    func setup(for cell: DetailPropertyViewCell) {
        let detailRow = detailRows![cell.indexPath!.row]
        if let pair = detailRow.value as? (String?, String?, Bool?, CredentialType?) {
            cell.config(title: pair.0, subtitle: pair.1, isLast: pair.2, type: pair.3)
        }
    }

    func setup(for cell: DetailFooterViewCell) {
        cell.config(isNew: detailDegree?.isNew ?? false, type: detailDegree?.type)
    }

    // MARK: Accept and Decline buttons

    func tappedDeclineAction(for cell: DetailFooterViewCell?) {
        // Do nothing
    }

    func tappedConfirmAction(for cell: DetailFooterViewCell?) {
        // Do nothing
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
        return shareEmployers?[index]
    }

    func shareItemCount(for view: ShareDialogViewController) -> Int {
        return shareEmployers?.count ?? 0
    }

    func shareItemTapped(for cell: ShareDialogItemCollectionViewCell?, at index: Int, item: Any?) {

        let employer = shareEmployers![index]
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

        let employer = shareEmployers![index]
        let isSelected = employerIsSelected(employer: employer)
        cell?.config(name: employer.name,
                     logoData: employer.logo,
                     placeholderNamed: "ico_placeholder_employer", isSelected: isSelected)
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
}
