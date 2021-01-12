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

    func config(imageNamed: String, title: String?, titleColor: UIColor = UIColor.appBlack,
                subtitle: String?, subtitleColor: UIColor = UIColor.appGreySub, buttonText: String?,
                buttonAction: SelectorAction?) {

        imageMain.image = UIImage(named: imageNamed)
        labelTitle.text = title
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
