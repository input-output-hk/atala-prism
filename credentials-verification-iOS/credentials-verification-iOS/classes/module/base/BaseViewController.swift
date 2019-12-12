//

class BaseViewController: UIViewController {

    var doPresenterSetup: Bool = true

    var presenter: BasePresenter {
        assert(false)
        return BasePresenter()
    }

    public override init(nibName nibNameOrNil: String?, bundle nibBundleOrNil: Bundle?) {
        super.init(nibName: nibNameOrNil, bundle: nibBundleOrNil)
        commonInit()
    }

    public required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        commonInit()
    }

    deinit {}

    public func commonInit() {

        if doPresenterSetup {
            presenter.view = self
        }
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        ViewControllerUtils.viewDidLoad(view: self, presenter: presenter)
        ViewControllerUtils.setupNotificationsForBackgroundCalls(view: self, didEnterForeground: #selector(didEnterForeground), willEnterForeground: #selector(willEnterForeground), didEnterBackground: #selector(didEnterBackground), willEnterBackground: #selector(willEnterBackground))
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        ViewControllerUtils.viewDidAppear(view: self, presenter: presenter, animated)
    }

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        ViewControllerUtils.viewDidDisappear(view: self, presenter: presenter, animated)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        ViewControllerUtils.viewWillAppear(view: self, presenter: presenter, animated)
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        ViewControllerUtils.viewWillDisappear(view: self, presenter: presenter, animated)
    }

    @objc func didEnterForeground() {
        ViewControllerUtils.didEnterForeground(view: self, presenter: presenter)
    }

    @objc func willEnterForeground() {
        ViewControllerUtils.willEnterForeground(view: self, presenter: presenter)
    }

    @objc func didEnterBackground() {
        ViewControllerUtils.didEnterBackground(view: self, presenter: presenter)
    }

    @objc func willEnterBackground() {
        ViewControllerUtils.willEnterBackground(view: self, presenter: presenter)
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        ViewControllerUtils.didReceiveMemoryWarning(view: self)
    }

    @objc func exitScreen() {

        navigationController?.popViewController(animated: true)
        dismiss(animated: true, completion: nil)
    }

    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        ViewControllerUtils.prepare(for: segue, caller: self, sender: sender)
    }
}
