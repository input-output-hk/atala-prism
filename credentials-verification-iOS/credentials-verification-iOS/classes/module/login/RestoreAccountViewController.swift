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

//    @IBOutlet weak var chipsField: MDCChipField!
    @IBOutlet weak var errorBg: UIView!
    @IBOutlet weak var errorIcon: UIImageView!
    @IBOutlet weak var errorLbl: UILabel!
    @IBOutlet weak var verifyBttn: UIButton!
    @IBOutlet weak var mainScroll: UIScrollView!
    @IBOutlet weak var scrollContent: UIView!
    @IBOutlet weak var wordsTitleLbl: UILabel!
    @IBOutlet weak var wordsDescLbl: UILabel!

    var chipsField: MDCChipField!

    var presenterImpl = RestoreAccountPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        setupButtons()
        setupViews()

        ViewControllerUtils.addTapToDismissKeyboard(view: self)
        ViewControllerUtils.addShiftKeyboardListeners(view: self)
    }

    // MARK: Setup

    func setupButtons() {
        verifyBttn.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON)
    }

    func setupViews() {
        errorBg.addRoundCorners(radius: 4)
        chipsField = MDCChipField(frame: CGRect(x: 0, y: 0, width: scrollContent.frame.width,
                                                    height: 50))
        chipsField.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR, borderWidth: 1,
                                       borderColor: UIColor.appGreyBlue.cgColor)

        chipsField.minTextFieldWidth = 100
        chipsField.showChipsDeleteButton = true
        chipsField.delegate = self
        chipsField.textField.placeholder = "restore_hint".localize()
        chipsField.textField.clearButtonMode = .never
        chipsField.delimiter = .all

        scrollContent.addSubview(chipsField)

        chipsField.translatesAutoresizingMaskIntoConstraints = false
        let horizontalConstraints = NSLayoutConstraint.constraints(withVisualFormat: "H:|-(36)-[chipsField]-(36)-|",
                                                                     options: [],
                                                                     metrics: nil,
                                                                     views: ["chipsField": chipsField!])
        let verticalConstraints = NSLayoutConstraint.constraints(withVisualFormat: "V:|-[titleLbl]-(8)-[descLbl]-(25)-[chipsField]-(8)-[error]-|",
                                                                     options: [],
                                                                     metrics: nil,
                                                                     views: ["titleLbl": wordsTitleLbl!,
                                                                             "descLbl": wordsDescLbl!,
                                                                             "chipsField": chipsField!,
                                                                             "error": errorBg!])
        NSLayoutConstraint.activate(horizontalConstraints)
        NSLayoutConstraint.activate(verticalConstraints)
    }

    override func getScrollableMainView() -> UIScrollView? {
        return mainScroll
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
        let mnemonics = chipsField.chips.map {
            getChipWord(chip: $0)
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
        if chip.titleLabel.text?.rangeOfCharacter(from: .whitespacesAndNewlines) != nil {
            return false
        }
        customizeChip(chip: chip)
        return chipField.chips.count < CryptoUtils.SEED_COUNT
    }

    func chipField(_ chipField: MDCChipField, didAddChip chip: MDCChipView) {
        let isComplete = chipField.chips.count < CryptoUtils.SEED_COUNT
        chipField.textField.isEnabled = isComplete
        chipField.textField.placeholder = isComplete ? "restore_hint".localize() : ""
        enableVerifyButton(enable: !isComplete)
        mainScroll.scrollRectToVisible(chipField.textField.frame, animated: true)
    }

    func chipField(_ chipField: MDCChipField, didRemoveChip chip: MDCChipView) {
        chipField.textField.isEnabled = true
        chipField.textField.placeholder = "restore_hint".localize()
        enableVerifyButton(enable: false)
        let index = Int(chip.titleLabel.text?.split(separator: ".")[0] ?? "")
        chipField.delegate = nil
        if index == chipField.chips.count + 1 {
            chipField.textField.text = getChipWord(chip: chip)
        } else {
            let words = chipField.chips.map {
                getChipWord(chip: $0)
            }
            chipField.chips.forEach {
                chipField.removeChip($0)
            }
            words.forEach {
                let chip = MDCChipView()
                chip.titleLabel.text = $0
                addClearButton(toChip: chip)
                customizeChip(chip: chip)
                chipField.addChip(chip)
            }
        }
        chipField.delegate = self
    }

    func chipField(_ chipField: MDCChipField, didChangeInput input: String?) {

        if (input?.rangeOfCharacter(from: .whitespacesAndNewlines)) == nil {
            if let input = input, !input.isEmpty,
               let selectedRange = chipField.textField.selectedTextRange {
                chipField.delegate = nil
                chipField.textField.text = input.filter({
                    !$0.unicodeScalars.contains(where: { !CharacterSet.letters.contains($0)})
                }).lowercased()
                chipField.textField.selectedTextRange = selectedRange
                chipField.delegate = self
            }
            return
        }
        if let words = input?.components(separatedBy: .whitespacesAndNewlines) {
            chipField.delegate = nil
            for word in words where !word.isEmpty {
                let chip = MDCChipView()
                chip.titleLabel.text = word.filter({
                    !$0.unicodeScalars.contains(where: { !CharacterSet.letters.contains($0)})
                }).lowercased()
                addClearButton(toChip: chip)
                customizeChip(chip: chip)
                chipField.addChip(chip)
                if chipsField.chips.count == CryptoUtils.SEED_COUNT {
                    break
                }
            }
            chipField.textField.text = ""
            chipField.delegate = self
            if chipsField.chips.count == CryptoUtils.SEED_COUNT {
                chipField.textField.isEnabled = false
                chipField.textField.placeholder = ""
                enableVerifyButton(enable: true)
            }
        }
    }

    func customizeChip(chip: MDCChipView) {
        chip.setBackgroundColor(.appRedLight, for: .normal)
        chip.setBackgroundColor(.appRed, for: .selected)
        chip.setTitleColor(.appRed, for: .normal)
        chip.setTitleColor(.appRedLight, for: .selected)
        chip.setInkColor(.appRedLight, for: .normal)
        if let clearBttn = chip.accessoryView as? UIControl,
            let image = clearBttn.subviews[0] as? UIImageView {
                image.image = #imageLiteral(resourceName: "ico_delete_x")
        }
        chip.titleLabel.text = "\(chipsField.chips.count + 1). \(chip.titleLabel.text ?? "")"
    }

    func addClearButton(toChip: MDCChipView) {
        let clearButton = UIControl(frame: CGRect(x: 0, y: 0, width: 24, height: 24))
        clearButton.addSubview(UIImageView(frame: CGRect(x: 3, y: 3, width: 18, height: 18)))
        toChip.accessoryView = clearButton
        clearButton.addTarget(self, action: #selector(removePastedChip(sender:)), for: .touchUpInside)

    }

    @objc func removePastedChip(sender: UIControl) {
        if let chip = sender.superview as? MDCChipView {
            chipsField.removeChip(chip)
        }
    }

    func getChipWord(chip: MDCChipView) -> String {
        guard let word = chip.titleLabel.text else { return "" }
        let parts = word.split(separator: ".")
        return parts.count > 1 ? parts[1].lowercased().trim() : parts[0].lowercased()
    }
}
