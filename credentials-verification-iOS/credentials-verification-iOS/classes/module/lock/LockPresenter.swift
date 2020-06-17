//
//  LockPresenter.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 30/04/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import UIKit
import LocalAuthentication

class LockPresenter: BasePresenter {

    var viewImpl: LockViewController? {
        return view as? LockViewController
    }

    func unlock() {
        viewImpl?.dismiss(animated: true, completion: nil)
    }

    func tappedBiometrics() {
        let context = LAContext()
        context.localizedCancelTitle = "Cancel"
        var error: NSError?
        if context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) {
            let reason = "unlock_app".localize()
            context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics,
                                   localizedReason: reason! ) { success, policyError in

                if success {

                    // Move to the main thread because a state update triggers UI changes.
                    DispatchQueue.main.asyncAfter(wallDeadline: .now() + 1) { [unowned self] in
                        self.sharedMemory.loggedUser?.appBiometrics = true
                        self.sharedMemory.loggedUser = self.sharedMemory.loggedUser
                        self.unlock()
                    }

                } else {
                    print(policyError?.localizedDescription ?? "Failed to authenticate")
                    var errorMsg = "unlock_biometrics_error".localize()
                    if policyError?._code == Int(kLAErrorUserCancel) {
                        errorMsg = "unlock_touch_id_cancel".localize()
                    } else if policyError?._code == Int(kLAErrorAuthenticationFailed) {
                        errorMsg = context.biometryType == .faceID
                            ? "unlock_face_id_error".localize()
                            : "unlock_touch_id_error".localize()
                    }
                    DispatchQueue.main.asyncAfter(wallDeadline: .now() + 1) { [unowned self] in
                        ViewUtils.showErrorMessage(doShow: true, view: self.viewImpl!, title: nil, message: errorMsg)
                        self.viewImpl?.disableBiometrics()
                    }
                }
            }
        } else {
            DispatchQueue.main.asyncAfter(wallDeadline: .now() + 0.3) { [unowned self] in
                if context.biometryType == .faceID {
                    ViewUtils.showErrorMessage(doShow: true, view: self.viewImpl!,
                                               title: nil, message: "unlock_unerroled_face_id".localize())
                } else {
                    ViewUtils.showErrorMessage(doShow: true, view: self.viewImpl!,
                                               title: nil, message: "unlock_unerroled_touch_id".localize())
                }
            }
        }
    }

    func validatePin() {
        let pin = """
        \(viewImpl?.pinDigOneTf.text ?? "")\
        \(viewImpl?.pinDigTwoTf.text ?? "")\
        \(viewImpl?.pinDigThreeTf.text ?? "")\
        \(viewImpl?.pinDigFourTf.text ?? "")
        """
        if sharedMemory.loggedUser?.appPin == pin {
            self.unlock()
        } else {
            viewImpl?.showErrorMessage(doShow: true, message: "unlock_pin_error".localize())
            viewImpl?.pinDigFourTf.text = ""
            viewImpl?.pinDigThreeTf.text = ""
            viewImpl?.pinDigTwoTf.text = ""
            viewImpl?.pinDigOneTf.text = ""
        }
    }

    func tappedNumber(digit: String) {
        if viewImpl?.pinDigOneTf.text?.isEmpty ?? false {
            viewImpl?.pinDigOneTf.text = digit
        } else if viewImpl?.pinDigTwoTf.text?.isEmpty ?? false {
            viewImpl?.pinDigTwoTf.text = digit
        } else if viewImpl?.pinDigThreeTf.text?.isEmpty ?? false {
            viewImpl?.pinDigThreeTf.text = digit
        } else {
            viewImpl?.pinDigFourTf.text = digit
            validatePin()
        }
    }

    func tappedBackspace() {
        if !(viewImpl?.pinDigFourTf.text?.isEmpty ?? false) {
            viewImpl?.pinDigFourTf.text = ""
        } else if !(viewImpl?.pinDigThreeTf.text?.isEmpty ?? false) {
            viewImpl?.pinDigThreeTf.text = ""
        } else if !(viewImpl?.pinDigTwoTf.text?.isEmpty ?? false) {
            viewImpl?.pinDigTwoTf.text = ""
        } else {
            viewImpl?.pinDigOneTf.text = ""
        }
    }

}
