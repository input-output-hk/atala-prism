//
//  PayIDWelcomeViewController.swift
//  prism-ios-wallet
//
//  Created by Roberto Daviduk on 17/03/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation
import UIKit

class PayIDWelcomeViewController: BaseViewController {
 
    @IBOutlet weak var continueButton: UIButton!

    var presenterImpl = PayIDWelcomePresenter()
    override var presenter: BasePresenter { return presenterImpl }

    override func viewDidLoad() {
        super.viewDidLoad()

        continueButton.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON)
    }

    override func onBackPressed() -> Bool {
        if !presenterImpl.tappedBackButton() {
            return super.onBackPressed()
        }
        return false
    }
    
    @IBAction func skip(_ sender: Any) {
        dismiss(animated: true, completion: nil)
    }
    
    @IBAction func skipNavBar(_ sender: Any) {
        dismiss(animated: true, completion: nil)
    }
}
