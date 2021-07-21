//
//  ConnectionsWorker.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 17/04/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import Foundation
import SwiftGRPC

protocol ConnectionsWorkerDelegate: class {

    func contactsFetched(contacts: [Contact])
    func config(isLoading: Bool)
    func showErrorMessage(doShow: Bool, message: String?)
    func showNewConnectMessage(type: Int, title: String?, logoData: Data?)
    func conectionAccepted(contact: Contact?)
}

class ConnectionsWorker: NSObject {

    weak var delegate: ConnectionsWorkerDelegate?

    var connectionRequest: ConnectionRequest?
    let sharedMemory: SharedMemory = SharedMemory.global

    func fetchConnections() {

        let dao = ContactDAO()
        let contacts = dao.listContacts() ?? []
        self.delegate?.contactsFetched(contacts: contacts)
    }

    func validateQrCode(_ str: String, contacts: [Contact]) {

        self.delegate?.config(isLoading: true)

        // Call the service
        ApiService.call(async: {
            do {
                let response = try ApiService.global.getConnectionTokenInfo(token: str)
                Logger.d("getConnectionTokenInfo response: \(response)")

                // Parse data
                guard let connection = ConnectionMaker.build(response) else {
                    return SimpleLocalizedError("Can't parse response")
                }
                let conn = ConnectionRequest()
                conn.info = connection
                conn.token = str

                self.connectionRequest = conn
            } catch {
                return error
            }
            return nil
        }, success: {
            let isDuplicated = contacts.contains { $0.did == self.connectionRequest?.info?.did }
            if isDuplicated {
                self.delegate?.showErrorMessage(doShow: true,
                                                message: String(format:
                                                    "connections_scan_qr_confirm_duplicated_title".localize(),
                                                                self.connectionRequest!.info?.name ?? ""))
            } else {
                self.delegate?.showNewConnectMessage(type: self.connectionRequest?.type ?? 0,
                                                     title: self.connectionRequest!.info?.name,
                                                     logoData: self.connectionRequest?.info?.logoData)
            }
        }, error: { error in
            var msg = "service_error".localize()
            if let err = error as? RPCError,
                err.callResult?.statusMessage?.contains("Unknown token") ?? false {
                msg = "connections_scan_qr_error".localize()
            }
            self.delegate?.config(isLoading: false)
            self.delegate?.showErrorMessage(doShow: true, message: msg)
        })
    }

    // TODO: Adding a target fix but ll this needs to be refactored:
    // There needs to be a component that takes care of the Crypto Utils thread issues
    // and assures that its always taking care in a single thread.
    // Also we can streamline this threading by the usage of Combine.
    // API should also be streamlined with Combine.
    func confirmQrCode(isKyc: Bool = false, isPayId: Bool = false) {
        self.delegate?.config(isLoading: true)
        var contact: Contact?
        // Call the service
        guard let token = connectionRequest?.token else {
            delegate?.config(isLoading: false)
            delegate?.showErrorMessage(doShow: true, message: "connections_scan_qr_confirm_error".localize())
            return
        }
        let keyPath = CryptoUtils.global.getNextPublicKeyPath()
        let encodedPublicKey = CryptoUtils.global.encodedPublicKey(keyPath: keyPath)
        do {
            let connectionRequest = try AddConnectionRequest(keyPath: keyPath, token: token, publicKey: encodedPublicKey)
            ApiService.call(async: {
                do {
                    let response = try ApiService.global.addConnectionToken(addConnectionRequest: connectionRequest)
                    Logger.d("addConnectionToken response: \(response)")
                    // Save the contact
                    let keyPath = CryptoUtils.global.confirmNewKeyUsed()
                    let dao = ContactDAO()
                    DispatchQueue.main.sync {
                        contact = dao.createContact(connectionInfo: response.connection, keyPath: keyPath,
                                                    isKyc: isKyc, isPayId: isPayId)
                        let historyDao = ActivityHistoryDAO()
                        historyDao.createActivityHistory(timestamp: contact?.dateCreated, type: .contactAdded,
                                                         credential: nil, contact: contact)
                    }
                } catch {
                    return error
                }
                return nil
            }, success: {
                self.delegate?.conectionAccepted(contact: contact)
            }, error: { error in
                print(error.localizedDescription)
                self.delegate?.config(isLoading: false)
                self.delegate?.showErrorMessage(doShow: true, message: "connections_scan_qr_confirm_error".localize())
            })
        } catch {
            delegate?.config(isLoading: false)
            delegate?.showErrorMessage(doShow: true, message: "connections_scan_qr_confirm_error".localize())
        }
    }
}
