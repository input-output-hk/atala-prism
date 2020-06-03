//
import Foundation

struct Common {

    enum ServerTarget: Int {
        case dev = 0
        case stg
        case prod
    }

    static let SERVER_TARGET: ServerTarget = .dev
    static let DEBUG = true

    // General values
    public static let KEY_PREF = "iohk.credentials-verification-iOS."

    // Api
    static var URL_API = "develop.atalaprism.io:50051"
    static let URL_TERMS = "https://\(URL_API.prefix(upTo: URL_API.lastIndex(of: ":")!))/terms-and-conditions"
    static let URL_PRIVACY = "https://\(URL_API.prefix(upTo: URL_API.lastIndex(of: ":")!))/privacy-policy"
    static let URL_SUPPORT = "https://www.amazon.com/gp/help/customer/display.html/ref=hp_nodeid_596184_terms?nodeId=14309551"
}
