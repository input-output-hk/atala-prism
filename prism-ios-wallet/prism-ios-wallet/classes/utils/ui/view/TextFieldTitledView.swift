//
import UIKit

protocol TextFieldTitledViewDelegate: UITextFieldDelegate {
    func textFieldDidChange(for view: TextFieldTitledView, textField: UITextField, text: String?)
}

@IBDesignable class TextFieldTitledView: BaseNibLoadingView {

    @IBOutlet weak var textField: UITextField!
    @IBOutlet weak var viewBorder: UIView!
    @IBOutlet weak var viewLabelContainer: UIView!
    @IBOutlet weak var viewLabelLine: UIView!
    @IBOutlet weak var labelTitle: UILabel!

    weak var delegate: TextFieldTitledViewDelegate?

    override func commonInit() {
        super.commonInit()

        viewBorder.addRoundCorners(radius: 10, borderWidth: 1.5, borderColor: UIColor.appTextfieldBorderColor.cgColor)
    }

    func config(delegate: TextFieldTitledViewDelegate, bgColor: UIColor = UIColor.white) {

        self.delegate = delegate
        self.textField.delegate = delegate
        self.viewLabelLine.backgroundColor = bgColor
        self.viewBorder.backgroundColor = bgColor
        self.textField.addTarget(self, action: #selector(textFieldDidChange(_:)), for: UIControl.Event.editingChanged)
    }

    func changeBorderColorIf(isEditing: Bool) {
        if isEditing {
            viewBorder.layer.borderColor = UIColor.appTextfieldBorderColor.cgColor
        }else{
            viewBorder.layer.borderColor = UIColor.clear.cgColor
        }
    }
    
    func config(title: String) {

        labelTitle.text = title
    }

    @objc func textFieldDidChange(_ textField: UITextField) {
        delegate?.textFieldDidChange(for: self, textField: textField, text: textField.text)
    }
}
