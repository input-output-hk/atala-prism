//

protocol ConnectionMainViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: ConnectionMainViewCell)
    func tappedDelete(for cell: ConnectionMainViewCell)
}

class ConnectionMainViewCell: BaseTableViewCell {
    
    @IBOutlet weak var viewMainBody: UIView!
    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var labelSubtitle: UILabel!
    @IBOutlet weak var imageLogo: UIImageView!
    @IBOutlet weak var constraintTitleVertical: NSLayoutConstraint!

    override class func default_NibName() -> String {
        return "ConnectionMainViewCell"
    }

    var delegateImpl: ConnectionMainViewCellPresenterDelegate? {
        return delegate as? ConnectionMainViewCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
        imageLogo.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR)
        viewMainBody.addDropShadow(opacity: 0.1)
    }

    // MARK: Component delegates

    @IBAction func actionDeleteTapped(_ sender: Any) {
        self.delegateImpl?.tappedDelete(for: self)
    }

    // MARK: Config

    func config(title: String?, logoData: Data?) {

        labelTitle.text = title
        labelSubtitle.isHidden = true
        constraintTitleVertical.constant = 0.0
        imageLogo.applyDataImage(data: logoData, placeholderNamed: "ico_placeholder_credential")
    }
}
