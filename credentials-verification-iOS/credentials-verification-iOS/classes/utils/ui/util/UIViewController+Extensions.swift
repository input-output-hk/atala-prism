//

import UIKit

protocol SegueableScreen: class {
    func configScreenFromSegue(params: [Any?]?)
}

extension UIViewController {

    func add(asChildViewController viewController: UIViewController, to contentView: UIView) {

        // Add Child View Controller
        addChild(viewController)

        // Add Child View as Subview
        contentView.addSubview(viewController.view)

        // Configure Child View
        viewController.view.frame = contentView.bounds
        viewController.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]

        // Notify Child View Controller
        viewController.didMove(toParent: self)
    }

    func app_canPerformSegue(withIdentifier id: String) -> Bool {

        let segues = self.value(forKey: "storyboardSegueTemplates") as? [NSObject]
        let filtered = segues?.filter { $0.value(forKey: "identifier") as? String == id }
        return ((filtered?.count ?? 0) > 0)
    }

    func app_mayPerformSegue(withIdentifier id: String, sender: AnyObject?) -> Bool {

        if app_canPerformSegue(withIdentifier: id) {
            self.performSegue(withIdentifier: id, sender: sender)
            return true
        }
        return false
    }

    var alertController: UIAlertController? {
        guard let alert = UIApplication.topViewController() as? UIAlertController else { return nil }
        return alert
    }
    
    func topViewController() -> UIViewController! {
        if self.isKind(of: UITabBarController.self) {
            let tabbarController =  self as! UITabBarController
            return tabbarController.selectedViewController!.topViewController()
        } else if (self.isKind(of: UINavigationController.self)) {
            let navigationController = self as! UINavigationController
            return navigationController.visibleViewController!.topViewController()
        } else if ((self.presentedViewController) != nil){
            let controller = self.presentedViewController
            return controller!.topViewController()
        } else {
            return self
        }
    }
}
