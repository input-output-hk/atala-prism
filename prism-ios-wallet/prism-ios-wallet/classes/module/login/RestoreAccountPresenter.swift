//
//  RestoreAccountPresenter.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 28/07/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import UIKit
import SwiftGRPC
import crypto

class RestoreAccountPresenter: BasePresenter {

    var viewImpl: RestoreAccountViewController? {
        return view as? RestoreAccountViewController
    }
    
    var payId: PayId?

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
                self.viewImpl?.showLoading(doShow: false)
                self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
            }
        })
    }

    func fetchCredentials(contact: Contact) {

        let contactsDao = ContactDAO()
        var cred: (Credential, Bool)?

        // Call the service
        ApiService.call(async: {
            do {
                let responses = try ApiService.global.getCredentials(contacts: [contact])
                Logger.d("getCredentials responses: \(responses)")

                let credentialsDao = CredentialDAO()

                // Parse the messages
                for response in responses {
                    for message in response.messages {
                        if let atalaMssg = try? Io_Iohk_Atala_Prism_Protos_AtalaMessage(serializedData:
                                                                                            message.message) {
                            if !atalaMssg.issuerSentCredential.credential.typeID.isEmpty {
                                cred = credentialsDao.createCredential(sentCredential:
                                    atalaMssg.issuerSentCredential.credential, viewed: false,
                                                                               messageId: message.id,
                                                                               connectionId: message.connectionID)
                            } else if !atalaMssg.plainCredential.encodedCredential.isEmpty {
                                cred = credentialsDao.createCredential(message: atalaMssg,
                                                                       viewed: false, messageId: message.id,
                                                                       connectionId: message.connectionID,
                                                                       issuerName: contact.name)
                            }
                            if cred != nil {
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
            if cred != nil {
                self.fetchNextConnection()
            } else {
                self.recoverPayId(contact: contact)
            }
        }, error: { error in
            print(error.localizedDescription)
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
        })
    }

    func recoverPayId(contact: Contact) {

        var payId: PayId?
        var messagesCount = 0
        // Call the service
        ApiService.call(async: {
            do {
                let response = try ApiService.global.getMessages(contact: contact)
                Logger.d("getMessages response: \(response)")
                

                for message in response.messages {
                    if let atalaMssg = try? Io_Iohk_Atala_Prism_Protos_AtalaMessage(serializedData:
                                                                                        message.message) {
                        let mirrorMessage = atalaMssg.mirrorMessage
                        let payIDName = mirrorMessage.payIDNameRegisteredMessage.name
                        if !payIDName.isEmpty {
                            let payIdDao = PayIdDAO()
                            payIdDao.deleteAllPayId()
                            payId = payIdDao.createPayId(payIdId: payIDName, name: payIDName,
                                                         connectionId: contact.connectionId)
                        } else if let payId = payId  {
                            let payIdDao = PayIdDAO()
                            
                            if !mirrorMessage.addressRegisteredMessage.cardanoAddress.isEmpty {
                                payIdDao.addAddress(payId: payId,
                                                    name: mirrorMessage.addressRegisteredMessage.cardanoAddress)
                                
                            } else  if !mirrorMessage.walletRegistered.extendedPublicKey.isEmpty {
                                payIdDao.addPublicKey(payId: payId,
                                                      publicKey: mirrorMessage.walletRegistered.extendedPublicKey)
                            }
                        }
                    }
                }
                let contactsDao = ContactDAO()
                contact.lastMessageId = response.messages.last?.id
                contactsDao.updateContact()
                messagesCount = response.messages.count
            } catch {
                return error
            }
            return nil
        }, success: {
            if payId != nil && messagesCount == ApiService.DEFAULT_REQUEST_LIMIT {
                self.recoverPayId(contact: contact)
            } else {
                self.payId = nil
                self.fetchNextConnection()
            }
        }, error: { _ in
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
        })
    }

    func recoveryCompleted() {
        let user = LoggedUser()
        user.apiUrl = Common.URL_API
        user.mnemonics = CryptoUtils.global.usedMnemonics
        user.dateFormat = Common.DAFAULT_DATE_FORMAT
        self.sharedMemory.loggedUser = user
        self.viewImpl?.changeScreenToSuccess(action: self.actionSuccessContinue)
    }

    // MARK: Buttons

    func tappedVerifyButton(mnemonics: [String]) {
        CryptoUtils.global.usedMnemonics = mnemonics
        CryptoUtils.global.generateSeed(mnemonics: MnemonicCode(words: mnemonics))
        if CryptoUtils.global.seed == nil {
            viewImpl?.showErrorMessage(doShow: true, message: "restore_phrase_invalid_error".localize())
        } else {
            viewImpl?.showLoading(doShow: true, message: "restore_recovering_data".localize())

            // There is a known bug in Kotlin Native that causes an error when calling "KeyDerivation().deriveKey(seed: seed, path: path)" from a background thread, making the app crash, the suggested workaround is to invoke this Kotlin method for the first time from the Main thread
            let keyPath = CryptoUtils.global.getNextPublicKeyPath()
            _ = CryptoUtils.global.signData(data: Data(), keyPath: keyPath)

            fetchNextConnection()
        }
    }

    lazy var actionSuccessContinue = SelectorAction(action: { [weak self] in
        self?.viewImpl?.goToMainScreen()
    })

}
