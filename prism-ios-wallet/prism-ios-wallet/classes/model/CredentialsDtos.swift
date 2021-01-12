//
import ObjectMapper

enum CredentialType: String {
    case demoUniversityDegree = "VerifiableCredential/AirsideDegreeCredential"
    case demoGovernmentIssuedId  = "VerifiableCredential/RedlandIdCredential"
    case demoProofOfEmployment = "VerifiableCredential/AtalaEmploymentCredential"
    case demoCertificateOfInsurance = "VerifiableCredential/AtalaCertificateOfInsurance"
    case univerityDegree = "educational"
    case governmentIssuedId  = "governmentId"
    case proofOfEmployment = "proofOfEmployment"
    case certificatOfInsurance = "healthIsurance"
    case georgiaEducationalDegree = "GeorgiaEducationalDegree"
    case georgiaEducationalDegreeTranscript = "GeorgiaEducationalDegreeTranscript"
    case georgiaNationalID = "GeorgiaNationalID"
}

class CredentialView: Mappable {

    var html: String?
    var credentialType: String?

    init() {}

    required init?(map: Map) {}

    // Mappable
    func mapping(map: Map) {
        html <- map["html"]
        credentialType <- map["credentialType"]
    }

}

class Issuer: Mappable {

    var id: String?
    var name: String?
    var address: String?

    init() {}

    required init?(map: Map) {}

    // Mappable
    func mapping(map: Map) {
        id <- map["id"]
        name <- map["name"]
        address <- map["address"]
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
    var employmentStatus: String?
    var policyNumber: String?
    var productClass: String?
    var intCredential: Data?
    var isNew: Bool?
    var messageId: String?
    var view: CredentialView?
    var issuerStr: String?
    var claims: CredentialView?

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
        employmentStatus <- map["employmentStatus"]
        policyNumber <- map["policyNumber"]
        productClass <- map["productClass"]
        view <- map["view"]
        issuerStr <- map["issuer"]
        claims <- map["claims"]
    }

    static func build(_ sentCredential: Io_Iohk_Atala_Prism_Protos_Credential, messageId: String,
                      isNew: Bool) -> Degree? {

        let credential = Mapper<Degree>().map(JSONString: sentCredential.credentialDocument)

        credential?.intCredential = try? sentCredential.serializedData()
        credential?.type = CredentialType(rawValue: sentCredential.typeID)
        credential?.isNew = isNew
        credential?.messageId = messageId

        return credential
    }
}
