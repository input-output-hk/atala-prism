//

protocol NewDegreeHeaderViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: NewDegreeHeaderViewCell)
}

class NewDegreeHeaderViewCell: BaseTableViewCell {

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
        viewMainBody.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR, onlyTops: true)
    }
}
