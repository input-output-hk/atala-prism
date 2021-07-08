//
import SwiftGRPC

class ApiService: NSObject {

    static let DEFAULT_REQUEST_LIMIT: Int32 = 10

    static let global = ApiService()
    let sharedMemory = SharedMemory.global

    // MARK: Service

    lazy var service: Io_Iohk_Atala_Prism_Protos_ConnectorServiceServiceClient = {
        let serv = Io_Iohk_Atala_Prism_Protos_ConnectorServiceServiceClient(address: Common.URL_API, secure: false)
        serv.channel.timeout = 10
        return serv
    }()
    
    lazy var kycBridgeService: Io_Iohk_Atala_Kycbridge_Protos_KycBridgeServiceServiceClient = {
        var base = String(Common.URL_API.split(separator: ":")[0])
        base.append(Common.KYC_PORT)
        let serv = Io_Iohk_Atala_Kycbridge_Protos_KycBridgeServiceServiceClient(address: base, secure: false)
        serv.channel.timeout = 10
        return serv
    }()

    lazy var mirrorService: Io_Iohk_Atala_Mirror_Protos_MirrorServiceServiceClient = {
        var base = String(Common.URL_API.split(separator: ":")[0])
        base.append(Common.MIRROR_PORT)
        let serv = Io_Iohk_Atala_Mirror_Protos_MirrorServiceServiceClient(address: base, secure: false)
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

    func getConnectionTokenInfo(token: String) throws -> Io_Iohk_Atala_Prism_Protos_GetConnectionTokenInfoResponse {

        let userId = FakeData.fakeUserId()
        return try service.getConnectionTokenInfo(Io_Iohk_Atala_Prism_Protos_GetConnectionTokenInfoRequest.with {
            $0.token = token
        }, metadata: makeMeta(userId))
    }

    func addConnectionToken(token: String) throws -> Io_Iohk_Atala_Prism_Protos_AddConnectionFromTokenResponse {
        let keyPath = CryptoUtils.global.getNextPublicKeyPath()
        let encodedPublicKey = CryptoUtils.global.encodedPublicKey(keyPath: keyPath)
        let request = Io_Iohk_Atala_Prism_Protos_AddConnectionFromTokenRequest.with {
            $0.token = token
            $0.holderEncodedPublicKey = encodedPublicKey
        }
        let metadata = makeSignedMeta(requestData: try request.serializedData(), keyPath: keyPath)
        return try service.addConnectionFromToken(request, metadata: metadata)
    }

    func getConnection(keyPath: String, limit: Int32 = DEFAULT_REQUEST_LIMIT)
        throws -> Io_Iohk_Atala_Prism_Protos_GetConnectionsPaginatedResponse {

        let request = Io_Iohk_Atala_Prism_Protos_GetConnectionsPaginatedRequest.with {
            // $0.lastSeenConnectionID = token
            $0.limit = limit
        }
        let metadata = makeSignedMeta(requestData: try request.serializedData(), keyPath: keyPath)
        let response = try service.getConnectionsPaginated(request, metadata: metadata)

        return response
    }

    // MARK: Credentials

    func getCredentials(contacts: [Contact]?, limit: Int32 = DEFAULT_REQUEST_LIMIT)
        throws -> [Io_Iohk_Atala_Prism_Protos_GetMessagesPaginatedResponse] {

        var responseList: [Io_Iohk_Atala_Prism_Protos_GetMessagesPaginatedResponse] = []
        for contact in contacts ?? [] {
            let request = Io_Iohk_Atala_Prism_Protos_GetMessagesPaginatedRequest.with {
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
                         credential: Credential) throws -> [Io_Iohk_Atala_Prism_Protos_SendMessageResponse] {

        let messageData = credential.encoded

        var responseList: [Io_Iohk_Atala_Prism_Protos_SendMessageResponse] = []

        for contact in contacts {
            let request = Io_Iohk_Atala_Prism_Protos_SendMessageRequest.with {
                $0.message = messageData
                $0.connectionID = contact.connectionId
            }
            let metadata = makeSignedMeta(requestData: try request.serializedData(), keyPath: contact.keyPath)
            let response = try service.sendMessage(request, metadata: metadata)
            responseList.append(response)
        }
        return responseList
    }

    func shareCredentials(contact: Contact,
                          credentials: [Credential]) throws -> [Io_Iohk_Atala_Prism_Protos_SendMessageResponse] {

        var responseList: [Io_Iohk_Atala_Prism_Protos_SendMessageResponse] = []

        for credential in credentials {
            let messageData = credential.encoded
            let request = Io_Iohk_Atala_Prism_Protos_SendMessageRequest.with {
                $0.message = messageData
                $0.connectionID = contact.connectionId
            }
            let metadata = makeSignedMeta(requestData: try request.serializedData(), keyPath: contact.keyPath)
            let response = try service.sendMessage(request, metadata: metadata)
            responseList.append(response)
        }
        return responseList
    }

    // MARK: KYC Account

    func createKycAccount() throws -> Io_Iohk_Atala_Kycbridge_Protos_CreateAccountResponse {

        let request = Io_Iohk_Atala_Kycbridge_Protos_CreateAccountRequest()
        let kyc = kycBridgeService
        return try kyc.createAccount(request)
    }
    
    func sendKycResult(contact: Contact, documentInstanceId: String,
                       selfieImage: Data) throws -> Io_Iohk_Atala_Prism_Protos_SendMessageResponse {

        var message = Io_Iohk_Atala_Prism_Protos_AtalaMessage()
        message.kycBridgeMessage.acuantProcessFinished.documentInstanceID = documentInstanceId
        message.kycBridgeMessage.acuantProcessFinished.selfieImage = selfieImage
        let messageData =  try message.serializedData()
        let request = Io_Iohk_Atala_Prism_Protos_SendMessageRequest.with {
            $0.message = messageData
            $0.connectionID = contact.connectionId
        }
        let metadata = makeSignedMeta(requestData: try request.serializedData(), keyPath: contact.keyPath)
        return try service.sendMessage(request, metadata: metadata)
    }

// MARK: Mirror Account

    func createMirrorAccount() throws -> Io_Iohk_Atala_Mirror_Protos_CreateAccountResponse {

        let request = Io_Iohk_Atala_Mirror_Protos_CreateAccountRequest()
        let mirror = mirrorService
        return try mirror.createAccount(request)
    }

    func registerPayIdName(contact: Contact,
                           name: String) throws -> Io_Iohk_Atala_Prism_Protos_SendMessageResponse {

        var message = Io_Iohk_Atala_Prism_Protos_AtalaMessage()
        message.mirrorMessage.payIDNameRegistrationMessage.name = name
        let messageData =  try message.serializedData()
        let request = Io_Iohk_Atala_Prism_Protos_SendMessageRequest.with {
            $0.message = messageData
            $0.connectionID = contact.connectionId
        }
        let metadata = makeSignedMeta(requestData: try request.serializedData(), keyPath: contact.keyPath)
        return try service.sendMessage(request, metadata: metadata)
    }

    func registerPayIdAddress(contact: Contact,
                              address: String) throws -> Io_Iohk_Atala_Prism_Protos_SendMessageResponse {

        var message = Io_Iohk_Atala_Prism_Protos_AtalaMessage()
        message.mirrorMessage.registerAddressMessage.cardanoAddress = address
        let messageData =  try message.serializedData()
        let request = Io_Iohk_Atala_Prism_Protos_SendMessageRequest.with {
            $0.message = messageData
            $0.connectionID = contact.connectionId
        }
        let metadata = makeSignedMeta(requestData: try request.serializedData(), keyPath: contact.keyPath)
        return try service.sendMessage(request, metadata: metadata)
    }

    func registerPayIdPublicKey(contact: Contact,
                                publicKey: String) throws -> Io_Iohk_Atala_Prism_Protos_SendMessageResponse {

        var message = Io_Iohk_Atala_Prism_Protos_AtalaMessage()
        message.mirrorMessage.registerWalletMessage.extendedPublicKey = publicKey
        let messageData =  try message.serializedData()
        let request = Io_Iohk_Atala_Prism_Protos_SendMessageRequest.with {
            $0.message = messageData
            $0.connectionID = contact.connectionId
        }
        let metadata = makeSignedMeta(requestData: try request.serializedData(), keyPath: contact.keyPath)
        return try service.sendMessage(request, metadata: metadata)
    }

    func validatePayIdName(contact: Contact,
                           name: String) throws -> Io_Iohk_Atala_Prism_Protos_SendMessageResponse {

        var message = Io_Iohk_Atala_Prism_Protos_AtalaMessage()
        message.mirrorMessage.checkPayIDNameAvailabilityMessage.nameToCheck = name
        let messageData =  try message.serializedData()
        let request = Io_Iohk_Atala_Prism_Protos_SendMessageRequest.with {
            $0.message = messageData
            $0.connectionID = contact.connectionId
        }
        let metadata = makeSignedMeta(requestData: try request.serializedData(), keyPath: contact.keyPath)
        return try service.sendMessage(request, metadata: metadata)
    }

    func getMessages(contact: Contact) throws -> Io_Iohk_Atala_Prism_Protos_GetMessagesPaginatedResponse {

        let request = Io_Iohk_Atala_Prism_Protos_GetMessagesPaginatedRequest.with {
            $0.limit = ApiService.DEFAULT_REQUEST_LIMIT
            if let lastMessage = contact.lastMessageId {
                $0.lastSeenMessageID = lastMessage
            }
        }
        let metadata = makeSignedMeta(requestData: try request.serializedData(), keyPath: contact.keyPath)
        return try service.getMessagesPaginated(request, metadata: metadata)
    }

    func recoverPayIdName(contact: Contact) throws -> Io_Iohk_Atala_Prism_Protos_SendMessageResponse {

        var message = Io_Iohk_Atala_Prism_Protos_AtalaMessage()
        let getPayIDNameMessage = Io_Iohk_Atala_Prism_Protos_GetPayIdNameMessage()
        message.mirrorMessage.getPayIDNameMessage = getPayIDNameMessage
        let messageData =  try message.serializedData()
        let request = Io_Iohk_Atala_Prism_Protos_SendMessageRequest.with {
            $0.message = messageData
            $0.connectionID = contact.connectionId
        }
        let metadata = makeSignedMeta(requestData: try request.serializedData(), keyPath: contact.keyPath)
        return try service.sendMessage(request, metadata: metadata)
    }

    func getPayIdAddresses(contact: Contact) throws -> Io_Iohk_Atala_Prism_Protos_SendMessageResponse {

        var message = Io_Iohk_Atala_Prism_Protos_AtalaMessage()
        let getPayIdAddressesMessage = Io_Iohk_Atala_Prism_Protos_GetPayIdAddressesMessage()
        message.mirrorMessage.getPayIDAddressesMessage = getPayIdAddressesMessage
        let messageData =  try message.serializedData()
        let request = Io_Iohk_Atala_Prism_Protos_SendMessageRequest.with {
            $0.message = messageData
            $0.connectionID = contact.connectionId
        }
        let metadata = makeSignedMeta(requestData: try request.serializedData(), keyPath: contact.keyPath)
        return try service.sendMessage(request, metadata: metadata)
    }
}
