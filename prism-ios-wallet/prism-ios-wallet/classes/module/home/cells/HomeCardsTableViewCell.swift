//
//  HmeeCardsTableViewCell.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 23/03/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import UIKit

protocol HomeCardsTableViewCellDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: HomeCardsTableViewCell)
    func payIdTapped(for cell: HomeCardsTableViewCell)
    func dismissPayIdTapped(for cell: HomeCardsTableViewCell)
    func verifyIdTapped(for cell: HomeCardsTableViewCell)
    func dismissVerifyIdTapped(for cell: HomeCardsTableViewCell)
}

class HomeCardsTableViewCell: BaseTableViewCell {

    @IBOutlet weak var payIdBg: UIView!
    @IBOutlet weak var payIdBannerBg: UIView!
    @IBOutlet weak var payIdDescLbl: UILabel!
    @IBOutlet weak var verifyBg: UIView!
    @IBOutlet weak var verifyBannerBg: UIView!
    @IBOutlet weak var verifyDescLbl: UILabel!
    @IBOutlet weak var scrollHeighCtr: NSLayoutConstraint!
    @IBOutlet weak var scroll: UIScrollView!

    var delegateImpl: HomeCardsTableViewCellDelegate? {
        return delegate as? HomeCardsTableViewCellDelegate
    }

    class var reuseIdentifier: String {
        return "HomeCards"
    }

    class var nibName: String {
        return "HomeCardsTableViewCell"
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
    }

    // MARK: Config

    func config(hidePayId: Bool, hideVerifyId: Bool, delegate: HomeActivityLogHeaderTableViewCellDelegate? = nil) {

        self.delegate = delegate
        payIdBg.addRoundCorners(radius: 10)
        payIdBg.addDropShadow(radius: 12, opacity: 0.1, offset: CGSize(width: 0, height: 0), color: .appBlack)
        payIdBannerBg.addRoundCorners(radius: 8)
        verifyBg.addRoundCorners(radius: 10)
        verifyBg.addDropShadow(radius: 14, opacity: 0.1, offset: CGSize(width: 0, height: 0), color: .appBlack)
        verifyBannerBg.addRoundCorners(radius: 8)

        let desc = NSMutableAttributedString(string: "home_payid_subtitle_first".localize())
        desc.append(NSMutableAttributedString(string: "home_payid_subtitle_bold".localize(),
                                              attributes: [.font: UIFont.boldSystemFont(ofSize: 12)]))
        desc.append(NSMutableAttributedString(string: "home_payid_subtitle_second".localize()))

        payIdDescLbl.attributedText = desc
        verifyDescLbl.attributedText = desc

        payIdBg.isHidden = hidePayId
        verifyBg.isHidden = hideVerifyId

        scrollHeighCtr.constant = hidePayId && hideVerifyId ? 0 : 204
        scroll.isHidden = hidePayId && hideVerifyId
    }

    // MARK: Buttons

    @IBAction func payIdTapped(_ sender: Any) {
        delegateImpl?.payIdTapped(for: self)
    }

    @IBAction func dismissPayIdTapped(_ sender: Any) {
        delegateImpl?.dismissPayIdTapped(for: self)
    }

    @IBAction func verifyIdTapped(_ sender: Any) {
        delegateImpl?.verifyIdTapped(for: self)
    }

    @IBAction func dismissVerifyIdTapped(_ sender: Any) {
        delegateImpl?.dismissVerifyIdTapped(for: self)
    }
}
