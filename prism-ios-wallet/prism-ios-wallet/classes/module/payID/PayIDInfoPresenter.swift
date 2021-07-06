//
//  PayIDInfoPresenter.swift
//  prism-ios-wallet
//
//  Created by Roberto Daviduk on 22/03/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation

class PayIDInfoPresenter: BasePresenter {
  
    var viewImpl: PayIDInfoViewController? {
        return view as? PayIDInfoViewController
    }

    var payId: PayId?

    func fetchPayId() {
        DispatchQueue.global(qos: .background).async {
            let dao = PayIdDAO()
            let payIds = dao.listPayId()
            if payIds?.count > 0, let payId = payIds?[0] {
                self.payId = payId
                DispatchQueue.main.async {
                    self.viewImpl?.setupData()
                }
                let contactsDao = ContactDAO()
                if let connectionId = payId.connectionId,
                   let contact = contactsDao.getContact(connectionId: connectionId) {
                    self.refreshPayIdAddresses(contact: contact, payId: payId)
                }
            }
        }
    }

    func addAddressOrKey(value: String) {
        if CryptoUtils.global.isValidShelleyAddress(address: value) {
            self.addAddress(value: value)
        } else if CryptoUtils.global.isValidExtendedPublicKey(key: value) {
            self.addPublicKey(value: value)
        }
    }

    func addAddress(value: String) {
        self.viewImpl?.showLoading(doShow: true)
        let contactsDao = ContactDAO()
        guard let connectionId = payId?.connectionId,
              let contact = contactsDao.getContact(connectionId: connectionId) else {
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
            return
        }

        var messageId = ""
        // Call the service
        ApiService.call(async: {
            do {
                let response = try ApiService.global.registerPayIdAddress(contact: contact, address: value)
                Logger.d("registerPayIdAddress response: \(response)")

                messageId = response.id
            } catch {
                return error
            }
            return nil
        }, success: {
            DispatchQueue.main.asyncAfter(deadline: .now() + 5) {
                self.getRegisteredAddress(messageId: messageId, contact: contact, address: value)
            }
        }, error: { _ in
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
        })
    }

    func getRegisteredAddress(messageId: String, contact: Contact, address: String) {

        var registered = false
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
                            registered = true
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
            self.viewImpl?.showLoading(doShow: false)
            if registered {
                self.viewImpl?.showSuccessMessage(doShow: true, message: "pay_id_add_address_message".localize())
                self.fetchPayId()
            } else {
                self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
            }
        }, error: { error in
            print(error)
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
        })
    }

    func addPublicKey(value: String) {
        self.viewImpl?.showLoading(doShow: true)
        let contactsDao = ContactDAO()
        guard let connectionId = payId?.connectionId,
              let contact = contactsDao.getContact(connectionId: connectionId) else {
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
            return
        }
        var messageId = ""

        // Call the service
        ApiService.call(async: {
            do {
                let response = try ApiService.global.registerPayIdPublicKey(contact: contact, publicKey: value)
                Logger.d("registerPayIdPublicKey response: \(response)")

                messageId = response.id
            } catch {
                return error
            }
            return nil
        }, success: {
            DispatchQueue.main.asyncAfter(deadline: .now() + 5) {
                self.getRegisteredPublicKey(messageId: messageId, contact: contact, publicKey: value)
            }
        }, error: { error in
            print(error.localizedDescription)
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
        })
    }

    func getRegisteredPublicKey(messageId: String, contact: Contact, publicKey: String) {

        var registered = false
        // Call the service
        ApiService.call(async: {
            do {
                let response = try ApiService.global.getMessages(contact: contact)
                Logger.d("shareCredential response: \(response)")
                for message in response.messages {
                    if let atalaMssg = try? Io_Iohk_Atala_Prism_Protos_AtalaMessage(serializedData:
                                                                                        message.message),
                       atalaMssg.replyTo == messageId {
                        if atalaMssg.mirrorMessage.walletRegistered.isInitialized {
                            registered = true
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
            self.viewImpl?.showLoading(doShow: false)
            if registered {
                self.viewImpl?.showSuccessMessage(doShow: true, message: "pay_id_add_address_message".localize())
                self.fetchPayId()
            } else {
                self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
            }
        }, error: { error in
            print(error)
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
        })
    }

    func refreshPayIdAddresses(contact: Contact, payId: PayId) {

        var messageId = ""

        // Call the service
        ApiService.call(async: {
            do {
                let response = try ApiService.global.getPayIdAddresses(contact: contact)
                Logger.d("getPayIdAddresses response: \(response)")
                messageId = response.id
                print("response.id: \(response.id)")
            } catch {
                return error
            }
            return nil
        }, success: {
            DispatchQueue.main.asyncAfter(deadline: .now() + 10) {
                self.getPayIdAddresses(contact: contact, messageId: messageId, payId: payId)
            }
        }, error: { _ in
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
        })
    }

    func getPayIdAddresses(contact: Contact, messageId: String, payId: PayId) {

        // Call the service
        ApiService.call(async: {
            do {
                let response = try ApiService.global.getMessages(contact: contact)
                Logger.d("getMessages response: \(response)")

                let contactsDao = ContactDAO()

                for message in response.messages {
                    if let atalaMssg = try? Io_Iohk_Atala_Prism_Protos_AtalaMessage(serializedData:
                                                                                        message.message) {
                        print("atalaMssg.replyTo: \(atalaMssg.replyTo)")
                        if atalaMssg.replyTo == messageId {
                            let payIDAddressesResponse = atalaMssg.mirrorMessage.getPayIDAddressesResponse
                            let payIdDao = PayIdDAO()
                            let manual = payIDAddressesResponse.manuallyRegisteredCardanoAddresses
                            for address in manual {
                                payIdDao.addAddress(payId: payId, name: address.address)
                            }
                            let generated = payIDAddressesResponse.generatedCardanoAddresses
                            for address in generated {
                                payIdDao.addAddress(payId: payId, name: address.address)
                            }
                            contactsDao.updateMessageId(connectionId: contact.connectionId, messageId: message.id)
                        }
                    }
                }
            } catch {
                return error
            }
            return nil
        }, success: {

        }, error: { _ in
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
        })
    }

    // MARK: Buttons

    func share() {

        let message: String  = "Pay ID \n \(payId?.name ?? "")\("pay_id_setup_name_field_right".localize())"

        let activityViewController = UIActivityViewController(activityItems: [message], applicationActivities: nil)
        activityViewController.popoverPresentationController?.sourceView = self.viewImpl?.view
        activityViewController.completionWithItemsHandler = { (activityType, completed: Bool,
                                                               returnedItems: [Any]?,
                                                               error: Error?) in
            if completed {
                self.viewImpl?.showAler()
            }
        }

        self.viewImpl?.present(activityViewController, animated: true, completion: nil)
    }

    func removeTapped() {
        viewImpl?.showDeletePayIdConfirmation()
    }

    // MARK: Delete

    func deletePayId() {
        let payIdao = PayIdDAO()
        let contactsDao = ContactDAO()
        if let payId = payId,
           let connectionId = payId.connectionId,
           let contact = contactsDao.getContact(connectionId: connectionId) {
            if payIdao.deletePayId(payId: payId) {
                if contactsDao.deleteContact(contact: contact) {
                    self.viewImpl?.onBackPressed()
                    return
                }
            }
        }
        self.viewImpl?.showErrorMessage(doShow: true, message: "pay_id_info_delete_error".localize())
    }
}
