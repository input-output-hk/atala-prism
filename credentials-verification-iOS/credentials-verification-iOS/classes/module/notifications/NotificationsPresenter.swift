//
//  NotificationsPresenter.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 16/04/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import SwiftGRPC
import SwiftProtobuf
import ObjectMapper

class NotificationsPresenter: ListingBasePresenter, ListingBaseTableUtilsPresenterDelegate,
                                NewDegreeViewCellPresenterDelegate, DegreeViewCellPresenterDelegate,
                                NewDegreeHeaderViewCellPresenterDelegate, DocumentViewCellPresenterDelegate,
                                DetailHeaderViewCellPresenterDelegate, DetailFooterViewCellPresenterDelegate,
                                DetailPropertyViewCellPresenterDelegate, ConnectionConfirmPresenterDelegate,
                                ConnectionsWorkerDelegate {

    var viewImpl: NotificationsViewController? {
        return view as? NotificationsViewController
    }

    enum ConnectionsSpecialState {
        case none
        case scanningQr
    }

    enum CredentialsMode {
        case degrees
        case document
        case detail
    }

    enum CredentialsCellType {
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
        var type: CredentialsCellType
        var value: Any?
    }

    var mode: CredentialsMode = .degrees

    var degreeRows: [CellRow]?
    var detailRows: [CellRow]?

    var detailDegree: Degree?

    var connectionsWorker = ConnectionsWorker()
    var stateSpecial: ConnectionsSpecialState = .none

    var contacts: [Contact] = []

//    var isFetching = false

    override init() {
        super.init()
        connectionsWorker.delegate = self
    }

    // MARK: Modes

    func isScanningQr() -> Bool {
        return self.state == .special && self.stateSpecial == .scanningQr
    }

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
        tappedConfirmAction(for: nil)
        mode = .detail
        updateViewToState()
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
        if mode != .degrees {
            actionPullToRefresh()
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
        self.connectionsWorker.fetchConnections()
    }

    func fetchCredentials() {

//        if isFetching { return }
//        isFetching = true
        let contactsDao = ContactDAO()
        let contacts = contactsDao.listContacts()

        // Call the service
        ApiService.call(async: {
            do {
                let responses = try ApiService.global.getCredentials(contacts: contacts)
                Logger.d("getCredentials responses: \(responses)")

                let credentialsDao = CredentialDAO()
                // Parse the messages
                for response in responses {
                    for message in response.messages {
                        if let atalaMssg = try? Io_Iohk_Prism_Protos_AtalaMessage(serializedData: message.message) {
                            if !atalaMssg.issuerSentCredential.credential.typeID.isEmpty,
                                let credential = credentialsDao.createCredential(sentCredential:
                                    atalaMssg.issuerSentCredential.credential, viewed: false,
                                                                               messageId: message.id) {
                                contactsDao.updateMessageId(did: credential.issuerId, messageId: message.id)
                            }
                        }
                    }
                }
                self.cleanData()
                let credentials = credentialsDao.listNewCredentials() ?? []
                self.makeDegreeRows(degrees: credentials)

            } catch {
                return error
            }
            return nil
        }, success: {
//            self.isFetching = false
            self.startListing()
        }, error: { _ in
//        self.isFetching = false
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
        })
    }

    private func makeDegreeRows(degrees: [Credential]) {

        // Transform data into rows
        if degrees.size() > 0 {
            self.degreeRows?.append(CellRow(type: .newDegreeHeader, value: nil))
        }
        degrees.forEach { degree in
            self.degreeRows?.append(CellRow(type: .newDegree, value: degree))
        }
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

        guard let rowIndex = cell.indexPath?.row, let cellRow = degreeRows?[rowIndex],
            let credential = cellRow.value as? Credential else {
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
        switch CredentialType(rawValue: credential.type) {
        case .univerityDegree:
            title = "credentials_university_degree".localize()
            placeholder = "icon_university"
        case .governmentIssuedId:
            title = "credentials_government_id".localize()
            placeholder = "icon_id"
        case .proofOfEmployment:
            title = "credentials_proof_employment".localize()
            placeholder = "icon_proof_employment"
        case .certificatOfInsurance:
            title = "credentials_certificate_insurance".localize()
            placeholder = "icon_insurance"
        default:
            print("Unrecognized type")
        }

        cell.config(title: title, subtitle: credential.issuerName, logoData: nil,
                    logoPlaceholderNamed: placeholder, isLast: isLast)
    }

    func tappedAction(for cell: NewDegreeViewCell) {

        guard let rowIndex = cell.indexPath?.row, let cellRow = degreeRows?[rowIndex],
            let credential = cellRow.value as? Credential else {
            return
        }
        // FIXME this should be updated for HTML credentials
        if let degree = Mapper<Degree>().map(JSONString: credential.htmlView) {

            degree.intCredential = credential.encoded
            degree.type = CredentialType(rawValue: credential.type)
            degree.isNew = credential.viewed
            degree.messageId = credential.credentialId
            startShowingDetails(degree: degree)
        }
    }

    func didSelectRowAt(indexPath: IndexPath) {
        if mode == .degrees {
            let rowIndex = indexPath.row
            guard let cellRow = degreeRows?[rowIndex], let credential = cellRow.value as? Credential else {
                return
            }
            // FIXME this should be updated for HTML credentials
            if let degree = Mapper<Degree>().map(JSONString: credential.htmlView) {

                degree.intCredential = credential.encoded
                degree.type = CredentialType(rawValue: credential.type)
                degree.isNew = credential.viewed
                degree.messageId = credential.credentialId
                startShowingDetails(degree: degree)
            }
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
                title = "credentials_university_degree".localize()
                placeholder = "icon_university"
            case .governmentIssuedId:
                title = "credentials_government_id".localize()
                placeholder = "icon_id"
            case .proofOfEmployment:
                title = "credentials_proof_employment".localize()
                placeholder = "icon_proof_employment"
            case .certificatOfInsurance:
                title = "credentials_certificate_insurance".localize()
                placeholder = "icon_insurance"
            default:
                print("Unrecognized type")
            }
            cell.config(title: title, subtitle: degree.issuer?.name, logoData: nil, logoPlaceholderNamed: placeholder)
        }
        // Config for an Id
        else if cellRow?.value is LoggedUser {
            cell.config(title: "credentials_document_title".localize(), subtitle: nil,
                        logoData: nil, logoPlaceholderNamed: "ico_placeholder_credential")
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

    func setup(for cell: NewDegreeHeaderViewCell) {
        cell.config(name: getLoggedUser()?.firstName)
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
        cell.config(isNew: false, type: detailDegree?.type)
    }

    // MARK: Accept and Decline buttons

    func tappedDeclineAction(for cell: DetailFooterViewCell?) {

        startShowingDegrees()
        actionPullToRefresh()
    }

    func tappedConfirmAction(for cell: DetailFooterViewCell?) {

        Tracker.global.trackCredentialNewConfirm()
        let credentialsDao = CredentialDAO()
        credentialsDao.setViewed(credentialId: detailDegree?.messageId ?? "")
        sharedMemory.loggedUser = sharedMemory.loggedUser
    }

    // MARK: QR Reader

    func scannedQrCode(_ str: String) {
        Logger.d("Scanned: \(str)")
        self.connectionsWorker.validateQrCode(str, contacts: self.contacts)
    }

    func tappedDeclineAction(for: ConnectionConfirmViewController) {

        Tracker.global.trackConnectionDecline()
    }

    func tappedConfirmAction(for viewController: ConnectionConfirmViewController) {

        Tracker.global.trackConnectionAccept()
        self.connectionsWorker.confirmQrCode()
    }

    // MARK: ConnectionsWorkerDelegate

    func contactsFetched(contacts: [Contact]) {
        self.contacts.removeAll()
        self.contacts.append(contacts)
        fetchCredentials()
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
        NotificationCenter.default.post(name: .showContactsScreen, object: nil)
    }

}
