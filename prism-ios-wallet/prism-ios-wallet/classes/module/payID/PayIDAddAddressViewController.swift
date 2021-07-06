//
//  PayIDAddAddressViewController.swift
//  prism-ios-wallet
//
//  Created by Roberto Daviduk on 25/03/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation
import UIKit

protocol PayIDAddAddressDelegate{
    func addAddress(value: String)
}

class PayIDAddAddressViewController: UIViewController, UITextFieldDelegate, SegueableScreen {
    
    @IBOutlet weak var topView: UIView!
    @IBOutlet weak var contentView: UIView!
    @IBOutlet weak var scanTextField: TextFieldTitledView!
    @IBOutlet weak var addButton: UIButton!

    var canContinue: Bool = false

    lazy var scanner: QRCode = { QRCode() }()

    var delegate: PayIDAddAddressDelegate?

    lazy var actionToDismiss = SelectorAction(action: { [weak self] in

        if !(self?.scanTextField.textField.isFirstResponder)! {
            self?.dismiss(animated: true, completion: nil)
        }
    })
    
    override func viewDidLoad() {
        super.viewDidLoad()

        ViewControllerUtils.addTapToDismissKeyboard(view: self)
        topView.addOnClickListener(action: actionToDismiss)

        contentView.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR, onlyTops: true)
    
        scanTextField.config(title: "pay_id_setup_scan_field".localize())
        scanTextField.textField.delegate = self
        scanTextField.textField.addRightViewWith(image: UIImage.init(named: "ico_qr")!)
        
        addButton.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        setupView()
        setupQRScanner()
    }
    
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        
        view.layoutIfNeeded()
    }
    
    // MARK: Segue Value

    func configScreenFromSegue(params: [Any?]?) {
        delegate = params?[0] as? PayIDAddAddressDelegate
    }

    func setupView() {

        self.view.backgroundColor = UIColor.clear
        self.view.backgroundColor = UIColor.black.withAlphaComponent(0.4)
    }
    
    func setupQRScanner() {
        scanner.prepareScan(self.view) { (value) in
            print("Code: ", value)
        }
        scanner.scanFrame = self.view.frame
        scanner.autoRemoveSubLayers = true
        scanner.lineWidth = 0
        scanner.strokeColor = UIColor.appRed
        scanner.maxDetectedCount = 1
    }

    func scanQR() {
        scanner.clearDrawLayer()
        scanner.startScan()
    }

    func stopQrScan() {
        scanner.stopScan()
    }

    func textFieldDidEndEditing(_ textField: UITextField) {

        if scanTextField.textField.text?.count > 0 {
            addButton.backgroundColor = UIColor.appRed
            canContinue = true
        } else {
            addButton.backgroundColor = UIColor.appGreyBlue
            canContinue = false
        }
    }
    
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {

        textField.resignFirstResponder()
        return true
    }

    @IBAction func add(_ sender: Any) {

        if canContinue {

            self.dismiss(animated: true) {

                self.delegate?.addAddress(value: self.scanTextField.textField.text ?? "")
            }
        }
    }
}
