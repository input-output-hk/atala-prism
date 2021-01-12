//
import ObjectMapper

class ConnectionBase: Mappable {

    var connectionId: String?
    var did: String?
    var name: String?
    var token: String?
    var logoUrl: String?
    // Note: Don't store LogoData since the logos are stored in
    // their own dictionary and are heavy to store and load.
    var logoData: Data?
    var type: Int? // 0 = Issuer, 1 = Verifier

    init() {}

    required init?(map: Map) {}

    // Mappable
    func mapping(map: Map) {

        connectionId <- map["connectionId"]
        did <- map["did"]
        name <- map["name"]
        token <- map["token"]
        logoUrl <- map["logoUrl"]
    }
}

class ConnectionMaker {

    static func build(_ item: Io_Iohk_Atala_Prism_Protos_ConnectionInfo) -> ConnectionBase? {

        if item.hasParticipantInfo {
            let res = build(item.participantInfo)
            res?.connectionId = item.connectionID
            res?.token = item.token
            return res
        }
        return nil
    }

    static func build(_ item: Io_Iohk_Atala_Prism_Protos_ParticipantInfo) -> ConnectionBase? {

        // Issuers
        if item.issuer.name.count > 0 {
            let issuer = ConnectionBase()
            issuer.did = item.issuer.did
            issuer.name = item.issuer.name
            issuer.logoData = item.issuer.logo
            issuer.type = 0
            return issuer
        }
        // Verifiers
        if item.verifier.name.count > 0 {
            let verifier = ConnectionBase()
            verifier.did = item.verifier.did
            verifier.name = item.verifier.name
            verifier.logoData = item.verifier.logo
            verifier.type = 1
            return verifier
        }
        return nil
    }

    static func parseResponseList(_ responses: [Io_Iohk_Atala_Prism_Protos_GetConnectionsPaginatedResponse])
        -> [ConnectionBase] {

        var connections: [ConnectionBase] = []

        for response in responses {
            response.connections.forEach { item in

                if let connection = ConnectionMaker.build(item) {
                    connections.append(connection)
                }

            }
        }
        return connections
    }
}

class ConnectionRequest: Mappable {

    var token: String?
    var type: Int? // 0 = University, 1 = Employer
    var info: ConnectionBase?
    var paymentToken: String?
    var paymentNonce: String?

    init() {}

    required init?(map: Map) {}

    // Mappable
    func mapping(map: Map) {

        token <- map["token"]
        type <- map["type"]
        info <- map["info"]
        paymentToken <- map["paymentToken"]
        paymentNonce <- map["paymentNonce"]
    }
}
