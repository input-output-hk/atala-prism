//
//  PayIDInfoListingPresenter.swift
//  prism-ios-wallet
//
//  Created by Roberto Daviduk on 23/03/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation

class PayIDInfoListingPresenter: ListingBasePresenter, ListingBaseTableUtilsPresenterDelegate,
                                 PayIDInfoListingCellPresenterDelegate {

    var viewImpl: PayIDInfoListingViewController? {
        return view as? PayIDInfoListingViewController
    }

    enum PayIDInfoListingCellType {
        case base(value: ListingBaseCellType)
        case listing
    }

    struct CellRow {
        var type: PayIDInfoListingCellType
        var value: String?
    }

    struct InitialCellValue {
        var icon: String
        var title: String
        var subtitle: String
    }

    var initialRows: [CellRow]?
    var payId: PayId?

    func startShowingInitial() {

        cleanData()
        guard let addresses = payId?.addresses?.allObjects as? [Address] else { return }
        for address in addresses {
            initialRows?.append(CellRow(type: .listing, value: address.name))
        }

        updateViewToState()
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

    func getSectionCount() -> Int? {
        return 1
    }
    
    func getSectionHeaderViews() -> [UIView] {
        return [UIView()]
    }

    func getElementCount() -> [Int] {
        return [initialRows?.count ?? 0]
    }

    func getElementType(indexPath: IndexPath) -> PayIDInfoListingCellType {
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
        DispatchQueue.global(qos: .background).async {
            let dao = PayIdDAO()
            let payIds = dao.listPayId()
            if payIds?.count > 0 {
                self.payId = payIds?[0]
                DispatchQueue.main.async {
                    self.viewImpl?.setupData()
                    self.startShowingInitial()
                    self.startListing()
                }
            }
        }
    }

    func setup(for cell: PayIDInfoListingCell) {

        cell.config(title: viewImpl?.type, value: initialRows?[cell.indexPath!.row].value)
    }

    // MARK: Buttons

    @discardableResult
    func tappedBackButton() -> Bool {
        return false
    }
}
