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
    func payIdTapped(for cell: HomeProfileTableViewCell)
    func setup(for cell: HomeProfileTableViewCell)
}

class HomeProfileTableViewCell: BaseTableViewCell {

    @IBOutlet weak var heyLbl: UILabel!
    @IBOutlet weak var nameLbl: UILabel!
    @IBOutlet weak var profileImg: UIImageView!
    @IBOutlet weak var verifiedImg: UIImageView!
    @IBOutlet weak var notificationsBttn: UIButton!
    @IBOutlet weak var payIdLbl: UILabel!
    @IBOutlet weak var payIdView: UIView!
    @IBOutlet weak var notifBttnBotomCtrt: NSLayoutConstraint!
    @IBOutlet weak var bgBotomCtrt: NSLayoutConstraint!

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

    func config(name: String, picture: Data?, notifications: Int, payId: PayId?,
                delegate: HomeProfileTableViewCellDelegate? = nil) {

        self.delegate = delegate
        if name == " " {
            nameLbl.text = "home_profile_name_empty".localize()
            heyLbl.text = "home_profile_hey_empty".localize()
            verifiedImg.isHidden = true
        } else {
            nameLbl.text = name
            heyLbl.text = "home_profile_hey".localize()
        }
        profileImg.applyDataImage(data: picture, placeholderNamed: "ico_placeholder_user")
        profileImg.addRoundCorners(radius: 49, borderWidth: 4, borderColor: UIColor.appWhite.cgColor)
        notificationsBttn.addRoundCorners(radius: 18, borderWidth: 1, borderColor: UIColor.appAqua.cgColor)
        let title = notifications == 0
            ? "home_profile_notifications_empty".localize()
            : String(format: "home_profile_notifications".localize(), notifications)
        notificationsBttn.setTitle(title, for: .normal)

        payIdView.addRoundCorners(radius: 10)
        payIdView.addDropShadow(radius: 8, opacity: 0.15, offset: CGSize(width: 0, height: 4), color: .appBlack)
        payIdView.isHidden = payId == nil
        notifBttnBotomCtrt.constant = payId == nil ? 27 : 52
        bgBotomCtrt.constant = payId == nil ? 0 : 50
        payIdLbl.text = "\(payId?.name ?? "")\("pay_id_setup_name_field_right".localize())"

    }

    @IBAction func profileTapped(_ sender: Any) {
        delegateImpl?.profileTapped(for: self)
    }

    @IBAction func notificationsTapped(_ sender: Any) {
        delegateImpl?.notificationsTapped(for: self)
    }

    @IBAction func payIdTapped(_ sender: Any) {
        delegateImpl?.payIdTapped(for: self)
    }

}
