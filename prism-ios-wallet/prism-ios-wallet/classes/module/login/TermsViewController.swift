//
import WebKit

class TermsViewController: BaseViewController, SwitchCustomDelegate {

    var presenterImpl = TermsPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    @IBOutlet weak var labelButtonTerms: UILabel!
    @IBOutlet weak var labelButtonPrivacy: UILabel!
    @IBOutlet weak var switchTerms: SwitchCustomView!
    @IBOutlet weak var switchPrivacy: SwitchCustomView!
    @IBOutlet weak var buttonContinue: UIButton!
    @IBOutlet weak var viewLegalContainer: UIView!
    @IBOutlet weak var viewLegalBg: UIView!
    @IBOutlet weak var viewLegalBody: UIView!
    @IBOutlet weak var webViewLegal: WKWebView!
    @IBOutlet weak var buttonLegalClose: WKWebView!

    override func navBarCustomStyle() -> NavBarCustomStyle {
        return NavBarCustomStyle(hasNavBar: true, hasBackButton: true)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        setupSwitchs()
        setupButtons()
        setupDecorators()
        setupWebView()
    }

    // MARK: Setup

    func setupSwitchs() {

        switchTerms.delegate = self
        switchPrivacy.delegate = self
    }

    func setupButtons() {

        buttonContinue.layer.cornerRadius = AppConfigs.CORNER_RADIUS_BUTTON
    }

    func setupDecorators() {

        viewLegalBody.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR, onlyTops: true)
    }

    func setupWebView() {

        webViewLegal.allowsBackForwardNavigationGestures = false
    }

    // MARK: Buttons

    @IBAction func actionContinueButton(_ sender: Any) {
        presenterImpl.tappedContinueButton()
    }

    @IBAction func actionLegalCloseButton(_ sender: Any) {
        presenterImpl.tappedLegalCloseButton()
    }

    @IBAction func actionOpenTermsButton(_ sender: Any) {
        presenterImpl.tappedOpenTerms()
    }

    @IBAction func actionOpenPrivacyButton(_ sender: Any) {
        presenterImpl.tappedOpenPrivacy()
    }

    func stateChanged(for view: SwitchCustomView, newState: Bool) {
        switch view {
        case switchTerms:
            presenterImpl.tappedTermsSwitch(newState: newState)
        case switchPrivacy:
            presenterImpl.tappedPrivacySwitch(newState: newState)
        default:
            return
        }
    }

    // MARK: Config

    func changeButtonState(isEnabled: Bool) {

        buttonContinue.isEnabled = isEnabled
        buttonContinue.backgroundColor = isEnabled ? .appRed : .appGreyMid
    }

    func showLegalView(doShow: Bool, urlStr: String?) {

        viewLegalContainer.isHidden = !doShow

        // Load page
        let urlStr = urlStr ?? "about:blank"
        let requestObj = URLRequest(url: URL(string: urlStr)!, cachePolicy: .reloadIgnoringLocalAndRemoteCacheData)
        webViewLegal.load(requestObj)
    }

    // MARK: Screens

    func changeScreenToRegister() {
        _ = app_mayPerformSegue(withIdentifier: "RegisterSegue", sender: self)
    }
}
