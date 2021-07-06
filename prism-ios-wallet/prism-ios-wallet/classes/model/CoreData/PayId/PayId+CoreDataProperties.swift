//
//  PayId+CoreDataProperties.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 07/06/2021.
//  Copyright © 2021 iohk. All rights reserved.
//
//

import Foundation
import CoreData

extension PayId {

    @nonobjc public class func createFetchRequest() -> NSFetchRequest<PayId> {
        return NSFetchRequest<PayId>(entityName: "PayId")
    }

    @NSManaged public var connectionId: String?
    @NSManaged public var name: String?
    @NSManaged public var payIdId: String?
    @NSManaged public var addresses: NSSet?
    @NSManaged public var publicKeys: NSSet?

}

// MARK: Generated accessors for addresses
extension PayId {

    @objc(addAddressesObject:)
    @NSManaged public func addToAddresses(_ value: Address)

    @objc(removeAddressesObject:)
    @NSManaged public func removeFromAddresses(_ value: Address)

    @objc(addAddresses:)
    @NSManaged public func addToAddresses(_ values: NSSet)

    @objc(removeAddresses:)
    @NSManaged public func removeFromAddresses(_ values: NSSet)

}

// MARK: Generated accessors for publicKeys
extension PayId {

    @objc(addPublicKeysObject:)
    @NSManaged public func addToPublicKeys(_ value: WalletPublicKey)

    @objc(removePublicKeysObject:)
    @NSManaged public func removeFromPublicKeys(_ value: WalletPublicKey)

    @objc(addPublicKeys:)
    @NSManaged public func addToPublicKeys(_ values: NSSet)

    @objc(removePublicKeys:)
    @NSManaged public func removeFromPublicKeys(_ values: NSSet)

}

extension PayId : Identifiable {

}
