//
//  VerifyIdResultsPresenter.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 08/02/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation

class VerifyIdResultsPresenter: ListingBasePresenter, ListingBaseTableUtilsPresenterDelegate,
                                VerifyIdResultsCellDelegate {

    var viewImpl: VerifyIdResultsViewController? {
        return view as? VerifyIdResultsViewController
    }

    struct InitialCellValue {
        var title: String
        var value: String?
    }

    lazy var initialStaticCells: [InitialCellValue] = []

    func config(values: [String?]) {

        for item in values {
            if let parts = item?.split(separator: ":", maxSplits: 2, omittingEmptySubsequences: false),
               parts.count > 1 {
                initialStaticCells.append(InitialCellValue(title: String(parts[0]), value: String(parts[1])))
            }
        }
    }

    // MARK: ListingBaseTableUtilsPresenterDelegate

    func cleanData() {

    }

    func fetchData() {

    }

    func hasData() -> Bool {
        return true
    }

    func getElementCount() -> Int {
        return initialStaticCells.count
    }

    // MARK: Table

    func hasPullToRefresh() -> Bool {
        true
    }

    func actionPullToRefresh() {

    }

    // MARK: TypeSelectCellDelegate

    func setup(for cell: VerifyIdResultsTableViewCell) {
        if let index = cell.indexPath?.row {
            let item = initialStaticCells[index]
            cell.config(title: item.title, value: item.value)
        }
    }

    // MARK: Buttons

    func continueTapped() {
        viewImpl?.goToMainScreen()
    }

    func retryTapped() {
        viewImpl?.onBackPressed()
    }
}
