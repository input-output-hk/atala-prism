//

protocol ProfileViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: ProfileViewCell)
    func choosePicture()
}

class ProfileViewCell: BaseTableViewCell {

    @IBOutlet weak var viewBg: UIView!
    @IBOutlet weak var labelName: UILabel!
    @IBOutlet weak var labelEmail: UILabel!
    @IBOutlet weak var imageLogo: UIImageView!
    @IBOutlet weak var imageVerified: UIImageView!
    @IBOutlet weak var constraintNameHorizontalCenter: NSLayoutConstraint!
    @IBOutlet weak var imageLogoBttn: UIButton!

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
        imageLogoBttn.addRoundCorners(radius: 40)
    }

    // MARK: Config

    func config(name: String?, email: String?, isVerified: Bool, logoData: Data?, isEnable: Bool = true) {

        labelName.text = name
        labelEmail.text = email
        imageVerified.isHidden = !isVerified
        constraintNameHorizontalCenter.constant = isVerified ? -10 : 0
        imageLogo.applyDataImage(data: logoData, isCircular: true, placeholderNamed: "ico_placeholder_user")
        imageLogo.addDropShadow()
        imageLogoBttn.isHidden = !isEnable
    }

    @IBAction func choosePictureTapped(_ sender: Any) {
        delegateImpl?.choosePicture()
    }
}
