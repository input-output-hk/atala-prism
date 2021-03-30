//
//  VerifyIdTypeSelectViewController.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 01/02/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import UIKit

class VerifyIdTypeSelectViewController: ListingBaseViewController {

    var presenterImpl = VerifyIdTypeSelectPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    @IBOutlet weak var nextBttn: UIButton!

    var navBar: NavBarCustomStyle = NavBarCustomStyle(hasNavBar: true, title: "verifyid_title".localize(),
                                                      hasBackButton: true)
    override func navBarCustomStyle() -> NavBarCustomStyle {
        return navBar
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        setupButtons()
    }

    // MARK: Table

    override func setupTable() {
        tableUtils = TableUtils(view: self, presenter: presenterImpl, table: table)
    }

    override func getHeaderHeight() -> CGFloat {
        return AppConfigs.TABLE_HEADER_HEIGHT_REGULAR
    }

    override func getCellIdentifier(for indexPath: IndexPath) -> String {

        return TypeSelectTableViewCell.reuseIdentifier
    }

    override func getCellNib(for indexPath: IndexPath) -> String? {

        return TypeSelectTableViewCell.default_NibName()
    }

    // MARK: Buttons

    func setupButtons() {
        nextBttn.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON)
    }
    
    @IBAction func continueTapped(_ sender: Any) {
        presenterImpl.continueTapped()
    }
    
    // MARK: Screens

    func changeScreenToScanFront() {
        _ = app_mayPerformSegue(withIdentifier: "VerifyIdScanSegue", sender: self)
    }

}
