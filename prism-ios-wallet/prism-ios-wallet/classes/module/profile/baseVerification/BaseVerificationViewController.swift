//
//  BaseVerificationViewController.swift
//  prism-ios-wallet
//
//  Created by Roberto Daviduk on 04/03/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation
import UIKit

class BaseVerificationViewController: BaseViewController, UINavigationControllerDelegate, PickerViewUtilsDelegate, SegueableScreen, UITextFieldDelegate {
    
    @IBOutlet weak var titleLabel: UILabel!
    @IBOutlet weak var subtitleLabel: UILabel!
    @IBOutlet weak var pickerView: UIView!
    @IBOutlet weak var pickerLabel: UILabel!
    @IBOutlet weak var inputTextfield: UITextField!
    @IBOutlet weak var verifyButton: UIButton!

    var picker: UIPickerView?
    var pickerViewUtils: PickerViewUtils?
    
    var countries: [String] = []
    
    var attributeType: String?
    var attributeLogo: String?

    var navBar: NavBarCustomStyle = NavBarCustomStyle(hasNavBar: true)
    override func navBarCustomStyle() -> NavBarCustomStyle {
        return navBar
    }
    
    var presenterImpl = BaseVerificationPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        setup()
        setupButtons()
        ViewControllerUtils.addTapToDismissKeyboard(view: self)
        ViewControllerUtils.addShiftKeyboardListeners(view: self)
    }

    override func onBackPressed() -> Bool {
        if !presenterImpl.tappedBackButton() {
            return super.onBackPressed()
        }
        return false
    }
    
    // MARK: Segue Value
    
    func configScreenFromSegue(params: [Any?]?) {
        attributeType = params?[0] as? String
        attributeLogo = params?[1] as? String
    }
    
    // MARK: Buttons

    func setupButtons() {
        verifyButton.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON, borderWidth: AppConfigs.BORDER_WIDTH_BUTTON,
                                      borderColor: UIColor.appWhite.cgColor)
    }
    
    @IBAction func verify(_ sender: Any) {
        
        if inputTextfield.text?.count > 0 {
            
            var params: [Any?] = []
            params.append(attributeType)
            params.append(inputTextfield.text)
            params.append(attributeLogo)

            ViewControllerUtils.changeScreenSegued(caller: self, segue: "BaseVerificationCodeSegue", params: params)
        }
    }
    
    // MARK: Textfield

    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        textField.resignFirstResponder()
        return true
    }
    
    // MARK: Country Picker Setup
    
    func setup() {
        
        switch attributeType {
        case "profile_attribute_email".localize():
            setupNavBarWith(title: "email_verification_title".localize())
            configForEmailValidation()
            break
        case "profile_attribute_phone".localize():
            setupNavBarWith(title: "phone_verification_title".localize())
            configForPhoneValidation()
            break
        default:
            break
        }
    }
    
    func configForEmailValidation() {

        titleLabel.text = "email_verification_body_title".localize()
        subtitleLabel.text = "email_verification_body_message".localize()
        inputTextfield.keyboardType = .emailAddress
        inputTextfield.placeholder = "email_verification_input".localize()
        verifyButton.setTitle("email_verification_button".localize(), for: .normal)
    }
    
    func configForPhoneValidation() {

        pickerView.addRoundCorners(radius: 5, borderWidth: 1.5, borderColor: UIColor.appTextfieldBorderColor.cgColor)
        pickerView.isHidden = false
        pickerView.addOnClickListener(action: openPicker, numberOfTaps: 1)
        
        countries.append(contentsOf: presenterImpl.setupPickerData())
        
        pickerLabel.text = countries[0]
                
        picker = UIPickerView(frame: CGRect(x: 5, y: 20, width: self.view.frame.size.width - 20, height: 140))
        
        pickerViewUtils = PickerViewUtils.init(view: self, picker: picker!)
        pickerViewUtils?.reloadPickerView()
    }
    
    func setupNavBarWith(title: String) {
        navBar = NavBarCustomStyle(hasNavBar: true, isWhite: true, title: title, hasBackButton: true)
        NavBarCustom.config(view: self)
    }
    
    lazy var openPicker = SelectorAction(action: { [weak self] in

        let alert = UIAlertController(title: "phone_verification_country_picker_title".localize(), message: "\n\n\n\n\n\n", preferredStyle: UIAlertController.Style.actionSheet)
                 
        alert.view.addSubview((self?.picker)!)

        let cancelAction = UIAlertAction(title: "close".localize(), style: .cancel, handler: nil)
        alert.addAction(cancelAction)
                
        self?.present(alert, animated: true, completion: nil)
    })
    
    // MARK: Country Picker Delegate

    func valueComponents() -> [String] {
        return countries
    }
    
    func didSelectRowInComponent(_ value: String) {
        pickerLabel.text = value
    }
}
