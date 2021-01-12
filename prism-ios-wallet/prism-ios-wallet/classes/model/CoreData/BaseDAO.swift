//
//  BaseDAO.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 10/09/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import UIKit
import CoreData

class BaseDAO: NSObject {

    func getManagedContext() -> NSManagedObjectContext? {
        guard let appDelegate = UIApplication.shared.delegate as? AppDelegate else { return nil }
        return appDelegate.persistentContainer.viewContext
    }

}
