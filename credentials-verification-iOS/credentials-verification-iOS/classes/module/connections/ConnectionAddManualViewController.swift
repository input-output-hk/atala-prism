//
//  ConnectionAddManualViewController.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 25/06/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import Presentr
import UIKit

class ConnectionAddManualViewController: UIViewController, PresentrDelegate {

    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var buttonConfirm: UIButton!
    @IBOutlet weak var buttonDecline: UIButton!
    @IBOutlet weak var viewBg: UIView!
    @IBOutlet weak var connectionTf: UITextField!

    var onAccept: ((String) -> Void)!

    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.

        viewBg.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR)
        buttonDecline.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON, borderWidth: 3,
                                      borderColor: UIColor.appRed.cgColor)
        buttonConfirm.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON)
    }

    static func makeThisView() -> ConnectionAddManualViewController {
        let storyboard = UIStoryboard(name: "ConnectionAddManual", bundle: nil)
        if let viewcontroller = storyboard.instantiateViewController(withIdentifier: "ConnectionAddManual")
            as? ConnectionAddManualViewController {
            return viewcontroller
        }
        return ConnectionAddManualViewController()
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

    func config(onAccept: @escaping (_ token: String) -> Void) {
        self.onAccept = onAccept
    }

    // MARK: Component delegates

    @IBAction func actionConfirmButtonTapped(_ sender: Any) {
        self.dismiss(animated: true, completion: nil)
        self.onAccept(self.connectionTf.text ?? "")
    }

    @IBAction func actionDeclineButtonTapped(_ sender: Any) {
        self.dismiss(animated: true, completion: nil)
    }

    // MARK: Presentr Delegate

    func presentrShouldDismiss(keyboardShowing: Bool) -> Bool {
        return false
    }

}
