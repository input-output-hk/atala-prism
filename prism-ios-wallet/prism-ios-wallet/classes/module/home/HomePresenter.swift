//
//  HomePresenter.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 22/02/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation

class HomePresenter: ListingBasePresenter, ListingBaseTableUtilsPresenterDelegate, HomeProfileTableViewCellDelegate,
                     HomeActivityLogHeaderTableViewCellDelegate, HomeActivityLogTableViewCellDelegate,
                     HomePromotionalTableViewCellDelegate, HomeCardsTableViewCellDelegate {

    var viewImpl: HomeViewController? {
        return view as? HomeViewController
    }

    var notificationsCount = 0
    var activities: [ActivityHistory] = []

    private let fetchLock = NSLock()
    @Atomic var isFetching = false

    // MARK: Table

    func cleanData() {
    }

    func fetchData() {
        DispatchQueue.global(qos: .background).async {
            let credentialDao = CredentialDAO()
            self.notificationsCount = credentialDao.countNewCredentials()

            let historyDao = ActivityHistoryDAO()
            self.activities = historyDao.listRecentActivityHistory() ?? []
        }

        fetchingQueue = 1
        fetchElements()
    }

    func hasData() -> Bool {
        false
    }
    
    func getSectionHeaderViews() -> [UIView] {
        return [UIView()]
    }
    
    func getSectionCount() -> Int? {
        return 1
    }

    func getElementCount() -> [Int] {
        return [activities.count + 4]
    }

    func hasPullToRefresh() -> Bool {
        true
    }

    func actionPullToRefresh() {
        self.fetchData()
        self.updateViewToState()
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
        self.state = .fetching

        // Call the service
        ApiService.call(async: {
            do {
                let responses = try ApiService.global.getCredentials(contacts: contacts)
                Logger.d("getCredentials responses: \(responses)")

                let historyDao = ActivityHistoryDAO()

                // Parse the messages
                for response in responses {
                    for message in response.messages {
                        if let atalaMssg = try? Io_Iohk_Atala_Prism_Protos_AtalaMessage(serializedData:
                                                                                            message.message) {
                            var cred: (Credential, Bool)?
                            if !atalaMssg.issuerSentCredential.credential.typeID.isEmpty {
                                cred = credentialsDao.createCredential(sentCredential:
                                    atalaMssg.issuerSentCredential.credential, viewed: false,
                                                                               messageId: message.id,
                                                                               connectionId: message.connectionID)
                            } else if !atalaMssg.plainCredential.encodedCredential.isEmpty {
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
                self.notificationsCount = credentialsDao.countNewCredentials()

            } catch {
                return error
            }
            return nil
        }, success: {
            self.startListing()
            self.isFetching = false
        }, error: { _ in
            self.startListing()
            self.isFetching = false
        })
    }

    // MARK: HomeProfileTableViewCellDelegate

    func profileTapped(for cell: HomeProfileTableViewCell) {
        viewImpl?.changeScreenToProfile()
    }

    func notificationsTapped(for cell: HomeProfileTableViewCell) {
        viewImpl?.changeScreenToNotifications()
    }

    func setup(for cell: HomeProfileTableViewCell) {
        let user = sharedMemory.loggedUser
        let name = "\(user?.firstName ?? "") \(user?.lastName ?? "")"
        cell.config(name: name, picture: sharedMemory.profilePic, notifications: notificationsCount, delegate: self)
    }

    // MARK: HomeProfileTableViewCellDelegate

    func activityLogTapped(for cell: HomeActivityLogHeaderTableViewCell) {
        viewImpl?.changeScreenToActivityLog()
    }

    func setup(for cell: HomeActivityLogHeaderTableViewCell) {
        cell.config(empty: activities.isEmpty, delegate: self)
    }

    // MARK: HomeActivityLogTableViewCellDelegate

    func setup(for cell: HomeActivityLogTableViewCell) {
        let index = (cell.indexPath?.row ?? 3) - 3
        cell.config(history: activities[index])
    }

    // MARK: HomePromotionalTableViewCellDelegate

    func setup(for cell: HomePromotionalTableViewCell) {
        cell.config(delegate: self)
    }

    func promotionalMoreInfoTapped(for cell: HomePromotionalTableViewCell) {
        viewImpl?.changeScreenToIntdemoTutorial()
    }

    func promotionalShareTapped(for cell: HomePromotionalTableViewCell) {
        let items: [Any] = [URL(string: "https://apps.apple.com/us/app/atala-prism/id1515523675")!]
        let activity = UIActivityViewController(activityItems: items, applicationActivities: nil)
        viewImpl?.present(activity, animated: true)
    }

    // MARK: HomeCardsTableViewCellDelegate

    func setup(for cell: HomeCardsTableViewCell) {
        let loggedUser = sharedMemory.loggedUser
        let payIdDao = PayIdDAO()
        let countPayId = payIdDao.countPayId()
        cell.config(hidePayId: (loggedUser?.payIdCardDismissed ?? false) || countPayId > 0,
                    hideVerifyId: loggedUser?.verifyIdCardDismissed ?? false, delegate: self)
    }

    func payIdTapped(for cell: HomeCardsTableViewCell) {
        viewImpl?.changeScreenToCreatePayId()
    }

    func dismissPayIdTapped(for cell: HomeCardsTableViewCell) {
        sharedMemory.loggedUser?.payIdCardDismissed = true
        sharedMemory.loggedUser = sharedMemory.loggedUser
        viewImpl?.table.reloadData()
    }

    func verifyIdTapped(for cell: HomeCardsTableViewCell) {
        // TODO: this will be implemented with the verify id functionality
    }

    func dismissVerifyIdTapped(for cell: HomeCardsTableViewCell) {
        sharedMemory.loggedUser?.verifyIdCardDismissed = true
        sharedMemory.loggedUser = sharedMemory.loggedUser
        viewImpl?.table.reloadData()
    }
}
