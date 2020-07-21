//
//  CreedentialDAO.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 15/07/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import UIKit

class CredentialDAO: NSObject {

    func listCredentials() -> [Credential]? {
        guard let appDelegate = UIApplication.shared.delegate as? AppDelegate else { return nil }
        let managedContext = appDelegate.persistentContainer.viewContext
        let fetchRequest = Credential.createFetchRequest()
        do {
            let result = try managedContext.fetch(fetchRequest)
            return result
        } catch let error as NSError {
            print(error.debugDescription)
            return nil
        }
    }

    func createCredential(type: String?, credentialId: String?, issuerId: String?, issuerName: String?,
                          htmlView: String?, dateReceived: Date?) -> Credential? {
        let credential = Credential()
        credential.type = type
        credential.credentialId = credentialId
        credential.issuerId = issuerId
        credential.issuerName = issuerName
        credential.htmlView = htmlView
        credential.dateReceived = dateReceived

        guard let appDelegate = UIApplication.shared.delegate as? AppDelegate else { return nil }
        let managedContext = appDelegate.persistentContainer.viewContext

        do {
            try managedContext.save()
            return credential

        } catch let error as NSError {
            print(error.debugDescription)
            return nil
        }
    }

    func deleteCredential(credential: Credential) -> Bool {
        guard let appDelegate = UIApplication.shared.delegate as? AppDelegate else { return false }
        let managedContext = appDelegate.persistentContainer.viewContext
        managedContext.delete(credential)
        do {
            try managedContext.save()
            return true
        } catch let error as NSError {
            print(error.debugDescription)
            return false
        }
    }

    func deleteAllCredentials() -> Bool {
        guard let appDelegate = UIApplication.shared.delegate as? AppDelegate else { return false }
        let managedContext = appDelegate.persistentContainer.viewContext
        let fetchRequest = Credential.fetchRequest()
        do {
            if let result = try managedContext.fetch(fetchRequest) as? [Credential] {
                for credential in result {
                    managedContext.delete(credential)
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
