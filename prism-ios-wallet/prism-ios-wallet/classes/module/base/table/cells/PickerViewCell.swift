//
//  PickerViewCell.swift
//  prism-ios-wallet
//
//  Created by Roberto Daviduk on 22/02/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

protocol PickerViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: PickerViewCell)
    func openPicker(for cell: PickerViewCell)
}

class PickerViewCell: BaseTableViewCell, TextFieldTitledViewDelegate {

    @IBOutlet weak var textField: TextFieldTitledView!
    @IBOutlet weak var viewBg: UIView!

    override class func default_NibName() -> String {
        return "PickerViewCell"
    }

    var delegateImpl: PickerViewCellPresenterDelegate? {
        return delegate as? PickerViewCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
        textField.config(delegate: self, bgColor: UIColor.appWhite)
    }

    // MARK: Config

    func config(title: String, text: String, bgColor: UIColor, isEnable: Bool, hasBorder: Bool = true) {

        textField.config(title: title)
        textField.textField.text = text
        viewBg.backgroundColor = bgColor
        textField.textField.isEnabled = false
        textField.textField.isUserInteractionEnabled = isEnable
        textField.textField.addDropDownIcon()
        textField.changeBorderColorIf(isEditing: hasBorder)
        setupTapGesture()
    }
    
    func textFieldDidChange(for view: TextFieldTitledView, textField: UITextField, text: String?) {
        
    }
    
    private func setupTapGesture() {
        
        let tap = UITapGestureRecognizer(target: self, action:#selector(self.selectCountry(_:)))
        
        textField.addGestureRecognizer(tap)
    }
    
    @objc func selectCountry(_ sender: UITapGestureRecognizer) {
        
        if(textField.textField.isUserInteractionEnabled){
            delegateImpl?.openPicker(for: self)
        }
    }
}
