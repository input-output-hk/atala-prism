//
//  PickerViewUtils.swift
//  prism-ios-wallet
//
//  Created by Roberto Daviduk on 05/03/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation
import UIKit

protocol PickerViewUtilsDelegate: class {

    func valueComponents() -> [String]
    func didSelectRowInComponent(_ value: String)
}

class PickerViewUtils: NSObject, UIPickerViewDelegate, UIPickerViewDataSource {

    weak var viewDelegate: PickerViewUtilsDelegate!
    weak var picker: UIPickerView!

    var values: [String] = []

    init(view: PickerViewUtilsDelegate, picker: UIPickerView) {
        super.init()

        self.viewDelegate = view
        self.picker = picker

        self.picker.delegate = self
        self.picker.dataSource = self
        
        values = self.viewDelegate.valueComponents()
    }

    func reloadPickerView() {

        self.picker.reloadAllComponents()
    }
    
    // MARK: Picker View Delegate - Datasource
    
    func numberOfComponents(in pickerView: UIPickerView) -> Int {
            
        return 1
    }

    func pickerView(_ pickerView: UIPickerView, numberOfRowsInComponent component: Int) -> Int {
        
        return values.count
    }

    func pickerView(_ pickerView: UIPickerView, titleForRow row: Int, forComponent component: Int) -> String? {
        
        return values[row]
    }

    func pickerView(_ pickerView: UIPickerView, didSelectRow row: Int, inComponent component: Int) {
        
        viewDelegate.didSelectRowInComponent(values[row])
    }
}
