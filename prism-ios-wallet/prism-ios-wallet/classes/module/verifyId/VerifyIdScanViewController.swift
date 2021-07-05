//
//  VerifyIdScanViewController.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 01/02/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import UIKit

class VerifyIdScanViewController: BaseViewController {

    var presenterImpl = VerifyIdScanPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    @IBOutlet weak var bgView: UIView!
    @IBOutlet weak var idImg: UIImageView!
    @IBOutlet weak var titleLbl: UILabel!
    @IBOutlet weak var helperLbl: UILabel!
    @IBOutlet weak var scanBttn: UIButton!
    @IBOutlet weak var stepLbl: UILabel!
    @IBOutlet weak var stepTitleLbl: UILabel!
    @IBOutlet weak var stepDescLbl: UILabel!

    var navBar: NavBarCustomStyle = NavBarCustomStyle(hasNavBar: true, title: "verifyid_title".localize(),
                                                      hasBackButton: true)
    override func navBarCustomStyle() -> NavBarCustomStyle {
        return navBar
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        setupButtons()
        bgView.addRoundCorners(radius: 30, onlyBottoms: true)
    }

    func toogleTitle(isBack: Bool) {
        stepLbl.text = "verifyid_typeselect_step".localize()
        stepTitleLbl.text = "verifyid_scanfront_title".localize()
        stepDescLbl.text = "verifyid_scanfront_desc".localize()
        idImg.image = isBack ? #imageLiteral(resourceName: "img_id_back") : #imageLiteral(resourceName: "img_id_front")
        titleLbl.text = isBack ? "verifyid_scanback_back".localize() : "verifyid_scanfront_front".localize()
        helperLbl.text = isBack ? "verifyid_scanback_helper".localize() : "verifyid_scanfront_helper".localize()
    }

    func toogleToSelfie() {
        stepLbl.text = "verifyid_selfie_step".localize()
        stepTitleLbl.text = "verifyid_selfie_title".localize()
        stepDescLbl.text = "verifyid_selfie_desc".localize()
        idImg.image = #imageLiteral(resourceName: "img_selfie")
        titleLbl.text = "verifyid_selfie_helper_title".localize()
        helperLbl.text = "verifyid_selfie_helper".localize()
    }

    // MARK: Screens

    func changeScreenToTypeSelect() {
        _ = app_mayPerformSegue(withIdentifier: "VerifyIdTypeSelectSegue", sender: self)
    }

    func changeScreenToResults(values: [String]?, contact: Contact?, image: UIImage?, documentInstanceId: String?) {
        let params = VerifyIdResultsViewController.makeSeguedParams(values: values, contact: contact, image: image,
                                                                    documentInstanceId: documentInstanceId)
        ViewControllerUtils.changeScreenSegued(caller: self, segue: "showVerifyIdResultsSegue", params: params)
    }

    func changeScreenToError() {
        ViewControllerUtils.changeScreenSegued(caller: self, segue: "showVerifyIdErrorSegue", params: nil)
    }

    // MARK: Buttons

    func setupButtons() {
        scanBttn.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON)
    }

    @IBAction func scanTapped(_ sender: Any) {
        presenterImpl.scanTapped()
    }

}

extension VerifyIdScanViewController: SegueableScreen {

    func configScreenFromSegue(params: [Any?]?) {

        presenterImpl.documentInstanceID = params?[0] as? String
        presenterImpl.kycToken = params?[1] as? String
        presenterImpl.contact = params?[2] as? Contact
    }

    static func makeSeguedParams(documentInstanceID: String?, kycToken: String?,
                                 contact: Contact?) -> [Any?]? {

        var params: [Any?] = []
        params.append(documentInstanceID)
        params.append(kycToken)
        params.append(contact)
        return params
    }
}
