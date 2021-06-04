//
//  PayIDSelectorViewController.swift
//  prism-ios-wallet
//
//  Created by Roberto Daviduk on 17/03/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation
import UIKit

class PayIDSelectorViewController: ListingBaseViewController {

    @IBOutlet weak var nextButton: UIButton!

    var navBar: NavBarCustomStyle = NavBarCustomStyle(hasNavBar: true)
    override func navBarCustomStyle() -> NavBarCustomStyle {
        return navBar
    }

    var presenterImpl = PayIDSelectorPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    override func viewDidLoad() {
        super.viewDidLoad()

        nextButton.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON)
    }

    override func onBackPressed() -> Bool {
        if !presenterImpl.tappedBackButton() {
            return super.onBackPressed()
        }
        return false
    }

    func changeScreenToSetup(credentials: [Credential]) {
        ViewControllerUtils.changeScreenSegued(caller: self, segue: "SeguePayIDSetupViewController",
                                               params: credentials)
    }

    func changeScreenToVerifyId() {
        ViewControllerUtils.changeScreenPresented(caller: self, storyboardName: "VerifyIdTutorial",
                                                  viewControllerIdentif: "VerifyIdTutorialNavigation", params: nil,
                                                  animated: true)
    }

    // MARK: Config

    override func config(mode: ListingBasePresenter.ListingBaseState) {

        let title: String = "pay_id_selector_title".localize()
        
        // Navigation bar
        navBar = NavBarCustomStyle(hasNavBar: true, isWhite: false, title: title, hasBackButton: true)
        
        NavBarCustom.config(view: self)
    }
    
    // MARK: Table

    override func setupTable() {
        tableUtils = TableUtils(view: self, presenter: presenterImpl, table: table)
    }

    override func getHeaderHeight(for section: Int) -> CGFloat {
        return AppConfigs.TABLE_HEADER_HEIGHT_REGULAR * 3
    }

    override func getCellIdentifier(for indexPath: IndexPath) -> String {

        switch presenterImpl.getElementType(indexPath: indexPath) {
        case .select:
            if indexPath.section == 0 && presenterImpl.idCredentials.count == 0 {
                return "SelectIdEmpty"
            }
            return "SelectId"
        default:
            return super.getCellIdentifier(for: indexPath)
        }
    }

    override func getCellNib(for indexPath: IndexPath) -> String? {

        switch presenterImpl.getElementType(indexPath: indexPath) {
        case .select:
            if indexPath.section == 0 && presenterImpl.idCredentials.count == 0 {
                return SelectIdEmptyCell.default_NibName()
            }
            return SelectIdCell.default_NibName()
        default:
            return super.getCellNib(for: indexPath)
        }
    }

    // MARK: Button

    func changeButtonState(isEnabled: Bool) {

        nextButton.isEnabled = isEnabled
        nextButton.backgroundColor = isEnabled ? .appRed : .appGreyMid
    }

    @IBAction func next(_ sender: Any) {
        presenterImpl.tappedNextButton()
    }
}
