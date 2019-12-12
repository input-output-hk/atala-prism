//

protocol NewDegreeViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: NewDegreeViewCell)
    func tappedAction(for cell: NewDegreeViewCell)
}

class NewDegreeViewCell: BaseTableViewCell {

    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var labelSubtitle: UILabel!
    @IBOutlet weak var buttonIconAction: UIButton!
    @IBOutlet weak var imageLogo: UIImageView!
    @IBOutlet weak var constraintTitleVertical: NSLayoutConstraint!
    @IBOutlet weak var viewSeparator: UIView!
    @IBOutlet weak var viewMainBody: UIView!
    @IBOutlet weak var constraintFooterVertical: NSLayoutConstraint!

    override class func default_NibName() -> String {
        return "NewDegreeViewCell"
    }

    var delegateImpl: NewDegreeViewCellPresenterDelegate? {
        return delegate as? NewDegreeViewCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
        viewMainBody.addDropShadow()
        imageLogo.layer.cornerRadius = AppConfigs.CORNER_RADIUS_REGULAR
    }

    // MARK: Component delegates

    @IBAction func actionMainButtonTapped(_ sender: Any) {
        self.delegateImpl?.tappedAction(for: self)
    }

    // MARK: Config

    func config(title: String?, subtitle: String?, logoUrl: String?, logoPlaceholderNamed: String, isLast: Bool) {

        labelTitle.text = title
        let hideSubtitle = subtitle == nil
        labelSubtitle.isHidden = hideSubtitle
        labelSubtitle.text = subtitle
        constraintTitleVertical.constant = hideSubtitle ? 0.0 : -10.0
        // Logo image
        imageLogo.applyUrlImage(url: logoUrl, placeholderNamed: logoPlaceholderNamed)

        let radius = isLast ? AppConfigs.CORNER_RADIUS_REGULAR : 0.0
        viewMainBody.addRoundCorners(radius: radius, onlyBottoms: true)
        constraintFooterVertical.constant = isLast ? 22.0 : 0
        viewSeparator.isHidden = isLast
    }
}
