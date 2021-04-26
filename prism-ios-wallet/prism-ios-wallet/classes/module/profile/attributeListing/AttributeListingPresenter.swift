//
//  AttributeListingPresenter.swift
//  prism-ios-wallet
//
//  Created by Roberto Daviduk on 08/03/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation

class AttributeListingPresenter: ListingBasePresenter, ListingBaseTableUtilsPresenterDelegate, AttributeListingCellPresenterDelegate {
    
    var viewImpl: AttributeListingViewController? {
        return view as? AttributeListingViewController
    }

    enum AttributeListingCellType {
        case base(value: ListingBaseCellType)
        case listing
    }

    struct CellRow {
        var type: AttributeListingCellType
        var value: Any?
    }
    
    struct InitialCellValue {
        var icon: String
        var title: String
        var subtitle: String
    }

    var initialRows: [CellRow]?
    var attributes: [Attribute] = []

    func startShowingInitial() {

        cleanData()

        var count: Int = initialRows?.count ?? 0

        for att in sharedMemory.loggedUser!.attributes {

            if att.type == viewImpl?.attributeType {
                initialRows?.append(CellRow(type: .listing, value: (count, true)))
                attributes.append(att)
                count+=1
            }
        }
                
        updateViewToState()
    }
    
    // MARK: ListingBaseTableUtilsPresenterDelegate

    func cleanData() {
        initialRows = []
        attributes = []
    }
    
    func fetchData() {
        state = .fetching
        fetchingQueue = 1

        fetchElements()
    }
    
    func hasData() -> Bool {
        return (initialRows?.size() ?? 0) > 0
    }
    
    func getSectionCount() -> Int? {
        return 1
    }
    
    func getSectionHeaderViews() -> [UIView] {
        return [UIView()]
    }
    
    func getElementCount() -> [Int] {
        return [initialRows?.count ?? 0]
    }
    
    func getElementType(indexPath: IndexPath) -> AttributeListingCellType {
        if let baseValue = super.getBaseElementType(indexPath: indexPath) {
            return .base(value: baseValue)
        }

        return initialRows![indexPath.row].type
    }

    func hasPullToRefresh() -> Bool {
        true
    }

    func actionPullToRefresh() {}
    
    // MARK: Fetch

    func fetchElements() {
        self.startShowingInitial()
        self.startListing()
    }
    
    private func getFieldCellRowValue(index: IndexPath) -> (Int, Bool)? {
        return (initialRows?[index.row])?.value as? (Int, Bool)
    }
    
    func setup(for cell: AttributeListingCell) {
        
        let type: String = (attributes[cell.indexPath!.row].type) ?? ""
        let value: String = (attributes[cell.indexPath!.row].value) ?? ""
        
        cell.config(type: type, value: value)
    }
    
    // MARK: Buttons

    @discardableResult
    func tappedBackButton() -> Bool {
        return false
    }
}
