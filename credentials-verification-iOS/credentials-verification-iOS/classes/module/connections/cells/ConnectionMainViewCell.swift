//

protocol ConnectionMainViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: ConnectionMainViewCell)
    func tappedAction(for cell: ConnectionMainViewCell)
}

class ConnectionMainViewCell: BaseTableViewCell {

    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var labelSubtitle: UILabel!
    @IBOutlet weak var buttonIconAction: UIButton!
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
        imageLogo.layer.cornerRadius = AppConfigs.CORNER_RADIUS_REGULAR
    }

    // MARK: Component delegates

    @IBAction func actionMainButtonTapped(_ sender: Any) {
        self.delegateImpl?.tappedAction(for: self)
    }

    // MARK: Config

    func config(title: String?, isUniversity: Bool, logoData: Data?) {

        labelTitle.text = title
        labelSubtitle.isHidden = isUniversity
        constraintTitleVertical.constant = isUniversity ? 0.0 : -10.0
        if isUniversity {
            buttonIconAction.setImage(#imageLiteral(resourceName: "ico_caret"), for: .normal)
        } else {
            buttonIconAction.isHidden = true
        }
        imageLogo.applyDataImage(data: logoData, placeholderNamed: isUniversity ? "ico_placeholder_university" : "ico_placeholder_employer")
    }
}
