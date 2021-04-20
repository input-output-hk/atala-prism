//
//  VerifyIdTypeSelectPresenter.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 01/02/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation

class VerifyIdTypeSelectPresenter: ListingBasePresenter, ListingBaseTableUtilsPresenterDelegate,
                                   TypeSelectCellDelegate, ConnectionsWorkerDelegate {

    var viewImpl: VerifyIdTypeSelectViewController? {
        return view as? VerifyIdTypeSelectViewController
    }

    struct InitialCellValue {
        var icon: String
        var title: String
        var isSelected: Bool
    }

    lazy var initialStaticCells: [InitialCellValue] = [
        InitialCellValue(icon: "icon_id", title: "verifyid_typeselect_national_id".localize(), isSelected: false),
        InitialCellValue(icon: "icon_passport", title: "verifyid_typeselect_passport".localize(), isSelected: false),
        InitialCellValue(icon: "icon_drivers", title: "verifyid_typeselect_drivers_license".localize(),
                         isSelected: false)
    ]

    var connectionsWorker = ConnectionsWorker()
    var connectionToken: String?
    var documentInstanceID: String?
    var kycToken: String?
    var contact: Contact?

    override init() {
        super.init()
        connectionsWorker.delegate = self
    }

    // MARK: ListingBaseTableUtilsPresenterDelegate

    func cleanData() {

    }

    func fetchData() {

    }

    func hasData() -> Bool {
        return true
    }

    func getElementCount() -> Int {
        return initialStaticCells.count
    }

    // MARK: Table

    func hasPullToRefresh() -> Bool {
        true
    }

    func actionPullToRefresh() {

    }

    // MARK: TypeSelectCellDelegate

    func setup(for cell: TypeSelectTableViewCell) {
        if let index = cell.indexPath?.row {
            let item = initialStaticCells[index]
            cell.config(name: item.title, icon: item.icon, isSelected: item.isSelected)
        }
    }

    func tappedAction(for cell: TypeSelectTableViewCell) {
        if let index = cell.indexPath?.row {
            for item in 0...2 {
                initialStaticCells[item].isSelected = item == index
            }
        }
        viewImpl?.enableNextButton()
        viewImpl?.table.reloadData()
    }

    // MARK: Buttons

    func continueTapped() {
        createAccount()
    }

    // MARK: Fetch

    func createAccount() {

        self.viewImpl?.config(isLoading: true)

        // Call the service
        ApiService.call(async: {
            do {
                let response = try ApiService.global.createKycAccount()
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
            self.viewImpl?.config(isLoading: false)
            self.viewImpl?.showErrorMessage(doShow: true, message: "connections_scan_qr_confirm_error".localize())
        })
    }

    func fetchMessages() {

        guard let contact = self.contact else {
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
            return
        }

        // Call the service
        ApiService.call(async: {
            do {
                let responses = try ApiService.global.getCredentials(contacts: [contact])
                Logger.d("getCredentials responses: \(responses)")

                // Parse the messages
                for response in responses {
                    for message in response.messages {
                        if let atalaMssg = try? Io_Iohk_Atala_Prism_Protos_AtalaMessage(serializedData:
                                                                                            message.message) {
                            if !atalaMssg.kycBridgeMessage.startAcuantProcess.documentInstanceID.isEmpty {
                                self.documentInstanceID = atalaMssg.kycBridgeMessage.startAcuantProcess.documentInstanceID
                                self.kycToken = atalaMssg.kycBridgeMessage.startAcuantProcess.bearerToken
                            }
                        }
                    }
                }
            } catch {
                return error
            }
            return nil
        }, success: {
            if let documentInstanceID = self.documentInstanceID, let kycToken = self.kycToken {
                self.viewImpl?.config(isLoading: false)
                self.viewImpl?.changeScreenToScanFront(documentInstanceID: documentInstanceID, kycToken: kycToken)
            } else {
                self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
            }
        }, error: { _ in
            self.viewImpl?.showErrorMessage(doShow: true, message: "service_error".localize())
        })
    }

    // MARK: ConnectionsWorkerDelegate

    func contactsFetched(contacts: [Contact]) {
    }

    func config(isLoading: Bool) {
         self.viewImpl?.config(isLoading: isLoading)
    }

    func showErrorMessage(doShow: Bool, message: String?) {
        self.viewImpl?.showErrorMessage(doShow: doShow, message: message)
    }

    func showNewConnectMessage(type: Int, title: String?, logoData: Data?) {
        self.viewImpl?.config(isLoading: true)
        self.connectionsWorker.confirmQrCode()
    }

    func conectionAccepted(contact: Contact?) {
        self.viewImpl?.config(isLoading: true)
        self.contact = contact
        DispatchQueue.main.asyncAfter(deadline: .now() +  15) {
            self.fetchMessages()
        }
    }
}
