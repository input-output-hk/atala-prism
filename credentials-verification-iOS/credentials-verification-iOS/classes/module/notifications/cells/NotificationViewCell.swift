//

protocol NotificationViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: NotificationViewCell)
}

class NotificationViewCell: BaseTableViewCell {

    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var labelSubtitle: UILabel!
    @IBOutlet weak var buttonIconAction: UIButton!
    @IBOutlet weak var imageLogo: UIImageView!
    @IBOutlet weak var constraintTitleVertical: NSLayoutConstraint!
    @IBOutlet weak var viewSeparator: UIView!
    @IBOutlet weak var viewMainBody: UIView!
    @IBOutlet weak var constraintFooterVertical: NSLayoutConstraint!
    @IBOutlet weak var labelDate: UILabel!

    override class func default_NibName() -> String {
        return "NotificationViewCell"
    }

    var delegateImpl: NotificationViewCellPresenterDelegate? {
        return delegate as? NotificationViewCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
        imageLogo.layer.cornerRadius = AppConfigs.CORNER_RADIUS_REGULAR
        viewMainBody.addRoundCorners(radius: 6)
        viewMainBody.addDropShadow()
    }

    // MARK: Config

    func config(title: String?, subtitle: String?, logoData: Data?, logoPlaceholderNamed: String, date: Date) {

        labelTitle.text = title
        let hideSubtitle = subtitle == nil
        labelSubtitle.isHidden = hideSubtitle
        labelSubtitle.text = subtitle
        // Logo image
        imageLogo.applyDataImage(data: logoData, placeholderNamed: logoPlaceholderNamed)
        labelDate.text = String(format: "credentials_degrees_new_date".localize(),
                                date.dateTimeString())

    }
}
