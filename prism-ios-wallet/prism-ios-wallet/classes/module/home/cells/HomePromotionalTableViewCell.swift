//
//  HomePromotionalTableViewCell.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 24/02/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import UIKit

protocol HomePromotionalTableViewCellDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: HomePromotionalTableViewCell)
    func promotionalMoreInfoTapped(for cell: HomePromotionalTableViewCell)
    func promotionalShareTapped(for cell: HomePromotionalTableViewCell)
}

class HomePromotionalTableViewCell: BaseTableViewCell {

    @IBOutlet weak var moreInfoBg: UIView!
    @IBOutlet weak var moreInfoBttn: UIButton!
    @IBOutlet weak var shareBttn: UIButton!

    var delegateImpl: HomePromotionalTableViewCellDelegate? {
        return delegate as? HomePromotionalTableViewCellDelegate
    }

    class var reuseIdentifier: String {
        return "HomePromotional"
    }

    class var nibName: String {
        return "HomePromotionalTableViewCell"
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
    }

    // MARK: Config

    func config(delegate: HomeActivityLogHeaderTableViewCellDelegate? = nil) {

        self.delegate = delegate
        moreInfoBg.addRoundCorners(radius: 30, onlyLefts: true)
        moreInfoBttn.addRoundCorners(radius: 12.5, borderWidth: 1, borderColor: UIColor.appRed.cgColor)
        shareBttn.addRoundCorners(radius: 22.5, borderWidth: 1, borderColor: UIColor.appWhite.cgColor)
    }

    // MARK: Buttons

    @IBAction func moreInfoTapped(_ sender: Any) {
        delegateImpl?.promotionalMoreInfoTapped(for: self)
    }

    @IBAction func shareTapped(_ sender: Any) {
        delegateImpl?.promotionalShareTapped(for: self)
    }
}
