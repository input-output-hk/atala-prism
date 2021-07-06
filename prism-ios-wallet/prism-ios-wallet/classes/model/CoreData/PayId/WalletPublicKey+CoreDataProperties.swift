//
//  WalletPublicKey+CoreDataProperties.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 07/06/2021.
//  Copyright © 2021 iohk. All rights reserved.
//
//

import Foundation
import CoreData

extension WalletPublicKey {

    @nonobjc public class func createFetchRequest() -> NSFetchRequest<WalletPublicKey> {
        return NSFetchRequest<WalletPublicKey>(entityName: "WalletPublicKey")
    }

    @NSManaged public var name: String?
    @NSManaged public var key: String?
    @NSManaged public var payId: PayId?

}

extension WalletPublicKey : Identifiable {

}
