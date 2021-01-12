//
//  CredentialHistoryDAO.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 15/07/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import UIKit
import CoreData

class CredentialHistoryDAO: NSObject {

    func getManagedContext() -> NSManagedObjectContext? {
        guard let appDelegate = UIApplication.shared.delegate as? AppDelegate else { return nil }
        return appDelegate.persistentContainer.viewContext
    }

    func listCredentialHistory(credential: Credential) -> [CredentialHistory]? {
        let fetchRequest = CredentialHistory.fetchRequest()
        fetchRequest.predicate = NSPredicate(format: "credential.credentialId = %@", credential.credentialId)
        let result = try? getManagedContext()?.fetch(fetchRequest)
        return result as? [CredentialHistory]
    }

    func createCredentialHistory(dateShared: Date?, isRequested: Bool, credential: Credential?,
                                 connection: Contact?) -> CredentialHistory? {
        guard let managedContext = getManagedContext() else { return nil }
        let credentialHistory = NSEntityDescription.insertNewObject(forEntityName: "CredentialHistory",
                                                                    into: managedContext) as? CredentialHistory
        credentialHistory?.dateShared = dateShared
        credentialHistory?.isRequested = isRequested
        credentialHistory?.credential = credential
        credentialHistory?.connection = connection

        do {
            try managedContext.save()
            return credentialHistory

        } catch let error as NSError {
            print(error.debugDescription)
            return nil
        }
    }

    func deleteCredentialHistory(credentialHistory: CredentialHistory) -> Bool {
        guard let managedContext = getManagedContext() else { return false }
        managedContext.delete(credentialHistory)
        do {
            try managedContext.save()
            return true
        } catch let error as NSError {
            print(error.debugDescription)
            return false
        }
    }

    func deleteAllCredentialHistory(credential: Credential) -> Bool {
        guard let managedContext = getManagedContext() else { return false }
        let fetchRequest = Credential.fetchRequest()
        fetchRequest.predicate = NSPredicate(format: "credential.credentialId = %@", credential.credentialId)
        do {
            if let result = try managedContext.fetch(fetchRequest) as? [CredentialHistory] {
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
