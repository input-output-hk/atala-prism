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

    static func build(_ item: Io_Iohk_Atala_Prism_Protos_GetConnectionTokenInfoResponse) -> ConnectionBase? {
        let issuer = ConnectionBase()
        issuer.did = item.creatorDid
        issuer.name = item.creatorName
        issuer.logoData = item.creatorLogo
        return issuer
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
