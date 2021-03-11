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
    
    var typeName: String {
        switch typeEnum {
        case .contactAdded:
            return "activitylog_connected".localize()
        case .contactDeleted:
            return "activitylog_deleted".localize()
        case .credentialAdded:
            return String(format: "activitylog_received".localize(), contactName ?? "")
        case .credentialShared:
            return String(format: "activitylog_shared".localize(), contactName ?? "")
        case .credentialRequested:
            return String(format: "activitylog_requested".localize(), contactName ?? "")
        case .credentialDeleted:
            return "activitylog_deleted".localize()
        case .undefined:
            return ""
        }
    }
    
    
    var detail: String? {
        switch typeEnum {
        case .contactAdded:
            return contactName
        case .contactDeleted:
            return contactName
        case .credentialAdded:
            return credentialName
        case .credentialShared:
            return credentialName
        case .credentialRequested:
            return credentialName
        case .credentialDeleted:
            return credentialName
        case .undefined:
            return ""
        }
    }
    
    
    var logo: UIImage {
        switch typeEnum {
        case .contactAdded:
            return #imageLiteral(resourceName: "icon_connected")
        case .contactDeleted:
            return #imageLiteral(resourceName: "icon_delete")
        case .credentialAdded:
            return #imageLiteral(resourceName: "icon_received")
        case .credentialShared:
            return #imageLiteral(resourceName: "icon_shared")
        case .credentialRequested:
            return #imageLiteral(resourceName: "icon_shared")
        case .credentialDeleted:
            return #imageLiteral(resourceName: "icon_delete")
        case .undefined:
            return #imageLiteral(resourceName: "icon_delete")
        }
    }
}
