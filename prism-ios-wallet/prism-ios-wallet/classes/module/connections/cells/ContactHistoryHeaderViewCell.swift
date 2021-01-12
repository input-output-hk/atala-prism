//
//  ContactHistoryHeaderViewCell.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 22/09/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import UIKit

protocol ContactHistoryHeaderViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: ContactHistoryHeaderViewCell)
}

class ContactHistoryHeaderViewCell: BaseTableViewCell {

    @IBOutlet weak var viewMainBody: UIView!
    @IBOutlet weak var imageLogo: UIImageView!
    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var labelSubtitle: UILabel!
    @IBOutlet weak var labelDate: UILabel!

    override class func default_NibName() -> String {
        return "ContactHistoryHeaderViewCell"
    }

    var delegateImpl: ContactHistoryHeaderViewCellPresenterDelegate? {
        return delegate as? ContactHistoryHeaderViewCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
        viewMainBody.addRoundCorners(radius: 6)
        viewMainBody.addDropShadow()
    }

    // MARK: Config

    func config(title: String, subtitle: String, date: Date, icon: Data?) {

        labelTitle.text = title
        labelSubtitle.text = String(format: "contacts_detail_did".localize(), subtitle)
        // Logo image
        imageLogo.applyDataImage(data: icon, placeholderNamed: "icon_id")
        labelDate.text = String(format: "contacts_detail_date_connected".localize(),
                                date.dateTimeString())
    }

}
