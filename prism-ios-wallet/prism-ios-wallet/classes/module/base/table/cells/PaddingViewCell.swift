//

protocol PaddingViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: PaddingViewCell)
}

class PaddingViewCell: BaseTableViewCell {

    @IBOutlet weak var constraintHeight: NSLayoutConstraint!

    override class func default_NibName() -> String {
        return "PaddingViewCell"
    }

    var delegateImpl: PaddingViewCellPresenterDelegate? {
        return delegate as? PaddingViewCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
    }

    func config(size: CGFloat) {
        constraintHeight.constant = size
    }
}
