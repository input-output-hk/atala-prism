//

protocol FieldViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: FieldViewCell)
    func textFieldDidChange(for cell: FieldViewCell, text: String?)
    func textFieldShouldReturn(for cell: FieldViewCell) -> (Bool, FieldViewCell?)
}

class FieldViewCell: BaseTableViewCell, TextFieldTitledViewDelegate {

    @IBOutlet weak var textField: TextFieldTitledView!
    @IBOutlet weak var viewBg: UIView!

    override class func default_NibName() -> String {
        return "FieldViewCell"
    }

    var delegateImpl: FieldViewCellPresenterDelegate? {
        return delegate as? FieldViewCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
        textField.config(delegate: self, bgColor: UIColor.appWhite)
    }

    // MARK: Config

    func config(title: String?, text: String?, bgColor: UIColor, isEnable: Bool = true,
                hasNext: Bool = false, hasAutocapitalization: Bool = false,
                hasAutocorrection: Bool = false) {

        textField.config(title: title ?? "")
        textField.textField.text = text
        viewBg.backgroundColor = bgColor
        textField.textField.isEnabled = isEnable
        textField.textField.isUserInteractionEnabled = isEnable
        textField.textField.returnKeyType = hasNext ? .next : .done
        textField.textField.autocapitalizationType = hasAutocapitalization ? .words : .none
        textField.textField.autocorrectionType = hasAutocorrection ? .yes : .no
    }

    func textFieldDidChange(for view: TextFieldTitledView, textField: UITextField, text: String?) {
        delegateImpl?.textFieldDidChange(for: self, text: text)
    }

    func textFieldShouldReturn(_ textField: UITextField) -> Bool {

        let res = delegateImpl?.textFieldShouldReturn(for: self) ?? (false, nil)
        if !res.0 {
            res.1?.textField.textField.becomeFirstResponder()
        } else {
            textField.resignFirstResponder()
        }
        return res.0
    }
}
