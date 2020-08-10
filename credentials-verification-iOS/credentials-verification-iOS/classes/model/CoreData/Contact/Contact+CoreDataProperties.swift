//
//  Contact+CoreDataProperties.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 22/07/2020.
//  Copyright © 2020 iohk. All rights reserved.
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
    @NSManaged public var lastMessageId: String?
    @NSManaged public var name: String
    @NSManaged public var token: String
    @NSManaged public var keyPath: String
    @NSManaged public var logo: Data?

}
