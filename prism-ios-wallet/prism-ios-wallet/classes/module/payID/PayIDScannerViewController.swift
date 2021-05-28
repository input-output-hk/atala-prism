//
//  PayIDScannerViewController.swift
//  prism-ios-wallet
//
//  Created by Roberto Daviduk on 30/03/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation
import UIKit

protocol PayIDScannerDelegate{
    func send(value: String)
}

class PayIDScannerViewController: BaseViewController, SegueableScreen {
 
    @IBOutlet weak var scanView: UIView!
    @IBOutlet weak var scanButton: UIButton!

    var presenterImpl = PayIDScannerPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    lazy var scanner: QRCode = { QRCode() }()
    
    var delegate:PayIDScannerDelegate?

    override func viewDidLoad() {
        super.viewDidLoad()

        scanView.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR, borderWidth: 1.5, borderColor: UIColor.appWhite.cgColor)
        scanButton.addRoundCorners(radius: scanButton.frame.size.width / 2)
        
        setupQRScanner()
    }

    override func onBackPressed() -> Bool {
        if !presenterImpl.tappedBackButton() {
            return super.onBackPressed()
        }
        return false
    }
    
    // MARK: Segue Value
    
    func configScreenFromSegue(params: [Any?]?) {
        delegate = params?[0] as? PayIDScannerDelegate
    }
    
    // MARK: Scaner Setup
    
    func setupQRScanner() -> Void {
        scanner.prepareScan(self.scanView) { (value) in
            self.dismiss(animated: true) {
                self.delegate?.send(value: value)
            }
        }
        scanner.scanFrame = self.scanView.frame
        scanner.autoRemoveSubLayers = true
        scanner.lineWidth = 0
        scanner.strokeColor = UIColor.appRed
        scanner.maxDetectedCount = 1
        
        scanQR()
    }
    
    func scanQR() -> Void {
        scanner.clearDrawLayer()
        scanner.startScan()
    }

    func stopQrScan() {
        scanner.stopScan()
    }
    
    // MARK: Button Action

    @IBAction func stopScann(_ sender: Any) {
        stopQrScan()
        dismiss(animated: true, completion: nil)
    }
}
