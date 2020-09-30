//
//  RestoreAccountViewController.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 28/07/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import UIKit
import MaterialComponents

class RestoreAccountViewController: BaseViewController {

    @IBOutlet weak var wordsContainer: UIView!
    @IBOutlet weak var errorBg: UIView!
    @IBOutlet weak var errorIcon: UIImageView!
    @IBOutlet weak var errorLbl: UILabel!
    @IBOutlet weak var verifyBttn: UIButton!

    var chipsField: MDCChipField!

    var presenterImpl = RestoreAccountPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        setupButtons()
        setupViews()

        ViewControllerUtils.addTapToDismissKeyboard(view: self)    }

    // MARK: Setup

    func setupButtons() {
        verifyBttn.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON)
    }

    func setupViews() {
        wordsContainer.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR, borderWidth: 1,
                                       borderColor: UIColor.appGreyBlue.cgColor)
        errorBg.addRoundCorners(radius: 4)
        chipsField = MDCChipField(frame: CGRect(x: 0, y: 0, width: wordsContainer.frame.width,
                                                    height: wordsContainer.frame.height))

        chipsField.minTextFieldWidth = 100
        chipsField.showChipsDeleteButton = true
        chipsField.delegate = self
        chipsField.textField.placeholder = "restore_hint".localize()
        chipsField.textField.clearButtonMode = .never

        wordsContainer.addSubview(chipsField)

        wordsContainer.addConstraints(NSLayoutConstraint.constraints(withVisualFormat: "H:|-[chipsField]-|",
                                                                     options: [],
                                                                     metrics: nil,
                                                                     views: ["chipsField": chipsField]))
        wordsContainer.addConstraints(NSLayoutConstraint.constraints(withVisualFormat: "V:|-[chipsField]-|",
                                                                     options: [],
                                                                     metrics: nil,
                                                                     views: ["chipsField": chipsField]))

    }

    @objc func removeChip(sender: MDCChipView!) {
        sender.removeFromSuperview()
    }
    override func navBarCustomStyle() -> NavBarCustomStyle {
        return NavBarCustomStyle(hasNavBar: true, title: nil, hasBackButton: true)
    }

    func enableVerifyButton(enable: Bool) {

        verifyBttn.isEnabled = enable
        verifyBttn.backgroundColor = enable ? .appRed : .appGreyMid
    }

    // MARK: Buttons

    @IBAction func verifyTapped(_ sender: Any) {
        let mnemonics = chipsField.chips.map { chip -> String in
            return chip.titleLabel.text?.lowercased() ?? ""
        }
        presenterImpl.tappedVerifyButton(mnemonics: mnemonics)
    }

    // MARK: Screens

    func changeScreenToSuccess(action: SelectorAction) {

        let params = SuccessViewController.makeSeguedParams(title: "success_restore_title".localize(),
                                                            subtitle: "success_restore_subtitle".localize(),
                                                            buttonText: "success_restore_button".localize(),
                                                            buttonAction: action)
        ViewControllerUtils.changeScreenSegued(caller: self, segue: "SuccessSegue", params: params)
    }

    func goToMainScreen() {
        MainNavViewController.openThisView()
    }

}

extension RestoreAccountViewController: MDCChipFieldDelegate {

    func chipField(_ chipField: MDCChipField, shouldAddChip chip: MDCChipView) -> Bool {
        chip.setBackgroundColor(UIColor(netHex: 0xFFEAEB), for: .normal)
        chip.setTitleColor(.appRed, for: .normal)
        chip.setInkColor(UIColor(netHex: 0xFFEAEB), for: .normal)
        if let clearBttn = chip.accessoryView as? UIControl,
            let image = clearBttn.subviews[0] as? UIImageView {
                image.image = #imageLiteral(resourceName: "ico_delete-1")
        }
        return chipField.chips.count < 12
    }

    func chipField(_ chipField: MDCChipField, didAddChip chip: MDCChipView) {
        let isComplete = chipField.chips.count < 12
        chipField.textField.isEnabled = isComplete
        chipField.textField.placeholder = isComplete ? "restore_hint".localize() : ""
        enableVerifyButton(enable: !isComplete)
    }

    func chipField(_ chipField: MDCChipField, didRemoveChip chip: MDCChipView) {
        chipField.textField.isEnabled = true
        chipField.textField.placeholder = "restore_hint".localize()
        enableVerifyButton(enable: false)
    }
}
