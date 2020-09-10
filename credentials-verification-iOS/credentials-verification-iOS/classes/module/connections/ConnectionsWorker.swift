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
    func conectionAccepted()
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
                guard let connection = ConnectionMaker.build(response.creator) else {
                    return SimpleLocalizedError("Can't parse response")
                }
                let conn = ConnectionRequest()
                conn.info = connection
                conn.token = str
                conn.type = connection.type

                self.connectionRequest = conn
            } catch {
                return error
            }
            return nil
        }, success: {
            let isDuplicated = contacts.contains { $0.did == self.connectionRequest?.info?.did }
            self.delegate?.config(isLoading: false)
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

    func confirmQrCode() {

        // 1. Get the connection payment token.
        // 2. Call the payment library.
        // 3. On library response, add new connection.

        self.delegate?.config(isLoading: true)

        // Call the service
        ApiService.call(async: {
            do {
                let response = try ApiService.global.getPaymentToken()
                Logger.d("getPaymentToken response: \(response)")
                // Save the payment token
                if response.tokenizationKey.isEmpty {
                    throw SimpleLocalizedError("Payment token failed to retrieve")
                }
                self.connectionRequest!.paymentToken = response.tokenizationKey
            } catch {
                return error
            }
            return nil
        }, success: {
            self.delegate?.config(isLoading: false)

            self.connectionRequest!.paymentNonce = ""
            self.sendNewConnectionToServer()
        }, error: { _ in
            self.delegate?.config(isLoading: false)
            self.delegate?.showErrorMessage(doShow: true, message: "connections_scan_qr_confirm_error".localize())
        })
    }

    func sendNewConnectionToServer() {

        self.delegate?.config(isLoading: true)

        // Call the service
        ApiService.call(async: {
            do {
                let response = try ApiService.global.addConnectionToken(token: self.connectionRequest!.token!,
                                                                        nonce: self.connectionRequest!.paymentNonce!)
                Logger.d("addConnectionToken response: \(response)")
                // Save the contact
                let keyPath = CryptoUtils.global.confirmNewKeyUsed()
                let dao = ContactDAO()
                _ = DispatchQueue.main.sync {
                    let contact = dao.createContact(connectionInfo: response.connection, keyPath: keyPath)
                    let historyDao = ActivityHistoryDAO()
                    historyDao.createActivityHistory(timestamp: contact?.dateCreated, type: .contactAdded,
                                                     credential: nil, contact: contact)
                }
            } catch {
                return error
            }
            return nil
        }, success: {
            self.delegate?.config(isLoading: false)
            self.delegate?.conectionAccepted()
        }, error: { error in
            print(error.localizedDescription)
            self.delegate?.config(isLoading: false)
            self.delegate?.showErrorMessage(doShow: true, message: "connections_scan_qr_confirm_error".localize())
        })
    }
}
