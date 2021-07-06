//
//  PayIDSetupPresenter.swift
//  prism-ios-wallet
//
//  Created by Roberto Daviduk on 19/03/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation

class PayIDSetupPresenter: BasePresenter, ConnectionsWorkerDelegate {

    var viewImpl: PayIDSetupViewController? {
        return view as? PayIDSetupViewController
    }

    var connectionsWorker = ConnectionsWorker()
    var connectionToken: String?
    var contact: Contact?
    var payIdName: String?
    var payIdAddress: String?
    var credentials: [Credential]?
    var payId: PayId?
    var availableName = false

    override init() {
        super.init()
        connectionsWorker.delegate = self
    }

    // MARK: Buttons

    @discardableResult
    func tappedBackButton() -> Bool {
        return false
    }

    // MARK: Fetch
    func createAccount() {

        let dao = ContactDAO()
        if let contact = dao.getPayIdContact() {
            self.conectionAccepted(contact: contact)
            return
        }

        self.viewImpl?.showLoading(doShow: true)

        // Call the service
        ApiService.call(async: {
            do {
                let response = try ApiService.global.createMirrorAccount()
                Logger.d("addConnectionToken response: \(response)")
                self.connectionToken = response.connectionToken
            } catch {
                return error
            }
            return nil
        }, success: {
            self.connectionsWorker.validateQrCode(self.connectionToken!, contacts: [])
        }, error: { error in
            print(error.localizedDescription)
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "connections_scan_qr_confirm_error".localize())
        })
    }

    func createPayId() {

        guard let contact = self.contact, let payIdName = payIdName else {
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
            return
        }
        self.viewImpl?.showLoading(doShow: true)
        var messageId = ""

        // Call the service
        ApiService.call(async: {
            do {
                let response = try ApiService.global.registerPayIdName(contact: contact, name: payIdName)
                Logger.d("shareCredential response: \(response)")

                let payIdDao = PayIdDAO()
                self.payId = payIdDao.createPayId(payIdId: payIdName, name: payIdName,
                                                  connectionId: contact.connectionId)
                messageId = response.id

            } catch {
                return error
            }
            return nil
        }, success: {
            DispatchQueue.main.asyncAfter(deadline: .now() + 5) {
                self.getCreatedPayId(messageId: messageId)
            }
        }, error: { _ in
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
        })
    }

    func getCreatedPayId(messageId: String) {

        guard let contact = self.contact else {
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
            return
        }

        // Call the service
        ApiService.call(async: {
            do {
                let response = try ApiService.global.getMessages(contact: contact)
                Logger.d("shareCredential response: \(response)")
                for message in response.messages {
                    if let atalaMssg = try? Io_Iohk_Atala_Prism_Protos_AtalaMessage(serializedData:
                                                                                        message.message),
                       atalaMssg.replyTo == messageId {
                        self.availableName = atalaMssg.mirrorMessage.payIDNameRegisteredMessage.isInitialized
                        let contactsDao = ContactDAO()
                        contactsDao.updateMessageId(connectionId: contact.connectionId, messageId: message.id)
                    }
                }
            } catch {
                return error
            }
            return nil
        }, success: {
            if let address = self.payIdAddress {
                if CryptoUtils.global.isValidShelleyAddress(address: address) {
                    self.registerAddress()
                } else {
                    self.registerPublicKey()
                }
            } else {
                self.viewImpl?.showLoading(doShow: false)
                self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
            }
        }, error: { error in
            print(error)
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
        })
    }

    private func registerAddress() {

        guard let contact = self.contact, let payIdAddress = payIdAddress else {
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
            return
        }

        var messageId = ""
        // Call the service
        ApiService.call(async: {
            do {
                let response = try ApiService.global.registerPayIdAddress(contact: contact, address: payIdAddress)
                Logger.d("registerPayIdAddress response: \(response)")

                messageId = response.id
            } catch {
                return error
            }
            return nil
        }, success: {
            DispatchQueue.main.asyncAfter(deadline: .now() + 5) {
                self.getRegisteredAddress(messageId: messageId, address: payIdAddress)
            }
        }, error: { error in
            print(error.localizedDescription)
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
        })
    }

    func getRegisteredAddress(messageId: String, address: String) {

        guard let contact = self.contact else {
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
            return
        }

        // Call the service
        ApiService.call(async: {
            do {
                let response = try ApiService.global.getMessages(contact: contact)
                Logger.d("registerPayIdPublicKey response: \(response)")
                for message in response.messages {
                    if let atalaMssg = try? Io_Iohk_Atala_Prism_Protos_AtalaMessage(serializedData:
                                                                                        message.message),
                       atalaMssg.replyTo == messageId {
                        if atalaMssg.mirrorMessage.walletRegistered.isInitialized {
                            let payIdDao = PayIdDAO()
                            payIdDao.addAddress(payId: self.payId!, name: address)
                            let contactsDao = ContactDAO()
                            contactsDao.updateMessageId(connectionId: contact.connectionId, messageId: message.id)
                        }
                    }
                }
            } catch {
                return error
            }
            return nil
        }, success: {
            self.shareCredentials()
        }, error: { error in
            print(error)
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
        })
    }

    private func registerPublicKey() {

        guard let contact = self.contact, let publicKey = payIdAddress else {
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
            return
        }
        var messageId = ""

        // Call the service
        ApiService.call(async: {
            do {
                let response = try ApiService.global.registerPayIdPublicKey(contact: contact, publicKey: publicKey)
                Logger.d("shareCredential response: \(response)")
                
                messageId = response.id
            } catch {
                return error
            }
            return nil
        }, success: {
            DispatchQueue.main.asyncAfter(deadline: .now() + 5) {
                self.getRegisteredPublicKey(messageId: messageId, publicKey: publicKey)
            }
        }, error: { error in
            print(error.localizedDescription)
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
        })
    }
    
    func getRegisteredPublicKey(messageId: String, publicKey: String) {

        guard let contact = self.contact else {
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
            return
        }

        // Call the service
        ApiService.call(async: {
            do {
                let response = try ApiService.global.getMessages(contact: contact)
                Logger.d("registerPayIdPublicKey response: \(response)")
                for message in response.messages {
                    if let atalaMssg = try? Io_Iohk_Atala_Prism_Protos_AtalaMessage(serializedData:
                                                                                        message.message),
                       atalaMssg.replyTo == messageId {
                        if atalaMssg.mirrorMessage.walletRegistered.isInitialized {
                            let payIdDao = PayIdDAO()
                            payIdDao.addPublicKey(payId: self.payId!, publicKey: publicKey)
                            let contactsDao = ContactDAO()
                            contactsDao.updateMessageId(connectionId: contact.connectionId, messageId: message.id)
                        }
                    }
                }
            } catch {
                return error
            }
            return nil
        }, success: {
            self.shareCredentials()
        }, error: { error in
            print(error)
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
        })
    }

    private func shareCredentials() {

            guard let contact = self.contact, let credentials = credentials else {
                self.viewImpl?.showLoading(doShow: false)
                self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
                return
            }

        // Call the service
        ApiService.call(async: {
            do {

                let response = try ApiService.global.shareCredentials(contact: contact,
                                                                       credentials: credentials)
                Logger.d("shareCredential response: \(response)")
                let historyDao = ActivityHistoryDAO()
                let timestamp = Date()
                for credential in credentials {
                    historyDao.createActivityHistory(timestamp: timestamp, type: .credentialShared,
                                                     credential: credential, contact: contact)
                }

            } catch {
                return error
            }
            return nil
        }, success: {
            self.viewImpl?.payIdRegistered()
        }, error: { error in
            print(error.localizedDescription)
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
        })
    }

    func validateName(name: String) {

        if name.isEmpty {
            viewImpl?.toogleNameAvailable(isAvailable: nil)
            return
        }
        guard let contact = self.contact else {
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
            return
        }
        self.viewImpl?.showLoading(doShow: true)
        var messageId = ""
        // Call the service
        ApiService.call(async: {
            do {
                let response = try ApiService.global.validatePayIdName(contact: contact, name: name)
                Logger.d("shareCredential response: \(response)")
                messageId = response.id
                print(messageId)
            } catch {
                return error
            }
            return nil
        }, success: {
            DispatchQueue.main.asyncAfter(deadline: .now() + 5) {
                self.getValidName(messageId: messageId)
            }
        }, error: { error in
            print(error.localizedDescription)
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
        })
    }

    func getValidName(messageId: String) {

        guard let contact = self.contact else {
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
            return
        }

        // Call the service
        ApiService.call(async: {
            do {
                let response = try ApiService.global.getMessages(contact: contact)
                Logger.d("shareCredential response: \(response)")
                for message in response.messages {
                    if let atalaMssg = try? Io_Iohk_Atala_Prism_Protos_AtalaMessage(serializedData:
                                                                                        message.message),
                       atalaMssg.replyTo == messageId {
                        self.availableName = atalaMssg.mirrorMessage.checkPayIDNameAvailabilityResponse.available
                    }
                }
            } catch {
                return error
            }
            return nil
        }, success: {
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.toogleNameAvailable(isAvailable: self.availableName)
        }, error: { error in
            print(error)
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
        })
    }

    func validateaddress(address: String) {

        if address.isEmpty {
            viewImpl?.toogleaddressValid(isValid: nil)
            return
        }
        let validAddress = CryptoUtils.global.isValidShelleyAddress(address: address)
        let validPublicKey = CryptoUtils.global.isValidExtendedPublicKey(key: address)
        self.viewImpl?.toogleaddressValid(isValid: validAddress || validPublicKey)
    }

    // MARK: ConnectionsWorkerDelegate

    func contactsFetched(contacts: [Contact]) {
    }

    func config(isLoading: Bool) {
         self.viewImpl?.showLoading(doShow: isLoading)
    }

    func showErrorMessage(doShow: Bool, message: String?) {
        self.viewImpl?.showErrorMessage(doShow: doShow, message: message)
    }

    func showNewConnectMessage(type: Int, title: String?, logoData: Data?) {
        self.viewImpl?.showLoading(doShow: true)
        self.connectionsWorker.confirmQrCode(isPayId: true)
    }

    func conectionAccepted(contact: Contact?) {
        self.viewImpl?.showLoading(doShow: false)
        self.contact = contact
//        DispatchQueue.main.asyncAfter(deadline: .now() +  15) {
//            self.createPayId()
//        }
    }
}
