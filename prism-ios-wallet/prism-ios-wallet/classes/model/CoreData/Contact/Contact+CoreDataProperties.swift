//
//  Contact+CoreDataProperties.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 27/05/2021.
//  Copyright © 2021 iohk. All rights reserved.
//
//

import Foundation
import CoreData

extension Contact {

    @nonobjc public class func createFetchRequest() -> NSFetchRequest<Contact> {
        return NSFetchRequest<Contact>(entityName: "Contact")
    }

    @NSManaged public var connectionId: String
    @NSManaged public var dateCreated: Date
    @NSManaged public var did: String
    @NSManaged public var keyPath: String
    @NSManaged public var lastMessageId: String?
    @NSManaged public var logo: Data?
    @NSManaged public var name: String
    @NSManaged public var token: String
    @NSManaged public var isPayId: Bool
    @NSManaged public var isKyc: Bool

}

extension Contact : Identifiable {

}
