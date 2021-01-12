//

protocol CredentialPayViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: CredentialPayViewCell)
}

class CredentialPayViewCell: BaseTableViewCell {

    @IBOutlet weak var textField: TextFieldTitledView!

    override class func default_NibName() -> String {
        return "CredentialPayViewCell"
    }

    var delegateImpl: CredentialPayViewCellPresenterDelegate? {
        return delegate as? CredentialPayViewCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
        textField.textField.isUserInteractionEnabled = false
        textField.config(title: "credentialpay_amount".localize())
    }

    // MARK: Config

    func config(amount: String?) {

        textField.textField.text = "       \(amount ?? "")"
    }
}
