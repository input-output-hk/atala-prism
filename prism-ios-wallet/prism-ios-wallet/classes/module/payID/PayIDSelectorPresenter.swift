//
//  PayIDSelectorPresenter.swift
//  prism-ios-wallet
//
//  Created by Roberto Daviduk on 17/03/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation

class PayIDSelectorPresenter: ListingBasePresenter, ListingBaseTableUtilsPresenterDelegate,
                              SelectIdCellPresenterDelegate {
    
    var viewImpl: PayIDSelectorViewController? {
        return view as? PayIDSelectorViewController
    }

    enum SelectIdCellType {
        case base(value: ListingBaseCellType)
        case select
    }

    struct CellRow {
        var type: SelectIdCellType
        var value: Any?
    }

    var initialRows: [CellRow]?

    lazy var initialStaticCells: [CellRow] = [
        CellRow(type: .select, value: (0, true)),
        CellRow(type: .select, value: (1, false)),
        CellRow(type: .select, value: (2, false)),
        CellRow(type: .select, value: (3, false))
    ]
    
    var selectedCells: [SelectIdCell] = []
    var idCredentials: [Credential] = []
    var otherCredentials: [Credential] = []

    func startShowingInitial() {

        cleanData()
        initialStaticCells.forEach { initialRows?.append($0) }
        updateViewToState()
    }
    
    // MARK: Buttons

    @discardableResult
    func tappedBackButton() -> Bool {

        return false
    }

    func tappedNextButton() {
        let selectedCredentials = selectedCells.map { cell -> Credential in
            if cell.indexPath?.section == 0 {
                return idCredentials[cell.indexPath?.row ?? 0]
            } else {
                return otherCredentials[cell.indexPath?.row ?? 0]
            }
        }
        viewImpl?.changeScreenToSetup(credentials: selectedCredentials)
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
        return 2
    }

    func getSectionHeaderViews() -> [UIView] {

        var arrViews = [UIView]()

        let numOfSections: Int = viewImpl?.table.numberOfSections ?? 1

        for section in 0..<numOfSections {
            switch section {
            case 0:
                arrViews.append(returnTalbeviewheaderWith(title: "pay_id_selector_section_0".localize()))
            case 1:
                arrViews.append(returnTalbeviewheaderWith(title: "pay_id_selector_section_1".localize()))
            default:
                break
            }
        }
        return arrViews
    }

    func returnTalbeviewheaderWith(title: String?) -> UIView {

        let headerView = UIView()

        if title != nil {
            
            headerView.backgroundColor = .white

            let sectionLabel = UILabel(frame: CGRect(x: 12, y: 16, width: viewImpl!.table.bounds.size.width,
                                                     height: viewImpl!.table.bounds.size.height))
            sectionLabel.font = UIFont.systemFont(ofSize: 16)
            sectionLabel.textColor = .lightGray
            sectionLabel.text = title
            sectionLabel.sizeToFit()
            headerView.addSubview(sectionLabel)
        }

        return headerView
    }

    func getElementCount() -> [Int] {
        return [idCredentials.count, otherCredentials.count]
    }

    func getElementType(indexPath: IndexPath) -> SelectIdCellType {
        if let baseValue = super.getBaseElementType(indexPath: indexPath) {
            return .base(value: baseValue)
        }
        return initialRows![indexPath.section].type
    }

    // MARK: Fetch

    func fetchElements() {
        DispatchQueue.global(qos: .background).async {
            let credentialsDao = CredentialDAO()
            let credentials = credentialsDao.listCredentials()
            self.idCredentials = credentials?.filter({ $0.credentialType == .demoGovernmentIssuedId
                                                        || $0.credentialType == .governmentIssuedId
                                                        || $0.credentialType == .georgiaNationalID
                                                        || $0.credentialType == .ethiopiaNationalID }) ?? []
            self.otherCredentials = credentials?.filter({ $0.credentialType != .demoGovernmentIssuedId
                                                            && $0.credentialType != .governmentIssuedId
                                                            && $0.credentialType != .georgiaNationalID
                                                            && $0.credentialType != .ethiopiaNationalID }) ?? []
            self.startShowingInitial()
            self.startListing()
        }
    }

    // MARK: Table

    func hasPullToRefresh() -> Bool {
        true
    }

    func actionPullToRefresh() {}

    func setup(for cell: SelectIdCell) {

        var fieldLogo: String?
        var fieldTitle: String?
        var fieldSubtitle: String?

        switch cell.indexPath?.section {
        case 0:
            fieldLogo = idCredentials[cell.indexPath?.row ?? 0].logoPlaceholder
            fieldTitle = idCredentials[cell.indexPath?.row ?? 0].credentialName
            fieldSubtitle = idCredentials[cell.indexPath?.row ?? 0].issuerName

        case 1:
            fieldLogo = otherCredentials[cell.indexPath?.row ?? 0].logoPlaceholder
            fieldTitle = otherCredentials[cell.indexPath?.row ?? 0].credentialName
            fieldSubtitle = otherCredentials[cell.indexPath?.row ?? 0].issuerName

        default:
            break
        }

        cell.config(logo: UIImage(named: fieldLogo!)!, title: fieldTitle, subtitle: fieldSubtitle)
    }

    func setSelected(for cell: SelectIdCell) {

        if selectedCells.contains(cell) {
            cell.setSelected(false)
            selectedCells.remove(cell)
        } else {
            cell.setSelected(true)
            selectedCells.append(cell)
        }

        let selectedCredentials = selectedCells.map { cell -> Credential? in
            switch cell.indexPath?.section {
            case 0:
                return idCredentials[cell.indexPath?.row ?? 0]

            case 1:
                return otherCredentials[cell.indexPath?.row ?? 0]

            default:
                return nil
            }
        }
        let hasId = selectedCredentials.contains {
            $0?.credentialType == .demoGovernmentIssuedId
                || $0?.credentialType == .governmentIssuedId
                || $0?.credentialType == .georgiaNationalID
                || $0?.credentialType == .ethiopiaNationalID
        }
        viewImpl?.changeButtonState(isEnabled: hasId)
    }
}
