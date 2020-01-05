//

protocol DocumentViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: DocumentViewCell)
}

class DocumentViewCell: BaseTableViewCell {

    /*@IBOutlet weak var labelTitle: UILabel!
     @IBOutlet weak var labelSubtitle: UILabel!
     @IBOutlet weak var buttonIconAction: UIButton!
     @IBOutlet weak var imageLogo: UIImageView!
     @IBOutlet weak var constraintTitleVertical: NSLayoutConstraint!*/

    override class func default_NibName() -> String {
        return "DocumentViewCell"
    }

    var delegateImpl: DocumentViewCellPresenterDelegate? {
        return delegate as? DocumentViewCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
    }

    // MARK: Config

    func config(title: String?, isUniversity: Bool, logoData: Data?) {

        // Not for this version
    }
}
