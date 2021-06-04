//
//  SelectIdEmptyCell.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 31/05/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import UIKit

protocol SelectIdEmptyCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: SelectIdEmptyCell)
    func tappedVerifyId(for cell: SelectIdEmptyCell)
}

class SelectIdEmptyCell: BaseTableViewCell {

    @IBOutlet weak var verifyIdBttn: UIButton!

    override class func default_NibName() -> String {
        return "SelectIdEmptyCell"
    }

    var delegateImpl: SelectIdEmptyCellPresenterDelegate? {
        return delegate as? SelectIdEmptyCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
    }

    // MARK: Config

    func config(delegate: SelectIdEmptyCellPresenterDelegate) {
        self.delegate = delegate
        verifyIdBttn.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON)
    }
    
    @IBAction func tappedVerifyId(_ sender: Any) {
        delegateImpl?.tappedVerifyId(for: self)
    }
    
}
