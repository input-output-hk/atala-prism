//
//  ContactDAO.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 15/07/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import UIKit
import CoreData

class ContactDAO: BaseDAO {

    func getSortDescriptors() -> [NSSortDescriptor] {
        return [NSSortDescriptor(key: "name", ascending: true),
                NSSortDescriptor(key: "connectionId", ascending: true)]
    }

    func listContacts() -> [Contact]? {
        let fetchRequest = Contact.createFetchRequest()
        fetchRequest.sortDescriptors = getSortDescriptors()

        let result = try? getManagedContext()?.fetch(fetchRequest)
        return result
    }

    func listContactsForShare(connectionId: String) -> [Contact]? {
        let fetchRequest = Contact.createFetchRequest()
        fetchRequest.predicate = NSPredicate(format: "connectionId != %@", connectionId)
        fetchRequest.sortDescriptors = getSortDescriptors()

        let result = try? getManagedContext()?.fetch(fetchRequest)
        return result
    }

    @discardableResult
    func updateMessageId(connectionId: String, messageId: String) -> Contact? {
        guard let managedContext = getManagedContext() else { return nil }
        let fetchRequest = Contact.createFetchRequest()
        fetchRequest.predicate = NSPredicate(format: "connectionId == %@", connectionId)
        if let result = try? managedContext.fetch(fetchRequest), let contact = result.first {
            contact.lastMessageId = messageId
            do {
                try managedContext.save()
                return contact

            } catch let error as NSError {
                print(error.debugDescription)
                return nil
            }
        }
        return nil
    }

    @discardableResult
    func updateContact() -> Bool? {
        guard let managedContext = getManagedContext() else { return nil }
        do {
            try managedContext.save()
            return true

        } catch let error as NSError {
            print(error.debugDescription)
            return false
        }
    }

    @discardableResult
    func createContact(connectionInfo: Io_Iohk_Atala_Prism_Protos_ConnectionInfo, keyPath: String) -> Contact? {
        if connectionInfo.hasParticipantInfo {
            // Issuers
            if connectionInfo.participantInfo.issuer.name.count > 0 {
                return createContact(dateCreated: Date(timeIntervalSince1970: connectionInfo.created.timeIntervalSince1970),
                                     connectionId: connectionInfo.connectionID,
                                     did: connectionInfo.participantInfo.issuer.did,
                                     name: connectionInfo.participantInfo.issuer.name,
                                     token: connectionInfo.token,
                                     logo: connectionInfo.participantInfo.issuer.logo,
                                     keyPath: keyPath)
            }
            // Verifiers
            if connectionInfo.participantInfo.verifier.name.count > 0 {
                return createContact(dateCreated: Date(timeIntervalSince1970: connectionInfo.created.timeIntervalSince1970),
                                     connectionId: connectionInfo.connectionID,
                                     did: connectionInfo.participantInfo.verifier.did,
                                     name: connectionInfo.participantInfo.verifier.name,
                                     token: connectionInfo.token,
                                     logo: connectionInfo.participantInfo.verifier.logo,
                                     keyPath: keyPath)
            }
        }
        return nil
    }

    func createContact(dateCreated: Date, connectionId: String, did: String, name: String,
                       token: String, logo: Data?, keyPath: String) -> Contact? {
        guard let managedContext = getManagedContext() else { return nil }
        let contact = NSEntityDescription.insertNewObject(forEntityName: "Contact",
                                                          into: managedContext) as? Contact
        contact?.dateCreated = dateCreated
        contact?.connectionId = connectionId
        contact?.did = did
        contact?.name = name
        contact?.token = token
        contact?.logo = logo
        contact?.keyPath = keyPath

        do {
            try managedContext.save()
            return contact

        } catch let error as NSError {
            print(error.debugDescription)
            return nil
        }
    }

    func deleteContact(contact: Contact) -> Bool {
        guard let managedContext = getManagedContext() else { return false }
        managedContext.delete(contact)
        do {
            try managedContext.save()
            return true
        } catch let error as NSError {
            print(error.debugDescription)
            return false
        }
    }

    @discardableResult
    func deleteAllContacts() -> Bool {
        guard let managedContext = getManagedContext() else { return false }
        let fetchRequest = Contact.createFetchRequest()
        do {
            if let result = try? managedContext.fetch(fetchRequest) {
                for contact in result {
                    managedContext.delete(contact)
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
