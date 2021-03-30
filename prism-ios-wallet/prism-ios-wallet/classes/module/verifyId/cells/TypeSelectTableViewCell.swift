//
//  TypeSelectTableViewCell.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 01/02/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import UIKit

protocol TypeSelectCellDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: TypeSelectTableViewCell)
    func tappedAction(for cell: TypeSelectTableViewCell)
}

class TypeSelectTableViewCell: BaseTableViewCell {

    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var imageLogo: UIImageView!
    @IBOutlet weak var viewTick: UIImageView!
    @IBOutlet weak var viewBg: UIView!

    class var reuseIdentifier: String {
        return "TypeSelect"
    }

    override class func default_NibName() -> String {
        return "TypeSelectTableViewCell"
    }

    var delegateImpl: TypeSelectCellDelegate? {
        return delegate as? TypeSelectCellDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
    }

    func config(name: String?, icon: String, isSelected: Bool) {

        self.labelTitle.text = name
        self.imageLogo.applyDataImage(data: nil, placeholderNamed: icon)
//        self.imageLogo.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR)
        self.viewTick.image = isSelected ? #imageLiteral(resourceName: "ico_share_tick") : #imageLiteral(resourceName: "ico_share_empty")
        self.viewBg.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR,
                                    borderWidth: isSelected ? 2 : 0, borderColor: UIColor.appRed.cgColor)
        self.viewBg.addShadowLayer(radius: 4)
        self.viewTick.addDropShadow()
    }

    @IBAction func actionItemTapped(_ sender: Any) {
        delegateImpl?.tappedAction(for: self)
    }

}
