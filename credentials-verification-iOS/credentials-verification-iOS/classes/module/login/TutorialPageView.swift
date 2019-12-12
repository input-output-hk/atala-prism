//

protocol TutorialPageViewDelegate: UIViewController {

    func tappedButtonAction(for view: TutorialPageView, buttonIndex: Int)
}

class TutorialPageView: BaseNibLoadingView {

    weak var delegate: TutorialPageViewDelegate?
    var pageIndex: Int = 0

    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var labelSubtitle: UILabel!
    @IBOutlet weak var imageMain: UIImageView!
    @IBOutlet weak var buttonFirst: UIButton!
    @IBOutlet weak var buttonSecond: UIButton!

    func config(delegate: TutorialPageViewDelegate, index: Int) {

        self.delegate = delegate
        self.pageIndex = index

        buttonFirst.layer.cornerRadius = AppConfigs.CORNER_RADIUS_BUTTON
        buttonSecond.layer.cornerRadius = AppConfigs.CORNER_RADIUS_BUTTON

        switch index {
        case 0:
            labelTitle.text = "tutorial_1_title".localize()
            labelSubtitle.text = "tutorial_1_subtitle".localize()
            imageMain.image = UIImage(named: "img_tutorial_step_1")
            buttonFirst.setTitle("tutorial_1_button".localize(), for: .normal)
            buttonSecond.removeFromSuperview()
        case 1:
            labelTitle.text = "tutorial_2_title".localize()
            labelSubtitle.text = "tutorial_2_subtitle".localize()
            imageMain.image = UIImage(named: "img_tutorial_step_2")
            buttonFirst.setTitle("tutorial_1_button".localize(), for: .normal)
            buttonSecond.removeFromSuperview()
        case 2:
            labelTitle.text = "tutorial_3_title".localize()
            labelSubtitle.text = "tutorial_3_subtitle".localize()
            imageMain.image = UIImage(named: "img_tutorial_step_3")
            buttonFirst.setTitle("tutorial_3_button_register".localize(), for: .normal)
            buttonSecond.removeFromSuperview()
        default:
            return
        }

        self.layoutIfNeeded()
    }

    @IBAction func actionFirstButton(_ sender: Any) {
        delegate?.tappedButtonAction(for: self, buttonIndex: 0)
    }

    @IBAction func actionSecondButton(_ sender: Any) {
        delegate?.tappedButtonAction(for: self, buttonIndex: 1)
    }
}
