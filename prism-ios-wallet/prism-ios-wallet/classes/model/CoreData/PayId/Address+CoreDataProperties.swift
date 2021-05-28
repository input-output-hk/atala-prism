//
//  Address+CoreDataProperties.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 26/04/2021.
//  Copyright © 2021 iohk. All rights reserved.
//
//

import Foundation
import CoreData


extension Address {

    @nonobjc public class func createFetchRequest() -> NSFetchRequest<Address> {
        return NSFetchRequest<Address>(entityName: "Address")
    }

    @NSManaged public var name: String?
    @NSManaged public var payId: PayId?

}

extension Address : Identifiable {

}
