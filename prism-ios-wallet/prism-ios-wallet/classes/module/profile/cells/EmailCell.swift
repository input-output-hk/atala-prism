//
//  AttributeListingCell.swift
//  prism-ios-wallet
//
//  Created by Roberto Daviduk on 08/03/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation

protocol AttributeListingCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: AttributeListingCell)
}

class AttributeListingCell: BaseTableViewCell {

    
    @IBOutlet weak var labelTitleType: UILabel!
    @IBOutlet weak var labelFieldType: UILabel!
    @IBOutlet weak var labelFieldValue: UILabel!
    @IBOutlet weak var viewField: UIView!

    override class func default_NibName() -> String {
        return "AttributeListingCell"
    }

    var delegateImpl: AttributeListingCellPresenterDelegate? {
        return delegate as? AttributeListingCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
        viewField.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR)
    }
    
    // MARK: Config
    
    func config(type: String?, value: String?) {

        labelTitleType.text = type
        labelFieldType.text = type
        labelFieldValue.text = value
    }
}
