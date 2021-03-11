//
//  HomeActivityLogHeaderTableViewCell.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 23/02/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import UIKit

protocol HomeActivityLogHeaderTableViewCellDelegate: BaseTableViewCellPresenterDelegate {

    func activityLogTapped(for cell: HomeActivityLogHeaderTableViewCell)
    func setup(for cell: HomeActivityLogHeaderTableViewCell)
}

class HomeActivityLogHeaderTableViewCell: BaseTableViewCell {

    @IBOutlet weak var activityLogBttn: UIButton!
    @IBOutlet weak var emptyImg: UIImageView!

    var delegateImpl: HomeActivityLogHeaderTableViewCellDelegate? {
        return delegate as? HomeActivityLogHeaderTableViewCellDelegate
    }

    class var reuseIdentifier: String {
        return "HomeActivityLogHeader"
    }

    class var nibName: String {
        return "HomeActivityLogHeaderTableViewCell"
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
    }

    // MARK: Config

    func config(empty: Bool, delegate: HomeActivityLogHeaderTableViewCellDelegate? = nil) {

        self.delegate = delegate
        activityLogBttn.addRoundCorners(radius: 12.5, borderWidth: 1,
                                        borderColor: empty ? UIColor.appGreyBlue.cgColor : UIColor.appRed.cgColor)
        activityLogBttn.setTitleColor(empty ? UIColor.appGreyBlue : UIColor.appRed, for: .normal)
        activityLogBttn.isEnabled = !empty
        emptyImg.isHidden = !empty
    }
    
    // MARK: Buttons

    @IBAction func activityLogTapped(_ sender: Any) {
        delegateImpl?.activityLogTapped(for: self)
    }

}
