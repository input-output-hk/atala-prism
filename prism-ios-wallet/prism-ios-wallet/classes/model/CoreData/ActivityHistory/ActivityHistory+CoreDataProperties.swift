//
//  ActivityHistory+CoreDataProperties.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 20/08/2020.
//  Copyright © 2020 iohk. All rights reserved.
//
//

import Foundation
import CoreData

extension ActivityHistory {

    @nonobjc public class func createFetchRequest() -> NSFetchRequest<ActivityHistory> {
        return NSFetchRequest<ActivityHistory>(entityName: "ActivityHistory")
    }

    @NSManaged public var timestamp: Date?
    @NSManaged public var type: Int16
    @NSManaged public var credentialName: String?
    @NSManaged public var credentialId: String?
    @NSManaged public var contactName: String?
    @NSManaged public var contactId: String?
    @NSManaged public var contactLogo: Data?

}
