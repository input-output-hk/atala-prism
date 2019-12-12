//

class WelcomeViewController: BaseViewController {

    var presenterImpl = WelcomePresenter()
    override var presenter: BasePresenter { return presenterImpl }

    @IBOutlet weak var informationView: InformationView!

    override func navBarCustomStyle() -> NavBarCustomStyle {
        return NavBarCustomStyle(hasNavBar: false)
    }

    static func openThisView(_ caller: UIViewController?) {

        ViewControllerUtils.changeScreenPresented(caller: caller, storyboardName: "Welcome", viewControllerIdentif: "Welcome")
        caller?.navigationController?.popViewController(animated: true)
    }

    // MARK: Setup

    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        informationView.config(imageNamed: "img_black_logo", title: "welcome_title".localize(), subtitle: nil, buttonText: "welcome_button".localize(), buttonAction: actionContinue)
    }

    lazy var actionContinue = SelectorAction(action: { [weak self] in
        self?.presenterImpl.tappedContinueButton()
    })

    // MARK: Screens

    func changeScreenToTutorial() {
        _ = app_mayPerformSegue(withIdentifier: "TutorialSegue", sender: self)
    }
}
