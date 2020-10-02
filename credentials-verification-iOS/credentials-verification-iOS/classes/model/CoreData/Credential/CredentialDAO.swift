//
//  CreedentialDAO.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 15/07/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import UIKit
import CoreData
import ObjectMapper

class CredentialDAO: BaseDAO {

    func getSortDescriptors() -> [NSSortDescriptor] {
        return [NSSortDescriptor(key: "issuerName", ascending: true),
                NSSortDescriptor(key: "credentialId", ascending: true)]
    }

    func getDidSuffix(did: String) -> String {
        String(did.split(separator: ":").last!)
    }

    func listCredentials() -> [Credential]? {
        let fetchRequest = Credential.createFetchRequest()
        fetchRequest.sortDescriptors = getSortDescriptors()
        let result = try? getManagedContext()?.fetch(fetchRequest)
        return result
    }

    func listNewCredentials() -> [Credential]? {
        let fetchRequest = Credential.createFetchRequest()
        fetchRequest.predicate = NSPredicate(format: "viewed == false")
        fetchRequest.sortDescriptors = getSortDescriptors()
        let result = try? getManagedContext()?.fetch(fetchRequest)
        return result
    }

    func listCredentialsForContact(did: String) -> [Credential]? {
        let fetchRequest = Credential.createFetchRequest()
        fetchRequest.sortDescriptors = getSortDescriptors()
        let didSufix = getDidSuffix(did: did)
        fetchRequest.predicate = NSPredicate(format: "issuerId ENDSWITH %@", didSufix)
        let result = try? getManagedContext()?.fetch(fetchRequest)
        return result
    }

    @discardableResult
    func setViewed(credentialId: String) -> Credential? {
        guard let managedContext = getManagedContext() else { return nil }
        let fetchRequest = Credential.createFetchRequest()
        fetchRequest.predicate = NSPredicate(format: "credentialId == %@", credentialId)
        if let result = try? managedContext.fetch(fetchRequest), let credential = result.first {
            credential.viewed = true
            do {
                try managedContext.save()
                return credential

            } catch let error as NSError {
                print(error.debugDescription)
                return nil
            }
        }
        return nil
    }

    func createCredential(sentCredential: Io_Iohk_Prism_Protos_Credential,
                          viewed: Bool, messageId: String, connectionId: String) -> (Credential, Bool)? {

        if let credential = Mapper<Degree>().map(JSONString: sentCredential.credentialDocument) {

            return try? createCredential(type: sentCredential.typeID, credentialId: messageId,
                                         issuerId: connectionId, issuerName: (credential.issuer?.name)!,
                                         htmlView: (credential.view?.html)!, dateReceived: Date(),
                                         viewed: viewed, encoded: sentCredential.serializedData())
        }
        return nil
    }

    func createCredential(type: String, credentialId: String, issuerId: String, issuerName: String,
                          htmlView: String, dateReceived: Date, viewed: Bool, encoded: Data) -> (Credential, Bool)? {
        guard let managedContext = getManagedContext() else { return nil }
        let fetchRequest = Credential.createFetchRequest()
        fetchRequest.predicate = NSPredicate(format: "credentialId == %@", credentialId)

        if let result = try? managedContext.fetch(fetchRequest), let credential = result.first {
            return (credential, false)
        }

        if let credential = NSEntityDescription.insertNewObject(forEntityName: "Credential",
                                                                into: managedContext) as? Credential {
            credential.type = type
            credential.credentialId = credentialId
            credential.issuerId = issuerId
            credential.issuerName = issuerName
            credential.htmlView = htmlView
            credential.dateReceived = dateReceived
            credential.viewed = viewed
            credential.encoded = encoded
            
            do {
                try managedContext.save()
                return (credential, true)
            } catch let error as NSError {
                print(error.debugDescription)
                return nil
            }
        }
        return nil
    }

    func deleteCredential(credential: Credential) -> Bool {
        guard let managedContext = getManagedContext() else { return false }
        managedContext.delete(credential)
        do {
            try managedContext.save()
            return true
        } catch let error as NSError {
            print(error.debugDescription)
            return false
        }
    }

    func deleteCredentials(credentials: [Credential]) -> Bool {
        guard let managedContext = getManagedContext() else { return false }
        for credential in credentials {
            managedContext.delete(credential)
        }
        do {
            try managedContext.save()
            return true
        } catch let error as NSError {
            print(error.debugDescription)
            return false
        }
    }

    @discardableResult
    func deleteAllCredentials() -> Bool {
        guard let managedContext = getManagedContext() else { return false }
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
