//
//  DateFormatViewController.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 02/09/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import UIKit

class DateFormatViewController: ListingBaseViewController {

    var presenterImpl = DateFormatPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    @IBOutlet weak var okBttn: UIButton!

    var navBar: NavBarCustomStyle = NavBarCustomStyle(hasNavBar: true)
    override func navBarCustomStyle() -> NavBarCustomStyle {
        return navBar
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        setupButtons()
        presenterImpl.fetchData()
    }

    // MARK: Config

    override func config(mode: ListingBasePresenter.ListingBaseState) {

        // Navigation bar
        navBar = NavBarCustomStyle(hasNavBar: true, isWhite: false, title: "dateformat_nav_bar".localize(),
                                   hasBackButton: true)
        NavBarCustom.config(view: self)
    }

    // MARK: Table

    override func setupTable() {
        tableUtils = TableUtils(view: self, presenter: presenterImpl, table: table)
    }

    override func getHeaderHeight() -> CGFloat {
        return 0
    }

    override func getCellIdentifier(for indexPath: IndexPath) -> String {

        return DateFormatTableViewCell.reuseIdentifier
    }

    override func getCellNib(for indexPath: IndexPath) -> String? {

        return DateFormatTableViewCell.nibName
    }

    // MARK: Buttons

    func setupButtons() {
        okBttn.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON)
    }

    @IBAction func okTapped(_ sender: Any) {
        presenterImpl.okTapped()
    }
}
