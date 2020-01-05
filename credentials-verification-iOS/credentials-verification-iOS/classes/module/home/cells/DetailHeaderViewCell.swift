//

protocol DetailHeaderViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: DetailHeaderViewCell)
}

class DetailHeaderViewCell: BaseTableViewCell {

    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var labelSubtitle: UILabel!
    @IBOutlet weak var imageLogo: UIImageView!
    @IBOutlet weak var viewMainBody: UIView!
    @IBOutlet weak var viewMainBodyShadow: UIView!

    override class func default_NibName() -> String {
        return "DetailHeaderViewCell"
    }

    var delegateImpl: DetailHeaderViewCellPresenterDelegate? {
        return delegate as? DetailHeaderViewCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
        imageLogo.layer.cornerRadius = AppConfigs.CORNER_RADIUS_REGULAR
        viewMainBodyShadow.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR, onlyTops: true)
        viewMainBody.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR, onlyTops: true)
    }

    // MARK: Config

    func config(title: String?, subtitle: String?, logoData: Data?) {

        labelTitle.text = title
        labelSubtitle.text = subtitle
        imageLogo.applyDataImage(data: logoData, placeholderNamed: "ico_placeholder_university")
    }
}
