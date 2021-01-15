//
//  SecurityPresenter.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 18/03/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import UIKit
import LocalAuthentication

class SecurityPresenter: ListingBasePresenter, ListingBaseTableUtilsPresenterDelegate,
                            SecurityMainViewCellPresenterDelegate {

    var viewImpl: SecurityViewController? {
        return view as? SecurityViewController
    }

    enum ScurityMode {
        case setup
        case main
        case changePin
    }

    enum SecurityCellType {
        case base(value: ListingBaseCellType)
        case main // initial mode
    }

    struct CellRow {
        var type: SecurityCellType
        var value: Any?
    }

    struct InitialCellValue {
        var icon: UIImage
        var title: String
        var hasSwitch: Bool
        var action: SelectorAction?
    }

    var mode: ScurityMode = .setup

    var initialRows: [CellRow]?

    // MARK: Modes

    func getMode() -> ScurityMode {
        return mode
    }

    lazy var initialStaticCells: [InitialCellValue] = [
        InitialCellValue(icon: #imageLiteral(resourceName: "logo_security"), title: "security_use_touch_id", hasSwitch: true, action: nil),
        InitialCellValue(icon: #imageLiteral(resourceName: "logo_pin_code"), title: "security_change_pin", hasSwitch: false, action: actionRowPin)
    ]

    var pinSetup: String?

    func startShowingInitial() {
        if let pin = sharedMemory.loggedUser?.appPin, !pin.isEmpty {
            startShowingMain()
        } else {

        }
    }

    func startShowingMain() {
        mode = .main
        state = .listing
        initialRows = []
        initialStaticCells.forEach { initialRows?.append(CellRow(type: .main, value: $0)) }
        viewImpl?.updateViewMode(mode: .main)
        updateViewToState()
    }

    func startShowingChangePin() {
        mode = .changePin
        viewImpl?.updateViewMode(mode: .changePin)
    }

    func validateSetupMode() {
        if let pin = sharedMemory.loggedUser?.appPin, !pin.isEmpty {
            startShowingMain()
        }
    }

    // MARK: Buttons

    @discardableResult
    func tappedBackButton() -> Bool {

        if mode == .changePin {
            startShowingMain()
            return true
        }
        return false
    }

    func tappedSetupConfirmBiometricsButton() {
        let context = LAContext()
        context.localizedCancelTitle = "Cancel"
        var error: NSError?
        if context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) {
            let reason = "Log in to your account"
            context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics,
                                   localizedReason: reason ) { success, error in

                if success {

                    // Move to the main thread because a state update triggers UI changes.
                    DispatchQueue.main.asyncAfter(wallDeadline: .now() + 1) { [unowned self] in
                        self.sharedMemory.loggedUser?.appBiometrics = true
                        self.sharedMemory.loggedUser?.appPin = self.pinSetup
                        self.sharedMemory.loggedUser = self.sharedMemory.loggedUser
                        Tracker.global.trackSecureAppPasscode()
                        if context.biometryType == .faceID {
                            Tracker.global.trackSecureAppFacial()
                        } else {
                            Tracker.global.trackSecureAppFingerprint()
                        }
                        self.startShowingMain()
                    }

                } else {
                    print(error?.localizedDescription ?? "Failed to authenticate")

                    // Fall back to a asking for username and password.
                    // ...
                }
            }
        } else {
            DispatchQueue.main.async { [unowned self] in
                if context.biometryType == .faceID {
                    ViewUtils.showErrorMessage(doShow: true, view: self.viewImpl!, title: nil,
                                               message: "security_unerroled_face_id".localize())
                } else {
                    ViewUtils.showErrorMessage(doShow: true, view: self.viewImpl!, title: nil,
                                               message: "security_unerroled_touch_id".localize())
                }
            }
        }
    }

    func tappedSetupSkipBiometricsButton() {
        sharedMemory.loggedUser?.appBiometrics = false
        sharedMemory.loggedUser?.appPin = self.pinSetup
        sharedMemory.loggedUser = sharedMemory.loggedUser
        Tracker.global.trackSecureAppPasscode()
        self.startShowingMain()
    }

    func tappedSetupConfirmPinButton(pin: String) {
        self.pinSetup = pin
        //        sharedMemory.loggedUser = sharedMemory.loggedUser
        let context = LAContext()
        context.localizedCancelTitle = "Cancel"
        var error: NSError?
        if context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) {
            if context.biometryType == .faceID {
                viewImpl?.setupForFaceID()
            } else {
                viewImpl?.setupForTouchID()
            }
        }
        self.viewImpl?.changeScreenToSetupStepTwo()
    }

    func tappedSetupConfirmChangePass(oldPin: String, newPin: String) {
        if sharedMemory.loggedUser?.appPin == oldPin {
            sharedMemory.loggedUser?.appPin = newPin
            sharedMemory.loggedUser = sharedMemory.loggedUser
            viewImpl?.showSuccessMessage(doShow: true, message: "security_change_pin_success".localize()) {
                self.startShowingMain()
            }
        } else {
            viewImpl?.showErrorMessage(doShow: true, message: "security_change_pin_error".localize())
        }
    }

    func toogleVisibility(digOneTf: UITextField, digtwoTf: UITextField, digThreeTf: UITextField, digFourTf: UITextField,
                          toogleBttn: UIButton) {
        digOneTf.isSecureTextEntry = !digOneTf.isSecureTextEntry
        digtwoTf.isSecureTextEntry = !digtwoTf.isSecureTextEntry
        digThreeTf.isSecureTextEntry = !digThreeTf.isSecureTextEntry
        digFourTf.isSecureTextEntry = !digFourTf.isSecureTextEntry
        let icon = digOneTf.isSecureTextEntry ? #imageLiteral(resourceName: "ico_visibility_on") : #imageLiteral(resourceName: "ico_visibility_off")
        toogleBttn.setImage(icon, for: .normal)
    }

    func validatePIN(pinOneDigOneTf: UITextField, pinOneDigTwoTf: UITextField, pinOneDigThreeTf: UITextField,
                     pinOneDigFourTf: UITextField, pinTwoDigOneTf: UITextField, pinTwoDigTwoTf: UITextField,
                     pinTwoDigThreeTf: UITextField, pinTwoDigFourTf: UITextField, passMatchImg: UIImageView,
                     passMatchLbl: UILabel, confirmBttn: UIButton) {

        if !pinOneDigOneTf.text!.isEmpty && !pinOneDigTwoTf.text!.isEmpty
            && !pinOneDigThreeTf.text!.isEmpty && !pinOneDigFourTf.text!.isEmpty
            && !pinTwoDigOneTf.text!.isEmpty && !pinTwoDigTwoTf.text!.isEmpty
            && !pinTwoDigThreeTf.text!.isEmpty && !pinTwoDigFourTf.text!.isEmpty {

            passMatchImg.isHidden = false
            passMatchLbl.isHidden = false

            if pinOneDigOneTf.text == pinTwoDigOneTf.text  && pinOneDigTwoTf.text == pinTwoDigTwoTf.text
                && pinOneDigThreeTf.text == pinTwoDigThreeTf.text  && pinOneDigFourTf.text == pinTwoDigFourTf.text {
                confirmBttn.isEnabled = true
                confirmBttn.backgroundColor = .appRed
                passMatchImg.image = #imageLiteral(resourceName: "ico_ok")
                passMatchLbl.textColor = .appGreen
                passMatchLbl.text = "security_pin_match".localize()
            } else {
                confirmBttn.isEnabled = false
                confirmBttn.backgroundColor = .appGreyMid
                passMatchImg.image = #imageLiteral(resourceName: "ico_err")
                passMatchLbl.textColor = .appRed
                passMatchLbl.text = "security_pin_mismatch".localize()
            }
        } else {
            confirmBttn.isEnabled = false
            confirmBttn.backgroundColor = .appGreyMid
            passMatchImg.isHidden = true
            passMatchLbl.isHidden = true
        }
    }

    lazy var actionRowPin = SelectorAction(action: { [weak self] in
        self?.startShowingChangePin()
    })

    // MARK: ListingBaseTableUtilsPresenterDelegate

    func cleanData() {
        initialRows = []
    }

    func fetchData() {

        state = .fetching
        fetchingQueue = 1

        fetchElements()
    }

    func hasData() -> Bool {
        switch mode {
        case .main:
            return (initialRows?.size() ?? 0) > 0
        default:
            return false
        }
    }

    func getElementCount() -> Int {
        if let baseValue = super.getBaseElementCount() {
            return baseValue
        }

        switch mode {
        case .main:
            return (initialRows?.size() ?? 0)
        default:
            return 0
        }
    }

    func getElementType(indexPath: IndexPath) -> SecurityCellType? {
        if let baseValue = super.getBaseElementType(indexPath: indexPath) {
            return .base(value: baseValue)
        }

        switch mode {
        case .main:
            return initialRows![indexPath.row].type
        default:
            return nil
        }
    }

    // MARK: Fetch

    func getLoggedUser() -> LoggedUser? {
        return sharedMemory.loggedUser
    }

    func fetchElements() {

        switch mode {
        case .main:
            self.startShowingMain()
            self.startListing()
        case .setup:
            self.startShowingInitial()
        case .changePin:
            self.startShowingChangePin()
        }
    }

    // MARK: Table

    func hasPullToRefresh() -> Bool {
        true
    }

    func actionPullToRefresh() {

        self.fetchData()
        self.updateViewToState()
    }

    func setup(for cell: SecurityMainViewCell) {

        if let value = initialRows![cell.indexPath!.row].value as? InitialCellValue {
            cell.config(title: value.title.localize(), hasSwitch: value.hasSwitch,
                        switchValue: sharedMemory.loggedUser?.appBiometrics ?? false, icon: value.icon)
        }
    }

    func tappedAction(for cell: SecurityMainViewCell) {

        if let value = initialRows![cell.indexPath!.row].value as? InitialCellValue {
            value.action?.action()
        }
    }

    func didSelectRowAt(indexPath: IndexPath) {
        if let value = initialRows![indexPath.row].value as? InitialCellValue {
            value.action?.action()
        }
    }

    func switchValueChanged(for cell: SecurityMainViewCell, value: Bool) {
        sharedMemory.loggedUser?.appBiometrics = value
        sharedMemory.loggedUser = sharedMemory.loggedUser
    }
}
