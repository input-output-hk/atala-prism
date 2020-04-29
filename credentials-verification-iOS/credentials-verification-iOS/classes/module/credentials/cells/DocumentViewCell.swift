//

protocol DocumentViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: DocumentViewCell)
}

class DocumentViewCell: BaseTableViewCell {

    @IBOutlet weak var imageProfile: UIImageView!
    @IBOutlet weak var labelIdNumber: UILabel!
    @IBOutlet weak var labelFullName: UILabel!
    @IBOutlet weak var labelBirthday: UILabel!
    @IBOutlet weak var labelExpirationDate: UILabel!
    @IBOutlet weak var labelIdNumberTitle: UILabel!
    @IBOutlet weak var labelFullNameTitle: UILabel!
    @IBOutlet weak var labelBirthdayTitle: UILabel!
    @IBOutlet weak var labelExpirationDateTitle: UILabel!
    @IBOutlet weak var viewMainBody: UIView!
    
    override class func default_NibName() -> String {
        return "DocumentViewCell"
    }

    var delegateImpl: DocumentViewCellPresenterDelegate? {
        return delegate as? DocumentViewCellPresenterDelegate
    }

    override func initialSetup(index: IndexPath?, delegate: BaseTableViewCellPresenterDelegate?) {
        super.initialSetup(index: index, delegate: delegate)

        // Setup
        delegateImpl?.setup(for: self)
        labelIdNumber.textColor = UIColor(red: 0.235, green: 0.224, blue: 0.227, alpha: 1)
        labelFullName.textColor = UIColor(red: 0.235, green: 0.224, blue: 0.227, alpha: 1)
        labelBirthday.textColor = UIColor(red: 0.235, green: 0.224, blue: 0.227, alpha: 1)
        labelExpirationDate.textColor = UIColor(red: 0.235, green: 0.224, blue: 0.227, alpha: 1)
        labelIdNumberTitle.textColor = UIColor(red: 0.51, green: 0.51, blue: 0.51, alpha: 1)
        labelFullNameTitle.textColor = UIColor(red: 0.51, green: 0.51, blue: 0.51, alpha: 1)
        labelBirthdayTitle.textColor = UIColor(red: 0.51, green: 0.51, blue: 0.51, alpha: 1)
        labelExpirationDateTitle.textColor = UIColor(red: 0.51, green: 0.51, blue: 0.51, alpha: 1)
    }

    // MARK: Config

    func config(degree: Degree?, logoData: Data?) {
        labelIdNumber.text = degree?.credentialSubject?.identityNumber
        labelFullName.text = degree?.credentialSubject?.name
        labelBirthday.text = degree?.credentialSubject?.dateOfBirth
        labelExpirationDate.text = degree?.expiryDate
        imageProfile.applyDataImage(data: nil, placeholderNamed: "icon_id")
    }
}
