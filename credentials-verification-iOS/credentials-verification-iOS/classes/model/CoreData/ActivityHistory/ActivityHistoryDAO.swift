//
//  CredentialHistoryDAO.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 15/07/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import UIKit
import CoreData

class ActivityHistoryDAO: BaseDAO {

    func getSortDescriptors() -> [NSSortDescriptor] {
        return [NSSortDescriptor(key: "timestamp", ascending: false),
                NSSortDescriptor(key: "credentialName", ascending: true)]
    }

    func listActivityHistory() -> [ActivityHistory]? {
        let fetchRequest = ActivityHistory.fetchRequest()
        fetchRequest.sortDescriptors = getSortDescriptors()
        let result = try? getManagedContext()?.fetch(fetchRequest)
        return result as? [ActivityHistory]
    }

    func listActivityHistory(for credentialId: String) -> [ActivityHistory]? {
        let fetchRequest = ActivityHistory.fetchRequest()
        fetchRequest.predicate = NSPredicate(format: "credentialId == %@", credentialId)
        fetchRequest.sortDescriptors = getSortDescriptors()
        let result = try? getManagedContext()?.fetch(fetchRequest)
        return result as? [ActivityHistory]
    }

    @discardableResult
    func createActivityHistory(timestamp: Date?, type: ActivityHistoryType, credential: Credential?,
                               contact: Contact?) -> ActivityHistory? {
        guard let managedContext = getManagedContext() else { return nil }
        let credentialHistory = NSEntityDescription.insertNewObject(forEntityName: "ActivityHistory",
                                                                    into: managedContext) as? ActivityHistory
        credentialHistory?.timestamp = timestamp
        credentialHistory?.typeEnum = type
        credentialHistory?.credentialId = credential?.credentialId
        credentialHistory?.credentialName = credential?.credentialName
        credentialHistory?.contactId = contact?.connectionId
        credentialHistory?.contactName = contact?.name ?? credential?.issuerName
        credentialHistory?.contactLogo = contact?.logo

        do {
            try managedContext.save()
            return credentialHistory

        } catch let error as NSError {
            print(error.debugDescription)
            return nil
        }
    }

    func deleteActivityHistory(activityHistory: ActivityHistory) -> Bool {
        guard let managedContext = getManagedContext() else { return false }
        managedContext.delete(activityHistory)
        do {
            try managedContext.save()
            return true
        } catch let error as NSError {
            print(error.debugDescription)
            return false
        }
    }

    func deleteAllActivityHistory() -> Bool {
        guard let managedContext = getManagedContext() else { return false }
        let fetchRequest = ActivityHistory.createFetchRequest()
        do {
            if let result = try? managedContext.fetch(fetchRequest) {
                for credentialHistory in result {
                    managedContext.delete(credentialHistory)
                }
                try managedContext.save()
                return true
            }
        } catch let error as NSError {
            print(error.debugDescription)
        }
        return false
    }
}
