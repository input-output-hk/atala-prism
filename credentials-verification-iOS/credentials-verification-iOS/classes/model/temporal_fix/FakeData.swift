//
import SwiftGRPC
import ObjectMapper

class FakeData {

    static func fakeUserId() -> String {
        return "ca4293a3-80bb-4d85-a7a7-fe3df67da7e1"
    }

    static func fakePublicKey() -> Io_Iohk_Prism_Protos_ConnectorPublicKey {

        var publicKey: Io_Iohk_Prism_Protos_ConnectorPublicKey = Io_Iohk_Prism_Protos_ConnectorPublicKey()
        publicKey.x = "1234"
        publicKey.y = "1234"
        return publicKey
    }

    static func fakeProfile() -> LoggedUser {

        let loggedUser = LoggedUser()
        // loggedUser.id = "ca4293a3-80bb-4d85-a7a7-fe3df67da7e1"
        loggedUser.email = "mauro@mauro.com"
        loggedUser.firstName = "Mauro"
        loggedUser.lastName = "Carreño"
        loggedUser.connectionUserIds = ["af3dccfa-6505-44cb-a24e-4a64ca2e564e": "320f42d9-1ffe-4ddf-b630-72e679cf4731", "fcd5959e-d904-4dc5-85bb-5130f9b565cd": "870543ae-e0d5-4d33-92a9-4654003819e3", "8421a54a-a2b9-43f3-b70b-b2a300e58ec6": "ca4293a3-80bb-4d85-a7a7-fe3df67da7e1"]
        loggedUser.countryFullName = "Republic of Georgia"
        loggedUser.countryShortName = "Georgia"
        loggedUser.nationalityName = "Georgian"
        loggedUser.dateOfBirth = "02/03/1987"
        // loggedUser.avatarUrl = "https://scontent.faep4-1.fna.fbcdn.net/v/t1.0-9/12246889_774081482721005_1079614860347768525_n.jpg?_nc_cat=104&_nc_oc=AQmHxNj4_aLwmT5a8GUGCSuhN-p7kSk0PoZ-cNMvylJQCkLDMoueiHWJOOCQvH5iAQs&_nc_ht=scontent.faep4-1.fna&oh=88106ffab5d09288811498cac3de0816&oe=5E820E6F"
        loggedUser.isVerified = true
        return loggedUser
    }

    static func universitiesList() -> [ConnectionBase] {

        var list: [ConnectionBase] = []

        let u1 = ConnectionBase()
        u1.connectionId = "111"
        u1.name = "Business and Techonoly University"
        u1.logoUrl = "https://studyqa.com/media/upload/univers/705/41/uni_profile_70541.jpg"
        list.append(u1)

        return list
    }

    static func employersList() -> [ConnectionBase] {

        var list: [ConnectionBase] = []

        let e1 = ConnectionBase()
        e1.connectionId = "1"
        e1.name = "HR.GE"
        e1.logoUrl = "https://studyqa.com/media/upload/univers/706/71/uni_profile_70671.jpg"
        list.append(e1)

        let e2 = ConnectionBase()
        e2.connectionId = "2"
        e2.name = "ABC Bank"
        e2.logoUrl = "https://studyqa.com/media/upload/univers/809/01/uni_profile_80901.jpg"
        list.append(e2)

        let e3 = ConnectionBase()
        e3.connectionId = "3"
        e3.name = "Important Employer"
        e3.logoUrl = "https://studyqa.com/media/upload/univers/706/71/uni_profile_70671.jpg"
        list.append(e3)

        let e4 = ConnectionBase()
        e4.connectionId = "4"
        e4.name = "Citi Bank"
        e4.logoUrl = "https://studyqa.com/media/upload/univers/809/01/uni_profile_80901.jpg"
        list.append(e4)

        let e5 = ConnectionBase()
        e5.connectionId = "5"
        e5.name = "Citi Bank 2"
        e5.logoUrl = "https://studyqa.com/media/upload/univers/809/01/uni_profile_80901.jpg"
        list.append(e5)

        return list
    }

    static func seedWords() -> [String] {
        return ["fan", "e", "win", "brick", "sniff", "act", "doll", "until", "test", "comic", "deposit", "bicycle"]
    }

    static func loginWords() -> [Int] {
        return [2, 6]
    }

    static func loginIsValid(words: [String]) -> Bool {
        let seeds = seedWords()
        if seeds[2 - 1] == words[0] && seeds[6 - 1] == words[1] {
            return true
        }
        return false
    }

    static func qrIsValid(code: String) -> ConnectionRequest? {
        if code == "http://www.arcgis.com/apps/Cascade/index.html?appid=10cdc865c31a4bd6ba76e6efefb57730" {
            let conn = ConnectionRequest()
            conn.token = code
            conn.info = ConnectionBase()
            conn.type = 0
            conn.info?.name = "Free University Tbilisi"
            conn.info?.logoUrl = "https://studyqa.com/media/upload/univers/705/41/uni_profile_70541.jpg"
            return conn
        } else if code == "california" {
            let conn = ConnectionRequest()
            conn.token = code
            conn.type = 1
            conn.info?.name = "Super Serious Company"
            conn.info?.logoUrl = "https://studyqa.com/media/upload/univers/706/71/uni_profile_70671.jpg"
            return conn
        }
        return nil
    }

    static func confirmQrCode(conn: ConnectionRequest) -> Bool {
        return conn.type == 0
    }

    static func paymentHistoryList() -> [PaymentHistory] {

        var list: [PaymentHistory] = []

        let e1 = PaymentHistory()
        e1.status = 0
        e1.text = "Credential Issuance Free University Tblisi"
        e1.date = "27 Aug 2019"
        e1.amount = "10"
        list.append(e1)

        let e2 = PaymentHistory()
        e2.status = 1
        e2.text = "Credential Issuance Business and Technical University (BTU)"
        e2.date = "27 Aug 2019"
        e2.amount = "8"
        list.append(e2)

        let e3 = PaymentHistory()
        e3.status = 0
        e3.text = "Credential Verification HR.GE"
        e3.date = "27 Aug 2019"
        e3.amount = "10"
        list.append(e3)

        return list
    }
    
    static func fakeProofOfEmployment() -> Degree {
        let credential = Mapper<Degree>().map(JSONString:  "\n{\n  \"id\": \"tAZFcqDNVVV_D4Y_FwpqXw==\",\n  \"type\": [\"VerifiableCredential\", \"RedlandIdCredential\"],\n  \"issuer\": {\n    \"id\": \"did:atala:091d41cc-e8fc-4c44-9bd3-c938dcf76dff\",\n    \"name\": \"Department of Interior, Republic of Redland\"\n  },\n  \"issuanceDate\": \"2020-03-02\",\n  \"expiryDate\": \"2030-03-02\",\n  \"credentialSubject\": {\n    \"id\": \"unknown\",\n    \"name\": \"Leandro \",\n    \"dateOfBirth\": \"2004-02-19\"\n  }\n}\n")
        credential?.type = .proofOfEmployment
        return credential!;
    }
    
    static func fakeCertificatOfInsurance() -> Degree {
        let credential = Mapper<Degree>().map(JSONString:  "\n{\n  \"id\": \"tAZFcqDNVVV_D4Y_FwpqXw==\",\n  \"type\": [\"VerifiableCredential\", \"RedlandIdCredential\"],\n  \"issuer\": {\n    \"id\": \"did:atala:091d41cc-e8fc-4c44-9bd3-c938dcf76dff\",\n    \"name\": \"Department of Interior, Republic of Redland\"\n  },\n  \"issuanceDate\": \"2020-03-02\",\n  \"expiryDate\": \"2030-03-02\",\n  \"credentialSubject\": {\n    \"id\": \"unknown\",\n    \"name\": \"Leandro \",\n    \"dateOfBirth\": \"2004-02-19\"\n  }\n}\n")
        credential?.type = .certificatOfInsurance
        return credential!;
    }

}
