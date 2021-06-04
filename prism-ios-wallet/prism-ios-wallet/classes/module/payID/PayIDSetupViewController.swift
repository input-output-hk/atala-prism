//
//  PayIDSetupViewController.swift
//  prism-ios-wallet
//
//  Created by Roberto Daviduk on 19/03/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation
import UIKit

class PayIDSetupViewController: BaseViewController, PayIDScannerDelegate, UITextFieldDelegate {

    @IBOutlet weak var nameTextField: TextFieldTitledView!
    @IBOutlet weak var scanTextField: TextFieldTitledView!
    @IBOutlet weak var nextButton: UIButton!
    @IBOutlet weak var nameInfoLbl: UILabel!
    @IBOutlet weak var nameValidImg: UIImageView!
    @IBOutlet weak var addressValidImg: UIImageView!

    var canContinue: Bool = false

    override func navBarCustomStyle() -> NavBarCustomStyle {
        return NavBarCustomStyle(hasNavBar: true, isWhite: false, title: "pay_id_setup_title".localize(),
                                 hasBackButton: true)
    }

    var presenterImpl = PayIDSetupPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    lazy var openCamera = SelectorAction(action: { [weak self] in

        var params: [Any?] = []
        params.append(self)

        ViewControllerUtils.changeScreenSegued(caller: self, segue: "SeguePayIDScannerViewController", params: params)
    })

    override func viewDidLoad() {
        super.viewDidLoad()

        ViewControllerUtils.addTapToDismissKeyboard(view: self)

        nameTextField.config(title: "pay_id_setup_name_field".localize(), trailing: 30)
        nameTextField.textField.delegate = self
        nameTextField.textField.addRightViewWith(text: "pay_id_setup_name_field_right".localize())

        scanTextField.config(title: "pay_id_setup_scan_field".localize(), trailing: 30)
        scanTextField.textField.delegate = self
        scanTextField.textField.addRightViewWith(image: UIImage.init(named: "ico_qr")!)
        scanTextField.textField.rightView?.addOnClickListener(action: openCamera)

        nextButton.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON)
        presenterImpl.createAccount()
    }

    func toogleNameAvailable(isAvailable: Bool?) {
        guard let isAvailable = isAvailable else {
            nameInfoLbl.text = "pay_id_setup_name_field_tip".localize()
            nameValidImg.isHidden = true
            nameInfoLbl.textColor = .textGrey
            return
        }
        nameValidImg.isHidden = false
        if isAvailable {
            nameInfoLbl.text = "pay_id_setup_name_field_available".localize()
            nameInfoLbl.textColor = .appGreen15per
            nameValidImg.image = #imageLiteral(resourceName: "ico_ok")
        } else {
            nameInfoLbl.text = "pay_id_setup_name_field_unavailable".localize()
            nameInfoLbl.textColor = .appRed
            nameValidImg.image = #imageLiteral(resourceName: "ico_err")
        }
    }
    
    func toogleaddressValid(isValid: Bool?) {
        guard let isValid = isValid else {
            addressValidImg.isHidden = true
            return
        }
        addressValidImg.isHidden = false
        if isValid {
            addressValidImg.image = #imageLiteral(resourceName: "ico_ok")
        } else {
            addressValidImg.image = #imageLiteral(resourceName: "ico_err")
        }
    }

    override func onBackPressed() -> Bool {
        if !presenterImpl.tappedBackButton() {
            return super.onBackPressed()
        }
        return false
    }

    func textFieldDidEndEditing(_ textField: UITextField) {

        if nameTextField.textField.text?.count > 0 && scanTextField.textField.text?.count > 0 {
            nextButton.backgroundColor = UIColor.appRed
            canContinue = true
        } else {
            nextButton.backgroundColor = UIColor.appGreyBlue
            canContinue = false
        }
        if textField == nameTextField.textField {
            presenterImpl.validateName(name: textField.text ?? "")
        } else if textField == scanTextField.textField {
            presenterImpl.validateaddress(address: textField.text ?? "")
        }
    }

    func textFieldShouldReturn(_ textField: UITextField) -> Bool {

        textField.resignFirstResponder()
        return true
    }

    func textField(_ textField: UITextField, shouldChangeCharactersIn range: NSRange,
                   replacementString string: String) -> Bool {
        if textField == nameTextField.textField {
            let currentText = textField.text ?? ""
            // attempt to read the range they are trying to change, or exit if we can't
            guard let stringRange = Range(range, in: currentText) else { return false }
            let updatedText = currentText.replacingCharacters(in: stringRange, with: string)
            return updatedText.count <= 50
        }
        return true
    }

    func payIdRegistered() {
        let popUp = PopupAlertViewController.makeThisView()

        popUp.setupWith(logoName: "logo_popup_success", title: "pay_id_success_title".localize(),
                        message: "pay_id_create_success_message".localize())

        popUp.actionConfirmButton {

            ViewControllerUtils.changeScreenSegued(caller: self, segue: "SeguePayIDInfoViewController",
                                                   params: nil)
        }

        customPresentViewController(popUp.presentr, viewController: popUp, animated: true)
    }

    @IBAction func next(_ sender: Any) {

        if canContinue {
            presenterImpl.payIdName = nameTextField.textField.text
            presenterImpl.payIdAddress = scanTextField.textField.text
            presenterImpl.createPayId()
        }
    }

    // MARK: Scanner Delegate

    func send(value: String) {
        scanTextField.textField.text = value
    }
}

extension PayIDSetupViewController: SegueableScreen {

    func configScreenFromSegue(params: [Any?]?) {
        if let credentials = params as? [Credential] {
            presenterImpl.credentials = credentials
        }
    }
}
