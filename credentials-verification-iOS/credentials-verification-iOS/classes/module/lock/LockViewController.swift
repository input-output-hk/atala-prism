//
//  LockViewController.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 30/04/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import UIKit

class LockViewController: BaseViewController {
    
    @IBOutlet weak var pinDigOneTf: UITextField!
    @IBOutlet weak var pinDigTwoTf: UITextField!
    @IBOutlet weak var pinDigThreeTf: UITextField!
    @IBOutlet weak var pinDigFourTf: UITextField!
    @IBOutlet weak var biometricsBttn: UIButton!
    @IBOutlet var keyboardButtons: [UIButton]!
    @IBOutlet weak var panelView: UIView!
    
    var presenterImpl = LockPresenter()
    override var presenter: BasePresenter { return presenterImpl }
    
    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        setupButtons()
        setupViews()
        if sharedMemory.loggedUser?.appBiometrics ?? false {
            presenterImpl.tappedBiometrics()
        }

    }
    
    func setupButtons() {
        keyboardButtons.forEach { $0.addRoundCorners(radius: 4) }
        biometricsBttn.isHidden = true
    }
    
    func setupViews() {
        panelView.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON, onlyTops: true)
    }
    
    func disableBiometrics() {
        biometricsBttn.isHidden = true
    }
    
    // MARK: Buttons
    
    @IBAction func actionBiometrics(_ sender: Any) {
        presenterImpl.tappedBiometrics()
    }
    
    @IBAction func actionNumber(_ sender: UIButton) {
        presenterImpl.tappedNumber(digit: (sender.titleLabel?.text)!)
    }
    
    @IBAction func actionBackspace(_ sender: Any) {
        presenterImpl.tappedBackspace()
    }
}
