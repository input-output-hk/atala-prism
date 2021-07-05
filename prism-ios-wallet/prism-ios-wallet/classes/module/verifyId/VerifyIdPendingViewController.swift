//
//  VerifyIdPendingViewController.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 23/06/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import UIKit

class VerifyIdPendingViewController: BaseViewController {

    var presenterImpl = VerifyIdPendingPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    @IBOutlet weak var closeBttn: UIButton!

    var navBar: NavBarCustomStyle = NavBarCustomStyle(hasNavBar: true, title: "verifyid_pending_title".localize(),
                                                      hasBackButton: true)
    override func navBarCustomStyle() -> NavBarCustomStyle {
        return navBar
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        setupButtons()
    }
    
    func goToMainScreen() {
        MainNavViewController.openThisView()
    }

    // MARK: Buttons

    func setupButtons() {
        closeBttn.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON)
    }

    @IBAction func closeTapped(_ sender: Any) {
        presenterImpl.closeTapped()
    }

}

extension VerifyIdPendingViewController: SegueableScreen {

    func configScreenFromSegue(params: [Any?]?) {

        if let isFlow = params?[0] as? Bool {
            self.presenterImpl.isFlow = isFlow
        }
    }
}
