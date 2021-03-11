//
//  ActivityLogPresenter.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 25/02/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation

class ActivityLogPresenter: ListingBasePresenter, ListingBaseTableUtilsPresenterDelegate,
                            ActivityLogViewCellPresenterDelegate {

    var viewImpl: ActivityLogViewController? {
        return view as? ActivityLogViewController
    }

    var activityLogs: [ActivityHistory]?

    // MARK: Modes

    func startShowingActivityLog() {
        DispatchQueue.global(qos: .background).async {
            let dao = ActivityHistoryDAO()
            self.activityLogs = dao.listActivityHistory()
            self.updateViewToState()
        }
    }

    // MARK: ListingBaseTableUtilsPresenterDelegate

    func cleanData() {
    }

    func fetchData() {

        fetchingQueue = 1
    }

    func hasData() -> Bool {
        return !(activityLogs?.isEmpty ?? true)
    }

    func getElementCount() -> Int {
        return activityLogs?.size() ?? 0
    }

    // MARK: Table

    func hasPullToRefresh() -> Bool {
        true
    }

    func actionPullToRefresh() {
    }

    func setup(for cell: ActivityLogTableViewCell) {
        let detailRow = activityLogs![cell.indexPath!.row]
        cell.config(history: detailRow)
    }

}
