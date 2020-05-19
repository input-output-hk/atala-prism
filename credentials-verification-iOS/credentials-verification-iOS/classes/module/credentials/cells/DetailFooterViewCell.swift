//

protocol DetailFooterViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: DetailFooterViewCell)
    func tappedDeclineAction(for cell: DetailFooterViewCell?)
    func tappedConfirmAction(for cell: DetailFooterViewCell?)
}

class DetailFooterViewCell: BaseTableViewCell {

    @IBOutlet weak var viewMainBody: UIView!
    @IBOutlet weak var viewButtons: UIView!
    @IBOutlet weak var buttonDecline: UIButton!
    @IBOutlet weak var buttonConfirm: UIButton!
    
    override class func default_NibName() -> String {
        return "DetailFooterViewCell"
    }

    var delegateImpl: DetailFooterViewCellPresenterDelegate? {
        return delegate as? DetailFooterViewCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
        viewMainBody.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR, onlyBottoms: true)
        viewMainBody.addShadowLayer(opacity: 0.2)
        buttonDecline.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON, borderWidth: 3, borderColor: UIColor.appRed.cgColor)
        buttonConfirm.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON, borderWidth: 3, borderColor: UIColor.appRed.cgColor)
    }

    // MARK: Component delegates

    @IBAction func actionConfirmButtonTapped(_ sender: Any) {
        self.delegateImpl?.tappedConfirmAction(for: self)
    }

    @IBAction func actionDeclineButtonTapped(_ sender: Any) {
        self.delegateImpl?.tappedDeclineAction(for: self)
    }

    // MARK: Config

    func config(isNew: Bool,type: CredentialType?) {

        switch type {
        case .univerityDegree:
            viewMainBody.backgroundColor = UIColor(red: 0.235, green: 0.224, blue: 0.227, alpha: 1)
        case .governmentIssuedId:
            viewMainBody.backgroundColor = .white
        case .certificatOfInsurance:
            viewMainBody.backgroundColor = .white
        case .proofOfEmployment:
            viewMainBody.backgroundColor = UIColor(red: 0.4, green: 0.149, blue: 0.553, alpha: 1)
        default:
            print("Unrecognized type")
        }

        viewButtons.isHidden = !isNew
    }
}
