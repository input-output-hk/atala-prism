//
//  Attribute.swift
//  prism-ios-wallet
//
//  Created by Roberto Daviduk on 02/03/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import ObjectMapper

class Attribute: Mappable {

    var category: String?
    var type: String?
    var value: String?
    var logo: String?

    init() {}

    required init?(map: Map) {}


    // Mappable
    func mapping(map: Map) {

        category <- map["category"]
        type <- map["type"]
        value <- map["value"]
        logo <- map["logo"]
    }
}
