//
//  VerifyIdTypeSelectViewController.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 01/02/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import UIKit

class VerifyIdTypeSelectViewController: ListingBaseViewController {

    var presenterImpl = VerifyIdTypeSelectPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    @IBOutlet weak var nextBttn: UIButton!

    var navBar: NavBarCustomStyle = NavBarCustomStyle(hasNavBar: true, title: "verifyid_title".localize(),
                                                      hasBackButton: true)
    override func navBarCustomStyle() -> NavBarCustomStyle {
        return navBar
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        setupButtons()
    }

    // MARK: Config

    func config(isLoading: Bool) {
        showLoading(doShow: isLoading)
    }

    // MARK: Table

    override func setupTable() {
        tableUtils = TableUtils(view: self, presenter: presenterImpl, table: table)
    }

    override func getHeaderHeight(for section: Int) -> CGFloat {
        return 0
    }

    override func getCellIdentifier(for indexPath: IndexPath) -> String {

        return TypeSelectTableViewCell.reuseIdentifier
    }

    override func getCellNib(for indexPath: IndexPath) -> String? {

        return TypeSelectTableViewCell.default_NibName()
    }

    // MARK: Buttons

    func setupButtons() {
        nextBttn.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON)
    }
    
    func enableNextButton() {
        nextBttn.backgroundColor = .appRed
        nextBttn.isEnabled = true
    }

    @IBAction func continueTapped(_ sender: Any) {
        presenterImpl.continueTapped()
    }

    // MARK: Screens

    func changeScreenToScanFront(documentInstanceID: String, kycToken: String) {
        let params = VerifyIdScanViewController.makeSeguedParams(documentInstanceID: documentInstanceID,
                                                                 kycToken: kycToken,
                                                                 contact: presenterImpl.contact)
        ViewControllerUtils.changeScreenSegued(caller: self, segue: "VerifyIdScanSegue",
                                               params: params)
    }

}
