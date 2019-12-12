//
import WebKit

class BrowserViewController: BaseViewController {

    var presenterImpl = BrowserPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    @IBOutlet weak var webView: WKWebView!

    var titleText: String? = ""
    var url: String? = ""

    override func navBarCustomStyle() -> NavBarCustomStyle {
        return NavBarCustomStyle(hasNavBar: true, title: titleText, hasBackButton: true)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        setupWebView()
    }

    // MARK: Setup

    func setupWebView() {

        webView.allowsBackForwardNavigationGestures = true
        webView.allowsLinkPreview = true
        loadlUrl(urlStr: url)
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
