//
//  VerifyIdPendingPresenter.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 23/06/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation

class VerifyIdPendingPresenter: BasePresenter {

    var viewImpl: VerifyIdPendingViewController? {
        return view as? VerifyIdPendingViewController
    }
    
    var isFlow = false

    // MARK: Buttons

    func closeTapped() {
        if isFlow {
            viewImpl?.goToMainScreen()
        } else {
            viewImpl?.onBackPressed()
        }
    }
}
