//

protocol DetailFooterViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: DetailFooterViewCell)
    func tappedDeclineAction(for cell: DetailFooterViewCell)
    func tappedConfirmAction(for cell: DetailFooterViewCell)
}

class DetailFooterViewCell: BaseTableViewCell {

    @IBOutlet weak var labelStartValue: UILabel!
    @IBOutlet weak var labelEndValue: UILabel!
    @IBOutlet weak var viewMainBody: UIView!
    @IBOutlet weak var viewButtons: UIView!
    @IBOutlet weak var buttonDecline: UIButton!
    @IBOutlet weak var buttonConfirm: UIButton!

    override class func default_NibName() -> String {
        return "DetailFooterViewCell"
    }

    var delegateImpl: DetailFooterViewCellPresenterDelegate? {
        return delegate as? DetailFooterViewCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
        viewMainBody.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR, onlyBottoms: true)
        buttonDecline.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON, borderWidth: 3, borderColor: UIColor.appRed.cgColor)
        buttonConfirm.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON, borderWidth: 3, borderColor: UIColor.appRed.cgColor)
    }

    // MARK: Component delegates

    @IBAction func actionConfirmButtonTapped(_ sender: Any) {
        self.delegateImpl?.tappedConfirmAction(for: self)
    }

    @IBAction func actionDeclineButtonTapped(_ sender: Any) {
        self.delegateImpl?.tappedDeclineAction(for: self)
    }

    // MARK: Config

    func config(startDate: String?, endDate: String?, isNew: Bool) {

        labelStartValue.text = startDate
        labelEndValue.text = endDate

        viewButtons.isHidden = !isNew
    }
}
