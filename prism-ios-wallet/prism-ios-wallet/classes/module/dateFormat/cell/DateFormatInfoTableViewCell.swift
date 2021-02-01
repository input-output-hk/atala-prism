//
//  DateFormatInfoTableViewCell.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 28/01/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import UIKit

protocol DateFormatInfoTableViewCellDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: DateFormatInfoTableViewCell)
}

class DateFormatInfoTableViewCell: BaseTableViewCell {

    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var viewBg: UIView!

    var delegateImpl: DateFormatInfoTableViewCellDelegate? {
        return delegate as? DateFormatInfoTableViewCellDelegate
    }

    class var reuseIdentifier: String {
        return "DateInfoFormat"
    }

    class var nibName: String {
        return "DateFormatInfoTableViewCell"
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
    }

    // MARK: Config

    func config(title: NSAttributedString, delegate: DateFormatTableViewCellDelegate? = nil) {

        self.delegate = delegate

        self.labelTitle.attributedText = title
        self.viewBg.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR,
                                    borderWidth: 1, borderColor: UIColor.appGreyMid.cgColor)
    }
}
