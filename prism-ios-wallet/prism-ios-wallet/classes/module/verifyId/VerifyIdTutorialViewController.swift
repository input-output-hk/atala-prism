//
//  VerifyIdTutorialViewController.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 29/01/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import UIKit

class VerifyIdTutorialViewController: BaseViewController {

    var presenterImpl = VerifyIdTutorialPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    @IBOutlet weak var continueBttn: UIButton!

    override func navBarCustomStyle() -> NavBarCustomStyle {
        return NavBarCustomStyle(hasNavBar: false)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        continueBttn.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON)
    }

    // MARK: Screens

    func goToMainScreen() {
        MainNavViewController.openThisView()
    }

    func changeScreenToTypeSelect() {
        _ = app_mayPerformSegue(withIdentifier: "VerifyIdTypeSelectSegue", sender: self)
    }

    // MARK: Buttons

    @IBAction func continueTapped(_ sender: Any) {
        presenterImpl.continueTapped()
    }

    @IBAction func skipTapped(_ sender: Any) {
        presenterImpl.skipTapped()
    }
}
