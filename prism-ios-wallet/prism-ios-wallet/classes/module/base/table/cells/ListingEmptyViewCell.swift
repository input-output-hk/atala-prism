//

protocol ListingEmptyViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: ListingEmptyViewCell)
    func emptyRetryTapped(for cell: ListingEmptyViewCell)
}

class ListingEmptyViewCell: BaseTableViewCell {

    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var labelSubtitle: UILabel!
    @IBOutlet weak var imageViewIcon: UIImageView!
    @IBOutlet weak var buttonRetry: UIButton!

    override class func default_NibName() -> String {
        return "ListingEmptyViewCell"
    }

    var delegateImpl: ListingEmptyViewCellPresenterDelegate? {
        return delegate as? ListingEmptyViewCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
        // imageViewIcon.setIcon(icon: .fontAwesomeSolid(.exclamationCircle), textColor: .colorDarkMidText)
        imageViewIcon.isHidden = true
        buttonRetry.addRoundCorners(radius: 20, borderWidth: 2, borderColor: UIColor.appWhite.cgColor)
    }

    // MARK: Component delegates

    @IBAction func buttonRetryAction(_ sender: Any) {
        self.delegateImpl?.emptyRetryTapped(for: self)
    }

    // MARK: Config

    func config(title: String?, subtitle: String? = nil) {

        labelTitle.text = title
        labelSubtitle.text = title
        labelSubtitle.isHidden = subtitle == nil
    }
}
