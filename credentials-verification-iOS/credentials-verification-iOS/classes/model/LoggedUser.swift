//
import ObjectMapper

class LoggedUser: Mappable {

    var id: String?
    var privateKey: String?
    var email: String?
    var firstName: String?
    var lastName: String?
    var connectionUserIds: [String: String]? // ConnectionId -> UserId
    var messagesAcceptedIds: [String]?
    var messagesRejectedIds: [String]?

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

    init() {
        connectionUserIds = [:]
        messagesAcceptedIds = []
        messagesRejectedIds = []
    }

    required init?(map: Map) {}

    // Mappable
    func mapping(map: Map) {

        id <- map["id"]
        privateKey <- map["publicKey"]
        email <- map["email"]
        firstName <- map["firstName"]
        lastName <- map["lastName"]
        connectionUserIds <- map["connectionUserIds"]
        messagesAcceptedIds <- map["messagesAcceptedIds"]
        messagesRejectedIds <- map["messagesRejectedIds"]

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
    }
}
