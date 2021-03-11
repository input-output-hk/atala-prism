//
//  ActivityLogViewController.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 25/02/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import UIKit

class ActivityLogViewController: ListingBaseViewController {

    var presenterImpl = ActivityLogPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    // Views
    @IBOutlet weak var viewTable: UIView!

    var navBar: NavBarCustomStyle = NavBarCustomStyle(hasNavBar: true, title: "activitylog_title".localize(),
                                                      hasBackButton: true)
    override func navBarCustomStyle() -> NavBarCustomStyle {
        return navBar
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        self.presenterImpl.startShowingActivityLog()
    }

    // MARK: Config

    func config(isLoading: Bool) {
        showLoading(doShow: isLoading)
    }

    // MARK: Table

    override func setupTable() {
        tableUtils = TableUtils(view: self, presenter: presenterImpl, table: table)
    }

    override func getHeaderHeight() -> CGFloat {
        return AppConfigs.TABLE_HEADER_HEIGHT_REGULAR
    }

    override func getCellIdentifier(for indexPath: IndexPath) -> String {
        return "activityLog"
    }

    override func getCellNib(for indexPath: IndexPath) -> String? {
        return ActivityLogTableViewCell.default_NibName()
    }

}
