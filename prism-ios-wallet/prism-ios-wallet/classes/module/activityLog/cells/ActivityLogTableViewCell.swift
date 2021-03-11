//
//  ActivityLogTableViewCell.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 19/08/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import UIKit

protocol ActivityLogViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: ActivityLogTableViewCell)
}

class ActivityLogTableViewCell: BaseTableViewCell {

    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var labelSubtitle: UILabel!
    @IBOutlet weak var imageLogo: UIImageView!
    @IBOutlet weak var labelDate: UILabel!

    override class func default_NibName() -> String {
        return "ActivityLogTableViewCell"
    }

    var delegateImpl: ActivityLogViewCellPresenterDelegate? {
        return delegate as? ActivityLogViewCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
    }

    // MARK: Config

    func config(history: ActivityHistory) {
        
        labelTitle.text = history.detail
        labelSubtitle.text = history.typeName
        imageLogo.image = history.logo
        labelDate.text = history.timestamp?.dateTimeString()
    }

}
