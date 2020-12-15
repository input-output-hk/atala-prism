//
//  Credential+CoreDataProperties.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 16/07/2020.
//  Copyright © 2020 iohk. All rights reserved.
//
//

import Foundation
import CoreData

extension Credential {

    @nonobjc public class func createFetchRequest() -> NSFetchRequest<Credential> {
        return NSFetchRequest<Credential>(entityName: "Credential")
    }

    @NSManaged public var credentialId: String
    @NSManaged public var dateReceived: Date
    @NSManaged public var htmlView: String
    @NSManaged public var issuerId: String
    @NSManaged public var issuerName: String
    @NSManaged public var type: String
    @NSManaged public var viewed: Bool
    @NSManaged public var encoded: Data
    @NSManaged public var json: String
    @NSManaged public var plainCredential: String?

}
