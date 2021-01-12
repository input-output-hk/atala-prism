//
//  DateFormatTableViewCell.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 02/09/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import UIKit

protocol DateFormatTableViewCellDelegate: BaseTableViewCellPresenterDelegate {

    func itemTapped(for cell: DateFormatTableViewCell)
    func setup(for cell: DateFormatTableViewCell)
}

class DateFormatTableViewCell: BaseTableViewCell {

    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var viewTick: UIImageView!
    @IBOutlet weak var viewBg: UIView!

    var delegateImpl: DateFormatTableViewCellDelegate? {
        return delegate as? DateFormatTableViewCellDelegate
    }

    class var reuseIdentifier: String {
        return "DateFormat"
    }

    class var nibName: String {
        return "DateFormatViewCell"
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
    }

    // MARK: Config

    func config(name: String, isSelected: Bool, delegate: DateFormatTableViewCellDelegate? = nil) {

        self.delegate = delegate

        self.labelTitle.text = name
        self.labelTitle.textColor = isSelected ? .appBlack : .appGreyBlue
        self.viewTick.image = isSelected ? #imageLiteral(resourceName: "ico_share_tick") : #imageLiteral(resourceName: "ico_share_empty")
        self.viewBg.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR,
                                    borderWidth: isSelected ? 2 : 0, borderColor: UIColor.appRed.cgColor)
        self.viewBg.addDropShadow(radius: 4)
    }

    @IBAction func actionItemTapped(_ sender: Any) {
        delegateImpl?.itemTapped(for: self)
    }
}
