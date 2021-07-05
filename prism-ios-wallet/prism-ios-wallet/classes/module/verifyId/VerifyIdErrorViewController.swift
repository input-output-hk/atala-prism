//
//  VerifyIdErrorViewController.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 22/06/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import UIKit

class VerifyIdErrorViewController: BaseViewController {

    var presenterImpl = VerifyIdErrorPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    @IBOutlet weak var confirmBttn: UIButton!
    @IBOutlet weak var retryBttn: UIButton!

    var navBar: NavBarCustomStyle = NavBarCustomStyle(hasNavBar: true, title: "verifyid_error_title".localize(),
                                                      hasBackButton: true)
    override func navBarCustomStyle() -> NavBarCustomStyle {
        return navBar
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        setupButtons()
    }

    // MARK: Buttons

    func setupButtons() {
        confirmBttn.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON)
        retryBttn.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON, borderWidth: 3,
                                  borderColor: UIColor.appRed.cgColor)
    }

    @IBAction func manualReviewTapped(_ sender: Any) {
        presenterImpl.manualReviewTapped()
    }

    @IBAction func retryTapped(_ sender: Any) {
        presenterImpl.retryTapped()
    }

    // MARK: Screens

    func changeScreenToPending() {
        ViewControllerUtils.changeScreenSegued(caller: self, segue: "VerifyIdPendingSegue", params: [true])
    }

}
