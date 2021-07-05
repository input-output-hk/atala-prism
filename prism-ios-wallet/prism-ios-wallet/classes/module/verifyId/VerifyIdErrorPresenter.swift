//
//  VerifyIdErrorPresenter.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 22/06/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation

class VerifyIdErrorPresenter: BasePresenter {

    var viewImpl: VerifyIdErrorViewController? {
        return view as? VerifyIdErrorViewController
    }

    // MARK: Buttons

    func retryTapped() {
        viewImpl?.onBackPressed()
    }

    func manualReviewTapped() {
        let loggedUser = sharedMemory.loggedUser
        loggedUser?.verifyIdManualPending = true
        sharedMemory.loggedUser = loggedUser
        // TODO: request manual review from server when implemented
        viewImpl?.changeScreenToPending()
    }
}
