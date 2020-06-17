//
import SwiftGRPC

class ApiService: NSObject {

    static let DEFAULT_REQUEST_LIMIT: Int32 = 10

    static let global = ApiService()
    let sharedMemory = SharedMemory.global

    // MARK: Service

    lazy var service: Io_Iohk_Prism_Protos_ConnectorServiceServiceClient = {
        Io_Iohk_Prism_Protos_ConnectorServiceServiceClient(address: Common.URL_API, secure: false)
    }()

    func makeMeta(_ userId: String? = nil) -> Metadata {

        let meta = Metadata()
        if userId != nil {
            try? meta.add(key: "userid", value: userId!)
        }
        return meta
    }

    static func call(async: @escaping () -> Error?, success: @escaping () -> Void, error: @escaping (Error) -> Void) {
        DispatchQueue.global(qos: .background).async {
            let errorRes = async()
            DispatchQueue.main.async {
                if errorRes == nil {
                    success()
                } else {
                    error(errorRes!)
                }
            }
        }
    }

    // MARK: Connections

    func getConnectionTokenInfo(token: String) throws -> Io_Iohk_Prism_Protos_GetConnectionTokenInfoResponse {

        let userId = FakeData.fakeUserId()
        return try service.getConnectionTokenInfo(Io_Iohk_Prism_Protos_GetConnectionTokenInfoRequest.with {
            $0.token = token
        }, metadata: makeMeta(userId))
    }

    func addConnectionToken(token: String,
                            nonce: String) throws -> Io_Iohk_Prism_Protos_AddConnectionFromTokenResponse {

        let publicKey = FakeData.fakePublicKey()
        let userId = FakeData.fakeUserId()
        return try service.addConnectionFromToken(Io_Iohk_Prism_Protos_AddConnectionFromTokenRequest.with {
            $0.token = token
            $0.holderPublicKey = publicKey
            $0.paymentNonce = nonce
        }, metadata: makeMeta(userId))
    }

    func getConnections(userIds: [String]?, limit: Int32 = DEFAULT_REQUEST_LIMIT)
        throws -> [Io_Iohk_Prism_Protos_GetConnectionsPaginatedResponse] {

        var responseList: [Io_Iohk_Prism_Protos_GetConnectionsPaginatedResponse] = []
        for userId in userIds ?? [] {
            let response = try service.getConnectionsPaginated(
                Io_Iohk_Prism_Protos_GetConnectionsPaginatedRequest.with {
                // $0.lastSeenConnectionID = token
                $0.limit = limit
            }, metadata: makeMeta(userId))
            responseList.append(response)
        }
        return responseList
    }

    // MARK: Credentials

    func getCredentials(userIds: [String]?, limit: Int32 = DEFAULT_REQUEST_LIMIT)
        throws -> [Io_Iohk_Prism_Protos_GetMessagesPaginatedResponse] {

        var responseList: [Io_Iohk_Prism_Protos_GetMessagesPaginatedResponse] = []
        for userId in userIds ?? [] {
            let response = try service.getMessagesPaginated(Io_Iohk_Prism_Protos_GetMessagesPaginatedRequest.with {
                $0.limit = limit
            }, metadata: makeMeta(userId))
            responseList.append(response)
        }
        return responseList
    }

    func shareCredential(userIds: [String]?, connectionIds: [String]?,
                         degree: Degree) throws -> [Io_Iohk_Prism_Protos_SendMessageResponse] {

        let messageData = try? degree.intCredential?.serializedData()

        var responseList: [Io_Iohk_Prism_Protos_SendMessageResponse] = []

        for pos in 0 ..< (userIds ?? []).count {
            let userId = userIds![pos]
            let connectionId = connectionIds![pos]
            let response = try service.sendMessage(Io_Iohk_Prism_Protos_SendMessageRequest.with {
                $0.message = messageData!
                $0.connectionID = connectionId
            }, metadata: makeMeta(userId))
            responseList.append(response)
        }
        return responseList
    }

    func shareCredentials(userId: String, connectionId: String,
                          degrees: [Degree]) throws -> [Io_Iohk_Prism_Protos_SendMessageResponse] {

        var responseList: [Io_Iohk_Prism_Protos_SendMessageResponse] = []

        for pos in 0 ..< (degrees).count {
            let messageData = try? degrees[pos].intCredential?.serializedData()
            let response = try service.sendMessage(Io_Iohk_Prism_Protos_SendMessageRequest.with {
                $0.message = messageData!
                $0.connectionID = connectionId
            }, metadata: makeMeta(userId))
            responseList.append(response)
        }
        return responseList
    }

    // MARK: Payments

    func getPaymentToken() throws -> Io_Iohk_Prism_Protos_GetBraintreePaymentsConfigResponse {

        return try service.getBraintreePaymentsConfig(Io_Iohk_Prism_Protos_GetBraintreePaymentsConfigRequest.with { _ in
        }, metadata: makeMeta())
    }

    func getPaymentsHistory(userIds: [String]?) throws -> [Io_Iohk_Prism_Protos_GetPaymentsResponse] {

        var responseList: [Io_Iohk_Prism_Protos_GetPaymentsResponse] = []
        for userId in userIds ?? [] {
            let response = try service.getPayments(Io_Iohk_Prism_Protos_GetPaymentsRequest.with { _ in
            }, metadata: makeMeta(userId))
            responseList.append(response)
        }
        return responseList
    }
}
