//
//  AttributeListingViewController.swift
//  prism-ios-wallet
//
//  Created by Roberto Daviduk on 08/03/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation
import UIKit

class AttributeListingViewController: ListingBaseViewController, UINavigationControllerDelegate, SegueableScreen {

    var attributeType: String?

    var navBar: NavBarCustomStyle = NavBarCustomStyle(hasNavBar: true)
    override func navBarCustomStyle() -> NavBarCustomStyle {
        return navBar
    }
    
    var presenterImpl = AttributeListingPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    override func viewDidLoad() {
        super.viewDidLoad()

    }

    override func onBackPressed() -> Bool {
        if !presenterImpl.tappedBackButton() {
            return super.onBackPressed()
        }
        return false
    }
    
    // MARK: Segue Value
    
    func configScreenFromSegue(params: [Any?]?) {
        attributeType = params?[0] as? String
    }
    
    // MARK: Config

    override func config(mode: ListingBasePresenter.ListingBaseState) {

        let title: String = "attribute_listing_title".localize()
        
        // Navigation bar
        navBar = NavBarCustomStyle(hasNavBar: true, isWhite: false, title: title, hasBackButton: true)
        
        NavBarCustom.config(view: self)
    }
    
    // MARK: Table

    override func setupTable() {
        tableUtils = TableUtils(view: self, presenter: presenterImpl, table: table)
    }

    override func getHeaderHeight(for section: Int) -> CGFloat {
        return AppConfigs.TABLE_HEADER_HEIGHT_REGULAR
    }

    override func getCellIdentifier(for indexPath: IndexPath) -> String {
        
        switch presenterImpl.getElementType(indexPath: indexPath) {
        case .listing:
            return "AttributeListingCell"
        default:
            return super.getCellIdentifier(for: indexPath)
        }
    } 

    override func getCellNib(for indexPath: IndexPath) -> String? {

        switch presenterImpl.getElementType(indexPath: indexPath) {
        case .listing:
            return AttributeListingCell.default_NibName()
        default:
            return super.getCellNib(for: indexPath)
        }
    }
}
