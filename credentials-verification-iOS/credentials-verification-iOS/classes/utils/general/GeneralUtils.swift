//

extension Bundle {

    var releaseVersionNumber: String? {
        return infoDictionary?["CFBundleShortVersionString"] as? String
    }

    var buildVersionNumber: String? {
        return infoDictionary?["CFBundleVersion"] as? String
    }
}

extension NSObject {

    static func currentTimeMillis() -> Int64 {
        return Int64(Date().timeIntervalSince1970 * 1000)
    }

    func currentTimeMillis() -> Int64 {
        return NSObject.currentTimeMillis()
    }

    static func execOnMain(delaySecs: Double, task: @escaping @convention(block) () -> Swift.Void) {
        let time = DispatchTime.now() + delaySecs
        DispatchQueue.global(qos: .background).asyncAfter(deadline: time) {
            DispatchQueue.main.async {
                task()
            }
        }
    }
}

extension UIApplication {

    func app_getVisibleViewController() -> UIViewController? {
        return app_getVisibleViewControllerInternal(UIApplication.shared.keyWindow?.rootViewController)
    }

    func app_getVisibleViewControllerInternal(_ rootViewController: UIViewController?) -> UIViewController? {

        var rootVC = rootViewController
        if rootVC == nil {
            rootVC = UIApplication.shared.keyWindow?.rootViewController
        }

        if rootVC?.presentedViewController == nil {
            return rootVC
        }

        if let presented = rootVC?.presentedViewController {
            if presented.isKind(of: UINavigationController.self) {
                if let navigationController = presented as? UINavigationController {
                    return navigationController.viewControllers.last!
                }
            }

            if presented.isKind(of: UITabBarController.self) {
                if let tabBarController = presented as? UITabBarController {
                    return tabBarController.selectedViewController!
                }
            }

            return app_getVisibleViewControllerInternal(presented)
        }
        return nil
    }
}

extension UIView {

    func snapshot() -> UIImage {
        UIGraphicsBeginImageContextWithOptions(bounds.size, false, UIScreen.main.scale)
        drawHierarchy(in: bounds, afterScreenUpdates: true)
        let result = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        return result!
    }
}

extension UIWindow {

    func replaceRootViewControllerWith(_ replacementController: UIViewController, animated: Bool,
                                       completion: (() -> Void)?) {

        let snapshotImageView = UIImageView(image: self.snapshot())
        self.addSubview(snapshotImageView)

        let dismissCompletion = { () -> Void in // dismiss all modal view controllers
            self.rootViewController = replacementController
            self.bringSubviewToFront(snapshotImageView)
            if animated {
                UIView.animate(withDuration: 0.4, animations: { () -> Void in
                    snapshotImageView.alpha = 0
                }, completion: { _ -> Void in
                    snapshotImageView.removeFromSuperview()
                    completion?()
                })
            } else {
                snapshotImageView.removeFromSuperview()
                completion?()
            }
        }
        if self.rootViewController!.presentedViewController != nil {
            self.rootViewController!.dismiss(animated: false, completion: dismissCompletion)
        } else {
            dismissCompletion()
        }
    }
}
