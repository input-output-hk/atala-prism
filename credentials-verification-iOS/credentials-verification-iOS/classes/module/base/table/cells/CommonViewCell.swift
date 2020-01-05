//

protocol CommonViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: CommonViewCell)
    func tappedAction(for cell: CommonViewCell)
}

class CommonViewCell: BaseTableViewCell {

    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var labelSubtitle: UILabel!
    @IBOutlet weak var buttonIconAction: UIButton!
    @IBOutlet weak var buttonComingSoon: UIButton!
    @IBOutlet weak var imageLogo: UIImageView!
    @IBOutlet weak var constraintTitleVertical: NSLayoutConstraint!
    @IBOutlet weak var viewMainBody: UIView!
    @IBOutlet weak var constraintTitleTrailing: NSLayoutConstraint!

    override class func default_NibName() -> String {
        return "CommonViewCell"
    }

    var delegateImpl: CommonViewCellPresenterDelegate? {
        return delegate as? CommonViewCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
        // viewMainBody.addDropShadow(opacity: 0.4)
        viewMainBody.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR)
        viewMainBody.addShadowLayer(opacity: 0.1)
        imageLogo.layer.cornerRadius = AppConfigs.CORNER_RADIUS_REGULAR
    }

    // MARK: Component delegates

    @IBAction func actionMainButtonTapped(_ sender: Any) {
        self.delegateImpl?.tappedAction(for: self)
    }

    // MARK: Config

    func config(title: String?, subtitle: String?, logoData: Data?, logoPlaceholderNamed: String, isComingSoon: Bool = false) {

        labelTitle.text = title
        let hideSubtitle = subtitle == nil
        labelSubtitle.isHidden = hideSubtitle
        labelSubtitle.text = subtitle
        constraintTitleVertical.constant = hideSubtitle ? 0.0 : -10.0
        imageLogo.applyDataImage(data: logoData, placeholderNamed: logoPlaceholderNamed)
        buttonIconAction.isHidden = isComingSoon
        buttonComingSoon.isHidden = !isComingSoon
        constraintTitleTrailing.constant = !isComingSoon ? 17.0 : 70.0
    }
}
