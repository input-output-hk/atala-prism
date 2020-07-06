//
import UIKit

class NavBarCustomStyle: NSObject {

    var hasNavBar: Bool
    var isWhite: Bool
    var title: String?
    var hasBackButton: Bool
    var rightIconName: String?
    var rightIconAction: SelectorAction?
    var textButtonTitle: NSAttributedString?
    var textButtonAction: SelectorAction?

    init(hasNavBar: Bool, isWhite: Bool = false, title: String? = nil, hasBackButton: Bool = false,
         rightIconName: String? = nil, rightIconAction: SelectorAction? = nil,
         textButtonTitle: NSAttributedString? = nil, textButtonAction: SelectorAction? = nil) {
        self.hasNavBar = hasNavBar
        self.isWhite = isWhite
        self.title = title
        self.hasBackButton = hasBackButton
        self.rightIconName = rightIconName
        self.rightIconAction = rightIconAction
        self.textButtonTitle = textButtonTitle
        self.textButtonAction = textButtonAction
        super.init()
    }
}

class NavBarCustom: BaseNibLoadingView {

    private static let NAV_BAR_TAG = 1100

    weak var containerViewController: UIViewController?
    @IBOutlet weak var buttonBack: UIButton!
    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var constraintLabelTitleShort: NSLayoutConstraint!
    @IBOutlet weak var constraintLabelTitleLong: NSLayoutConstraint!
    @IBOutlet weak var buttonRight: UIButton!
    @IBOutlet weak var buttonText: UIButton!

    private static func findNavBarIn(view: UIView) -> NavBarCustom? {
        return view.subviews.first(where: { $0.tag == NAV_BAR_TAG }) as? NavBarCustom
    }

    private static func create(view: UIView) -> NavBarCustom {

        let navBar = NavBarCustom()

        navBar.tag = NAV_BAR_TAG
        view.insertSubview(navBar, at: view.subviews.size())

        navBar.translatesAutoresizingMaskIntoConstraints = false
        view.addConstraint(NSLayoutConstraint(item: navBar, attribute: .width, relatedBy: .equal,
                                              toItem: view, attribute: .width, multiplier: 1.0, constant: 0))
        view.addConstraint(NSLayoutConstraint(item: navBar, attribute: .leading, relatedBy: .equal,
                                              toItem: view, attribute: .leading, multiplier: 1.0, constant: 0))
        view.addConstraint(NSLayoutConstraint(item: navBar, attribute: .top, relatedBy: .equal,
                                              toItem: view, attribute: .top, multiplier: 1.0, constant: 0))

        view.layoutIfNeeded()
        return navBar
    }

    static func config(view viewController: UIViewController) {

        // Hide the OS navigation bar if present
        viewController.navigationController?.isNavigationBarHidden = true

        let view: UIView = viewController.view
        let style = viewController.navBarCustomStyle()

        // If it doenst have and doesnt want one, exit
        var navBar = findNavBarIn(view: view)
        if navBar == nil && !style.hasNavBar {
            return
        }
        // If there wasnt one, create it
        if navBar == nil {
            navBar = create(view: view)
        }
        // If there was one but is not longer wanted, remove it and exit
        if !style.hasNavBar {
            navBar?.removeFromSuperview()
            view.layoutIfNeeded()
            return
        }

        // Config the style
        navBar?.containerViewController = viewController
        let titleColor = style.isWhite ? UIColor.appWhite : UIColor.appBlack
        navBar?.labelTitle.text = style.title
        navBar?.labelTitle.textColor = titleColor
        navBar?.constraintLabelTitleLong.priority = style.hasBackButton ? .defaultHigh : .defaultLow
        navBar?.constraintLabelTitleShort.priority = style.hasBackButton ? .defaultLow : .defaultHigh
        navBar?.buttonBack.isHidden = !style.hasBackButton
        navBar?.buttonRight.isHidden = style.rightIconName == nil
        if style.rightIconName != nil {
            let rightImg = UIImage(named: style.rightIconName!)
            navBar?.buttonRight.setImage(rightImg, for: .normal)
        }
        navBar?.buttonText.isHidden = style.textButtonTitle == nil
        navBar?.buttonText.setAttributedTitle(style.textButtonTitle, for: .normal)
        view.layoutIfNeeded()
    }

    @IBAction func actionBackButton(_ sender: Any) {
        containerViewController?.onBackPressed()
    }

    @IBAction func actionRightButton(_ sender: Any) {
        containerViewController?.navBarCustomStyle().rightIconAction?.action()
    }

    @IBAction func actionTextButton(_ sender: Any) {
        containerViewController?.navBarCustomStyle().textButtonAction?.action()
    }
}
