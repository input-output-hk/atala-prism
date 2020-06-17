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
        loggedUser.connectionUserIds = ["af3dccfa-6505-44cb-a24e-4a64ca2e564e": "320f42d9-1ffe-4ddf-b630-72e679cf4731",
                                        "fcd5959e-d904-4dc5-85bb-5130f9b565cd": "870543ae-e0d5-4d33-92a9-4654003819e3",
                                        "8421a54a-a2b9-43f3-b70b-b2a300e58ec6": "ca4293a3-80bb-4d85-a7a7-fe3df67da7e1"]
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

        let uni1 = ConnectionBase()
        uni1.connectionId = "111"
        uni1.name = "Business and Techonoly University"
        uni1.logoUrl = "https://studyqa.com/media/upload/univers/705/41/uni_profile_70541.jpg"
        list.append(uni1)

        return list
    }

    static func employersList() -> [ConnectionBase] {

        var list: [ConnectionBase] = []

        let element1 = ConnectionBase()
        element1.connectionId = "1"
        element1.name = "HR.GE"
        element1.logoUrl = "https://studyqa.com/media/upload/univers/706/71/uni_profile_70671.jpg"
        list.append(element1)

        let element2 = ConnectionBase()
        element2.connectionId = "2"
        element2.name = "ABC Bank"
        element2.logoUrl = "https://studyqa.com/media/upload/univers/809/01/uni_profile_80901.jpg"
        list.append(element2)

        let element3 = ConnectionBase()
        element3.connectionId = "3"
        element3.name = "Important Employer"
        element3.logoUrl = "https://studyqa.com/media/upload/univers/706/71/uni_profile_70671.jpg"
        list.append(element3)

        let element4 = ConnectionBase()
        element4.connectionId = "4"
        element4.name = "Citi Bank"
        element4.logoUrl = "https://studyqa.com/media/upload/univers/809/01/uni_profile_80901.jpg"
        list.append(element4)

        let element5 = ConnectionBase()
        element5.connectionId = "5"
        element5.name = "Citi Bank 2"
        element5.logoUrl = "https://studyqa.com/media/upload/univers/809/01/uni_profile_80901.jpg"
        list.append(element5)

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

        let element1 = PaymentHistory()
        element1.status = 0
        element1.text = "Credential Issuance Free University Tblisi"
        element1.date = "27 Aug 2019"
        element1.amount = "10"
        list.append(element1)

        let element2 = PaymentHistory()
        element2.status = 1
        element2.text = "Credential Issuance Business and Technical University (BTU)"
        element2.date = "27 Aug 2019"
        element2.amount = "8"
        list.append(element2)

        let element3 = PaymentHistory()
        element3.status = 0
        element3.text = "Credential Verification HR.GE"
        element3.date = "27 Aug 2019"
        element3.amount = "10"
        list.append(element3)

        return list
    }
}
