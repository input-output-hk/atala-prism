//
//  ProfileDetailHeaderCell.swift
//  prism-ios-wallet
//
//  Created by Roberto Daviduk on 12/03/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation

class ProfileDetailHeaderCell: BaseTableViewCell {

    override class func default_NibName() -> String {
        return "ProfileDetailHeaderCell"
    }

    var delegateImpl: AttributeListingCellPresenterDelegate? {
        return delegate as? AttributeListingCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
    }
}
