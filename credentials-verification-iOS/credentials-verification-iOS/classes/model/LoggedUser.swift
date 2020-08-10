//
import ObjectMapper

class LoggedUser: Mappable {

    var id: String?
    var privateKey: String?
    var email: String?
    var firstName: String?
    var lastName: String?

    var idCode: String?
    var identityNumber: String?
    var countryFullName: String?
    var countryShortName: String?
    var nationalityName: String?
    var dateOfBirth: String?
    var qrUrlString: String?
    var avatarUrl: String?
    var isVerified: Bool?

    var apiUrl: String?

    var seed: Data?
    var mnemonics: [String]?
    var lastUsedKeyIndex: Int?

    var appPin: String?
    var appBiometrics: Bool?

    init() {}

    required init?(map: Map) {}

    // Mappable
    func mapping(map: Map) {

        id <- map["id"]
        privateKey <- map["publicKey"]
        email <- map["email"]
        firstName <- map["firstName"]
        lastName <- map["lastName"]

        idCode <- map["idCode"]
        identityNumber <- map["identityNumber"]
        countryFullName <- map["countryFullName"]
        countryShortName <- map["countryShortName"]
        nationalityName <- map["nationalityName"]
        dateOfBirth <- map["dateOfBirth"]
        qrUrlString <- map["qrUrlString"]
        avatarUrl <- map["avatarUrl"]
        isVerified <- map["isVerified"]

        apiUrl <- map["apiUrl"]

        seed <- map["seed"]
        mnemonics <- map["mnemonics"]
        lastUsedKeyIndex <- map["lastUsedKeyIndex"]

        appPin <- map["appPin"]
        appBiometrics <- map["appBiometrics"]
    }
}
