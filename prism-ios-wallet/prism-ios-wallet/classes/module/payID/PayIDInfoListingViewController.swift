//
//  PayIDInfoListingViewController.swift
//  prism-ios-wallet
//
//  Created by Roberto Daviduk on 23/03/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation
import UIKit

class PayIDInfoListingViewController: ListingBaseViewController, UINavigationControllerDelegate, SegueableScreen {

    @IBOutlet weak var nameLbl: UILabel!

    var navBar: NavBarCustomStyle = NavBarCustomStyle(hasNavBar: true)
    override func navBarCustomStyle() -> NavBarCustomStyle {
        return navBar
    }

    var presenterImpl = PayIDInfoListingPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    var titleValue: String?
    var type: String?

    override func viewDidLoad() {
        super.viewDidLoad()

    }

    override func onBackPressed() -> Bool {
        if !presenterImpl.tappedBackButton() {
            return super.onBackPressed()
        }
        return false
    }

    func setupData() {
        nameLbl.text = "\(presenterImpl.payId?.name ?? "")\("pay_id_setup_name_field_right".localize())"
    }

    // MARK: Segue Value

    func configScreenFromSegue(params: [Any?]?) {
        titleValue = params?[0] as? String
        type = params?[1] as? String
    }
    
    // MARK: Config

    override func config(mode: ListingBasePresenter.ListingBaseState) {

        // Navigation bar
        navBar = NavBarCustomStyle(hasNavBar: true, isWhite: false, title: titleValue, hasBackButton: true)
        
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
            return "PayIDInfoListingCell"
        default:
            return super.getCellIdentifier(for: indexPath)
        }
    }

    override func getCellNib(for indexPath: IndexPath) -> String? {

        switch presenterImpl.getElementType(indexPath: indexPath) {
        case .listing:
            return PayIDInfoListingCell.default_NibName()
        default:
            return super.getCellNib(for: indexPath)
        }
    }
}
