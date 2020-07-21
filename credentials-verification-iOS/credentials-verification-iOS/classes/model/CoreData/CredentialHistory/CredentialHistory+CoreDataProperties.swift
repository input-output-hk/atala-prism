//
//  CredentialHistory+CoreDataProperties.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 15/07/2020.
//  Copyright © 2020 iohk. All rights reserved.
//
//

import Foundation
import CoreData

extension CredentialHistory {

    @nonobjc public class func createFetchRequest() -> NSFetchRequest<CredentialHistory> {
        return NSFetchRequest<CredentialHistory>(entityName: "CredentialHistory")
    }

    @NSManaged public var dateShared: Date?
    @NSManaged public var isRequested: Bool
    @NSManaged public var credential: Credential?
    @NSManaged public var connection: Contact?

}
