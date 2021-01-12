//
//  CredentialSharedViewCellViewCell.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 14/09/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import UIKit

protocol CredentialSharedViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: CredentialSharedViewCellViewCell)
}

class CredentialSharedViewCellViewCell: BaseTableViewCell {

    @IBOutlet weak var viewMainBody: UIView!
    @IBOutlet weak var imageLogo: UIImageView!
    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var labelDate: UILabel!

    override class func default_NibName() -> String {
        return "CredentialSharedViewCellViewCell"
    }

    var delegateImpl: CredentialSharedViewCellPresenterDelegate? {
        return delegate as? CredentialSharedViewCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
        viewMainBody.addRoundCorners(radius: 6)
        viewMainBody.addDropShadow()
    }

    // MARK: Config

    func config(title: String, date: Date, logoData: Data?) {

        labelTitle.text = title
        // Logo image
        imageLogo.applyDataImage(data: logoData, placeholderNamed: "icon_id")
        labelDate.text = String(format: "credentials_history_date".localize(),
                                date.dateTimeString())
    }

}
