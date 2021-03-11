//
//  HomeViewController.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 22/02/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import UIKit

class HomeViewController: ListingBaseViewController {

    var presenterImpl = HomePresenter()
    override var presenter: BasePresenter { return presenterImpl }

    var navBar: NavBarCustomStyle = NavBarCustomStyle(hasNavBar: false)
    override func navBarCustomStyle() -> NavBarCustomStyle {
        return navBar
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        self.presenterImpl.actionPullToRefresh()
    }

    func config(isLoading: Bool) {
        showLoading(doShow: isLoading)
    }

    // MARK: Table

    override func setupTable() {
        tableUtils = TableUtils(view: self, presenter: presenterImpl, table: table)
    }

    override func getHeaderHeight() -> CGFloat {
        return 0
    }

    override func getCellIdentifier(for indexPath: IndexPath) -> String {

        if indexPath.row == 0 {
            return HomeProfileTableViewCell.reuseIdentifier
        } else if indexPath.row == 1 {
            return HomeActivityLogHeaderTableViewCell.reuseIdentifier
        } else if indexPath.row > 1 && indexPath.row < presenterImpl.activities.count + 2 {
            return HomeActivityLogTableViewCell.reuseIdentifier
        } else if indexPath.row == presenterImpl.activities.count + 2 {
            return HomePromotionalTableViewCell.reuseIdentifier
        }
        return super.getCellIdentifier(for: indexPath)
    }

    override func getCellNib(for indexPath: IndexPath) -> String? {

        if indexPath.row == 0 {
            return HomeProfileTableViewCell.nibName
        } else if indexPath.row == 1 {
            return HomeActivityLogHeaderTableViewCell.nibName
        } else if indexPath.row > 1 && indexPath.row < presenterImpl.activities.count + 2 {
            return HomeActivityLogTableViewCell.nibName
        } else if indexPath.row == presenterImpl.activities.count + 2 {
            return HomePromotionalTableViewCell.nibName
        }
        return super.getCellNib(for: indexPath)
    }

    // MARK: Screens

    func changeScreenToProfile() {
        ViewControllerUtils.changeScreenSegued(caller: self, segue: "ProfileSegue", params: nil)
    }

    func changeScreenToNotifications() {
        ViewControllerUtils.changeScreenSegued(caller: self, segue: "NotificationsSegue", params: nil)
    }

    func changeScreenToActivityLog() {
        ViewControllerUtils.changeScreenSegued(caller: self, segue: "ActivityLogSegue", params: nil)
    }

}
