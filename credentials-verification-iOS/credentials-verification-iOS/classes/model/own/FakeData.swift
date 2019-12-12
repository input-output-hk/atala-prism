//

class FakeData {

    static func profile() -> LoggedUser {

        let loggedUser = LoggedUser()
        loggedUser.id = "ABC123"
        loggedUser.email = "mauro@mauro.com"
        loggedUser.firstName = "Mauro"
        loggedUser.lastName = "Carreño"
        loggedUser.idCode = "1BoatSLRHtKNngkdXEeobR76b53LETtpyT"
        loggedUser.identityNumber = "123 456 789"
        loggedUser.countryFullName = "Republic of Georgia"
        loggedUser.countryShortName = "Georgia"
        loggedUser.nationalityName = "Georgian"
        loggedUser.dateOfBirth = "02/03/1987"
        loggedUser.qrUrlString = "https://www.qr-code-generator.com/wp-content/themes/qr/new_structure/markets/core_market_full/generator/dist/generator/assets/images/websiteQRCode_noFrame.png"
        loggedUser.avatarUrl = "https://scontent.faep4-1.fna.fbcdn.net/v/t1.0-9/12246889_774081482721005_1079614860347768525_n.jpg?_nc_cat=104&_nc_oc=AQmHxNj4_aLwmT5a8GUGCSuhN-p7kSk0PoZ-cNMvylJQCkLDMoueiHWJOOCQvH5iAQs&_nc_ht=scontent.faep4-1.fna&oh=88106ffab5d09288811498cac3de0816&oe=5E820E6F"
        loggedUser.isVerified = true
        return loggedUser
    }

    static func universitiesList() -> [University] {

        var list: [University] = []

        let u1 = University()
        u1.name = "Business and Techonoly University"
        u1.url = "http://www.fi.uba.ar/"
        u1.logoUrl = "https://studyqa.com/media/upload/univers/705/41/uni_profile_70541.jpg"
        list.append(u1)

        return list
    }

    static func employersList() -> [Employer] {

        var list: [Employer] = []

        let e1 = Employer()
        e1.id = "1"
        e1.name = "HR.GE"
        e1.url = "https://www.google.com/"
        e1.logoUrl = "https://studyqa.com/media/upload/univers/706/71/uni_profile_70671.jpg"
        list.append(e1)

        let e2 = Employer()
        e2.id = "2"
        e2.name = "ABC Bank"
        e2.url = "https://www.google.com/"
        e2.logoUrl = "https://studyqa.com/media/upload/univers/809/01/uni_profile_80901.jpg"
        list.append(e2)

        let e3 = Employer()
        e3.id = "3"
        e3.name = "Important Employer"
        e3.url = "https://www.google.com/"
        e3.logoUrl = "https://studyqa.com/media/upload/univers/706/71/uni_profile_70671.jpg"
        list.append(e3)

        let e4 = Employer()
        e4.id = "4"
        e4.name = "Citi Bank"
        e4.url = "https://www.google.com/"
        e4.logoUrl = "https://studyqa.com/media/upload/univers/809/01/uni_profile_80901.jpg"
        list.append(e4)

        let e5 = Employer()
        e5.id = "5"
        e5.name = "Citi Bank 2"
        e5.url = "https://www.google.com/"
        e5.logoUrl = "https://studyqa.com/media/upload/univers/809/01/uni_profile_80901.jpg"
        list.append(e5)

        return list
    }

    static func degreesList() -> [Degree] {

        var list: [Degree] = []

        let d1 = Degree()
        d1.id = "A1"
        d1.name = "Credential Free University Tbilisi"
        d1.subtitle = nil
        d1.type = 1
        d1.logoUrl = "https://studyqa.com/media/upload/univers/705/41/uni_profile_70541.jpg"
        d1.preLogoUrl = "https://www.botinero.net/images/BORRAME/logo_cap_bluet.png"
        d1.isNew = true
        d1.cost = 10
        d1.fullName = "Bachelor's in Engineering"
        d1.startDate = "03/01/2009"
        d1.endDate = "03/01/2013"
        d1.properties = ["University Name": "Free University Tbilisi", "Award": "First Class Honours", "Full Name": "Mauro Carreño"]
        list.append(d1)

        let d2 = Degree()
        d2.id = "A2"
        d2.name = "Credential BTU"
        d2.subtitle = "Lorem ipsum text"
        d2.type = 1
        d2.logoUrl = "https://studyqa.com/media/upload/univers/706/71/uni_profile_70671.jpg"
        d2.preLogoUrl = "https://www.botinero.net/images/BORRAME/logo_cap_green.png"
        d2.isNew = true
        d2.cost = 30
        d2.fullName = "Bachelor's in Engineering"
        d2.startDate = "03/01/2009"
        d2.endDate = "03/01/2013"
        d2.properties = ["University Name": "Credential BTU", "Full Name": "Mauro Carreño"]
        list.append(d2)

        let d3 = Degree()
        d3.id = "A3"
        d3.name = "Univerity of Texas"
        d3.subtitle = "In Lorem we Ipsum"
        d3.type = 1
        d3.logoUrl = "https://studyqa.com/media/upload/univers/706/71/uni_profile_70671.jpg"
        d3.preLogoUrl = "https://www.botinero.net/images/BORRAME/logo_cap_red.png"
        d3.isNew = false
        d3.fullName = "Bachelor's in Engineering"
        d3.startDate = "03/01/2009"
        d3.endDate = "03/01/2013"
        d3.properties = ["University Name": "National Identity Card", "Award": "Best Student", "Nickname": "The Best-O", "Full Name": "Mauro Carreño"]
        list.append(d3)

        let d4 = Degree()
        d4.id = "A4"
        d4.name = "School Certificate"
        d4.subtitle = "Lorem ipsum text"
        d4.type = 2
        d4.logoUrl = "https://studyqa.com/media/upload/univers/706/71/uni_profile_70671.jpg"
        d4.preLogoUrl = "https://www.botinero.net/images/BORRAME/logo_building_violet.png"
        d4.isNew = false
        d4.fullName = "Primary Schooler"
        d4.startDate = "03/01/2009"
        d4.endDate = "03/01/2013"
        d4.properties = ["University Name": "School Certificate", "Award": "Best Student", "Nickname": "The Best-O", "Full Name": "Mauro Carreño"]
        list.append(d4)

        let d5 = Degree()
        d5.id = "A5"
        d5.name = "Prueba nueva"
        d5.subtitle = "Lorem ipsum text"
        d5.type = 2
        d5.logoUrl = "https://studyqa.com/media/upload/univers/706/71/uni_profile_70671.jpg"
        d5.preLogoUrl = "https://www.botinero.net/images/BORRAME/logo_building_violet.png"
        d5.isNew = true
        d5.cost = 20
        d5.fullName = "Primary Schooler"
        d5.startDate = "03/01/2009"
        d5.endDate = "03/01/2013"
        d5.properties = ["University Name": "School Certificate", "Award": "Best Student", "Nickname": "The Best-O", "Full Name": "Mauro Carreño"]
        list.append(d5)

        return list
    }

    static func seedWords() -> [String] {
        return ["fan", "e", "win", "brick", "sniff", "act", "doll", "until", "test", "comic", "deposit", "bicycle"]
    }

    static func loginWords() -> [Int] {
        return [2, 6]
    }

    static func loginIsValid(words: [String]) -> LoggedUser? {
        let seeds = seedWords()
        if seeds[2 - 1] == words[0] && seeds[6 - 1] == words[1] {
            return profile()
        }
        return nil
    }

    static func qrIsValid(code: String) -> ConnectionRequest? {
        if code == "http://www.arcgis.com/apps/Cascade/index.html?appid=10cdc865c31a4bd6ba76e6efefb57730" {
            let conn = ConnectionRequest()
            conn.name = "Free University Tbilisi"
            conn.type = 0
            conn.logoUrl = "https://studyqa.com/media/upload/univers/705/41/uni_profile_70541.jpg"
            return conn
        } else if code == "california" {
            let conn = ConnectionRequest()
            conn.name = "Super Serious Company"
            conn.type = 1
            conn.logoUrl = "https://studyqa.com/media/upload/univers/706/71/uni_profile_70671.jpg"
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
        e1.amount = 10
        list.append(e1)

        let e2 = PaymentHistory()
        e2.status = 1
        e2.text = "Credential Issuance Business and Technical University (BTU)"
        e2.date = "27 Aug 2019"
        e2.amount = 8
        list.append(e2)

        let e3 = PaymentHistory()
        e3.status = 0
        e3.text = "Credential Verification HR.GE"
        e3.date = "27 Aug 2019"
        e3.amount = 10
        list.append(e3)

        return list
    }
}
