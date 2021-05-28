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
            if payIds?.count > 0 {
                self.payId = payIds?[0]
                DispatchQueue.main.async {
                    self.viewImpl?.setupData()
                }
            }
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

        // Call the service
        ApiService.call(async: {
            do {
                let responses = try ApiService.global.registerPayIdAddress(contact: contact, address: value)
                Logger.d("shareCredential response: \(responses)")

                let payIdDao = PayIdDAO()
                payIdDao.addAddress(payId: self.payId!, name: value)
            } catch {
                return error
            }
            return nil
        }, success: {
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showSuccessMessage(doShow: true, message: "pay_id_add_address_message".localize())
            self.fetchPayId()
        }, error: { _ in
            self.viewImpl?.showLoading(doShow: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
        })
    }

    // MARK: Buttons

    func share() {

        let message: String  = "Pay ID \n \(payId?.name ?? "")"

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
