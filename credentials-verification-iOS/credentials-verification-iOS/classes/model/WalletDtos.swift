//
import ObjectMapper

class PaymentHistory: Mappable {

    var id: String?
    var status: Int?
    var text: String?
    var date: String?
    var amount: Int?

    init() {}

    required init?(map: Map) {}

    // Mappable
    func mapping(map: Map) {

        id <- map["id"]
        status <- map["status"]
        text <- map["text"]
        date <- map["date"]
        amount <- map["amount"]
    }
}
