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
                                NotificationViewCellPresenterDelegate, DegreeViewCellPresenterDelegate,
                                 ActivityLogViewCellPresenterDelegate {

    var viewImpl: NotificationsViewController? {
        return view as? NotificationsViewController
    }

    enum CredentialsMode {
        case degrees
        case activityLog
        case detail
    }

    enum CredentialsCellType {
        case base(value: ListingBaseCellType)
        case degree // degrees mode
        case newDegreeHeader // degrees mode
        case newDegree // degree mode
        case activityLog // activity log mode
    }

    struct CellRow {
        var type: CredentialsCellType
        var value: Any?
    }

    var mode: CredentialsMode = .degrees

    var degreeRows: [CellRow]?

    var detailCredential: Credential?

    var activityLogs: [ActivityHistory]?

    // MARK: Modes

    func getMode() -> CredentialsMode {
        return mode
    }

    func startShowingDegrees() {

        mode = .degrees
        updateViewToState()
    }

    func startShowingActivityLog() {

        mode = .activityLog
        let dao = ActivityHistoryDAO()
        activityLogs = dao.listActivityHistory()
        updateViewToState()
    }

    func startShowingDetails(credential: Credential) {

        detailCredential = credential
        markViewed()
        mode = .detail
        updateViewToState()
    }

    // MARK: Buttons

    func tappedHistoryButton() {
        self.startShowingActivityLog()
    }

    @discardableResult
    func tappedBackButton() -> Bool {

        if mode != .degrees {
            self.startShowingDegrees()
            self.fetchData()
            self.updateViewToState()
            return true
        }
        return false
    }

    // MARK: ListingBaseTableUtilsPresenterDelegate

    func cleanData() {
        detailCredential = nil
        degreeRows = []
    }

    func fetchData() {

        state = .fetching
        fetchingQueue = 1

        fetchElements()
    }

    func hasData() -> Bool {
        switch mode {
        case .degrees:
            return !(degreeRows?.isEmpty ?? true)
        case .detail:
            return true
        case .activityLog:
            return !(activityLogs?.isEmpty ?? true)
        }
    }

    func getElementCount() -> Int {
        if let baseValue = super.getBaseElementCount() {
            return baseValue
        }

        switch mode {
        case .degrees:
            return degreeRows?.size() ?? 0
        case .activityLog:
            return activityLogs?.size() ?? 0
        case .detail:
            return 0
        }
    }

    func getElementType(indexPath: IndexPath) -> CredentialsCellType {
        if let baseValue = super.getBaseElementType(indexPath: indexPath) {
            return .base(value: baseValue)
        }

        switch mode {
        case .degrees:
            return degreeRows![indexPath.row].type
        case .activityLog:
            return .activityLog
        case .detail:
            return .degree
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
                self.cleanData()
                let credentials = credentialsDao.listNewCredentials() ?? []
                self.makeDegreeRows(credentials: credentials)

            } catch {
                return error
            }
            return nil
        }, success: {
            self.startListing()
        }, error: { _ in
            self.cleanData()
            let credentialsDao = CredentialDAO()
            let credentials = credentialsDao.listNewCredentials() ?? []
            self.makeDegreeRows(credentials: credentials)
            self.startListing()
        })
    }

    private func makeDegreeRows(credentials: [Credential]) {

        // Transform data into rows
        if credentials.size() > 0 {
            self.degreeRows?.append(CellRow(type: .newDegreeHeader, value: nil))
        }
        credentials.forEach { credential in
            self.degreeRows?.append(CellRow(type: .newDegree, value: credential))
        }
    }

    // MARK: Table

    func hasPullToRefresh() -> Bool {
        mode == .degrees
    }

    func actionPullToRefresh() {

        if mode == .degrees {
            self.startShowingDegrees()
            self.fetchData()
            self.updateViewToState()
        }
    }

    func setup(for cell: NotificationViewCell) {

        guard let rowIndex = cell.indexPath?.row, let cellRow = degreeRows?[rowIndex],
            let credential = cellRow.value as? Credential else {
            return
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
                    logoPlaceholderNamed: placeholder, date: credential.dateReceived)
    }

    func tappedAction(for cell: NotificationViewCell) {

        guard let rowIndex = cell.indexPath?.row, let cellRow = degreeRows?[rowIndex],
            let credential = cellRow.value as? Credential else {
            return
        }
        startShowingDetails(credential: credential)
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
        if let credential = cellRow?.value as? Credential {
            startShowingDetails(credential: credential)
        }
    }

    func setup(for cell: ActivityLogTableViewCell) {
        let detailRow = activityLogs![cell.indexPath!.row]
        cell.config(history: detailRow)
    }

    // MARK: Accept and Decline buttons

    func markViewed() {

        Tracker.global.trackCredentialNewConfirm()
        let credentialsDao = CredentialDAO()
        credentialsDao.setViewed(credentialId: detailCredential?.credentialId ?? "")
        sharedMemory.loggedUser = sharedMemory.loggedUser
    }

}
