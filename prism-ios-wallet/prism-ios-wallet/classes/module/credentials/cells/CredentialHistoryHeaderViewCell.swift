//
//  CredentialHistoryHeaderViewCell.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 14/09/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import UIKit

protocol CredentialHistoryHeaderViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: CredentialHistoryHeaderViewCell)
}

class CredentialHistoryHeaderViewCell: BaseTableViewCell {

    @IBOutlet weak var viewMainBody: UIView!
    @IBOutlet weak var imageLogo: UIImageView!
    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var labelDate: UILabel!

    override class func default_NibName() -> String {
        return "CredentialHistoryHeaderViewCell"
    }

    var delegateImpl: CredentialHistoryHeaderViewCellPresenterDelegate? {
        return delegate as? CredentialHistoryHeaderViewCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
        viewMainBody.addRoundCorners(radius: 6)
        viewMainBody.addDropShadow()
    }

    // MARK: Config

    func config(title: String, date: Date, icon: String) {

        labelTitle.text = title
        // Logo image
        imageLogo.applyDataImage(data: nil, placeholderNamed: icon)
        labelDate.text = String(format: "credentials_detail_date".localize(),
                                date.dateTimeString())
    }

}
