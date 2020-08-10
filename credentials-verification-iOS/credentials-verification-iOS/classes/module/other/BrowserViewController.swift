//
import WebKit

class BrowserViewController: BaseViewController {

    var presenterImpl = BrowserPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    @IBOutlet weak var webView: WKWebView!
    @IBOutlet weak var titleLbl: UILabel!

    var titleText: String? = ""
    var url: String? = ""

    override func navBarCustomStyle() -> NavBarCustomStyle {
        return NavBarCustomStyle(hasNavBar: true, title: "", hasBackButton: true)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        setupWebView()
        setutpTitle()
    }

    // MARK: Setup

    func setupWebView() {

        webView.allowsBackForwardNavigationGestures = true
        webView.allowsLinkPreview = true
        loadlUrl(urlStr: url)
    }

    func setutpTitle() {
        titleLbl.text = titleText
    }

    func loadlUrl(urlStr: String?) {

        // Load page
        let urlStr = urlStr ?? "about:blank"
        let requestObj = URLRequest(url: URL(string: urlStr)!, cachePolicy: .useProtocolCachePolicy)
        webView.load(requestObj)
    }
}

extension BrowserViewController: SegueableScreen {

    func configScreenFromSegue(params: [Any?]?) {

        titleText = params?[0] as? String
        url = params?[1] as? String
    }
}
