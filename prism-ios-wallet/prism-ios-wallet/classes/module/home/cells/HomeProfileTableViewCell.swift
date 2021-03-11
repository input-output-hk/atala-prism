//
//  HomeProfileTableViewCell.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 22/02/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import UIKit

protocol HomeProfileTableViewCellDelegate: BaseTableViewCellPresenterDelegate {

    func profileTapped(for cell: HomeProfileTableViewCell)
    func notificationsTapped(for cell: HomeProfileTableViewCell)
    func setup(for cell: HomeProfileTableViewCell)
}

class HomeProfileTableViewCell: BaseTableViewCell {

    @IBOutlet weak var nameLbl: UILabel!
    @IBOutlet weak var profileImg: UIImageView!
    @IBOutlet weak var notificationsBttn: UIButton!

    var delegateImpl: HomeProfileTableViewCellDelegate? {
        return delegate as? HomeProfileTableViewCellDelegate
    }

    class var reuseIdentifier: String {
        return "HomeProfile"
    }

    class var nibName: String {
        return "HomeProfileTableViewCell"
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
    }

    // MARK: Config

    func config(name: String, picture: Data?, notifications: Int, delegate: HomeProfileTableViewCellDelegate? = nil) {

        self.delegate = delegate
        nameLbl.text = name
        profileImg.applyDataImage(data: picture, placeholderNamed: "ico_placeholder_user")
        profileImg.addRoundCorners(radius: 49, borderWidth: 4, borderColor: UIColor.appWhite.cgColor)
        notificationsBttn.addRoundCorners(radius: 18, borderWidth: 1, borderColor: UIColor.appAqua.cgColor)
        let title = notifications == 0
            ? "home_profile_notifications_empty".localize()
            : String(format: "home_profile_notifications".localize(), notifications)
        notificationsBttn.setTitle(title, for: .normal)
    }

    @IBAction func profileTapped(_ sender: Any) {
        delegateImpl?.profileTapped(for: self)
    }

    @IBAction func notificationsTapped(_ sender: Any) {
        delegateImpl?.notificationsTapped(for: self)
    }

}
