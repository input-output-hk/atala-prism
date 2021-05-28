//
//  PayIDInfoListingCell.swift
//  prism-ios-wallet
//
//  Created by Roberto Daviduk on 23/03/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation

protocol PayIDInfoListingCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: PayIDInfoListingCell)
}

class PayIDInfoListingCell: BaseTableViewCell {
    
    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var labelValue: UILabel!

    override class func default_NibName() -> String {
        return "PayIDInfoListingCell"
    }

    var delegateImpl: PayIDInfoListingCellPresenterDelegate? {
        return delegate as? PayIDInfoListingCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
    }

    // MARK: Config

    func config(title: String?, value: String?) {

        labelTitle.text = title
        labelValue.text = value
    }
    
    @IBAction func copyValue(_ sender: Any) {
        UIPasteboard.general.string = labelValue.text
    }
}
