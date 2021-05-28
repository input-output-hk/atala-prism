//
//  PayIDWelcomePresenter.swift
//  prism-ios-wallet
//
//  Created by Roberto Daviduk on 17/03/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation

class PayIDWelcomePresenter: BasePresenter {
  
    var viewImpl: PayIDWelcomeViewController? {
        return view as? PayIDWelcomeViewController
    }
    
    // MARK: Buttons

    @discardableResult
    func tappedBackButton() -> Bool {

        return false
    }
}
