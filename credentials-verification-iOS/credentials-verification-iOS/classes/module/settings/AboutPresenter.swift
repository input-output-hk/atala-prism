//
//  AboutPresenter.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 26/02/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import UIKit

class AboutPresenter: BasePresenter {
    
    var viewImpl: AboutViewController? {
        return view as? AboutViewController
    }
    
    func tappedLegalCloseButton() {
        viewImpl?.showLegalView(doShow: false, urlStr: nil)
    }
    
    func tappedOpenTerms() {
        viewImpl?.showLegalView(doShow: true, urlStr: Common.URL_TERMS)
    }
    
    func tappedOpenPrivacy() {
        viewImpl?.showLegalView(doShow: true, urlStr: Common.URL_PRIVACY)
    }
}
