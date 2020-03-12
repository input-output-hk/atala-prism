//
import Foundation
import UIKit

protocol BaseView: class {

    func showLoading(doShow: Bool, message: String?)

    func showErrorMessage(doShow: Bool, message: String?, afterErrorAction: (() -> Void)?)

    func showSuccessMessage(doShow: Bool, message: String?, actions: [UIAlertAction]?)

    func callOnMain(millis: Int, method: (() -> Void)?)

    func exitToLogin()
}

extension UIViewController {

    var sharedMemory: SharedMemory {
        return SharedMemory.global
    }

    // MARK: Navigation bar

    // Changed the statusbar color of all the app's ViewControllers
    var preferredStatusBarStyle: UIStatusBarStyle {
        return AppConfigs.preferredStatusBarStyle
    }

    // Override if customBackButton action is wanted
    @objc @discardableResult func onBackPressed() -> Bool {

        ViewControllerUtils.defaultBackPressed(view: self)
        return false
    }

    // Override to add a nav bar
    @objc func navBarCustomStyle() -> NavBarCustomStyle {
        return NavBarCustomStyle(hasNavBar: false)
    }

    // MARK: Keyaboard

    @objc public func hideKeyboard() {
        self.view.endEditing(true)
    }

    // Override if backbutton needs to be removed particularly
    @objc @discardableResult func getScrollableMainView() -> UIScrollView? {
        return nil
    }

    @objc func shiftKeyboardOnWillShow(notification: NSNotification) {

        // If there is a main scrollview, move the content
        if let scrollView = getScrollableMainView() {
            let userInfo = notification.userInfo!
            var keyboardFrame: CGRect = (userInfo[UIResponder.keyboardFrameEndUserInfoKey] as! NSValue).cgRectValue
            keyboardFrame = self.view.convert(keyboardFrame, from: nil)

            var contentInset: UIEdgeInsets = scrollView.contentInset
            contentInset.bottom = keyboardFrame.size.height
            scrollView.contentInset = contentInset
        }
        // Otherwise, if there is no scrollview but there is a keyboardsize available, shift the whole view
        else if let keyboardSize = (notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? NSValue)?.cgRectValue {
            if self.view.frame.origin.y == 0 {
                self.view.frame.origin.y -= keyboardSize.height
            }
        }
    }

    @objc func shiftKeyboardOnWillHide(notification: NSNotification) {

        // If there is was a main scrollview, restore the content change
        if let scrollView = getScrollableMainView() {
            let contentInset: UIEdgeInsets = UIEdgeInsets.zero
            scrollView.contentInset = contentInset
        }
        // Otherwise, if the main view was shifted, restore it
        else if self.view.frame.origin.y != 0 {
            self.view.frame.origin.y = 0
        }
    }
}

// MARK: BaseView implementation

extension UIViewController: BaseView {

    func callOnMain(millis: Int, method: (() -> Void)?) {

        let seconds: TimeInterval = Double(millis) * 1000.0
        DispatchQueue.main.asyncAfter(deadline: .now() + seconds) {
            method?()
        }
    }

    func showLoading(doShow: Bool) {
        showLoading(doShow: doShow, message: nil)
    }

    func showLoading(doShow: Bool, message: String?) {
        ViewUtils.showLoading(doShow: doShow, view: self, message: message)
    }

    func showErrorMessage(doShow: Bool) {
        showErrorMessage(doShow: doShow, message: nil)
    }

    func showErrorMessage(doShow: Bool, message: String?, afterErrorAction: (() -> Void)? = nil) {
        ViewUtils.showErrorMessage(doShow: doShow, view: self, title: nil, message: message, afterErrorAction: afterErrorAction)
    }

    func showSuccessMessage(doShow: Bool, message: String?, actions: [UIAlertAction]? = nil) {
        ViewUtils.showSuccessMessage(doShow: doShow, view: self, title: nil, message: message, actions: actions)
    }

    func showComingSoonMessage() {
        ViewUtils.showToast(view: self, message: "coming_soon_title".localize())
    }

    func exitToLogin() {
        UserUtils.exitToLogin(caller: self)
    }
}
