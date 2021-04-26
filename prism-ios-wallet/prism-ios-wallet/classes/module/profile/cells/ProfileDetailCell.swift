//
//  ProfileDetailCell.swift
//  prism-ios-wallet
//
//  Created by Roberto Daviduk on 12/03/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation

protocol ProfileDetailCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: ProfileDetailCell)
}

class ProfileDetailCell: BaseTableViewCell {

    
    @IBOutlet weak var labelFieldType: UILabel!
    @IBOutlet weak var labelFieldValue: UILabel!
    @IBOutlet weak var viewField: UIView!

    override class func default_NibName() -> String {
        return "ProfileDetailCell"
    }

    var delegateImpl: ProfileDetailCellPresenterDelegate? {
        return delegate as? ProfileDetailCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
        viewField.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR)
    }
    
    // MARK: Config
    
    func config(type: String?, value: String?) {

        labelFieldType.text = type
        labelFieldValue.text = value
    }
}
