//
//  VerifyIdResultsViewController.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 08/02/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import UIKit

class VerifyIdResultsViewController: ListingBaseViewController {

    var presenterImpl = VerifyIdResultsPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    @IBOutlet weak var confirmBttn: UIButton!
    @IBOutlet weak var retryBttn: UIButton!

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

    // MARK: Screens

    func changeScreenToSuccess(action: SelectorAction) {

        let params = SuccessViewController.makeSeguedParams(title: "verifyid_success_title".localize(),
                                                            subtitle: "verifyid_success_subtitle".localize(),
                                                            buttonText: "close".localize(),
                                                            buttonAction: action,
                                                            image: "img_verify_id_success")
        ViewControllerUtils.changeScreenSegued(caller: self, segue: "SuccessSegue", params: params)
    }

    func goToMainScreen() {
        MainNavViewController.openThisView()
    }

    // MARK: Table

    override func setupTable() {
        tableUtils = TableUtils(view: self, presenter: presenterImpl, table: table)
    }

    override func getHeaderHeight(for section: Int) -> CGFloat {
        return 0
    }

    override func getCellIdentifier(for indexPath: IndexPath) -> String {

        return VerifyIdResultsTableViewCell.reuseIdentifier
    }

    override func getCellNib(for indexPath: IndexPath) -> String? {

        return VerifyIdResultsTableViewCell.default_NibName()
    }

    // MARK: Buttons

    func setupButtons() {
        confirmBttn.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON)
        retryBttn.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON, borderWidth: 3,
                                  borderColor: UIColor.appRed.cgColor)
    }

    @IBAction func confirmTapped(_ sender: Any) {
        presenterImpl.continueTapped()
    }

    @IBAction func retryTapped(_ sender: Any) {
        presenterImpl.retryTapped()
    }

    // MARK: Screens

    func changeScreenToScanFront() {
        _ = app_mayPerformSegue(withIdentifier: "VerifyIdScanSegue", sender: self)
    }

}

extension VerifyIdResultsViewController: SegueableScreen {

    func configScreenFromSegue(params: [Any?]?) {

        if let values = params?[0] as? [String] {
            self.presenterImpl.config(values: values)
        }
        presenterImpl.contact = params?[1] as? Contact
        presenterImpl.selfieImg = params?[2] as? UIImage
        presenterImpl.documentInstanceId = params?[3] as? String
    }

    static func makeSeguedParams(values: [String]?, contact: Contact?, image: UIImage?,
                                 documentInstanceId: String?) -> [Any?]? {

        var params: [Any?] = []
        params.append(values)
        params.append(contact)
        params.append(image)
        params.append(documentInstanceId)
        return params
    }
}
