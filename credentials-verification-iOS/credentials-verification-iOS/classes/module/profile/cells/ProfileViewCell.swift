//

protocol ProfileViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: ProfileViewCell)
}

class ProfileViewCell: BaseTableViewCell {

    @IBOutlet weak var viewBg: UIView!
    @IBOutlet weak var labelName: UILabel!
    @IBOutlet weak var labelEmail: UILabel!
    @IBOutlet weak var imageLogo: UIImageView!
    @IBOutlet weak var imageVerified: UIImageView!
    @IBOutlet weak var constraintNameHorizontalCenter: NSLayoutConstraint!

    override class func default_NibName() -> String {
        return "ProfileViewCell"
    }

    var delegateImpl: ProfileViewCellPresenterDelegate? {
        return delegate as? ProfileViewCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
        viewBg.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR)
        viewBg.addShadowLayer()
    }

    // MARK: Config

    func config(name: String?, email: String?, isVerified: Bool, logoUrl: String?) {

        labelName.text = name
        labelEmail.text = email
        imageVerified.isHidden = !isVerified
        constraintNameHorizontalCenter.constant = isVerified ? -10 : 0
        imageLogo.applyUrlImage(url: logoUrl, isCircular: true, placeholderNamed: "ico_placeholder_user")
        imageLogo.addDropShadow()
    }
}
