//
import Foundation

struct Common {

    enum ServerTarget: Int {
        case dev = 0
        case stg
        case prod
    }

    static let SERVER_TARGET: ServerTarget = .dev

    // General values
    public static let KEY_PREF = "iohk.credentials-verification-iOS."
    public static let DAFAULT_DATE_FORMAT = "dd/MM/yyyy"

    // Api
    static var URL_API = Env.isProduction() ? "www.atalaprism.io:50051": "develop.atalaprism.io:50051"
    static let KYC_PORT = ":8081"
    static let URL_TERMS = "https://legal.atalaprism.io/terms-and-conditions.html"
    static let URL_PRIVACY = "https://legal.atalaprism.io/privacy-policy.html"
    static let URL_SUPPORT = "https://iohk.zendesk.com/hc/en-us/requests/new"
}
