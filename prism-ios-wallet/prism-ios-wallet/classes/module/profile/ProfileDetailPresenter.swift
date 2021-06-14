//
//  ProfileDetailPresenter.swift
//  prism-ios-wallet
//
//  Created by Roberto Daviduk on 24/02/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

class ProfileDetailPresenter: ListingBasePresenter, ListingBaseTableUtilsPresenterDelegate,
                              TabsViewCellPresenterDelegate, ProfileDetailCellPresenterDelegate {

    var viewImpl: ProfileDetailViewController? {
        return view as? ProfileDetailViewController
    }

    enum ProfileMode {
        case initial
    }

    enum ProfileDetailCellType {
        case base(value: ListingBaseCellType)
        case header
        case field
    }

    struct CellRow {
        var type: ProfileDetailCellType
        var value: Attribute?
    }

    var initialRows = [CellRow(type: .header, value: nil)]

    // MARK: Buttons

    @discardableResult
    func tappedBackButton() -> Bool {
        return false
    }
    // MARK: ListingBaseTableUtilsPresenterDelegate

    func cleanData() {
        initialRows = []
    }

    func fetchData() {

        state = .fetching
        fetchingQueue = 1

        fetchElements()
    }

    func hasData() -> Bool {
        return initialRows.size() > 1
    }

    func getSectionHeaderViews() -> [UIView] {
        return [UIView()]
    }

    func getSectionCount() -> Int? {
        return 1
    }

    func getElementCount() -> [Int] {
        return [initialRows.size()]
    }

    func getElementType(indexPath: IndexPath) -> ProfileDetailCellType {
        if let baseValue = super.getBaseElementType(indexPath: indexPath) {
            return .base(value: baseValue)
        }
        return initialRows[indexPath.row].type
    }

    // MARK: Fetch

    func fetchElements() {
        if let attributes = sharedMemory.loggedUser?.personalAttributes {
            for attribute in attributes {
                initialRows.append(CellRow(type: .field, value: attribute))
            }
        }
        self.startListing()
    }

    // MARK: Table

    func hasPullToRefresh() -> Bool {
        true
    }

    func actionPullToRefresh() {}

    func setup(for cell: TabsViewCell) {
        cell.config()
    }

    private func getFieldCellRowValue(index: IndexPath) -> Attribute? {
        return initialRows[index.row].value
    }

    func setup(for cell: ProfileDetailCell) {

        let attribute = getFieldCellRowValue(index: cell.indexPath!)!
        cell.config(type: attribute.category, value: attribute.value)
    }
}
