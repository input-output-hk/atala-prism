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
        case .demoUniversityDegree, .univerityDegree:
            return "credentials_university_degree".localize()
        case .demoGovernmentIssuedId, .governmentIssuedId:
            return "credentials_government_id".localize()
        case .demoProofOfEmployment, .proofOfEmployment:
            return "credentials_proof_employment".localize()
        case .demoCertificateOfInsurance, .certificatOfInsurance:
            return "credentials_certificate_insurance".localize()
        case .georgiaNationalID:
            return "credentials_georgia_national_id".localize()
        case .georgiaEducationalDegree:
            return "credentials_georgia_educational_degree".localize()
        case .georgiaEducationalDegreeTranscript:
            return "credentials_georgia_educational_degree_transcript".localize()
        default:
            return ""
        }
    }

    var logoPlaceholder: String {
        switch credentialType {
        case .demoUniversityDegree, .univerityDegree:
            return "icon_university"
        case .demoGovernmentIssuedId, .governmentIssuedId:
            return "icon_id"
        case .demoProofOfEmployment, .proofOfEmployment:
            return "icon_proof_employment"
        case .demoCertificateOfInsurance, .certificatOfInsurance:
            return "icon_insurance"
        case .georgiaNationalID:
            return "icon_national_id"
        case .georgiaEducationalDegree:
            return "icon_educational_degree"
        case .georgiaEducationalDegreeTranscript:
            return "icon_educational_degree_transcript"
        default:
            return "icon_id"
        }
    }

}
