//
import ObjectMapper

class LoggedUser: Mappable {

    var id: String?
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

    init() {}

    required init?(map: Map) {}

    // Mappable
    func mapping(map: Map) {

        id <- map["id"]
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
    }
}
