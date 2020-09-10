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

        switch history.typeEnum {
        case .contactAdded:
            labelTitle.text = history.contactName
            labelSubtitle.text = "activitylog_connected".localize()
            imageLogo.image = #imageLiteral(resourceName: "icon_connected")
        case .contactDeleted:
            labelTitle.text = history.contactName
            labelSubtitle.text = "activitylog_deleted".localize()
            imageLogo.image = #imageLiteral(resourceName: "icon_delete")
        case .credentialAdded:
            labelTitle.text = history.credentialName
            labelSubtitle.text = String(format: "activitylog_received".localize(), history.contactName ?? "")
            imageLogo.image = #imageLiteral(resourceName: "icon_received")
        case .credentialShared:
            labelTitle.text = history.credentialName
            labelSubtitle.text = String(format: "activitylog_shared".localize(), history.contactName ?? "")
            imageLogo.image = #imageLiteral(resourceName: "icon_shared")
        case .credentialRequested:
            labelTitle.text = history.credentialName
            labelSubtitle.text = String(format: "activitylog_requested".localize(), history.contactName ?? "")
            imageLogo.image = #imageLiteral(resourceName: "icon_shared")
        case .credentialDeleted:
            labelTitle.text = history.credentialName
            labelSubtitle.text = "activitylog_deleted".localize()
            imageLogo.image = #imageLiteral(resourceName: "icon_delete")
        case .undefined:
            print("Undefined type")
        }
        labelDate.text = history.timestamp?.dateTimeString()

    }

}
