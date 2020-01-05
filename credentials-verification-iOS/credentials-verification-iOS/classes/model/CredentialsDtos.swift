//
import ObjectMapper

class Degree: Mappable {

    var connectionId: String?
    var messageId: String?
    var type: Int?
    var name: String?
    var subtitle: String?
    var logoUrl: String?
    var preLogoUrl: String?
    var isNew: Bool?
    var fullName: String?
    var startDate: String?
    var endDate: String?
    var properties: [String: String]?
    var cost: Int?
    // Note: Can't store Io_Iohk_Cvp_Credential_Credential because
    // it doesn't implement Mappable.
    var intCredential: Io_Iohk_Cvp_Credential_Credential?

    init() {}

    required init?(map: Map) {}

    // Mappable
    func mapping(map: Map) {

        connectionId <- map["connectionId"]
        messageId <- map["messageId"]
        type <- map["type"]
        name <- map["name"]
        subtitle <- map["subtitle"]
        logoUrl <- map["logoUrl"]
        preLogoUrl <- map["preLogoUrl"]
        isNew <- map["isNew"]
        fullName <- map["fullName"]
        startDate <- map["startDate"]
        endDate <- map["endDate"]
        properties <- map["properties"]
        cost <- map["cost"]
    }

    static func build(_ message: Io_Iohk_Cvp_Connector_ReceivedMessage, isNew: Bool) -> Degree? {

        guard let sentCredential = try? Io_Iohk_Cvp_Credential_SentCredential(serializedData: message.message) else {
            return nil
        }
        if !sentCredential.issuerSentCredential.hasCredential {
            return nil
        }
        let intCredential = sentCredential.issuerSentCredential.credential

        let credential = Degree()
        credential.intCredential = intCredential
        credential.type = 1 // Degree
        credential.isNew = isNew
        credential.fullName = intCredential.degreeAwarded
        credential.name = intCredential.issuerType.academicAuthority
        credential.startDate = intCredential.hasAdmissionDate ? parseDate(intCredential.admissionDate) : ""
        credential.endDate = intCredential.hasGraduationDate ? parseDate(intCredential.graduationDate) : ""
        credential.connectionId = message.connectionID
        credential.messageId = message.id
        credential.properties = [:]
        if intCredential.hasSubjectData {
            credential.properties?["home_detail_full_name".localize()] = parseFullName(intCredential.subjectData)
        }
        if intCredential.additionalSpeciality != "" {
            credential.properties?["home_detail_award".localize()] = intCredential.additionalSpeciality
        }
        if intCredential.hasIssuerType {
            credential.properties?["home_detail_university_name".localize()] = intCredential.issuerType.academicAuthority
        }

        return credential
    }

    private static func parseDate(_ intDate: Io_Iohk_Cvp_Credential_Date) -> String {
        return "\(intDate.month)/\(intDate.day)/\(intDate.year)"
    }

    private static func parseFullName(_ intSubject: Io_Iohk_Cvp_Credential_SubjectData) -> String {

        var fullName = ""
        for name in intSubject.names {
            fullName = "\(fullName) \(name)"
        }
        for surname in intSubject.surname {
            fullName = "\(fullName) \(surname)"
        }
        return fullName.trim()
    }
}
