//

protocol DocumentViewCellPresenterDelegate: BaseTableViewCellPresenterDelegate {

    func setup(for cell: DocumentViewCell)
}

class DocumentViewCell: BaseTableViewCell {

    /*@IBOutlet weak var labelTitle: UILabel!
     @IBOutlet weak var labelSubtitle: UILabel!
     @IBOutlet weak var buttonIconAction: UIButton!
     @IBOutlet weak var imageLogo: UIImageView!
     @IBOutlet weak var constraintTitleVertical: NSLayoutConstraint!*/

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
    }

    // MARK: Config

    func config(title: String?, isUniversity: Bool, logoUrl: String?) {

        /* labelTitle.text = title
         labelSubtitle.isHidden = isUniversity
         constraintTitleVertical.constant = isUniversity ? 0.0 : -10.0
         buttonIconAction.setImage(UIImage(named: isUniversity ? "ico_caret" : "ico_open_link"), for: .normal)
         // Logo image
         let placeholder = UIImage(named: isUniversity ? "ico_placeholder_university" : "ico_placeholder_employer")
         if let logoUrl = logoUrl, let url = URL(string: logoUrl) {
             imageLogo.af_setImage(withURL: url, placeholderImage: placeholder)
         } else {
             imageLogo.image = placeholder
         } */
    }
}
