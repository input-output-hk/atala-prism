//
import ObjectMapper

class LoggedUser: Mappable {

    var id: String?
    var privateKey: String?

    var attributes = [Attribute]()
    var personalAttributes: [Attribute]?

    var isVerified: Bool?

    var apiUrl: String?

    var seed: Data?
    var mnemonics: [String]?
    var lastUsedKeyIndex: Int?

    var appPin: String?
    var appBiometrics: Bool?

    var dateFormat: String?

    var payIdCardDismissed: Bool?
    var verifyIdCardDismissed: Bool?
    
    var fullName: String {
        if let name = personalAttributes?.first(where: {
                                                    $0.category?.lowercased().contains("full name") ?? false }) {
                return name.value ?? ""
            }
            return ""
    }

    init() {}

    required init?(map: Map) {}

    // Mappable
    func mapping(map: Map) {

        id <- map["id"]
        privateKey <- map["publicKey"]

        attributes <- map["attributes"]
        personalAttributes <- map["personalAttributes"]

        isVerified <- map["isVerified"]

        apiUrl <- map["apiUrl"]

        seed <- map["seed"]
        mnemonics <- map["mnemonics"]
        lastUsedKeyIndex <- map["lastUsedKeyIndex"]

        appPin <- map["appPin"]
        appBiometrics <- map["appBiometrics"]

        dateFormat <- map["dateFormat"]

        payIdCardDismissed <- map["payIdCardDismissed"]
        verifyIdCardDismissed <- map["verifyIdCardDismissed"]
    }
}
