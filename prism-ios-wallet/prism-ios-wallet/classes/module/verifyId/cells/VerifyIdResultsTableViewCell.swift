//
//  VerifyIdResultsTableViewCell.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 08/02/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import UIKit

protocol VerifyIdResultsCellDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: VerifyIdResultsTableViewCell)
}

class VerifyIdResultsTableViewCell: BaseTableViewCell {

    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var labelValue: UILabel!

    class var reuseIdentifier: String {
        return "VerifyIdResults"
    }

    override class func default_NibName() -> String {
        return "VerifyIdResultsTableViewCell"
    }

    var delegateImpl: VerifyIdResultsCellDelegate? {
        return delegate as? VerifyIdResultsCellDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
    }

    func config(title: String, value: String?) {

        self.labelTitle.text = title
        self.labelValue.text = value
        self.backgroundColor = (indexPath?.row ?? 0) % 2 == 0 ? .appGreyLight : .appWhite
    }
}
