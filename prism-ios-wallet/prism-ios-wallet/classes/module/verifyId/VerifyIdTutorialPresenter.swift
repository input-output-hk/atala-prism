//
//  VerifyIdTutorialPresenter.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 29/01/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation

class VerifyIdTutorialPresenter: BasePresenter {

    var viewImpl: VerifyIdTutorialViewController? {
        return view as? VerifyIdTutorialViewController
    }

    // MARK: Buttons

    func continueTapped() {
        viewImpl?.changeScreenToTypeSelect()
    }

    func skipTapped() {
        viewImpl?.goToMainScreen()
    }
}
