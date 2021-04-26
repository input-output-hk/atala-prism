//
//  ProfileDetailPresenter.swift
//  prism-ios-wallet
//
//  Created by Roberto Daviduk on 24/02/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

class ProfileDetailPresenter: ListingBasePresenter, ListingBaseTableUtilsPresenterDelegate, TabsViewCellPresenterDelegate, ProfileDetailCellPresenterDelegate {

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
        var value: Any?
    }

    var mode: ProfileMode = .initial

    var initialRows: [CellRow]?

    // MARK: Modes

    func getMode() -> ProfileMode {
        return mode
    }

    lazy var initialStaticCells: [CellRow] = [
        CellRow(type: .header, value: nil),
        CellRow(type: .field, value: (0, true)),
        CellRow(type: .field, value: (1, false)),
        CellRow(type: .field, value: (2, false)),
        CellRow(type: .field, value: (3, false)),
        CellRow(type: .field, value: (4, false)),
        CellRow(type: .field, value: (5, false))
    ]

    func startShowingInitial() {

        mode = .initial
        cleanData()
        initialStaticCells.forEach { initialRows?.append($0) }
        updateViewToState()
    }

    // MARK: Buttons

    @discardableResult
    func tappedBackButton() -> Bool {

        if mode != .initial {
            startShowingInitial()
            return true
        }
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
        return (initialRows?.size() ?? 0) > 0
    }
    
    func getSectionHeaderViews() -> [UIView] {
        return [UIView()]
    }
    
    func getSectionCount() -> Int? {
        return 1
    }

    func getElementCount() -> [Int] {
        if let baseValue = super.getBaseElementCount() {
            return [baseValue]
        }
        return [(initialRows?.size() ?? 0)]
    }

    func getElementType(indexPath: IndexPath) -> ProfileDetailCellType {
        if let baseValue = super.getBaseElementType(indexPath: indexPath) {
            return .base(value: baseValue)
        }
        return initialRows![indexPath.row].type
    }

    // MARK: Fetch

    func fetchElements() {
        self.startShowingInitial()
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

    private func getFieldCellRowValue(index: IndexPath) -> (Int, Bool)? {
        return (initialRows?[index.row])?.value as? (Int, Bool)
    }

    func setup(for cell: ProfileDetailCell) {

        let user = sharedMemory.loggedUser
        let aux = getFieldCellRowValue(index: cell.indexPath!)!

        var fieldType: String?
        var fieldValue: String?
        
        switch aux.0 {
        case 0:
            fieldType = "profile_detail_field_title_firstname".localize()
            fieldValue = user?.firstName
        case 1:
            fieldType = "profile_detail_field_title_lastname".localize()
            fieldValue = user?.lastName
        case 2:
            fieldType = "profile_detail_field_title_nationality".localize()
            fieldValue = user?.countryShortName
        case 3:
            fieldType = "profile_detail_field_title_cityzenship".localize()
            fieldValue = user?.countryShortName
        case 4:
            fieldType = "profile_detail_field_title_gender".localize()
            fieldValue = user?.gender
        case 5:
            fieldType = "profile_detail_field_title_birthday".localize()
            fieldValue = String.init(format: "%d", user?.age ?? 0)
        default:
            break
        }
        
        cell.config(type: fieldType, value: fieldValue)
    }
}
