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
    
    func getContact(connectionId: String) -> Contact? {
        let fetchRequest = Contact.createFetchRequest()
        fetchRequest.predicate = NSPredicate(format: "connectionId == %@", connectionId)
        fetchRequest.sortDescriptors = getSortDescriptors()

        let result = try? getManagedContext()?.fetch(fetchRequest)
        if result?.count > 0 {
            return result?[0]
        }
        return nil
    }
    
    func getKycContact() -> Contact? {
        let fetchRequest = Contact.createFetchRequest()
        fetchRequest.predicate = NSPredicate(format: "isKyc == true")
        fetchRequest.sortDescriptors = getSortDescriptors()

        let result = try? getManagedContext()?.fetch(fetchRequest)
        return result?.first
    }
    
    func getPayIdContact() -> Contact? {
        let fetchRequest = Contact.createFetchRequest()
        fetchRequest.predicate = NSPredicate(format: "isPayId == true")
        fetchRequest.sortDescriptors = getSortDescriptors()

        let result = try? getManagedContext()?.fetch(fetchRequest)
        return result?.first
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
    func createContact(connectionInfo: Io_Iohk_Atala_Prism_Protos_ConnectionInfo, keyPath: String,
                       isKyc: Bool = false, isPayId: Bool = false) -> Contact? {

        guard let managedContext = getManagedContext() else { return nil }
        let contact = NSEntityDescription.insertNewObject(forEntityName: "Contact",
                                                          into: managedContext) as? Contact
        contact?.dateCreated = Date(timeIntervalSince1970: connectionInfo.created.timeIntervalSince1970)
        contact?.connectionId = connectionInfo.connectionID
        contact?.did = connectionInfo.participantDid
        contact?.name = connectionInfo.participantName
        contact?.token = connectionInfo.token
        contact?.logo = connectionInfo.participantLogo
        contact?.keyPath = keyPath
        contact?.isKyc = isKyc
        contact?.isPayId = isPayId

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
