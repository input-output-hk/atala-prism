//
import GRPC
import NIO
import NIOHPACK

struct AddConnectionRequest {
    fileprivate let request: Io_Iohk_Atala_Prism_Protos_AddConnectionFromTokenRequest
    fileprivate let metadata: HPACKHeaders
    
    init(
        service: ApiService = ApiService.global,
        keyPath: String,
        token: String,
        publicKey: Io_Iohk_Atala_Prism_Protos_EncodedPublicKey
    ) throws {
        let req = Io_Iohk_Atala_Prism_Protos_AddConnectionFromTokenRequest.with {
            $0.token = token
            $0.holderEncodedPublicKey = publicKey
        }
        self.request = req
        self.metadata = service.makeSignedMeta(requestData: try req.serializedData(), keyPath: keyPath)
    }
}

struct GetCredentialsPaginatedRequest {
    
    private let service: ApiService
    fileprivate let request: Io_Iohk_Atala_Prism_Protos_GetMessagesPaginatedRequest
    fileprivate let metadata: HPACKHeaders
    
    init(service: ApiService = ApiService.global, contactKeyPath: String, lastMessageId: String? = nil) throws {
        self.service = service
        let req = Io_Iohk_Atala_Prism_Protos_GetMessagesPaginatedRequest.with { request in
            request.limit = ApiService.DEFAULT_REQUEST_LIMIT
            lastMessageId.map { request.lastSeenMessageID = $0}
        }
        self.request = req
        self.metadata = service.makeSignedMeta(requestData: try req.serializedData(), keyPath: contactKeyPath)
    }
}

class ApiService: NSObject {

    static let DEFAULT_REQUEST_LIMIT: Int32 = 10
    static let global = ApiService()
    
    let sharedMemory = SharedMemory.global
    
    private let defaultTimeout = TimeLimit.timeout(.seconds(Int64(ApiService.DEFAULT_REQUEST_LIMIT)))
    private let group = MultiThreadedEventLoopGroup(numberOfThreads: 1)
    
    private lazy var ataConfiguration = ClientConnection.Configuration.default(
        target: .hostAndPort(Common.ATALA_URL, Common.ATALA_PORT),
        eventLoopGroup: group
    )
    
    private lazy var kycConfiguration = ClientConnection.Configuration.default(
        target: .hostAndPort(Common.ATALA_URL, Common.KYC_PORT),
        eventLoopGroup: group
    )
    
    private lazy var mirrorConfiguration = ClientConnection.Configuration.default(
        target: .hostAndPort(Common.ATALA_URL, Common.MIRROR_PORT),
        eventLoopGroup: group
    )


    // MARK: Service

    lazy var service: Io_Iohk_Atala_Prism_Protos_ConnectorServiceClientProtocol = {
        let serv = Io_Iohk_Atala_Prism_Protos_ConnectorServiceClient(channel: ClientConnection(configuration: ataConfiguration))
        return serv
    }()
    
    lazy var kycBridgeService: Io_Iohk_Atala_Kycbridge_Protos_KycBridgeServiceClientProtocol = {
        let serv = Io_Iohk_Atala_Kycbridge_Protos_KycBridgeServiceClient(channel: ClientConnection(configuration: kycConfiguration))
        return serv
    }()

    lazy var mirrorService: Io_Iohk_Atala_Mirror_Protos_MirrorServiceClientProtocol = {
        let serv = Io_Iohk_Atala_Mirror_Protos_MirrorServiceClient(channel: ClientConnection(configuration: mirrorConfiguration))
        return serv
    }()

    func makeSignedMeta(requestData: Data, keyPath: String) -> HPACKHeaders {
        var headers = HPACKHeaders()
        CryptoUtils.global.signData(data: requestData, keyPath: keyPath).map {
            headers.add(name: "signature", value: $0.0)
            headers.add(name: "publickey", value: $0.1)
            headers.add(name: "requestnonce", value: $0.2)
        }
        return headers
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
        return try service
            .getConnectionTokenInfo(
                Io_Iohk_Atala_Prism_Protos_GetConnectionTokenInfoRequest.with {
                    $0.token = token
                },
                callOptions: nil
            ).response.wait()
    }

    func addConnectionToken(
        addConnectionRequest: AddConnectionRequest
    ) throws -> Io_Iohk_Atala_Prism_Protos_AddConnectionFromTokenResponse {
        return try service.addConnectionFromToken(
            addConnectionRequest.request,
            callOptions: .init(
                customMetadata: addConnectionRequest.metadata,
                timeLimit: defaultTimeout)
        ).response.wait()
    }

    func getConnection(keyPath: String, limit: Int32 = DEFAULT_REQUEST_LIMIT)
        throws -> Io_Iohk_Atala_Prism_Protos_GetConnectionsPaginatedResponse {
        let request = Io_Iohk_Atala_Prism_Protos_GetConnectionsPaginatedRequest.with {
            // $0.lastSeenConnectionID = token
            $0.limit = limit
        }
        let metadata = makeSignedMeta(requestData: try request.serializedData(), keyPath: keyPath)
        let response = try service.getConnectionsPaginated(request,
            callOptions: .init(
                customMetadata: metadata,
                timeLimit: defaultTimeout)
        ).response.wait()

        return response
    }

    // MARK: Credentials

    func getCredentials(
        credentialRequests: [GetCredentialsPaginatedRequest]
    ) throws -> [Io_Iohk_Atala_Prism_Protos_GetMessagesPaginatedResponse] {
        var responseList: [Io_Iohk_Atala_Prism_Protos_GetMessagesPaginatedResponse] = []
        for credentialRequest in credentialRequests {
            
            let response = try service.getMessagesPaginated(
                credentialRequest.request,
                callOptions: .init(
                    customMetadata: credentialRequest.metadata,
                    timeLimit: defaultTimeout
                )).response.wait()
            responseList.append(response)
        }
        return responseList
    }

    func shareCredential(
        contacts: [Contact],
        credential: Credential
    ) throws -> [Io_Iohk_Atala_Prism_Protos_SendMessageResponse] {
        let messageData = credential.encoded
        var responseList: [Io_Iohk_Atala_Prism_Protos_SendMessageResponse] = []

        for contact in contacts {
            let request = Io_Iohk_Atala_Prism_Protos_SendMessageRequest.with {
                $0.message = messageData
                $0.connectionID = contact.connectionId
            }
            let headers = makeSignedMeta(requestData: try request.serializedData(), keyPath: contact.keyPath)
            let response = try service.sendMessage(
                request,
                callOptions: .init(
                    customMetadata: headers,
                    timeLimit: defaultTimeout
                )
            ).response.wait()
            responseList.append(response)
        }
        return responseList
    }

    func shareCredentials(
        contact: Contact,
        credentials: [Credential]
    ) throws -> [Io_Iohk_Atala_Prism_Protos_SendMessageResponse] {
        var responseList: [Io_Iohk_Atala_Prism_Protos_SendMessageResponse] = []

        for credential in credentials {
            let messageData = credential.encoded
            let request = Io_Iohk_Atala_Prism_Protos_SendMessageRequest.with {
                $0.message = messageData
                $0.connectionID = contact.connectionId
            }
            let headers = makeSignedMeta(requestData: try request.serializedData(), keyPath: contact.keyPath)
            let response = try service.sendMessage(
                request,
                callOptions: .init(
                    customMetadata: headers,
                    timeLimit: defaultTimeout
                )).response.wait()
            responseList.append(response)
        }
        return responseList
    }

    // MARK: KYC Account

    func createKycAccount() throws -> Io_Iohk_Atala_Kycbridge_Protos_CreateAccountResponse {
        let request = Io_Iohk_Atala_Kycbridge_Protos_CreateAccountRequest()
        let kyc = kycBridgeService
        return try kyc.createAccount(
            request,
            callOptions: .init(timeLimit: defaultTimeout)
        ).response.wait()
    }
    
    func sendKycResult(
        contact: Contact,
        documentInstanceId: String,
        selfieImage: Data
    ) throws -> Io_Iohk_Atala_Prism_Protos_SendMessageResponse {
        var message = Io_Iohk_Atala_Prism_Protos_AtalaMessage()
        message.kycBridgeMessage.acuantProcessFinished.documentInstanceID = documentInstanceId
        message.kycBridgeMessage.acuantProcessFinished.selfieImage = selfieImage
        let messageData =  try message.serializedData()
        let request = Io_Iohk_Atala_Prism_Protos_SendMessageRequest.with {
            $0.message = messageData
            $0.connectionID = contact.connectionId
        }
        let headers = makeSignedMeta(requestData: try request.serializedData(), keyPath: contact.keyPath)
        return try service.sendMessage(
            request,
            callOptions: .init(
                customMetadata: headers,
                timeLimit: defaultTimeout
            )).response.wait()
    }

// MARK: Mirror Account

    func createMirrorAccount() throws -> Io_Iohk_Atala_Mirror_Protos_CreateAccountResponse {
        let request = Io_Iohk_Atala_Mirror_Protos_CreateAccountRequest()
        let mirror = mirrorService
        return try mirror.createAccount(
            request,
            callOptions: .init(timeLimit: defaultTimeout)
        ).response.wait()
    }

    func registerPayIdName(
        contact: Contact,
        name: String
    ) throws -> Io_Iohk_Atala_Prism_Protos_SendMessageResponse {
        var message = Io_Iohk_Atala_Prism_Protos_AtalaMessage()
        message.mirrorMessage.payIDNameRegistrationMessage.name = name
        let messageData =  try message.serializedData()
        let request = Io_Iohk_Atala_Prism_Protos_SendMessageRequest.with {
            $0.message = messageData
            $0.connectionID = contact.connectionId
        }
        let headers = makeSignedMeta(requestData: try request.serializedData(), keyPath: contact.keyPath)
        return try service.sendMessage(
            request,
            callOptions: .init(
                customMetadata: headers,
                timeLimit: defaultTimeout
        )).response.wait()
    }

    func registerPayIdAddress(
        contact: Contact,
        address: String
    ) throws -> Io_Iohk_Atala_Prism_Protos_SendMessageResponse {
        var message = Io_Iohk_Atala_Prism_Protos_AtalaMessage()
        message.mirrorMessage.registerAddressMessage.cardanoAddress = address
        let messageData =  try message.serializedData()
        let request = Io_Iohk_Atala_Prism_Protos_SendMessageRequest.with {
            $0.message = messageData
            $0.connectionID = contact.connectionId
        }
        let headers = makeSignedMeta(requestData: try request.serializedData(), keyPath: contact.keyPath)
        return try service.sendMessage(
            request,
            callOptions: .init(
                customMetadata: headers,
                timeLimit: defaultTimeout
        )).response.wait()
    }

    func registerPayIdPublicKey(
        contact: Contact,
        publicKey: String
    ) throws -> Io_Iohk_Atala_Prism_Protos_SendMessageResponse {
        var message = Io_Iohk_Atala_Prism_Protos_AtalaMessage()
        message.mirrorMessage.registerWalletMessage.extendedPublicKey = publicKey
        let messageData =  try message.serializedData()
        let request = Io_Iohk_Atala_Prism_Protos_SendMessageRequest.with {
            $0.message = messageData
            $0.connectionID = contact.connectionId
        }
        let headers = makeSignedMeta(requestData: try request.serializedData(), keyPath: contact.keyPath)
        return try service.sendMessage(
            request,
            callOptions: .init(
                customMetadata: headers,
                timeLimit: defaultTimeout
        )).response.wait()
    }

    func validatePayIdName(
        contact: Contact,
        name: String
    ) throws -> Io_Iohk_Atala_Prism_Protos_SendMessageResponse {
        var message = Io_Iohk_Atala_Prism_Protos_AtalaMessage()
        message.mirrorMessage.checkPayIDNameAvailabilityMessage.nameToCheck = name
        let messageData =  try message.serializedData()
        let request = Io_Iohk_Atala_Prism_Protos_SendMessageRequest.with {
            $0.message = messageData
            $0.connectionID = contact.connectionId
        }
        let headers = makeSignedMeta(requestData: try request.serializedData(), keyPath: contact.keyPath)
        return try service.sendMessage(
            request,
            callOptions: .init(
                customMetadata: headers,
                timeLimit: defaultTimeout
        )).response.wait()
    }

    func getMessages(contact: Contact) throws -> Io_Iohk_Atala_Prism_Protos_GetMessagesPaginatedResponse {
        let request = Io_Iohk_Atala_Prism_Protos_GetMessagesPaginatedRequest.with {
            $0.limit = ApiService.DEFAULT_REQUEST_LIMIT
            if let lastMessage = contact.lastMessageId {
                $0.lastSeenMessageID = lastMessage
            }
        }
        let headers = makeSignedMeta(requestData: try request.serializedData(), keyPath: contact.keyPath)
        return try service.getMessagesPaginated(
            request,
            callOptions: .init(
                customMetadata: headers,
                timeLimit: defaultTimeout
        )).response.wait()
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
        let headers = makeSignedMeta(requestData: try request.serializedData(), keyPath: contact.keyPath)
        return try service.sendMessage(
            request,
            callOptions: .init(
                customMetadata: headers,
                timeLimit: defaultTimeout
        )).response.wait()
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
        let headers = makeSignedMeta(requestData: try request.serializedData(), keyPath: contact.keyPath)
        return try service.sendMessage(
            request,
            callOptions: .init(
                customMetadata: headers,
                timeLimit: defaultTimeout
        )).response.wait()
    }
}
