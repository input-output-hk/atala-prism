//
//  RestoreAccountPresenter.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 28/07/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import UIKit
import SwiftGRPC

class RestoreAccountPresenter: BasePresenter {

    var viewImpl: RestoreAccountViewController? {
        return view as? RestoreAccountViewController
    }

    func fetchNextConnection() {

        var contact: Contact?
        // Call the service
        ApiService.call(async: {
            do {
                let response = try ApiService.global.getConnection(keyPath: CryptoUtils.global.getNextPublicKeyPath())
                Logger.d("getConnections responsed: \(response)")

                // Parse data// Save the contact
                if !response.connections.isEmpty {
                    let keyPath = CryptoUtils.global.confirmNewKeyUsed()
                    let dao = ContactDAO()
                    DispatchQueue.main.sync {
                        contact =  dao.createContact(connectionInfo: response.connections[0], keyPath: keyPath)
                    }
                }
            } catch {
                return error
            }
            return nil
        }, success: {
            self.fetchCredentials(contact: contact!)
        }, error: { error in
            if let err = error as? RPCError,
                err.callResult?.statusMessage?.contains("Unknown encodedPublicKey") ?? false {
                self.recoveryCompleted()
            } else {
                self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
            }
        })
    }

    func fetchCredentials(contact: Contact) {

        let contactsDao = ContactDAO()

        // Call the service
        ApiService.call(async: {
            do {
                let responses = try ApiService.global.getCredentials(contacts: [contact])
                Logger.d("getCredentials responses: \(responses)")

                let credentialsDao = CredentialDAO()

                // Parse the messages
                for response in responses {
                    for message in response.messages {
                        if let atalaMssg = try? Io_Iohk_Prism_Protos_AtalaMessage(serializedData: message.message) {
                            if !atalaMssg.issuerSentCredential.credential.typeID.isEmpty,
                                let credential = credentialsDao.createCredential(sentCredential:
                                    atalaMssg.issuerSentCredential.credential, viewed: false,
                                                                               messageId: message.id,
                                                                               connectionId: message.connectionID) {
                                contact.lastMessageId = message.id
                                contactsDao.updateContact()
                            }
                        }
                    }
                }

            } catch {
                return error
            }
            return nil
        }, success: {
            self.fetchNextConnection()
        }, error: { error in
            print(error.localizedDescription)
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
        })
    }

    func recoveryCompleted() {
        let user = LoggedUser()
        user.apiUrl = Common.URL_API
        user.mnemonics = CryptoUtils.global.usedMnemonics
        user.seed = CryptoUtils.global.seed
        user.dateFormat = Common.DAFAULT_DATE_FORMAT
        self.sharedMemory.loggedUser = user
        self.viewImpl?.changeScreenToSuccess(action: self.actionSuccessContinue)
    }

    // MARK: Buttons

    func tappedVerifyButton(mnemonics: [String]) {
        CryptoUtils.global.usedMnemonics = mnemonics
        CryptoUtils.global.generateSeed()
        if CryptoUtils.global.seed == nil {
            viewImpl?.showErrorMessage(doShow: true, message: "Invalid mnemonics")
        } else {
            viewImpl?.showLoading(doShow: true, message: "Recovering data")
            fetchNextConnection()
        }
    }

    lazy var actionSuccessContinue = SelectorAction(action: { [weak self] in
        self?.viewImpl?.goToMainScreen()
    })

}
