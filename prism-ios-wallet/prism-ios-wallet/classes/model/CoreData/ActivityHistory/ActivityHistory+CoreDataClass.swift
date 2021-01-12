//
//  ActivityHistory+CoreDataClass.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 19/08/2020.
//  Copyright © 2020 iohk. All rights reserved.
//
//

import Foundation
import CoreData

enum ActivityHistoryType: Int16 {
    case contactAdded
    case contactDeleted
    case credentialAdded
    case credentialShared
    case credentialRequested
    case credentialDeleted
    case undefined
}

@objc(ActivityHistory)
public class ActivityHistory: NSManagedObject {

    var typeEnum: ActivityHistoryType {                    //  ↓ If self.type is invalid.
        get { return ActivityHistoryType(rawValue: self.type) ?? .undefined }
        set { self.type = newValue.rawValue }
    }
}
