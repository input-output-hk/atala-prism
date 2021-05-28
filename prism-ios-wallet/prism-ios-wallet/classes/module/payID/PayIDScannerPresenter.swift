//
//  PayIDScannerPresenter.swift
//  prism-ios-wallet
//
//  Created by Roberto Daviduk on 30/03/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation

class PayIDScannerPresenter: BasePresenter {
  
    var viewImpl: PayIDScannerViewController? {
        return view as? PayIDScannerViewController
    }
    
    // MARK: Buttons

    @discardableResult
    func tappedBackButton() -> Bool {

        return false
    }
}
