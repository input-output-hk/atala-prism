//
//  DeleteContactViewController.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 11/08/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import UIKit
import Presentr

class DeleteContactViewController: UIViewController {

    @IBOutlet weak var buttonConfirm: UIButton!
    @IBOutlet weak var buttonDecline: UIButton!
    @IBOutlet weak var viewBg: UIView!
    @IBOutlet weak var contactIcon: UIImageView!
    @IBOutlet weak var contactNameLbl: UILabel!
    @IBOutlet weak var credentialsLbl: UILabel!
    @IBOutlet weak var warningLbl: UILabel!
    @IBOutlet weak var separator: UIView!

    var onDelete: (() -> Void)!
    var contact: Contact?
    var credentials: [Credential]?

    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
        viewBg.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR)
        buttonDecline.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON, borderWidth: 3,
                                      borderColor: UIColor.appRed.cgColor)
        buttonConfirm.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON)

        contactNameLbl.text = contact?.name
        contactIcon.applyDataImage(data: contact?.logo, placeholderNamed: "ico_placeholder_credential")
        var credentialsList = ""
        for credential in credentials ?? [] {
            credentialsList.append("• \(credential.credentialName)")
        }
        credentialsLbl.text = credentialsList
        let emptyCredentials = credentials?.isEmpty ?? true
        warningLbl.text = emptyCredentials ? "" : "contacts_delete_description".localize()
        separator.isHidden = emptyCredentials
    }

    static func makeThisView() -> DeleteContactViewController {
        let storyboard = UIStoryboard(name: "DeleteContact", bundle: nil)
        if let viewcontroller = storyboard.instantiateViewController(withIdentifier: "DeleteContact")
            as? DeleteContactViewController {
            return viewcontroller
        }
        return DeleteContactViewController()
    }

    let presentr: Presentr = {

        let presenter = Presentr(presentationType: .alert)
        presenter.transitionType = TransitionType.coverHorizontalFromRight
        presenter.dismissOnSwipe = false

        let width = ModalSize.sideMargin(value: 21.0)
        let height = ModalSize.custom(size: 400)
        let center = ModalCenterPosition.center
        presenter.presentationType = .custom(width: width, height: height, center: center)
        presenter.transitionType = nil
        presenter.dismissTransitionType = nil
        presenter.dismissAnimated = true

        return presenter
    }()

    func config(contact: Contact, credentials: [Credential]?, onDelete: @escaping () -> Void) {
        self.onDelete = onDelete
        self.contact = contact
        self.credentials = credentials
    }

    // MARK: Component delegates

    @IBAction func actionConfirmButtonTapped(_ sender: Any) {
        self.dismiss(animated: true, completion: nil)
        self.onDelete()
    }

    @IBAction func actionDeclineButtonTapped(_ sender: Any) {
        self.dismiss(animated: true, completion: nil)
    }

    // MARK: Presentr Delegate

    func presentrShouldDismiss(keyboardShowing: Bool) -> Bool {
        return false
    }

}
