//

protocol NewDegreeHeaderViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: NewDegreeHeaderViewCell)
}

class NewDegreeHeaderViewCell: BaseTableViewCell {

    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var viewMainBody: UIView!

    override class func default_NibName() -> String {
        return "NewDegreeHeaderViewCell"
    }

    var delegateImpl: NewDegreeHeaderViewCellPresenterDelegate? {
        return delegate as? NewDegreeHeaderViewCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
        viewMainBody.addDropShadow()
    }

    // MARK: Config

    func config(name: String?) {

        let greeting = String(format: "credentials_degrees_new_hello".localize(), name ?? "")
        labelTitle.text = greeting

        viewMainBody.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR, onlyTops: true)
    }
}
