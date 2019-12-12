//
import ObjectMapper

class University: Mappable {

    var id: String?
    var name: String?
    var url: String?
    var logoUrl: String?

    init() {}

    required init?(map: Map) {}

    // Mappable
    func mapping(map: Map) {

        id <- map["id"]
        name <- map["name"]
        url <- map["url"]
        logoUrl <- map["logoUrl"]
    }
}

class Employer: Mappable {

    var id: String?
    var name: String?
    var url: String?
    var logoUrl: String?

    init() {}

    required init?(map: Map) {}

    // Mappable
    func mapping(map: Map) {

        id <- map["id"]
        name <- map["name"]
        url <- map["url"]
        logoUrl <- map["logoUrl"]
    }
}

class Degree: Mappable {

    var id: String?
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
    var cost: Double?

    init() {}

    required init?(map: Map) {}

    // Mappable
    func mapping(map: Map) {

        id <- map["id"]
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
}

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

class ConnectionRequest: Mappable {

    var id: String?
    var type: Int? // 0 = University, 1 = Employer
    var name: String?
    var logoUrl: String?

    init() {}

    required init?(map: Map) {}

    // Mappable
    func mapping(map: Map) {

        id <- map["id"]
        type <- map["type"]
        name <- map["name"]
        logoUrl <- map["logoUrl"]
    }
}
