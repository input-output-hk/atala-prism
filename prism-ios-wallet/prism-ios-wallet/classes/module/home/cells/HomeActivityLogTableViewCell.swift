//
//  HomeActivityLogTableViewCell.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 23/02/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import UIKit

protocol HomeActivityLogTableViewCellDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: HomeActivityLogTableViewCell)
}

class HomeActivityLogTableViewCell: BaseTableViewCell {

    @IBOutlet weak var viewMainBody: UIView!
    @IBOutlet weak var imageLogo: UIImageView!
    @IBOutlet weak var typeLbl: UILabel!
    @IBOutlet weak var titleLbl: UILabel!
    @IBOutlet weak var dateLbl: UILabel!

    var delegateImpl: HomeActivityLogTableViewCellDelegate? {
        return delegate as? HomeActivityLogTableViewCellDelegate
    }

    class var reuseIdentifier: String {
        return "HomeActivityLog"
    }

    class var nibName: String {
        return "HomeActivityLogTableViewCell"
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
        viewMainBody.addRoundCorners(radius: 8)
        viewMainBody.addDropShadow(opacity: 0.1)
    }

    // MARK: Config

    func config(history: ActivityHistory, delegate: HomeActivityLogHeaderTableViewCellDelegate? = nil) {

        self.delegate = delegate

        titleLbl.text = history.detail
        typeLbl.text = history.typeName.uppercased()
        imageLogo.image = history.logo

        if let time = history.timestamp {
            let calendar = Calendar.current
            if calendar.isDateInToday(time) {
                let formatter = RelativeDateTimeFormatter()
                formatter.unitsStyle = .full
                dateLbl.text = formatter.localizedString(for: time, relativeTo: Date())
            } else {
                dateLbl.text = time.dateString()
            }
        }
    }

}
