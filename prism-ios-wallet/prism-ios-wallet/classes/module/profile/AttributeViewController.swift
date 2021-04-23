//
//  AttributeViewController.swift
//  prism-ios-wallet
//
//  Created by Roberto Daviduk on 26/02/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import Presentr
import UIKit

class AttributeViewController: UIViewController, PickerViewUtilsDelegate {
    
    @IBOutlet weak var viewBg: UIView!
    @IBOutlet weak var typetextField: TextFieldTitledView!
    @IBOutlet weak var buttonAdd: UIButton!

    var pickerView: UIPickerView?
    var pickerViewUtils: PickerViewUtils?
    
    var types: [String] = []

    var onAccept: ((String, String) -> Void)!

    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
        viewBg.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR)
        
        typetextField.config(title: "attribute_field_attribute".localize())
        typetextField.textField.isEnabled = false
        typetextField.textField.isUserInteractionEnabled = true
        typetextField.textField.addDropDownIcon()
        
        buttonAdd.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON)
        
        setup()
    }

    static func makeThisView() -> AttributeViewController {
        let storyboard = UIStoryboard(name: "Attribute", bundle: nil)
        if let viewcontroller = storyboard.instantiateViewController(withIdentifier: "Attribute")
            as? AttributeViewController {
            return viewcontroller
        }
        return AttributeViewController()
    }

    let presentr: Presentr = {

        let presenter = Presentr(presentationType: .alert)
        presenter.transitionType = TransitionType.coverHorizontalFromRight
        presenter.dismissOnSwipe = false

        let width = ModalSize.sideMargin(value: 21.0)
        let height = ModalSize.custom(size: 367)
        let center = ModalCenterPosition.center
        presenter.presentationType = .custom(width: width, height: height, center: center)
        presenter.transitionType = nil
        presenter.dismissTransitionType = nil
        presenter.dismissAnimated = true

        return presenter
    }()

    func config(onAccept: @escaping (_ type: String, _ logo: String) -> Void) {
        self.onAccept = onAccept
    }
    
    // MARK: Type Picker
    
    func setup() {
        
        let tap = UITapGestureRecognizer(target: self, action:#selector(self.openPicker(_:)))
        typetextField.addGestureRecognizer(tap)
        
        types.append("profile_attribute_email".localize())
        types.append("profile_attribute_phone".localize())
        
        typetextField.textField.text = types[0]
                
        pickerView = UIPickerView(frame: CGRect(x: 5, y: 20, width: self.view.frame.size.width - 20, height: 140))
        
        pickerViewUtils = PickerViewUtils.init(view: self, picker: pickerView!)
        pickerViewUtils?.reloadPickerView()
    }
    
    @objc func openPicker(_ sender: UITapGestureRecognizer) {

        let alert = UIAlertController(title: "attribute_field_attribute".localize(), message: "\n\n\n\n\n\n", preferredStyle: UIAlertController.Style.actionSheet)
                 
        alert.view.addSubview(self.pickerView!)

        let cancelAction = UIAlertAction(title: "close".localize(), style: .cancel, handler: nil)
        alert.addAction(cancelAction)
                
        self.present(alert, animated: true, completion: nil)
    }
    
    func valueComponents() -> [String] {
        return types
    }
    
    func didSelectRowInComponent(_ value: String) {
        typetextField.textField.text = value
    }

    // MARK: Component delegates

    @IBAction func addAttribute(_ sender: Any) {
    
        self.dismiss(animated: true, completion: nil)
        
        var logo: String?
        
        switch typetextField.textField.text! {
        case "profile_attribute_email".localize():
            logo = "logo_email"
            break
        case "profile_attribute_phone".localize():
            logo = "logo_phone"
            break
        default:
            break
        }
        self.onAccept(typetextField.textField.text ?? "", logo!)
    }
    
    // MARK: Presentr Delegate

    func presentrShouldDismiss(keyboardShowing: Bool) -> Bool {
        return false
    }

    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        
        textField.resignFirstResponder()
        return true
    }
}
