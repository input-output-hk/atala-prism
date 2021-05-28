//
//  PopupAlertViewController.swift
//  prism-ios-wallet
//
//  Created by Roberto Daviduk on 22/03/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Presentr
import UIKit

class PopupAlertViewController: UIViewController {
    
    @IBOutlet weak var viewBg: UIView!
    
    @IBOutlet weak var logoImageview: UIImageView!
    
    @IBOutlet weak var titleLabel: UILabel!
    @IBOutlet weak var messageLabel: UILabel!
    
    @IBOutlet weak var buttonConfirm: UIButton!
    @IBOutlet weak var buttonDecline: UIButton!
    
    var logoName: String = ""
    var titleValue: String = ""
    var message: String = ""
    var declineButton: Bool = false

    var onAccept: (() -> Void)?

    override func viewDidLoad() {
        super.viewDidLoad()
        
        viewBg.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR)

        logoImageview.image = UIImage.init(named: logoName)
        titleLabel.text = titleValue
        messageLabel.text = message
        
        buttonDecline.isHidden = !declineButton
        
        buttonDecline.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON, borderWidth: 3,
                                      borderColor: UIColor.appRed.cgColor)
        buttonConfirm.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON)
    }
    
    static func makeThisView() -> PopupAlertViewController {
        let storyboard = UIStoryboard(name: "PopupAlert", bundle: nil)
        if let viewcontroller = storyboard.instantiateViewController(withIdentifier: "PopupAlert")
            as? PopupAlertViewController {
            return viewcontroller
        }
        return PopupAlertViewController()
    }

    let presentr: Presentr = {

        let presenter = Presentr(presentationType: .alert)
        presenter.transitionType = TransitionType.coverHorizontalFromRight
        presenter.dismissOnSwipe = false

        let width = ModalSize.sideMargin(value: 21.0)
        let height = ModalSize.custom(size: 367)
        let center = ModalCenterPosition.center
        presenter.presentationType = .custom(width: width, height: height, center: center)
        presenter.transitionType = nil
        presenter.dismissTransitionType = nil
        presenter.dismissAnimated = true

        return presenter
    }()
    
    func setupWith(logoName: String, title: String, message: String, declineButton: Bool = false) {
        self.logoName = logoName
        self.titleValue = title
        self.message = message
        self.declineButton = declineButton
    }
    
    func actionConfirmButton(onAccept: @escaping () -> Void) {
        self.onAccept = onAccept
    }

    // MARK: Component delegates

    @IBAction func actionConfirmButtonTapped(_ sender: Any) {
        self.dismiss(animated: true, completion: nil)
        self.onAccept?()
    }

    @IBAction func actionDeclineButtonTapped(_ sender: Any) {
        self.dismiss(animated: true, completion: nil)
    }

    // MARK: Presentr Delegate

    func presentrShouldDismiss(keyboardShowing: Bool) -> Bool {
        return false
    }
}
    
