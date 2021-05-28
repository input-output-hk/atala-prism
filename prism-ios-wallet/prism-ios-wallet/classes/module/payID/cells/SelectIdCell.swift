//
//  SelectIdCell.swift
//  prism-ios-wallet
//
//  Created by Roberto Daviduk on 18/03/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation

protocol SelectIdCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: SelectIdCell)
    func setSelected(for cell: SelectIdCell)
}

class SelectIdCell: BaseTableViewCell {

    @IBOutlet weak var viewContent: UIView!
    @IBOutlet weak var imageLogo: UIImageView!
    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var labelSubtitle: UILabel!
    @IBOutlet weak var viewCheck: UIView!
    @IBOutlet weak var imageCheck: UIImageView!
    
    lazy var setSelected = SelectorAction(action: { [weak self] in
        self?.delegateImpl?.setSelected(for: self!)
    })

    override class func default_NibName() -> String {
        return "SelectIdCell"
    }

    var delegateImpl: SelectIdCellPresenterDelegate? {
        return delegate as? SelectIdCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
        viewContent.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR)
        viewContent.addShadowLayer(opacity: 0.1)
        imageLogo.layer.cornerRadius = AppConfigs.CORNER_RADIUS_REGULAR
        viewContent.addOnClickListener(action: setSelected)
        imageCheck.layer.cornerRadius = AppConfigs.CORNER_RADIUS_REGULAR / 2
    }
    // MARK: Config
    
    func config(logo: UIImage, title: String?, subtitle: String?) {

        imageLogo.image = logo
        labelTitle.text = title
        labelSubtitle.text = subtitle
    }
    
    func setSelected(_ select: Bool) {
        self.imageCheck.image = select ? #imageLiteral(resourceName: "ico_share_tick") : #imageLiteral(resourceName: "ico_share_empty")
        self.viewContent.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR,
                                    borderWidth: select ? 2 : 0, borderColor: UIColor.appRed.cgColor)
    }
}
