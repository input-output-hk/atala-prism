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

    static func build(_ item: Io_Iohk_Prism_Protos_ConnectionInfo) -> ConnectionBase? {

        if item.hasParticipantInfo {
            let res = build(item.participantInfo)
            res?.connectionId = item.connectionID
            res?.token = item.token
            return res
        }
        return nil
    }

    static func build(_ item: Io_Iohk_Prism_Protos_ParticipantInfo) -> ConnectionBase? {

        // Issuers
        if item.issuer.name.count > 0 {
            let u = ConnectionBase()
            u.did = item.issuer.did
            u.name = item.issuer.name
            u.logoData = item.issuer.logo
            u.type = 0
            return u
        }
        // Verifiers
        if item.verifier.name.count > 0 {
            let e = ConnectionBase()
            e.did = item.verifier.did
            e.name = item.verifier.name
            e.logoData = item.verifier.logo
            e.type = 1
            return e
        }
        return nil
    }

    static func parseResponseList(_ responses: [Io_Iohk_Prism_Protos_GetConnectionsPaginatedResponse]) -> [ConnectionBase] {

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
