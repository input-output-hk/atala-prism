//
//  CredentialSectionViewCell.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 14/09/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import UIKit

protocol CredentialSectionViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: CredentialSectionViewCell)
}

class CredentialSectionViewCell: BaseTableViewCell {

    @IBOutlet weak var dividerView: UIView!
    @IBOutlet weak var dividerCtrt: NSLayoutConstraint!
    @IBOutlet weak var labelCtrt: NSLayoutConstraint!
    @IBOutlet weak var labelTitle: UILabel!

    override class func default_NibName() -> String {
        return "CredentialSectionViewCell"
    }

    var delegateImpl: CredentialSectionViewCellPresenterDelegate? {
        return delegate as? CredentialSectionViewCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
    }

    // MARK: Config

    func config(title: String, hideDivider: Bool) {

        labelTitle.text = title

        dividerView.isHidden = hideDivider
        dividerCtrt.constant = hideDivider ? 0 : 23
        labelCtrt.constant = hideDivider ? 6 : 26
    }

}
