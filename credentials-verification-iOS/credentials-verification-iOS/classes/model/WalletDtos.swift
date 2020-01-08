//
import ObjectMapper

class PaymentHistory: Mappable {

    var id: String?
    var status: Int? // 0 = Charged, 1 = Failed
    var text: String?
    var date: String?
    var amount: String?

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

    static func build(_ intPayment: Io_Iohk_Cvp_Connector_Payment) -> PaymentHistory? {

        let payment = PaymentHistory()
        payment.amount = intPayment.amount
        payment.date = ApiParseUtils.parseDate(intPayment.createdOn)
        payment.status = intPayment.status == "CHARGED" ? 0 : 1
        payment.id = intPayment.id
        payment.text = ""

        return payment
    }
}
