//
//  BaseVerificationPresenter.swift
//  prism-ios-wallet
//
//  Created by Roberto Daviduk on 04/03/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation

class BaseVerificationPresenter: BasePresenter {
    
    var viewImpl: BaseVerificationViewController? {
        return view as? BaseVerificationViewController
    }
    
    // MARK: Buttons

    @discardableResult
    func tappedBackButton() -> Bool {

        return false
    }
    
    // MARK: Country Picker Data
    
    func setupPickerData() -> [String] {
            
        var countries: [String] = []

        for code in NSLocale.isoCountryCodes  {
            
            let id = NSLocale.localeIdentifier(fromComponents: [NSLocale.Key.countryCode.rawValue: code])
            let name = NSLocale(localeIdentifier: "en_US").displayName(forKey: NSLocale.Key.identifier, value: id) ?? "Country not found for code: \(code)"
            
            let phoneCode = PhoneHelper.getCountryPhoneCodeFrom(regionCode: code)
            
            let val: String = String.init(format: "%@ %@ (%@)", flag(country: code), name, phoneCode)
            
            countries.append(val)
        }
        
        return countries
    }
    
    func flag(country:String) -> String {
        
        let base = 127397
        var usv = String.UnicodeScalarView()
        
        for index in country.utf16 {
            usv.append(UnicodeScalar(base + Int(index))!)
        }
        return String(usv)
    }
}
