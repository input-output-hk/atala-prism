//
import ObjectMapper

enum CredentialType: String {
    case univerityDegree = "VerifiableCredential/AirsideDegreeCredential"
    case governmentIssuedId  = "VerifiableCredential/RedlandIdCredential"
    case proofOfEmployment = "VerifiableCredential/CertificateOfEmployment"
    case certificatOfInsurance = "VerifiableCredential/CertificateOfInsurance"
}

class Issuer: Mappable {

    var id: String?
    var name: String?

    init() {}

    required init?(map: Map) {}

    // Mappable
    func mapping(map: Map) {
        id <- map["id"]
        name <- map["name"]
    }
    
}

class CredentialSubject: Mappable {

    var identityNumber: String?
    var name: String?
    var dateOfBirth: String?
    var sex: String?
    var nationality: String?
    var degreeAwarded: String?
    var degreeResult: String?
    var graduationYear: String?

    init() {}

    required init?(map: Map) {}

    // Mappable
    func mapping(map: Map) {
        identityNumber <- map["identityNumber"]
        name <- map["name"]
        dateOfBirth <- map["dateOfBirth"]
        sex <- map["sex"]
        nationality <- map["nationality"]
        degreeAwarded <- map["degreeAwarded"]
        degreeResult <- map["degreeResult"]
        graduationYear <- map["graduationYear"]
    }
    
}

class Degree: Mappable {

    var connectionId: String?
    var types: [String]?
    var type: CredentialType?
    var issuer: Issuer?
    var issuanceDate: String?
    var expiryDate: String?
    var credentialSubject: CredentialSubject?
    // Note: Can't store Io_Iohk_Cvp_Credential_Credential because
    // it doesn't implement Mappable.
    var intCredential: Io_Iohk_Cvp_Credential_Credential?
    var isNew: Bool?
    var messageId: String?

    init() {}

    required init?(map: Map) {}

    // Mappable
    func mapping(map: Map) {

        connectionId <- map["id"]
        types <- map["type"]
        issuer <- map["issuer"]
        issuanceDate <- map["issuanceDate"]
        expiryDate <- map["expiryDate"]
        credentialSubject <- map["credentialSubject"]
    }

    static func build(_ message: Io_Iohk_Prism_Protos_ReceivedMessage, isNew: Bool) -> Degree? {

        guard let sentCredential = try? Io_Iohk_Cvp_Credential_Credential(serializedData: message.message) else {
            return nil
        }
        let credential = Mapper<Degree>().map(JSONString: sentCredential.credentialDocument)

        credential?.intCredential = sentCredential
        credential?.type = CredentialType(rawValue: sentCredential.typeID)
        credential?.isNew = isNew
        credential?.messageId = message.id


        return credential
    }
}
