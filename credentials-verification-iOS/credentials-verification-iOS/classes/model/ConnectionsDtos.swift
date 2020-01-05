//
import ObjectMapper

class ConnectionBase: Mappable {

    var connectionId: String?
    var did: String?
    var name: String?
    var logoUrl: String?
    // Note: Don't store LogoData since the logos are stored in
    // their own dictionary and are heavy to store and load.
    var logoData: Data?

    init() {}

    required init?(map: Map) {}

    // Mappable
    func mapping(map: Map) {

        connectionId <- map["connectionId"]
        did <- map["did"]
        name <- map["name"]
        logoUrl <- map["logoUrl"]
    }
}

class ConnectionMaker {

    static func build(_ item: Io_Iohk_Cvp_Connector_ConnectionInfo) -> ConnectionBase? {

        if item.hasParticipantInfo {
            let res = build(item.participantInfo)
            res?.connectionId = item.connectionID
            return res
        }
        return nil
    }

    static func build(_ item: Io_Iohk_Cvp_Connector_ParticipantInfo) -> ConnectionBase? {

        // Universities
        if item.issuer.name.count > 0 {
            let u = University()
            u.did = item.issuer.did
            u.name = item.issuer.name
            u.logoData = item.issuer.logo
            return u
        }
        // Employers
        if item.verifier.name.count > 0 {
            let e = Employer()
            e.did = item.verifier.did
            e.name = item.verifier.name
            e.logoData = item.verifier.logo
            return e
        }
        return nil
    }

    static func parseResponseList(_ responses: [Io_Iohk_Cvp_Connector_GetConnectionsPaginatedResponse]) -> ([University], [Employer]) {

        var universities: [University] = []
        var employers: [Employer] = []

        for response in responses {
            response.connections.forEach { item in

                let connection = ConnectionMaker.build(item)
                if let u = connection as? University {
                    universities.append(u)
                }
                if let e = connection as? Employer {
                    employers.append(e)
                }
            }
        }
        return (universities, employers)
    }
}

class University: ConnectionBase {

    required init?(map: Map) {
        super.init(map: map)
    }

    override init() {
        super.init()
    }
}

class Employer: ConnectionBase {

    required init?(map: Map) {
        super.init(map: map)
    }

    override init() {
        super.init()
    }
}

class ConnectionRequest: Mappable {

    var token: String?
    var type: Int? // 0 = University, 1 = Employer
    var info: ConnectionBase?

    init() {}

    required init?(map: Map) {}

    // Mappable
    func mapping(map: Map) {

        token <- map["token"]
        type <- map["type"]
        info <- map["info"]
    }
}
