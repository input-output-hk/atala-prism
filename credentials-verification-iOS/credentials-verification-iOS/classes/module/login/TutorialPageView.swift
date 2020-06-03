//

class TutorialPageView: BaseNibLoadingView {

    var pageIndex: Int = 0

    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var labelSubtitle: UILabel!
    @IBOutlet weak var imageMain: UIImageView!

    func config(index: Int) {

        self.pageIndex = index

        switch index {
        case 0:
            labelTitle.text = "tutorial_1_title".localize()
            labelSubtitle.text = "tutorial_1_subtitle".localize()
            imageMain.image = UIImage(named: "img_tutorial_step_1")
        case 1:
            labelTitle.text = "tutorial_2_title".localize()
            labelSubtitle.text = "tutorial_2_subtitle".localize()
            imageMain.image = UIImage(named: "img_tutorial_step_2")
        case 2:
            labelTitle.text = "tutorial_3_title".localize()
            labelSubtitle.text = "tutorial_3_subtitle".localize()
            imageMain.image = UIImage(named: "img_tutorial_step_3")
        default:
            return
        }

        self.layoutIfNeeded()
    }

}
