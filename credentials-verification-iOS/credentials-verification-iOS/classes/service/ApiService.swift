//
import SwiftGRPC

class ApiService: NSObject {

    static let DEFAULT_REQUEST_LIMIT: Int32 = 10

    static let global = ApiService()
    let sharedMemory = SharedMemory.global

    // MARK: Service

    lazy var service: Io_Iohk_Prism_Protos_ConnectorServiceServiceClient = {
        let serv = Io_Iohk_Prism_Protos_ConnectorServiceServiceClient(address: Common.URL_API, secure: false)
        serv.channel.timeout = 10
        return serv
    }()

    func makeMeta(_ userId: String? = nil) -> Metadata {

        let meta = Metadata()
        if userId != nil {
            try? meta.add(key: "userid", value: userId!)
        }
        return meta
    }

    func makeSignedMeta(requestData: Data, keyPath: String) -> Metadata {

        let meta = Metadata()
        if let signature = CryptoUtils.global.signData(data: requestData, keyPath: keyPath) {
            try? meta.add(key: "signature", value: signature.0)
            try? meta.add(key: "publickey", value: signature.1)
            try? meta.add(key: "requestnonce", value: signature.2)
        }
        print(meta.dictionaryRepresentation.debugDescription)
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
        let keyPath = CryptoUtils.global.getNextPublicKeyPath()
        let encodedPublicKey = CryptoUtils.global.encodedPublicKey(keyPath: keyPath)
        let request = Io_Iohk_Prism_Protos_AddConnectionFromTokenRequest.with {
            $0.token = token
            $0.holderEncodedPublicKey = encodedPublicKey
            $0.paymentNonce = nonce
        }
        let metadata = makeSignedMeta(requestData: try request.serializedData(), keyPath: keyPath)
        return try service.addConnectionFromToken(request, metadata: metadata)
    }

    func getConnection(keyPath: String, limit: Int32 = DEFAULT_REQUEST_LIMIT)
        throws -> Io_Iohk_Prism_Protos_GetConnectionsPaginatedResponse {

        let request = Io_Iohk_Prism_Protos_GetConnectionsPaginatedRequest.with {
            // $0.lastSeenConnectionID = token
            $0.limit = limit
        }
        let metadata = makeSignedMeta(requestData: try request.serializedData(), keyPath: keyPath)
        let response = try service.getConnectionsPaginated(request, metadata: metadata)

        return response
    }

    // MARK: Credentials

    func getCredentials(contacts: [Contact]?, limit: Int32 = DEFAULT_REQUEST_LIMIT)
        throws -> [Io_Iohk_Prism_Protos_GetMessagesPaginatedResponse] {

        var responseList: [Io_Iohk_Prism_Protos_GetMessagesPaginatedResponse] = []
        for contact in contacts ?? [] {
            let request = Io_Iohk_Prism_Protos_GetMessagesPaginatedRequest.with {
                $0.limit = limit
                if let lastMessage = contact.lastMessageId {
                    $0.lastSeenMessageID = lastMessage
                }
            }
            let metadata = makeSignedMeta(requestData: try request.serializedData(), keyPath: contact.keyPath)
            let response = try service.getMessagesPaginated(request, metadata: metadata)
            responseList.append(response)
        }
        return responseList
    }

    func shareCredential(contacts: [Contact],
                         degree: Degree) throws -> [Io_Iohk_Prism_Protos_SendMessageResponse] {

        let messageData = degree.intCredential

        var responseList: [Io_Iohk_Prism_Protos_SendMessageResponse] = []

        for contact in contacts {
            let request = Io_Iohk_Prism_Protos_SendMessageRequest.with {
                $0.message = messageData!
                $0.connectionID = contact.connectionId
            }
            let metadata = makeSignedMeta(requestData: try request.serializedData(), keyPath: contact.keyPath)
            let response = try service.sendMessage(request, metadata: metadata)
            responseList.append(response)
        }
        return responseList
    }

    func shareCredentials(contact: Contact,
                          credentials: [Credential]) throws -> [Io_Iohk_Prism_Protos_SendMessageResponse] {

        var responseList: [Io_Iohk_Prism_Protos_SendMessageResponse] = []

        for credential in credentials {
            let messageData = credential.encoded
            let request = Io_Iohk_Prism_Protos_SendMessageRequest.with {
                $0.message = messageData
                $0.connectionID = contact.connectionId
            }
            let metadata = makeSignedMeta(requestData: try request.serializedData(), keyPath: contact.keyPath)
            let response = try service.sendMessage(request, metadata: metadata)
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
