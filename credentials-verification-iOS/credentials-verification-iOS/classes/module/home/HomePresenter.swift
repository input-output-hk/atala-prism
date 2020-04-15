//
import SwiftGRPC
import SwiftProtobuf

class HomePresenter: ListingBasePresenter, ListingBaseTableUtilsPresenterDelegate, NewDegreeViewCellPresenterDelegate, DegreeViewCellPresenterDelegate, NewDegreeHeaderViewCellPresenterDelegate, DocumentViewCellPresenterDelegate, DetailHeaderViewCellPresenterDelegate, DetailFooterViewCellPresenterDelegate, DetailPropertyViewCellPresenterDelegate, ShareDialogPresenterDelegate {

    var viewImpl: HomeViewController? {
        return view as? HomeViewController
    }

    enum HomeMode {
        case degrees
        case document
        case detail
    }

    enum HomeCellType {
        case base(value: ListingBaseCellType)
        case degree // degrees mode
        case newDegreeHeader // degrees mode
        case newDegree // degree mode
        case document // document mode
        case detailHeader // detail mode
        case detailProperty // detail mode
        case detailFooter // detail mode
    }

    struct CellRow {
        var type: HomeCellType
        var value: Any?
    }

    var mode: HomeMode = .degrees

    var degreeRows: [CellRow]?
    var detailRows: [CellRow]?

    var detailDegree: Degree?

    var shareEmployers: [ConnectionBase]?
    var shareSelectedEmployers: [ConnectionBase]?

    // MARK: Modes

    func getMode() -> HomeMode {
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
            detailRows?.append(CellRow(type: .detailProperty, value: ("home_detail_university_name".localize(), degree.issuer?.name, false, degree.type)))
            detailRows?.append(CellRow(type: .detailProperty, value: ("home_detail_award".localize(), degree.credentialSubject?.degreeResult, false, degree.type)))
            detailRows?.append(CellRow(type: .detailProperty, value: ("home_detail_full_name".localize(), degree.credentialSubject?.name, false, degree.type)))
            detailRows?.append(CellRow(type: .detailProperty, value: ("home_detail_graduation_date".localize(), degree.issuanceDate, true,  degree.type)))
        case .governmentIssuedId:
            detailRows?.append(CellRow(type: .document, value: degree))
        case .certificatOfInsurance:
            detailRows?.append(CellRow(type: .detailProperty, value: ("home_detail_employment_class_insurance".localize(), degree.productClass, false, degree.type)))
            detailRows?.append(CellRow(type: .detailProperty, value: ("home_detail_employment_policy_number".localize(), degree.policyNumber, false, degree.type)))
            detailRows?.append(CellRow(type: .detailProperty, value: ("home_detail_full_name".localize(), degree.credentialSubject?.name, false, degree.type)))
            detailRows?.append(CellRow(type: .detailProperty, value: ("home_detail_employment_policy_end_date".localize(), degree.expiryDate, true,  degree.type)))
        case .proofOfEmployment:
            detailRows?.append(CellRow(type: .detailProperty, value: ("home_detail_employment_status".localize(), degree.employmentStatus, false, degree.type)))
            detailRows?.append(CellRow(type: .detailProperty, value: ("home_detail_full_name".localize(), degree.credentialSubject?.name, false, degree.type)))
            detailRows?.append(CellRow(type: .detailProperty, value: ("home_detail_employment_start_date".localize(), degree.issuanceDate, true,  degree.type)))
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
        degreeRows = []
        detailRows = []
    }

    func fetchData() {

        state = .fetching
        fetchingQueue = 1

        fetchElements()
    }

    func hasData() -> Bool {
        switch mode {
        case .degrees:
            return (degreeRows?.size() ?? 0) > 0
        case .detail:
            return (detailRows?.size() ?? 0) > 0
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
            return (degreeRows?.size() ?? 0)
        case .document:
            return 1
        case .detail:
            return (detailRows?.size() ?? 0)
        }
    }

    func getElementType(indexPath: IndexPath) -> HomeCellType {
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

        guard let user = self.sharedMemory.loggedUser else {
            return
        }

        // Call the service
        ApiService.call(async: {
            do {
                let responses = try ApiService.global.getCredentials(userIds: user.connectionUserIds?.valuesArray)
                Logger.d("getCredentials responses: \(responses)")

                self.cleanData()

                var credentials: [Degree] = []
                // Parse the messages
                for response in responses {
                    for message in response.messages {
                        let isRejected = user.messagesRejectedIds?.contains(message.id) ?? false
                        let isNew = !(user.messagesAcceptedIds?.contains(message.id) ?? false)
                        if !isRejected {
                            if let atalaMssg = try? Io_Iohk_Prism_Protos_AtalaMessage(serializedData: message.message) {
                                if !atalaMssg.issuerSentCredential.credential.typeID.isEmpty {
                                    if let credential = Degree.build(atalaMssg.issuerSentCredential.credential, messageId: message.id, isNew: isNew) {
                                        credentials.append(credential)
                                    }
                                }
                            }
                        }
                    }
                }
                self.makeDegreeRows(degrees: credentials)

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

    private func makeDegreeRows(degrees: [Degree]) {

        let newDeg = degrees.filter { $0.isNew ?? false }
        let oldDeg = degrees.filter { !($0.isNew ?? false) }

        // Transform data into rows
        if newDeg.size() > 0 {
            self.degreeRows?.append(CellRow(type: .newDegreeHeader, value: nil))
        }
        newDeg.forEach { degree in
            self.degreeRows?.append(CellRow(type: .newDegree, value: degree))
        }
        oldDeg.forEach { degree in
            self.degreeRows?.append(CellRow(type: .degree, value: degree))
        }
        // NOTE: Won't add the National Id for now
        if getLoggedUser()?.identityNumber != nil {
            // self.degreeRows?.append(CellRow(type: .degree, value: getLoggedUser()))
        }
    }

    private func fetchShareEmployers() {

        viewImpl?.config(isLoading: true)

        // Call the service
        ApiService.call(async: {
            do {
                let responses = try ApiService.global.getConnections(userIds: self.sharedMemory.loggedUser?.connectionUserIds?.valuesArray)
                Logger.d("getConnections response: \(responses)")

                // Clean data
                self.shareEmployers = []
                self.shareSelectedEmployers = []

                // Parse data
                let parsedResponse = ConnectionMaker.parseResponseList(responses)
                self.shareEmployers?.append(contentsOf: parsedResponse)
            } catch {
                return error
            }
            return nil
        }, success: {
            self.viewImpl?.config(isLoading: false)
            self.viewImpl?.showShareDialog()
        }, error: { error in
            self.viewImpl?.config(isLoading: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: error.localizedDescription)
        })
    }

    private func shareWithSelectedEmployers() {

        Tracker.global.trackCredentialShareCompleted()
        viewImpl?.config(isLoading: true)

        // Call the service
        ApiService.call(async: {
            do {
                var userIds: [String] = []
                var connectionIds: [String] = []
                for employer in self.shareSelectedEmployers ?? [] {
                    if let connId = employer.connectionId, let userId = self.sharedMemory.loggedUser?.connectionUserIds?[connId] {
                        connectionIds.append(connId)
                        userIds.append(userId)
                    }
                }
                let responses = try ApiService.global.shareCredential(userIds: userIds, connectionIds: connectionIds, degree: self.detailDegree!)
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
            self.viewImpl?.showSuccessMessage(doShow: true, message: "home_detail_share_success".localize(), actions: actions)
        }, error: { error in
            self.viewImpl?.config(isLoading: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: error.localizedDescription)
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

    func setup(for cell: NewDegreeViewCell) {

        guard let rowIndex = cell.indexPath?.row, let cellRow = degreeRows?[rowIndex], let degree = cellRow.value as? Degree else {
            return
        }

        var isLast = degreeRows!.count - 1 == rowIndex
        if !isLast {
            switch degreeRows![rowIndex + 1].type {
            case .newDegree:
                isLast = false
            default:
                isLast = true
            }
        }
        var title = ""
        var placeholder = ""
        switch degree.type {
        case .univerityDegree:
            title = "home_university_degree".localize()
            placeholder = "icon_university"
        case .governmentIssuedId:
            title = "home_government_id".localize()
            placeholder = "icon_id"
        case .proofOfEmployment:
            title = "home_proof_employment".localize()
            placeholder = "icon_proof_employment"
        case .certificatOfInsurance:
            title = "home_certificate_insurance".localize()
            placeholder = "icon_insurance"
        default:
            print("Unrecognized type")
        }

        cell.config(title: title, subtitle: degree.issuer?.name, logoData: nil, logoPlaceholderNamed: placeholder, isLast: isLast)
    }

    func tappedAction(for cell: NewDegreeViewCell) {

        guard let rowIndex = cell.indexPath?.row, let cellRow = degreeRows?[rowIndex], let degree = cellRow.value as? Degree else {
            return
        }
        Tracker.global.trackCredentialNewTapped()
        startShowingDetails(degree: degree)
    }
    
    func didSelectRowAt(indexPath: IndexPath) {
        if mode == .degrees {
            let rowIndex = indexPath.row
            guard let cellRow = degreeRows?[rowIndex], let degree = cellRow.value as? Degree else {
                return
            }
            Tracker.global.trackCredentialNewTapped()
            startShowingDetails(degree: degree)
        }
    }

    func setup(for cell: CommonViewCell) {

        let cellRow = degreeRows?[cell.indexPath!.row]
        // Config for a Degree
        if let degree = cellRow?.value as? Degree {
            var title = ""
            var placeholder = ""
            switch degree.type {
            case .univerityDegree:
                title = "home_university_degree".localize()
                placeholder = "icon_university"
            case .governmentIssuedId:
                title = "home_government_id".localize()
                placeholder = "icon_id"
            case .proofOfEmployment:
                title = "home_proof_employment".localize()
                placeholder = "icon_proof_employment"
            case .certificatOfInsurance:
                title = "home_certificate_insurance".localize()
                placeholder = "icon_insurance"
            default:
                print("Unrecognized type")
            }
            cell.config(title: title, subtitle: degree.issuer?.name, logoData: nil, logoPlaceholderNamed: placeholder)
        }
        // Config for an Id
        else if let _ = cellRow?.value as? LoggedUser {
            cell.config(title: "home_document_title".localize(), subtitle: nil, logoData: nil, logoPlaceholderNamed: "ico_placeholder_credential")
        }
    }

    func tappedAction(for cell: CommonViewCell) {

        let cellRow = degreeRows?[cell.indexPath!.row]
        // Config for a Degree
        if let degree = cellRow?.value as? Degree {
            startShowingDetails(degree: degree)
        }
        // Config for an Id
        else if let _ = cellRow?.value as? LoggedUser {
            startShowingDocument()
        }
    }

    func setup(for cell: NewDegreeHeaderViewCell) {
        cell.config(name: getLoggedUser()?.firstName)
    }

    func setup(for cell: DocumentViewCell) {
        cell.config(degree: detailDegree, logoData: sharedMemory.imageBank?.logo(for: detailDegree?.connectionId))
    }

    func setup(for cell: DetailHeaderViewCell) {
        switch detailDegree?.type {
        case .univerityDegree:
            cell.config(title: "home_detail_degree_name".localize(), subtitle: detailDegree?.credentialSubject?.degreeAwarded, logoData: sharedMemory.imageBank?.logo(for: detailDegree?.connectionId), type: detailDegree?.type)
        case .governmentIssuedId:
            cell.config(title: "home_detail_national_id_card".localize(), subtitle: detailDegree?.issuer?.name, logoData: sharedMemory.imageBank?.logo(for: detailDegree?.connectionId), type: detailDegree?.type)
        case .certificatOfInsurance:
        cell.config(title: "home_detail_provider_name".localize(), subtitle: detailDegree?.issuer?.name, logoData: sharedMemory.imageBank?.logo(for: detailDegree?.connectionId), type: detailDegree?.type)
        case .proofOfEmployment:
        cell.config(title: "home_detail_company_name".localize(), subtitle: detailDegree?.issuer?.name, logoData: sharedMemory.imageBank?.logo(for: detailDegree?.connectionId), type: detailDegree?.type)
        default:
            print("Unrecognized type")
        }
        
    }

    func setup(for cell: DetailPropertyViewCell) {
        let detailRow = detailRows![cell.indexPath!.row]
        let pair = detailRow.value as! (String?, String?, Bool?, CredentialType?)
        cell.config(title: pair.0, subtitle: pair.1, isLast: pair.2, type: pair.3)
    }
    
    func setup(for cell: DetailFooterViewCell) {
        cell.config(isNew: detailDegree?.isNew ?? false, type: detailDegree?.type)
    }

    // MARK: Accept and Decline buttons

    func tappedDeclineAction(for cell: DetailFooterViewCell) {

        Tracker.global.trackCredentialNewDecline()
        sharedMemory.loggedUser?.messagesRejectedIds?.append(detailDegree!.messageId!)
        sharedMemory.loggedUser = sharedMemory.loggedUser
        startShowingDegrees()
        actionPullToRefresh()
    }

    func tappedConfirmAction(for cell: DetailFooterViewCell) {

        Tracker.global.trackCredentialNewConfirm()
        sharedMemory.loggedUser?.messagesAcceptedIds?.append(detailDegree!.messageId!)
        sharedMemory.loggedUser = sharedMemory.loggedUser
        tappedBackButton()
        actionPullToRefresh()
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
        // TODO: Refresh better
        shareItemConfig(for: cell, at: index, item: item)
        cell?.refreshView()
    }

    func employerIsSelected(employer: ConnectionBase) -> Bool {
        return shareSelectedEmployers?.contains(where: { $0.connectionId == employer.connectionId }) ?? false
    }

    func shareItemConfig(for cell: ShareDialogItemCollectionViewCell?, at index: Int, item: Any?) {

        let employer = shareEmployers![index]
        let isSelected = employerIsSelected(employer: employer)
        cell?.config(name: employer.name, logoData: sharedMemory.imageBank?.logo(for: employer.connectionId), placeholderNamed: "ico_placeholder_employer", isSelected: isSelected)
    }
}
