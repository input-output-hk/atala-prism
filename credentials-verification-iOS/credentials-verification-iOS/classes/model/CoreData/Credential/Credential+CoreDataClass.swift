//
//  Credential+CoreDataClass.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 15/07/2020.
//  Copyright © 2020 iohk. All rights reserved.
//
//

import Foundation
import CoreData

@objc(Credential)
public class Credential: NSManagedObject {

    var credentialType: CredentialType? {
        CredentialType(rawValue: self.type)
    }

    var credentialName: String {
        switch credentialType {
        case .univerityDegree:
            return "credentials_university_degree".localize()
        case .governmentIssuedId:
            return "credentials_government_id".localize()
        case .proofOfEmployment:
            return "credentials_proof_employment".localize()
        case .certificatOfInsurance:
            return "credentials_certificate_insurance".localize()
        default:
            return ""
        }
    }

    var logoPlaceholder: String {
        switch credentialType {
        case .univerityDegree:
            return "icon_university"
        case .governmentIssuedId:
            return "icon_id"
        case .proofOfEmployment:
            return "icon_proof_employment"
        case .certificatOfInsurance:
            return "icon_insurance"
        default:
            return "icon_id"
        }
    }

}
