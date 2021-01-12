//
//  SecurityViewController.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 18/03/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import UIKit

class SecurityViewController: ListingBaseViewController, UIScrollViewDelegate {

    @IBOutlet weak var setupView: UIView!
    @IBOutlet weak var setupTitleLbl: UILabel!
    @IBOutlet weak var setupStepLbl: UILabel!
    @IBOutlet weak var setupStepOneView: UIView!
    @IBOutlet weak var setupStepTwoView: UIView!
    @IBOutlet weak var setupScroll: UIScrollView!
    @IBOutlet weak var setupSetepOneConfirmBttn: UIButton!
    @IBOutlet weak var setupStepOneDescriptionLbl: UILabel!
    @IBOutlet weak var setupStepTwoPageView: UIView!
    @IBOutlet weak var setupPinOneDigOneTf: UITextField!
    @IBOutlet weak var setupPinOneDigTwoTf: UITextField!
    @IBOutlet weak var setupPinOneDigThreeTf: UITextField!
    @IBOutlet weak var setupPinOneDigFourTf: UITextField!
    @IBOutlet weak var setupPinOneBttn: UIButton!
    @IBOutlet weak var setupPinTwoDigOneTf: UITextField!
    @IBOutlet weak var setupPinTwoDigTwoTf: UITextField!
    @IBOutlet weak var setupPinTwoDigThreeTf: UITextField!
    @IBOutlet weak var setupPinTwoDigFourTf: UITextField!
    @IBOutlet weak var setupPinTwoBttn: UIButton!
    @IBOutlet weak var setupPassMatchImg: UIImageView!
    @IBOutlet weak var setupPassMatchLbl: UILabel!
    @IBOutlet weak var setupSetepTwoConfirmBttn: UIButton!

    @IBOutlet weak var changeDescriptionLbl: UILabel!
    @IBOutlet weak var changePinView: UIView!
    @IBOutlet weak var changePinScroll: UIScrollView!
    @IBOutlet weak var changePinOldDigOneTf: UITextField!
    @IBOutlet weak var changePinOldDigTwoTf: UITextField!
    @IBOutlet weak var changePinOldDigThreeTf: UITextField!
    @IBOutlet weak var changePinOldDigFourTf: UITextField!
    @IBOutlet weak var changePinOldBttn: UIButton!
    @IBOutlet weak var changePinOneDigOneTf: UITextField!
    @IBOutlet weak var changePinOneDigTwoTf: UITextField!
    @IBOutlet weak var changePinOneDigThreeTf: UITextField!
    @IBOutlet weak var changePinOneDigFourTf: UITextField!
    @IBOutlet weak var changePinOneBttn: UIButton!
    @IBOutlet weak var changePinTwoDigOneTf: UITextField!
    @IBOutlet weak var changePinTwoDigTwoTf: UITextField!
    @IBOutlet weak var changePinTwoDigThreeTf: UITextField!
    @IBOutlet weak var changePinTwoDigFourTf: UITextField!
    @IBOutlet weak var changePinTwoBttn: UIButton!
    @IBOutlet weak var changePassMatchImg: UIImageView!
    @IBOutlet weak var changePassMatchLbl: UILabel!
    @IBOutlet weak var changeConfirmBttn: UIButton!

    var presenterImpl = SecurityPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    var isSetupStepTwo = false

    override func navBarCustomStyle() -> NavBarCustomStyle {
        return NavBarCustomStyle(hasNavBar: true, title: nil, hasBackButton: true)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        setupButtons()
        setupViews()

        ViewControllerUtils.addTapToDismissKeyboard(view: self)
        ViewControllerUtils.addShiftKeyboardListeners(view: self)
    }

    @discardableResult
    override func onBackPressed() -> Bool {
        if !presenterImpl.tappedBackButton() {
            return super.onBackPressed()
        }
        return false
    }

    func setupButtons() {
        setupSetepOneConfirmBttn.layer.cornerRadius = AppConfigs.CORNER_RADIUS_BUTTON
        setupSetepTwoConfirmBttn.layer.cornerRadius = AppConfigs.CORNER_RADIUS_BUTTON
        changeConfirmBttn.layer.cornerRadius = AppConfigs.CORNER_RADIUS_BUTTON
    }

    func setupViews() {
        setupStepOneView.layer.cornerRadius = 3
        setupStepTwoView.layer.cornerRadius = 3
        setupScroll.delegate = self
    }

    func setupForTouchID() {
        setupTitleLbl.text = "security_touch_id".localize()
        setupStepOneDescriptionLbl.text = "security_description_touch_id".localize()
        setupSetepOneConfirmBttn.setTitle("security_allow_touch_id".localize(), for: .normal)
    }

    func setupForFaceID() {
        setupTitleLbl.text = "security_face_id".localize()
        setupStepOneDescriptionLbl.text = "security_description_face_id".localize()
        setupSetepOneConfirmBttn.setTitle("security_allow_face_id".localize(), for: .normal)
    }

    func updateViewMode(mode: SecurityPresenter.ScurityMode) {
        switch mode {
        case .setup:
            setupView.isHidden = false
            changePinView.isHidden = true
        case .main:
            setupView.isHidden = true
            changePinView.isHidden = true
        case .changePin:
            changeScreenToChangePin()
        }
    }

    override func getScrollableMainView() -> UIScrollView? {
        switch presenterImpl.mode {
        case .setup:
            return setupScroll
        case .main:
            return nil
        case .changePin:
            return changePinScroll
        }
    }

    func scrollViewDidScroll(_ scrollView: UIScrollView) {
        let offset = isSetupStepTwo ? scrollView.frame.width : 0
        if scrollView.contentOffset.x != offset {
            scrollView.contentOffset.x = offset
        }
    }

    // MARK: Table

    override func setupTable() {
        tableUtils = TableUtils(view: self, presenter: presenterImpl, table: table)
    }

    override func getHeaderHeight() -> CGFloat {
        return AppConfigs.TABLE_HEADER_HEIGHT_REGULAR
    }

    override func getCellIdentifier(for indexPath: IndexPath) -> String {

        switch presenterImpl.getElementType(indexPath: indexPath) {
        case .main:
            return "main"
        default:
            return super.getCellIdentifier(for: indexPath)
        }
    }

    override func getCellNib(for indexPath: IndexPath) -> String? {

        switch presenterImpl.getElementType(indexPath: indexPath) {
        case .main:
            return SecurityMainViewCell.default_NibName()
        default:
            return super.getCellNib(for: indexPath)
        }
    }

    // MARK: Buttons

    @IBAction func actionSetupConfirmStepOne(_ sender: Any) {
        presenterImpl.tappedSetupConfirmBiometricsButton()
    }

    @IBAction func actionSetupSkipStepOne(_ sender: Any) {
        presenterImpl.tappedSetupSkipBiometricsButton()
    }

    @IBAction func showSetupPassOne(_ sender: Any) {
        presenterImpl.toogleVisibility(digOneTf: setupPinOneDigOneTf, digtwoTf: setupPinOneDigTwoTf,
                                       digThreeTf: setupPinOneDigThreeTf, digFourTf: setupPinOneDigFourTf,
                                       toogleBttn: setupPinOneBttn)
    }

    @IBAction func showSetupPassTwo(_ sender: Any) {
        presenterImpl.toogleVisibility(digOneTf: setupPinTwoDigOneTf, digtwoTf: setupPinTwoDigTwoTf,
                                       digThreeTf: setupPinTwoDigThreeTf, digFourTf: setupPinTwoDigFourTf,
                                       toogleBttn: setupPinTwoBttn)
    }

    @IBAction func showChangePassOne(_ sender: Any) {
        presenterImpl.toogleVisibility(digOneTf: changePinOneDigOneTf, digtwoTf: changePinOneDigTwoTf,
                                       digThreeTf: changePinOneDigThreeTf, digFourTf: changePinOneDigFourTf,
                                       toogleBttn: changePinOneBttn)
    }

    @IBAction func showChangePassTwo(_ sender: Any) {
        presenterImpl.toogleVisibility(digOneTf: changePinTwoDigOneTf, digtwoTf: changePinTwoDigTwoTf,
                                       digThreeTf: changePinTwoDigThreeTf, digFourTf: changePinTwoDigFourTf,
                                       toogleBttn: changePinTwoBttn)
    }

    @IBAction func showChangePassCurrent(_ sender: Any) {
        presenterImpl.toogleVisibility(digOneTf: changePinOldDigOneTf, digtwoTf: changePinOldDigTwoTf,
                                       digThreeTf: changePinOldDigThreeTf, digFourTf: changePinOldDigFourTf,
                                       toogleBttn: changePinOldBttn)
    }

    @IBAction func actionSetupConfirmStepTwo(_ sender: Any) {
        let pin = """
        \(setupPinOneDigOneTf.text!)\
        \(setupPinOneDigTwoTf.text!)\
        \(setupPinOneDigThreeTf.text!)\
        \(setupPinOneDigFourTf.text!)
        """
        presenterImpl.tappedSetupConfirmPinButton(pin: pin)
    }

    @IBAction func actionConfirmChangePass(_ sender: Any) {
        let oldPin = """
        \(changePinOldDigOneTf.text!)\
        \(changePinOldDigTwoTf.text!)\
        \(changePinOldDigThreeTf.text!)\
        \(changePinOldDigFourTf.text!)
        """
        let newPin = """
        \(changePinOneDigOneTf.text!)\
        \(changePinOneDigTwoTf.text!)\
        \(changePinOneDigThreeTf.text!)\
        \(changePinOneDigFourTf.text!)
        """

        presenterImpl.tappedSetupConfirmChangePass(oldPin: oldPin, newPin: newPin)

    }

    // MARK: Screens

    func changeScreenToSetupStepTwo() {
        setupScroll.delegate = nil
        setupScroll.scrollRectToVisible(setupStepTwoPageView.frame, animated: true)
        isSetupStepTwo = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            self.setupScroll.delegate = self
        }
        setupStepLbl.text = "security_step_two".localize()
        setupStepTwoView.alpha = 1
    }

    func changeScreenToChangePin() {
        setupView.isHidden = true
        changePinView.isHidden = false
        changePinOldDigOneTf.isSecureTextEntry = true
        changePinOldDigTwoTf.isSecureTextEntry = true
        changePinOldDigThreeTf.isSecureTextEntry = true
        changePinOldDigFourTf.isSecureTextEntry = true
        changePinOldDigOneTf.text = ""
        changePinOldDigTwoTf.text = ""
        changePinOldDigThreeTf.text = ""
        changePinOldDigFourTf.text = ""
        changePinOldBttn.setImage(#imageLiteral(resourceName: "ico_visibility_on"), for: .normal)
        changePinOneDigOneTf.isSecureTextEntry = true
        changePinOneDigTwoTf.isSecureTextEntry = true
        changePinOneDigThreeTf.isSecureTextEntry = true
        changePinOneDigFourTf.isSecureTextEntry = true
        changePinOneDigOneTf.text = ""
        changePinOneDigTwoTf.text = ""
        changePinOneDigThreeTf.text = ""
        changePinOneDigFourTf.text = ""
        changePinOneBttn.setImage(#imageLiteral(resourceName: "ico_visibility_on"), for: .normal)
        changePinTwoDigOneTf.isSecureTextEntry = true
        changePinTwoDigTwoTf.isSecureTextEntry = true
        changePinTwoDigThreeTf.isSecureTextEntry = true
        changePinTwoDigFourTf.isSecureTextEntry = true
        changePinTwoDigOneTf.text = ""
        changePinTwoDigTwoTf.text = ""
        changePinTwoDigThreeTf.text = ""
        changePinTwoDigFourTf.text = ""
        changePinTwoBttn.setImage(#imageLiteral(resourceName: "ico_visibility_on"), for: .normal)
        changePassMatchLbl.isHidden = true
        changePassMatchImg.isHidden = true
    }
}

extension SecurityViewController: UITextFieldDelegate {

    func textField(_ textField: UITextField, shouldChangeCharactersIn range: NSRange,
                   replacementString string: String) -> Bool {

        if !string.isEmpty {
            if textField == setupPinOneDigOneTf {
                setupPinOneDigTwoTf.becomeFirstResponder()
            } else if textField == setupPinOneDigTwoTf {
                setupPinOneDigThreeTf.becomeFirstResponder()
            } else if textField == setupPinOneDigThreeTf {
                setupPinOneDigFourTf.becomeFirstResponder()
            } else if textField == setupPinOneDigFourTf {
                setupPinTwoDigOneTf.becomeFirstResponder()
            } else if textField == setupPinTwoDigOneTf {
                setupPinTwoDigTwoTf.becomeFirstResponder()
            } else if textField == setupPinTwoDigTwoTf {
                setupPinTwoDigThreeTf.becomeFirstResponder()
            } else if textField == setupPinTwoDigThreeTf {
                setupPinTwoDigFourTf.becomeFirstResponder()
            } else if textField == setupPinTwoDigFourTf {
                setupPinTwoDigFourTf.resignFirstResponder()
            } else if textField == changePinOldDigOneTf {
                changePinOldDigTwoTf.becomeFirstResponder()
            } else if textField == changePinOldDigTwoTf {
                changePinOldDigThreeTf.becomeFirstResponder()
            } else if textField == changePinOldDigThreeTf {
                changePinOldDigFourTf.becomeFirstResponder()
            } else if textField == changePinOldDigFourTf {
                changePinOneDigOneTf.becomeFirstResponder()
            } else if textField == changePinOneDigOneTf {
                changePinOneDigTwoTf.becomeFirstResponder()
            } else if textField == changePinOneDigTwoTf {
                changePinOneDigThreeTf.becomeFirstResponder()
            } else if textField == changePinOneDigThreeTf {
                changePinOneDigFourTf.becomeFirstResponder()
            } else if textField == changePinOneDigFourTf {
                changePinTwoDigOneTf.becomeFirstResponder()
            } else if textField == changePinTwoDigOneTf {
                changePinTwoDigTwoTf.becomeFirstResponder()
            } else if textField == changePinTwoDigTwoTf {
                changePinTwoDigThreeTf.becomeFirstResponder()
            } else if textField == changePinTwoDigThreeTf {
                changePinTwoDigFourTf.becomeFirstResponder()
            } else if textField == changePinTwoDigFourTf {
                changePinTwoDigFourTf.resignFirstResponder()
            }
            textField.text = string
            if presenterImpl.mode == .changePin {
                presenterImpl.validatePIN(pinOneDigOneTf: changePinOneDigOneTf,
                                          pinOneDigTwoTf: changePinOneDigTwoTf,
                                          pinOneDigThreeTf: changePinOneDigThreeTf,
                                          pinOneDigFourTf: changePinOneDigFourTf,
                                          pinTwoDigOneTf: changePinTwoDigOneTf,
                                          pinTwoDigTwoTf: changePinTwoDigTwoTf,
                                          pinTwoDigThreeTf: changePinTwoDigThreeTf,
                                          pinTwoDigFourTf: changePinTwoDigFourTf,
                                          passMatchImg: changePassMatchImg,
                                          passMatchLbl: changePassMatchLbl,
                                          confirmBttn: changeConfirmBttn)
            } else {
                presenterImpl.validatePIN(pinOneDigOneTf: setupPinOneDigOneTf,
                                          pinOneDigTwoTf: setupPinOneDigTwoTf,
                                          pinOneDigThreeTf: setupPinOneDigThreeTf,
                                          pinOneDigFourTf: setupPinOneDigFourTf,
                                          pinTwoDigOneTf: setupPinTwoDigOneTf,
                                          pinTwoDigTwoTf: setupPinTwoDigTwoTf,
                                          pinTwoDigThreeTf: setupPinTwoDigThreeTf,
                                          pinTwoDigFourTf: setupPinTwoDigFourTf,
                                          passMatchImg: setupPassMatchImg,
                                          passMatchLbl: setupPassMatchLbl,
                                          confirmBttn: setupSetepTwoConfirmBttn)
            }
            return false
        } else if !textField.text!.isEmpty && string.isEmpty {

            textField.text = ""
            if presenterImpl.mode == .changePin {
                presenterImpl.validatePIN(pinOneDigOneTf: changePinOneDigOneTf,
                                          pinOneDigTwoTf: changePinOneDigTwoTf,
                                          pinOneDigThreeTf: changePinOneDigThreeTf,
                                          pinOneDigFourTf: changePinOneDigFourTf,
                                          pinTwoDigOneTf: changePinTwoDigOneTf,
                                          pinTwoDigTwoTf: changePinTwoDigTwoTf,
                                          pinTwoDigThreeTf: changePinTwoDigThreeTf,
                                          pinTwoDigFourTf: changePinTwoDigFourTf,
                                          passMatchImg: changePassMatchImg,
                                          passMatchLbl: changePassMatchLbl,
                                          confirmBttn: changeConfirmBttn)
            } else {
                presenterImpl.validatePIN(pinOneDigOneTf: setupPinOneDigOneTf,
                                          pinOneDigTwoTf: setupPinOneDigTwoTf,
                                          pinOneDigThreeTf: setupPinOneDigThreeTf,
                                          pinOneDigFourTf: setupPinOneDigFourTf,
                                          pinTwoDigOneTf: setupPinTwoDigOneTf,
                                          pinTwoDigTwoTf: setupPinTwoDigTwoTf,
                                          pinTwoDigThreeTf: setupPinTwoDigThreeTf,
                                          pinTwoDigFourTf: setupPinTwoDigFourTf,
                                          passMatchImg: setupPassMatchImg,
                                          passMatchLbl: setupPassMatchLbl,
                                          confirmBttn: setupSetepTwoConfirmBttn)
            }
            return false
        }
        return true
    }
}
