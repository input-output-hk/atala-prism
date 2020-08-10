//
import SwiftGRPC
import ObjectMapper

class FakeData {

    static func fakeUserId() -> String {
        return "ca4293a3-80bb-4d85-a7a7-fe3df67da7e1"
    }

    static func fakeProfile() -> LoggedUser {

        let loggedUser = LoggedUser()
        // loggedUser.id = "ca4293a3-80bb-4d85-a7a7-fe3df67da7e1"
        loggedUser.email = "mauro@mauro.com"
        loggedUser.firstName = "Mauro"
        loggedUser.lastName = "Carreño"
        loggedUser.countryFullName = "Republic of Georgia"
        loggedUser.countryShortName = "Georgia"
        loggedUser.nationalityName = "Georgian"
        loggedUser.dateOfBirth = "02/03/1987"
        // loggedUser.avatarUrl = "https://scontent.faep4-1.fna.fbcdn.net/v/t1.0-9/12246889_774081482721005_1079614860347768525_n.jpg?_nc_cat=104&_nc_oc=AQmHxNj4_aLwmT5a8GUGCSuhN-p7kSk0PoZ-cNMvylJQCkLDMoueiHWJOOCQvH5iAQs&_nc_ht=scontent.faep4-1.fna&oh=88106ffab5d09288811498cac3de0816&oe=5E820E6F"
        loggedUser.isVerified = true
        return loggedUser
    }
}
