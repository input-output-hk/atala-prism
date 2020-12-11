//
//  DeleteCredentialViewController.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 07/08/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import UIKit
import Presentr

class DeleteCredentialViewController: UIViewController {

    @IBOutlet weak var buttonConfirm: UIButton!
    @IBOutlet weak var buttonDecline: UIButton!
    @IBOutlet weak var viewBg: UIView!
    @IBOutlet weak var credentialIcon: UIImageView!
    @IBOutlet weak var credentialName: UILabel!

    var onDelete: (() -> Void)!
    var credential: Credential?

    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
        viewBg.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR)
        buttonDecline.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON, borderWidth: 3,
                                      borderColor: UIColor.appRed.cgColor)
        buttonConfirm.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON)

        credentialName.text = credential?.credentialName
        credentialIcon.image = UIImage(named: credential?.logoPlaceholder ?? "")
    }

    static func makeThisView() -> DeleteCredentialViewController {
        let storyboard = UIStoryboard(name: "DeleteCredential", bundle: nil)
        if let viewcontroller = storyboard.instantiateViewController(withIdentifier: "DeleteCredential")
            as? DeleteCredentialViewController {
            return viewcontroller
        }
        return DeleteCredentialViewController()
    }

    let presentr: Presentr = {

        let presenter = Presentr(presentationType: .alert)
        presenter.transitionType = TransitionType.coverHorizontalFromRight
        presenter.dismissOnSwipe = false

        let width = ModalSize.sideMargin(value: 21.0)
        let height = ModalSize.custom(size: 301)
        let center = ModalCenterPosition.center
        presenter.presentationType = .custom(width: width, height: height, center: center)
        presenter.transitionType = nil
        presenter.dismissTransitionType = nil
        presenter.dismissAnimated = true

        return presenter
    }()

    func config(credential: Credential?, onDelete: @escaping () -> Void) {
        self.onDelete = onDelete
        self.credential = credential
    }

    // MARK: Component delegates

    @IBAction func actionConfirmButtonTapped(_ sender: Any) {
        self.dismiss(animated: true, completion: nil)
        self.onDelete()
    }

    @IBAction func actionDeclineButtonTapped(_ sender: Any) {
        self.dismiss(animated: true, completion: nil)
    }

}
