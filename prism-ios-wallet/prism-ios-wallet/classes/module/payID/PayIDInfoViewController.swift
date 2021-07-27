//
//  PayIDInfoViewController.swift
//  prism-ios-wallet
//
//  Created by Roberto Daviduk on 22/03/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Foundation
import UIKit

class PayIDInfoViewController: BaseViewController, PayIDAddAddressDelegate {
 
    @IBOutlet weak var subtitleLabel: UILabel!

    @IBOutlet weak var contentView: UIView!
    
    @IBOutlet weak var headerView: UIView!

    @IBOutlet weak var nameValueLabel: UILabel!
    @IBOutlet weak var addressValueLabel: UILabel!
    @IBOutlet weak var keyValueLabel: UILabel!

    @IBOutlet weak var addAddressButton: UIButton!

    @IBOutlet weak var newButton: UIButton!

    var presenterImpl = PayIDInfoPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    override func navBarCustomStyle() -> NavBarCustomStyle {
        return NavBarCustomStyle(hasNavBar: true, isWhite: false,
                                 title: "pay_id_info_title".localize(), hasBackButton: true)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        contentView.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR)
        contentView.addShadowLayer(opacity: 0.1)
        headerView.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR, onlyTops: true)
        addAddressButton.addRoundCorners(radius: 15.0, borderWidth: AppConfigs.BORDER_WIDTH_BUTTON,
                                         borderColor: UIColor.appRed.cgColor)
        newButton.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON)
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        presenterImpl.fetchPayId()
    }

    func setupData() {
        nameValueLabel.text = "\(presenterImpl.payId?.name ?? "")\("pay_id_setup_name_field_right".localize())"
        addressValueLabel.text = "\(presenterImpl.payId?.addresses?.count ?? 0)"
        keyValueLabel.text = "\(presenterImpl.payId?.publicKeys?.count ?? 0)"
    }

    override func onBackPressed() -> Bool {
        dismiss(animated: true, completion: nil)
        return super.onBackPressed()
    }

    // MARK: Button

    @IBAction func share(_ sender: Any) {
        presenterImpl.share()
    }

    @IBAction func remove(_ sender: Any) {
            presenterImpl.removeTapped()
    }

    @IBAction func showAddress(_ sender: Any) {
        changeScreenWith(title: "pay_id_listing_address_title".localize(),
                         type: "pay_id_listing_address_row_title".localize(), isAddress: true)
    }

    @IBAction func showKey(_ sender: Any) {
        changeScreenWith(title: "pay_id_listing_key_title".localize(),
                         type: "pay_id_listing_key_row_title".localize(), isAddress: false)
    }

    @IBAction func addAddress(_ sender: Any) {

        var params: [Any?] = []
        params.append(self)

        ViewControllerUtils.changeScreenSegued(caller: self, segue: "SeguePayIDAddAddressViewController",
                                               params: params)
    }

    @IBAction func addKey(_ sender: Any) {

        var params: [Any?] = []
        params.append(self)

        ViewControllerUtils.changeScreenSegued(caller: self, segue: "SeguePayIDAddAddressViewController",
                                               params: params)

    }

    @IBAction func new(_ sender: Any) {

    }

    func showAler() {
        let popUp = PopupAlertViewController.makeThisView()

        popUp.setupWith(logoName: "logo_popup_success", title: "pay_id_success_title".localize(),
                        message: "pay_id_share_success_message".localize())

        customPresentViewController(popUp.presentr, viewController: popUp, animated: true)
    }

    func changeScreenWith(title: String, type: String, isAddress: Bool) {

        var params: [Any?] = []
        params.append(title)
        params.append(type)
        params.append(isAddress)

        ViewControllerUtils.changeScreenSegued(caller: self, segue: "SeguePayIDInfoListingViewController",
                                               params: params)
    }

    // MARK: Add Address

    func addAddress(value: String) {
        presenterImpl.addAddressOrKey(value: value)
    }

    // MARK: Delete

    func showDeletePayIdConfirmation() {
        let confirmation = DeletePayIdViewController.makeThisView()
        confirmation.config(payId: presenterImpl.payId) {
            self.presenterImpl.deletePayId()
        }
        customPresentViewController(confirmation.presentr, viewController: confirmation, animated: true)

    }
}
