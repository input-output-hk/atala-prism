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
                                NotificationViewCellPresenterDelegate, DegreeViewCellPresenterDelegate {

    var viewImpl: NotificationsViewController? {
        return view as? NotificationsViewController
    }

    enum CredentialsMode {
        case degrees
        case detail
    }

    enum CredentialsCellType {
        case base(value: ListingBaseCellType)
        case degree // degrees mode
        case newDegreeHeader // degrees mode
        case newDegree // degree mode
    }

    struct CellRow {
        var type: CredentialsCellType
        var value: Any?
    }

    var mode: CredentialsMode = .degrees

    var degreeRows: [CellRow]?

    var detailCredential: Credential?

    private let fetchLock = NSLock()
    @Atomic var isFetching = false

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
        markViewed()
        mode = .detail
        updateViewToState()
    }

    // MARK: Buttons

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
        if mode != .detail {
            detailCredential = nil
        }
        degreeRows = []
    }

    func fetchData() {

        fetchingQueue = 1

        fetchElements()
    }

    func hasData() -> Bool {
        switch mode {
        case .degrees:
            return !(degreeRows?.isEmpty ?? true)
        case .detail:
            return true
        }
    }
    
    func getSectionHeaderViews() -> [UIView] {
        return [UIView()]
    }
    
    func getSectionCount() -> Int? {
        return 1
    }

    func getElementCount() -> [Int] {
        if let baseValue = super.getBaseElementCount() {
            return [baseValue]
        }

        switch mode {
        case .degrees:
            return [(degreeRows?.size() ?? 0)]
        case .detail:
            return [0]
        }
    }

    func getElementType(indexPath: IndexPath) -> CredentialsCellType {
        if let baseValue = super.getBaseElementType(indexPath: indexPath) {
            return .base(value: baseValue)
        }

        switch mode {
        case .degrees:
            return degreeRows![indexPath.row].type
        case .detail:
            return .degree
        }
    }

    // MARK: Fetch

    func getLoggedUser() -> LoggedUser? {
        return sharedMemory.loggedUser
    }

    func fetchElements() {

        fetchLock.lock()
        if isFetching {
            fetchLock.unlock()
            return
        }
        isFetching = true
        fetchLock.unlock()

        let contactsDao = ContactDAO()
        let contacts = contactsDao.listContacts()

        self.cleanData()
        let credentialsDao = CredentialDAO()
        let credentials = credentialsDao.listNewCredentials() ?? []
        self.makeDegreeRows(credentials: credentials)
        self.startListing()
        
        do {
            var credentialsRequests = [GetCredentialsPaginatedRequest]()
            for contact in contacts ?? [] {
                credentialsRequests.append(try .init(contactKeyPath: contact.keyPath, lastMessageId: contact.lastMessageId))
            }
            // Call the service
            ApiService.call(async: {
                do {
                    let responses = try ApiService.global.getCredentials(credentialRequests: credentialsRequests)
                    Logger.d("getCredentials responses: \(responses)")

                    let historyDao = ActivityHistoryDAO()

                    // Parse the messages
                    for response in responses {
                        for message in response.messages {
                            if let atalaMssg = try? Io_Iohk_Atala_Prism_Protos_AtalaMessage(serializedData:
                                                                                                message.message) {
                                var cred: (Credential, Bool)?

                                if !atalaMssg.plainCredential.encodedCredential.isEmpty {
                                    let issuer = contacts?.first(where: { $0.connectionId == message.connectionID})
                                    cred = credentialsDao.createCredential(message: atalaMssg,
                                                                           viewed: false, messageId: message.id,
                                                                           connectionId: message.connectionID,
                                                                           issuerName: issuer?.name ?? "")
                                }
                                if let credential = cred {
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
                    self.cleanData()
                    let credentials = credentialsDao.listNewCredentials() ?? []
                    self.makeDegreeRows(credentials: credentials)

                } catch {
                    return error
                }
                return nil
            }, success: {
                self.startListing()
                self.isFetching = false
            }, error: { _ in
                self.cleanData()
                let credentialsDao = CredentialDAO()
                let credentials = credentialsDao.listNewCredentials() ?? []
                self.makeDegreeRows(credentials: credentials)
                self.startListing()
                self.isFetching = false
            })
        } catch {
            self.cleanData()
            let credentialsDao = CredentialDAO()
            let credentials = credentialsDao.listNewCredentials() ?? []
            self.makeDegreeRows(credentials: credentials)
            self.startListing()
            self.isFetching = false
        }
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

        cell.config(title: credential.credentialName, subtitle: credential.issuerName, logoData: nil,
                    logoPlaceholderNamed: credential.logoPlaceholder, date: credential.dateReceived)
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

    // MARK: Accept and Decline buttons

    func markViewed() {

        Tracker.global.trackCredentialNewConfirm()
        let credentialsDao = CredentialDAO()
        credentialsDao.setViewed(credentialId: detailCredential?.credentialId ?? "")
        sharedMemory.loggedUser = sharedMemory.loggedUser
    }

}
