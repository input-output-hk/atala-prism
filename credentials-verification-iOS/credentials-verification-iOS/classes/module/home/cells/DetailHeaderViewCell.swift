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

    func config(title: String?, subtitle: String?, logoData: Data?, type: CredentialType?) {

        labelTitle.text = title
        labelSubtitle.text = subtitle
        switch type {
        case .univerityDegree:
            imageLogo.applyDataImage(data: logoData, placeholderNamed: "ico_placeholder_university")
            viewMainBody.backgroundColor = UIColor(red: 0.149, green: 0.137, blue: 0.141, alpha: 1)
            viewMainBodyShadow.backgroundColor = UIColor(red: 0.467, green: 0.525, blue: 0.62, alpha: 0.2)
            labelTitle.textColor = .white
            labelSubtitle.textColor = .white
        case .governmentIssuedId:
            imageLogo.applyDataImage(data: nil, placeholderNamed: "redland_flag")
            viewMainBody.backgroundColor = UIColor(red: 0.592, green: 0.592, blue: 0.592, alpha: 0.15)
            viewMainBodyShadow.backgroundColor = .clear
            labelTitle.textColor = UIColor(red: 0.51, green: 0.51, blue: 0.51, alpha: 1)
            labelSubtitle.textColor = UIColor(red: 0.235, green: 0.224, blue: 0.227, alpha: 1)
        case .certificatOfInsurance:
            imageLogo.applyDataImage(data: logoData, placeholderNamed: "ico_placeholder_insurance")
            viewMainBody.backgroundColor = UIColor(red: 0.29, green: 0.176, blue: 1, alpha: 1)
            viewMainBodyShadow.backgroundColor = UIColor(red: 0.467, green: 0.525, blue: 0.62, alpha: 0.2)
            labelTitle.textColor = .white
            labelSubtitle.textColor = .white
        case .proofOfEmployment:
            imageLogo.applyDataImage(data: logoData, placeholderNamed: "ico_placeholder_employer")
            viewMainBody.backgroundColor = UIColor(red: 0.317, green: 0.141, blue: 0.380, alpha: 1)
            viewMainBodyShadow.backgroundColor = UIColor(red: 0.467, green: 0.525, blue: 0.62, alpha: 0.2)
            labelTitle.textColor = .white
            labelSubtitle.textColor = .white
        default:
            print("Unrecognized type")
        }
    }
}
