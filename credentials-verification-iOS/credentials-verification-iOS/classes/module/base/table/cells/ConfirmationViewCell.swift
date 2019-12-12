//

protocol ConfirmationViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: ConfirmationViewCell)
    func tappedAction(for cell: ConfirmationViewCell)
}

class ConfirmationViewCell: BaseTableViewCell {

    @IBOutlet weak var buttonContinue: UIButton!

    override class func default_NibName() -> String {
        return "ConfirmationViewCell"
    }

    var delegateImpl: ConfirmationViewCellPresenterDelegate? {
        return delegate as? ConfirmationViewCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        delegateImpl?.setup(for: self)

        buttonContinue.layer.cornerRadius = AppConfigs.CORNER_RADIUS_BUTTON
    }

    // MARK: Component delegates

    @IBAction func actionMainButtonTapped(_ sender: Any) {
        self.delegateImpl?.tappedAction(for: self)
    }

    // MARK: Config

    func config(title: String?, isEnabled: Bool) {

        buttonContinue.setTitle(title, for: .normal)
        buttonContinue.isEnabled = isEnabled
        buttonContinue.backgroundColor = isEnabled ? .appRed : .appGreyMid
    }
}
