//

protocol DetailPropertyViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: DetailPropertyViewCell)
}

class DetailPropertyViewCell: BaseTableViewCell {

    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var labelSubtitle: UILabel!

    override class func default_NibName() -> String {
        return "DetailPropertyViewCell"
    }

    var delegateImpl: DetailPropertyViewCellPresenterDelegate? {
        return delegate as? DetailPropertyViewCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
    }

    // MARK: Config

    func config(title: String?, subtitle: String?) {

        labelTitle.text = title
        labelSubtitle.text = subtitle
    }
}
