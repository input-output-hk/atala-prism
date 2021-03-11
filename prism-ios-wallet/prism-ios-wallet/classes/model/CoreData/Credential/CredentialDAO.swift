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

    func countNewCredentials() -> Int {
        let fetchRequest = Credential.createFetchRequest()
        fetchRequest.predicate = NSPredicate(format: "viewed == false")
        fetchRequest.sortDescriptors = getSortDescriptors()
        let result = try? getManagedContext()?.count(for: fetchRequest)
        return result ?? 0
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

    func createCredential(sentCredential: Io_Iohk_Atala_Prism_Protos_Credential,
                          viewed: Bool, messageId: String, connectionId: String) -> (Credential, Bool)? {

        if let credential = Mapper<Degree>().map(JSONString: sentCredential.credentialDocument) {
            var jsonStr = ""
            if let jsonData = sentCredential.credentialDocument.data(using: String.Encoding.utf8) {
                var jsonObject = try? JSONSerialization.jsonObject(with: jsonData,
                                                                   options: .allowFragments) as? [String: Any]
                jsonObject?.removeValue(forKey: "view")
                let prettyJsonData = try? JSONSerialization.data(withJSONObject: jsonObject!, options: .prettyPrinted)
                let prettyPrintedJson = String(data: prettyJsonData!, encoding: String.Encoding.utf8)
                jsonStr = prettyPrintedJson ?? ""
            }
            return try? createCredential(type: sentCredential.typeID, credentialId: messageId,
                                         issuerId: connectionId, issuerName: (credential.issuer?.name)!,
                                         htmlView: (credential.view?.html)!, dateReceived: Date(),
                                         viewed: viewed, encoded: sentCredential.serializedData(), json: jsonStr,
                                         plainCredential: nil)
        }
        return nil
    }

    func createCredential(message: Io_Iohk_Atala_Prism_Protos_AtalaMessage,
                          viewed: Bool, messageId: String, connectionId: String,
                          issuerName: String) -> (Credential, Bool)? {
        let plainCredential = message.plainCredential
        let base64 =  String(plainCredential.encodedCredential.split(separator: ".")[0])
        if let jsonData = Data(base64urlEncoded: base64),
           var jsonObject = try? JSONSerialization.jsonObject(with: jsonData,
                                                              options: .allowFragments) as? [String: Any],
           let credentialSubjectStr = jsonObject["credentialSubject"] as? String,
           let credentialSubjectData = credentialSubjectStr.data(using: .utf8) {

            var claims = try? JSONSerialization.jsonObject(with: credentialSubjectData,
                                                           options: .allowFragments) as? [String: Any]
            let htmlEncoded = claims?.removeValue(forKey: "html") as? String ?? ""
            let htmlView = String(htmlEncodedString: (htmlEncoded)) ?? ""
            jsonObject["credentialSubject"] = claims
            let prettyJsonData = (try? JSONSerialization.data(withJSONObject: jsonObject,
                                                              options: .prettyPrinted)) ?? Data()
            let prettyPrintedJson = String(data: prettyJsonData, encoding: String.Encoding.utf8) ?? ""
            let credentialType = claims?["credentialType"] as? String ?? ""

            return try? createCredential(type: credentialType, credentialId: messageId,
                                         issuerId: connectionId, issuerName: issuerName,
                                         htmlView: htmlView, dateReceived: Date(), viewed: viewed,
                                         encoded: message.serializedData(), json: prettyPrintedJson,
                                         plainCredential: plainCredential.encodedCredential)
        }
        return nil
    }

    func createCredential(type: String, credentialId: String, issuerId: String, issuerName: String,
                          htmlView: String, dateReceived: Date, viewed: Bool, encoded: Data,
                          json: String, plainCredential: String?) -> (Credential, Bool)? {
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
            credential.json = json
            credential.plainCredential = plainCredential

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
