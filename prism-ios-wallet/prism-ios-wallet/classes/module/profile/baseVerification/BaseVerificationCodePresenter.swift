//
//  BaseVerificationCodePresenter.swift
//  prism-ios-wallet
//
//  Created by Roberto Daviduk on 04/03/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation

class BaseVerificationCodePresenter: BasePresenter {
    
    var viewImpl: BaseVerificationCodeViewController? {
        return view as? BaseVerificationCodeViewController
    }
    
    // MARK: Buttons

    @discardableResult
    func tappedBackButton() -> Bool {

        return false
    }
}
