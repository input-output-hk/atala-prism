//
//  DateFormatPresenter.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 02/09/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import Foundation

class DateFormatPresenter: ListingBasePresenter, ListingBaseTableUtilsPresenterDelegate,
                            DateFormatTableViewCellDelegate, DateFormatInfoTableViewCellDelegate {

    var viewImpl: DateFormatViewController? {
        return view as? DateFormatViewController
    }

    struct InitialCellValue {
        var title: String
        var selected: Bool
        var format: String
    }

    lazy var initialStaticCells: [InitialCellValue] = [
        InitialCellValue(title: "dateformat_format_dd_mm_yyyy".localize(), selected: false, format: "dd/MM/yyyy"),
        InitialCellValue(title: "dateformat_format_mm_dd_yyyy".localize(), selected: false, format: "MM/dd/yyyy"),
        InitialCellValue(title: "dateformat_format_yyyy_mm_dd".localize(), selected: false, format: "yyyy/MM/dd")
    ]

    // MARK: Buttons

    func okTapped() {
        let user = self.sharedMemory.loggedUser
        for item in initialStaticCells where item.selected {
            user?.dateFormat = item.format
        }
        self.sharedMemory.loggedUser = user
        self.viewImpl?.onBackPressed()
    }

    // MARK: ListingBaseTableUtilsPresenterDelegate

    func cleanData() {
    }

    func fetchData() {
        let dateFormat = self.sharedMemory.loggedUser?.dateFormat
        for index in 0...2 {
            initialStaticCells[index].selected = initialStaticCells[index].format == dateFormat
        }

        viewImpl?.table.reloadData()
    }

    func hasData() -> Bool {
        true
    }
    
    func getSectionHeaderViews() -> [UIView] {
        return [UIView()]
    }
    
    func getSectionCount() -> Int? {
        return 1
    }

    func getElementCount() -> [Int] {
        return [initialStaticCells.count + 1]
    }

    // MARK: Table

    func hasPullToRefresh() -> Bool {
        false
    }

    func actionPullToRefresh() {

    }

    func setup(for cell: DateFormatTableViewCell) {

        if let row = cell.indexPath?.row {
            cell.config(name: initialStaticCells[row].title, isSelected: initialStaticCells[row].selected,
                        delegate: self)
        }
    }

    func itemTapped(for cell: DateFormatTableViewCell) {
        if let row = cell.indexPath?.row {
            for index in 0...2 {
                initialStaticCells[index].selected = index == row
            }
        }
        viewImpl?.table.reloadData()
    }

    func setup(for cell: DateFormatInfoTableViewCell) {
        let title = NSMutableAttributedString(attributedString: "dateformat_info_bold".localize().bold)
        title.append("dateformat_info".localize().regular)
        cell.config(title: title, delegate: self)
    }

}
