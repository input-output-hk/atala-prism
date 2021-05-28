//
//  DeletePayIdViewController.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 28/04/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation
import Presentr

class DeletePayIdViewController: UIViewController {

    @IBOutlet weak var buttonConfirm: UIButton!
    @IBOutlet weak var buttonDecline: UIButton!
    @IBOutlet weak var viewBg: UIView!
    @IBOutlet weak var payIdName: UILabel!

    var onDelete: (() -> Void)!
    var payId: PayId?

    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
        viewBg.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR)
        buttonDecline.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON, borderWidth: 3,
                                      borderColor: UIColor.appRed.cgColor)
        buttonConfirm.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON)

        payIdName.text = "\(payId?.name ?? "")\("pay_id_setup_name_field_right".localize())"
    }

    static func makeThisView() -> DeletePayIdViewController {
        let storyboard = UIStoryboard(name: "PayID", bundle: nil)
        if let viewcontroller = storyboard.instantiateViewController(withIdentifier: "DeletePayId")
            as? DeletePayIdViewController {
            return viewcontroller
        }
        return DeletePayIdViewController()
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

    func config(payId: PayId?, onDelete: @escaping () -> Void) {
        self.onDelete = onDelete
        self.payId = payId
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

