//

class ListingLoadingViewCell: BaseTableViewCell {

    @IBOutlet weak var spinner: UIActivityIndicatorView!

    override class func default_NibName() -> String {
        return "ListingLoadingViewCell"
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        spinner.startAnimating()
    }
}
