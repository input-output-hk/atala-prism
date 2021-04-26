//
//  BaseVerificationCodeViewController.swift
//  prism-ios-wallet
//
//  Created by Roberto Daviduk on 04/03/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation
import UIKit

class BaseVerificationCodeViewController: BaseViewController, UITextFieldDelegate, SegueableScreen {
    
    @IBOutlet weak var messageLabel: UILabel!
    @IBOutlet weak var stackview: UIStackView!
    @IBOutlet weak var confirmButton: UIButton!

    var codeCount = 0
    let textfieldCount = 4
    
    var attributeType: String?
    var attributeValue: String?
    var attributeLogo: String?

    var navBar: NavBarCustomStyle = NavBarCustomStyle(hasNavBar: true)
    override func navBarCustomStyle() -> NavBarCustomStyle {
        return navBar
    }
    
    var presenterImpl = BaseVerificationCodePresenter()
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
        attributeValue = params?[1] as? String
        attributeLogo = params?[2] as? String
    }
    
    // MARK: Setup
    func setup() {
        
        switch attributeType {
        case "profile_attribute_email".localize():
            setupNavBarWith(title: "email_verification_title".localize())
            break
        case "profile_attribute_phone".localize():
            setupNavBarWith(title: "phone_verification_title".localize())
            break
        default:
            break
        }
        
        let font = messageLabel.font
        let boldFont = UIFont.boldSystemFont(ofSize: messageLabel.font.pointSize)
        
        let normalString = "phone_verification_code_body_message".localize() as NSString
        let boldString = "phone_verification_code_body_message_bold".localize() as NSString

        messageLabel.attributedText = "".addBoldText(fullString: normalString, boldPartsOfString: [boldString], font: font, boldFont: boldFont)
    }
    
    func setupNavBarWith(title: String) {
        navBar = NavBarCustomStyle(hasNavBar: true, isWhite: true, title: title, hasBackButton: true)
        NavBarCustom.config(view: self)
    }
    
    // MARK: Buttons

    func setupButtons() {
        
        confirmButton.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON, borderWidth: AppConfigs.BORDER_WIDTH_BUTTON,
                                      borderColor: UIColor.appWhite.cgColor)
    }
        
    @IBAction func confirm(_ sender: Any) {
        
        if(codeCount == textfieldCount){
            sendConfirmationCode()
        }
    }
    
    // MARK: Textfield Delegate
    
    func textField(_ textField: UITextField, shouldChangeCharactersIn range: NSRange, replacementString string: String) -> Bool {
        
        if (textField.text!.count < 1  && string.count > 0){

            let nextTag = textField.tag + 1

            if(nextTag <= textfieldCount){
                
                var nextResponder = textField.superview?.viewWithTag(nextTag)

                if (nextResponder == nil){

                    nextResponder = textField.superview?.viewWithTag(1)
                }
                
                textField.text = string
                nextResponder?.becomeFirstResponder()
                
                codeCount += 1
                
                return false
            }
            
        }else if(textField.text!.count >= 1 && string.count >= 1){
            
            let nextTag = textField.tag + 1

            if(nextTag <= textfieldCount){
                
                var nextResponder = textField.superview?.viewWithTag(nextTag)

                if (nextResponder == nil){

                    nextResponder = textField.superview?.viewWithTag(1)
                }
                
                if let aux = nextResponder as? UITextField {
                    
                    aux.text = string
                    nextResponder?.becomeFirstResponder()
                    
                    codeCount += 1
                    showConfirmationTick()
                }
                                                
                return false
            }
            
        }else if (textField.text!.count >= 1  && string.count == 0){
            
            let previousTag = textField.tag - 1

            var previousResponder = textField.superview?.viewWithTag(previousTag)

            if (previousResponder == nil){
                
                previousResponder = textField.superview?.viewWithTag(1)
            }
            
            textField.text = ""
            previousResponder?.becomeFirstResponder()
            
            codeCount -= 1
            showConfirmationTick()
            
            return false
            
        }
        
        if (textField.text!.count >= 1) {
            return false
        }
        
        codeCount += 1
        showConfirmationTick()
        
        return true
    }
    
    func showConfirmationTick() {
        stackview.isHidden = !(codeCount >= textfieldCount)
        
        if(!stackview.isHidden){
            navBar.hasBackButton = false
            NavBarCustom.config(view: self)
        }
    }
    
    func sendConfirmationCode() {
        if(codeCount == textfieldCount){
            let json = ["type": attributeType, "value": attributeValue, "logo": attributeLogo]
            // Save the results
            if let attribute = Attribute(JSON: json as [String : Any]) {
                sharedMemory.loggedUser?.attributes.append(attribute)
                sharedMemory.loggedUser = sharedMemory.loggedUser
      
                self.navigationController?.dismiss(animated: true, completion: nil)
                
                NotificationCenter.default.post(name: .reloadAttributes, object: nil)
            }
        }
    }
}
