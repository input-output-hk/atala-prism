//
//  ContactDetailSharedViewCell.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 22/09/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import UIKit

protocol ContactDetailSharedViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: ContactDetailSharedViewCell)
}

class ContactDetailSharedViewCell: BaseTableViewCell {

    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var labelDate: UILabel!

    override class func default_NibName() -> String {
        return "ContactDetailSharedViewCell"
    }

    var delegateImpl: ContactDetailSharedViewCellPresenterDelegate? {
        return delegate as? ContactDetailSharedViewCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
    }

    // MARK: Config

    func config(title: String, date: Date, type: ActivityHistoryType) {

        labelTitle.text = "· \(title)"
        switch type {
        case .credentialAdded:
            labelDate.text = String(format: "contacts_detail_issued_date".localize(),
                                    date.dateTimeString())
        case .credentialRequested:
            labelDate.text = String(format: "contacts_detail_requested_date".localize(),
                                    date.dateTimeString())
        case .credentialShared:
            labelDate.text = String(format: "contacts_detail_shared_date".localize(),
                                    date.dateTimeString())
        default:
            break
        }
    }

}
