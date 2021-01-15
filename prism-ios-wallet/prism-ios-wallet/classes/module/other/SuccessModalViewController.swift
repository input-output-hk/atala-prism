//
//  SuccessModalViewController.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 12/01/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import UIKit
import Presentr

class SuccessModalViewController: UIViewController {

    @IBOutlet weak var viewBg: UIView!
    @IBOutlet weak var descLbl: UILabel!
    @IBOutlet weak var titleLbl: UILabel!
    @IBOutlet weak var buttonOk: UIButton!

    var onOk: (() -> Void)?
    var subtitleStr: String?
    var titleStr: String?

    override func viewDidLoad() {
        super.viewDidLoad()
        viewBg.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR)
        buttonOk.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON)
        titleLbl.text = titleStr
        descLbl.text = subtitleStr
    }

    static func makeThisView() -> SuccessModalViewController {
        let storyboard = UIStoryboard(name: "Success", bundle: nil)
        if let viewcontroller = storyboard.instantiateViewController(withIdentifier: "SuccessModal")
            as? SuccessModalViewController {
            return viewcontroller
        }
        return SuccessModalViewController()
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

    func config(subtitle: String?, title: String?, onOk: (() -> Void)? = nil) {
        self.onOk = onOk
        self.subtitleStr = subtitle
        self.titleStr = title
    }

    // MARK: Component delegates

    @IBAction func actionOkTapped(_ sender: Any) {
        self.dismiss(animated: true, completion: nil)
        if let onOk = onOk {
            onOk()
        }
    }
}
