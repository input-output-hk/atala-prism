//
//  PayIdDAO.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 26/04/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation
import CoreData

class PayIdDAO: BaseDAO {
    
    func getSortDescriptors() -> [NSSortDescriptor] {
        return [NSSortDescriptor(key: "name", ascending: false)]
    }
    
    
    func listPayId() -> [PayId]? {
        let fetchRequest = PayId.createFetchRequest()
        fetchRequest.sortDescriptors = getSortDescriptors()
        let result = try? getManagedContext()?.fetch(fetchRequest)
        return result
    }
    
    func countPayId() -> Int {
        let fetchRequest = PayId.createFetchRequest()
        fetchRequest.sortDescriptors = getSortDescriptors()
        let result = try? getManagedContext()?.count(for: fetchRequest)
        return result ?? 0
    }
    
    @discardableResult
    func createPayId(payIdId: String, name: String?, connectionId: String) -> PayId? {
        guard let managedContext = getManagedContext() else { return nil }
        let payId = NSEntityDescription.insertNewObject(forEntityName: "PayId", into: managedContext) as? PayId

        payId?.payIdId = payIdId
        payId?.name = name
        payId?.connectionId = connectionId
        
        do {
            try managedContext.save()
            return payId

        } catch let error as NSError {
            print(error.debugDescription)
            return nil
        }
    }
    
    func addAddress(payId: PayId, name: String) {
        guard let managedContext = getManagedContext() else { return }
        let fetchRequest = Address.createFetchRequest()
        fetchRequest.predicate = NSPredicate(format: "name == %@", name)

        if let result = try? managedContext.fetch(fetchRequest), !result.isEmpty {
            return
        }
        let address = NSEntityDescription.insertNewObject(forEntityName: "Address",
                                                             into: managedContext) as? Address
        
        address?.name = name
        address?.payId = payId
        
        do {
            try managedContext.save()

        } catch let error as NSError {
            print(error.debugDescription)
        }
    }
    
    func addPublicKey(payId: PayId, publicKey: String?) {
        guard let managedContext = getManagedContext() else { return }
        let walletPublicKey = NSEntityDescription.insertNewObject(forEntityName: "WalletPublicKey",
                                                                  into: managedContext) as? WalletPublicKey

        walletPublicKey?.key = publicKey
        walletPublicKey?.payId = payId

        do {
            try managedContext.save()

        } catch let error as NSError {
            print(error.debugDescription)
        }
    }
    
    func deletePayId(payId: PayId) -> Bool {
        guard let managedContext = getManagedContext() else { return false }
        managedContext.delete(payId)
        do {
            try managedContext.save()
            return true
        } catch let error as NSError {
            print(error.debugDescription)
            return false
        }
    }
    
    @discardableResult
    func deleteAllPayId() -> Bool {
        guard let managedContext = getManagedContext() else { return false }
        let fetchRequest = PayId.fetchRequest()
        do {
            if let result = try managedContext.fetch(fetchRequest) as? [PayId] {
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
