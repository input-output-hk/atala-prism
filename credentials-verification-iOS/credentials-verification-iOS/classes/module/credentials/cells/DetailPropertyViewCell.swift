//

protocol DetailPropertyViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: DetailPropertyViewCell)
}

class DetailPropertyViewCell: BaseTableViewCell {

    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var labelSubtitle: UILabel!
    @IBOutlet weak var viewMainBody: UIView!
    @IBOutlet weak var viewSeparator: UIView!
    
    override class func default_NibName() -> String {
        return "DetailPropertyViewCell"
    }

    var delegateImpl: DetailPropertyViewCellPresenterDelegate? {
        return delegate as? DetailPropertyViewCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
        viewMainBody.addShadowLayer(opacity: 0.2)
    }

    // MARK: Config

    func config(title: String?, subtitle: String?, isLast: Bool?, type: CredentialType?) {

        labelTitle.text = title
        labelSubtitle.text = subtitle
        viewSeparator.isHidden = isLast ?? false
        switch type {
        case .univerityDegree:
            viewMainBody.backgroundColor = UIColor(red: 0.235, green: 0.224, blue: 0.227, alpha: 1)
            labelTitle.textColor = .white
            labelSubtitle.textColor = .white
        case .governmentIssuedId:
            viewMainBody.backgroundColor = .white
            labelTitle.textColor = UIColor(red: 0.51, green: 0.51, blue: 0.51, alpha: 1)
            labelSubtitle.textColor =  UIColor(red: 0.235, green: 0.224, blue: 0.227, alpha: 1)
        case .certificatOfInsurance:
            viewMainBody.backgroundColor = .white
            labelTitle.textColor =  UIColor(red: 0.125, green: 0.125, blue: 0.125, alpha: 1)
            labelSubtitle.textColor =  UIColor(red: 0.125, green: 0.125, blue: 0.125, alpha: 1)
        case .proofOfEmployment:
            viewMainBody.backgroundColor = UIColor(red: 0.4, green: 0.149, blue: 0.553, alpha: 1)
            labelTitle.textColor = .white
            labelSubtitle.textColor = .white
        default:
            print("Unrecognized type")
        }
    }
}
