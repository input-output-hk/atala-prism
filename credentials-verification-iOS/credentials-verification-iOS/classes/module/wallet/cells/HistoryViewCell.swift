//

protocol HistoryViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: HistoryViewCell)
}

class HistoryViewCell: BaseTableViewCell {

    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var labelDate: UILabel!
    @IBOutlet weak var buttonStatus: UIButton!
    @IBOutlet weak var labelAmount: UILabel!

    override class func default_NibName() -> String {
        return "HistoryViewCell"
    }

    var delegateImpl: HistoryViewCellPresenterDelegate? {
        return delegate as? HistoryViewCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
    }

    // MARK: Config

    func config(title: String?, date: String?, amount: String?, status: Int) {

        labelTitle.text = title
        labelDate.text = date
        labelAmount.text = "\(amount ?? "-")"

        let statusColor = UIColor(hexString: status == 0 ? "00D793" : "FFB954")
        buttonStatus.setTitle((status == 0 ? "wallet_history_status_completed" : "wallet_history_status_pending").localize(), for: .normal)
        buttonStatus.setTitleColor(statusColor, for: .normal)
        buttonStatus.addRoundCorners(radius: 13, borderWidth: 1.5, borderColor: statusColor.cgColor)
    }
}
