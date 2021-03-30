//
//  VerifyIdTypeSelectPresenter.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 01/02/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation

class VerifyIdTypeSelectPresenter: ListingBasePresenter, ListingBaseTableUtilsPresenterDelegate,
                                   TypeSelectCellDelegate {
    

    var viewImpl: VerifyIdTypeSelectViewController? {
        return view as? VerifyIdTypeSelectViewController
    }

    struct InitialCellValue {
        var icon: String
        var title: String
        var isSelected: Bool
//        var action: SelectorAction?
    }

    lazy var initialStaticCells: [InitialCellValue] = [
        InitialCellValue(icon: "icon_id", title: "verifyid_typeselect_national_id".localize(), isSelected: true),
        InitialCellValue(icon: "icon_passport", title: "verifyid_typeselect_passport".localize(), isSelected: false),
        InitialCellValue(icon: "icon_drivers", title: "verifyid_typeselect_drivers_license".localize(),
                         isSelected: false)
    ]

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

    func setup(for cell: TypeSelectTableViewCell) {
        if let index = cell.indexPath?.row {
            let item = initialStaticCells[index]
            cell.config(name: item.title, icon: item.icon, isSelected: item.isSelected)
        }
    }

    func tappedAction(for cell: TypeSelectTableViewCell) {
        if let index = cell.indexPath?.row {
            for item in 0...2 {
                initialStaticCells[item].isSelected = item == index
            }
        }
        viewImpl?.table.reloadData()
    }
    
    // MARK: Buttons

    func continueTapped() {
        viewImpl?.changeScreenToScanFront()
    }
}
