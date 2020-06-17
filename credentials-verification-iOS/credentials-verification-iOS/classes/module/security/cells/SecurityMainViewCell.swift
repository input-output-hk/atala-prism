//
//  SecurityMainViewCell.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 19/03/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import UIKit

protocol SecurityMainViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: SecurityMainViewCell)
    func tappedAction(for cell: SecurityMainViewCell)
    func switchValueChanged(for cell: SecurityMainViewCell, value: Bool)
}

class SecurityMainViewCell: BaseTableViewCell {

    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var buttonIconAction: UIButton!
    @IBOutlet weak var imageLogo: UIImageView!
    @IBOutlet weak var switchAction: UISwitch!

    override class func default_NibName() -> String {
        return "SecurityMainViewCell"
    }

    var delegateImpl: SecurityMainViewCellPresenterDelegate? {
        return delegate as? SecurityMainViewCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
    }

    // MARK: Component delegates

    @IBAction func actionMainButtonTapped(_ sender: Any) {
        self.delegateImpl?.tappedAction(for: self)
    }

    @IBAction func switchValueChanged(_ sender: Any) {
        self.delegateImpl?.switchValueChanged(for: self, value: switchAction.isOn)
    }

    // MARK: Config

    func config(title: String, hasSwitch: Bool, switchValue: Bool, icon: UIImage) {

        labelTitle.text = title
        buttonIconAction.isHidden = hasSwitch
        switchAction.isHidden = !hasSwitch
        switchAction.setOn(switchValue, animated: false)
        imageLogo.image = icon
    }

}
