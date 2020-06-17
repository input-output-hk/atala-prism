//
//  ConnectionsWorker.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 17/04/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import Foundation

protocol ConnectionsWorkerDelegate: class {

    func connectionsFetched(connections: [ConnectionBase])
    func config(isLoading: Bool)
    func showErrorMessage(doShow: Bool, message: String?)
    func showNewConnectMessage(type: Int, title: String?, logoData: Data?, isDuplicated: Bool)
    func conectionAccepted()
}

class ConnectionsWorker: NSObject {

    weak var delegate: ConnectionsWorkerDelegate?

    var connectionRequest: ConnectionRequest?
    let sharedMemory: SharedMemory = SharedMemory.global

    func fetchConnections() {

        var connections: [ConnectionBase] = []

        // Call the service
        ApiService.call(async: {
            do {
                let user = self.sharedMemory.loggedUser
                let responses = try ApiService.global.getConnections(userIds: user?.connectionUserIds?.valuesArray)
                Logger.d("getConnections responsed: \(responses)")

                // Parse data
                let parsedResponse = ConnectionMaker.parseResponseList(responses)
                connections.append(contentsOf: parsedResponse)
            } catch {
                return error
            }
            return nil
        }, success: {
            self.delegate?.connectionsFetched(connections: connections)
        }, error: { _ in
            self.delegate?.showErrorMessage(doShow: true, message: "service_error".localize())
        })
    }

    func validateQrCode(_ str: String, connections: [ConnectionBase]) {

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
            let isDuplicated = connections.contains { $0.did == self.connectionRequest?.info?.did }
            self.delegate?.config(isLoading: false)
            self.delegate?.showNewConnectMessage(type: self.connectionRequest?.type ?? 0,
                                                 title: self.connectionRequest!.info?.name,
                                                 logoData: self.connectionRequest?.info?.logoData,
                                                 isDuplicated: isDuplicated)
        }, error: { _ in
            self.delegate?.config(isLoading: false)
            self.delegate?.showErrorMessage(doShow: true, message: "connections_scan_qr_error".localize())
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
                // Save the userId
                self.sharedMemory.loggedUser?.connectionUserIds?[response.connection.connectionID] = response.userID
                self.sharedMemory.loggedUser = self.sharedMemory.loggedUser
            } catch {
                return error
            }
            return nil
        }, success: {
            self.delegate?.config(isLoading: false)
            self.delegate?.conectionAccepted()
        }, error: { _ in
            self.delegate?.config(isLoading: false)
            self.delegate?.showErrorMessage(doShow: true, message: "connections_scan_qr_confirm_error".localize())
        })
    }
}
