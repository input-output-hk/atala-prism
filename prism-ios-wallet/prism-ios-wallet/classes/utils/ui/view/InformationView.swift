//

protocol InformationViewDelegate: class {}

@IBDesignable class InformationView: BaseNibLoadingView {

    @IBOutlet weak var imageMain: UIImageView!
    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var labelSubtitle: UILabel!
    @IBOutlet weak var buttonMain: UIButton!

    var buttonAction: SelectorAction?

    weak var delegate: InformationViewDelegate?

    override func commonInit() {
        super.commonInit()

        buttonMain.layer.cornerRadius = AppConfigs.CORNER_RADIUS_BUTTON
    }

    func config(imageNamed: String, title: String?, titleBold: String? = nil, titleColor: UIColor = UIColor.appBlack,
                subtitle: String?, subtitleColor: UIColor = UIColor.appGreySub, buttonText: String?,
                buttonAction: SelectorAction?) {

        imageMain.image = UIImage(named: imageNamed)
        if let title = title, let titleBold = titleBold, let font = labelTitle.font {
            let attributedString = NSMutableAttributedString(string: title,
                                                             attributes: [NSAttributedString.Key.font: font])
            let boldFontAttribute: [NSAttributedString.Key: Any] = [NSAttributedString.Key.font: UIFont.boldSystemFont(ofSize: font.pointSize)]
            let range = (title as NSString).range(of: titleBold)
            attributedString.addAttributes(boldFontAttribute, range: range)
            labelTitle.attributedText = attributedString
        } else {
            labelTitle.text = title
        }
        labelTitle.textColor = titleColor
        labelSubtitle.text = subtitle
        labelSubtitle.textColor = subtitleColor
        buttonMain.setTitle(buttonText, for: .normal)
        self.buttonAction = buttonAction
        buttonMain.isHidden = buttonAction == nil

        self.refreshView()
    }

    @IBAction func actionButton(_ sender: Any) {
        buttonAction?.action()
    }
}
