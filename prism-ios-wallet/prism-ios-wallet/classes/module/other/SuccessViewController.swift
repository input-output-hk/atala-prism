//

class SuccessViewController: BaseViewController {

    var presenterImpl = SuccessPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    @IBOutlet weak var informationView: InformationView!

    var titleText: String? = ""
    var titleBoldText: String?
    var subtitleText: String? = ""
    var buttonText: String? = ""
    var buttonAction: SelectorAction?

    override func navBarCustomStyle() -> NavBarCustomStyle {
        return NavBarCustomStyle(hasNavBar: false)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        informationView.config(imageNamed: "img_success", title: titleText, titleBold: titleBoldText,
                               subtitle: subtitleText, buttonText: buttonText, buttonAction: buttonAction)
    }
}

extension SuccessViewController: SegueableScreen {

    func configScreenFromSegue(params: [Any?]?) {

        titleText = params?[0] as? String
        subtitleText = params?[1] as? String
        buttonText = params?[2] as? String
        buttonAction = params?[3] as? SelectorAction
        titleBoldText = params?[4] as? String
    }

    static func makeSeguedParams(title: String?, subtitle: String?, buttonText: String?,
                                 buttonAction: SelectorAction?, titleBold: String? = nil) -> [Any?]? {

        var params: [Any?] = []
        params.append(title)
        params.append(subtitle)
        params.append(buttonText)
        params.append(buttonAction)
        params.append(titleBold)
        return params
    }
}
