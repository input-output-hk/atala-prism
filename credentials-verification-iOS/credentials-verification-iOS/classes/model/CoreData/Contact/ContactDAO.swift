//
//  ContactDAO.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 15/07/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import UIKit

class ContactDAO: NSObject {

    func listContacts() -> [Contact]? {
        guard let appDelegate = UIApplication.shared.delegate as? AppDelegate else { return nil }
        let managedContext = appDelegate.persistentContainer.viewContext
        let fetchRequest = Contact.createFetchRequest()
        do {
            let result = try managedContext.fetch(fetchRequest)
            return result
        } catch let error as NSError {
            print(error.debugDescription)
            return nil
        }
    }

    func createContact(dateCreated: Date?, connectionId: String?, did: String?, name: String?,
                       token: String?) -> Contact? {
        let contact = Contact()
        contact.dateCreated = dateCreated
        contact.connectionId = connectionId
        contact.did = did
        contact.name = name
        contact.token = token

        guard let appDelegate = UIApplication.shared.delegate as? AppDelegate else { return nil }
        let managedContext = appDelegate.persistentContainer.viewContext

        do {
            try managedContext.save()
            return contact

        } catch let error as NSError {
            print(error.debugDescription)
            return nil
        }
    }

    func deleteContact(contact: Contact) -> Bool {
        guard let appDelegate = UIApplication.shared.delegate as? AppDelegate else { return false }
        let managedContext = appDelegate.persistentContainer.viewContext
        managedContext.delete(contact)
        do {
            try managedContext.save()
            return true
        } catch let error as NSError {
            print(error.debugDescription)
            return false
        }
    }

    func deleteAllContacts() -> Bool {
        guard let appDelegate = UIApplication.shared.delegate as? AppDelegate else { return false }
        let managedContext = appDelegate.persistentContainer.viewContext
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
