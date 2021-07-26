//
import MBProgressHUD
import Toast

class ViewUtils: NSObject {

    public static func showLoading(doShow: Bool, view: UIViewController, message: String? = nil) {

        if doShow {
            var loadingNotification = view.view.viewWithTag(10001) as? MBProgressHUD
            if loadingNotification == nil {
                loadingNotification = MBProgressHUD.showAdded(to: view.view, animated: true)
                loadingNotification!.mode = MBProgressHUDMode.indeterminate
                loadingNotification!.tag = 10001
            }
            loadingNotification!.label.text = (message == nil ? "loading_title".localize() : message)
        } else {
            MBProgressHUD.hide(for: view.view, animated: true)
        }
    }

    public static func showErrorMessage(doShow: Bool, view: UIViewController, title: String? = nil,
                                        message: String? = nil, afterErrorAction: (() -> Void)? = nil) {

        if doShow {
            let titleStr = (title == nil || title!.isEmpty) ? "error_generic_title".localize() : title
            let messageStr = (message == nil || message!.isEmpty) ? "error_generic_error".localize() : message
            let actions = [UIAlertAction(title: "ok".localize(), style: .default, handler: { _ in
                Logger.d("The \"OK\" alert occured. After error action will trigger now if available.")
                afterErrorAction?()
            })]
            showAlertView(view: view, title: titleStr!, message: messageStr!, actions: actions)
        }
    }

    public static func showSuccessMessage(doShow: Bool, view: UIViewController, title: String? = nil,
                                          message: String? = nil,
                                          action: (() -> Void)? = nil) {

        if doShow {
            let titleStr = title == nil ? "success_generic_title".localize() : title
            let messageStr = message == nil ? "error_generic_error".localize() : message
            let modal = SuccessModalViewController.makeThisView()
            modal.config(subtitle: messageStr, title: titleStr, onOk: action)
            
            view.customPresentViewController(modal.presentr, viewController: modal, animated: true)
        }
    }

    public static func showAlertView(view: UIViewController, title: String, message: String,
                                     actions: [UIAlertAction]? = nil) {

        let alert = UIAlertController(title: title, message: message, preferredStyle: .alert)
        var actions = actions
        // If no actions were indicated, create a default ok action to close the alert
        if actions == nil {
            actions = [UIAlertAction(title: "ok".localize(), style: .default, handler: { _ in
                Logger.d("The \"OK\" alert occured.")
            })]
        }
        // Add the buttons (actions) to the alert
        for action in actions! {
            alert.addAction(action)
        }
        // Show it
        view.present(alert, animated: true, completion: nil)
    }

    public static func showToast(view: UIViewController, message: String) {

        view.view.makeToast(message, duration: 2.0)
    }
}
