//

protocol ListingErrorRetryViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: ListingErrorRetryViewCell)
    func errorRetryTapped(for cell: ListingErrorRetryViewCell)
}

class ListingErrorRetryViewCell: BaseTableViewCell {

    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var labelSubtitle: UILabel!
    @IBOutlet weak var imageViewIcon: UIImageView!
    @IBOutlet weak var buttonRetry: UIButton!

    override class func default_NibName() -> String {
        return "ListingErrorRetryViewCell"
    }

    var delegateImpl: ListingErrorRetryViewCellPresenterDelegate? {
        return delegate as? ListingErrorRetryViewCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
        // imageViewIcon.setIcon(icon: .fontAwesomeSolid(.exclamationCircle), textColor: .colorDarkMidText)
        buttonRetry.addRoundCorners(radius: 20, borderWidth: 2, borderColor: UIColor.appWhite.cgColor)
    }

    // MARK: Component delegates

    @IBAction func buttonRetryAction(_ sender: Any) {
        self.delegateImpl?.errorRetryTapped(for: self)
    }

    // MARK: Config

    func config(title: String?, subtitle: String?) {

        labelTitle.text = title
        labelSubtitle.text = title
    }
}
