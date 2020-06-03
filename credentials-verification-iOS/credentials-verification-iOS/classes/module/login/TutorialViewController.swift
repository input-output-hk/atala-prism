//

class TutorialViewController: BaseViewController, UIScrollViewDelegate {

    @IBOutlet weak var scrollView: UIScrollView!
    @IBOutlet weak var viewIndicator1: UIView!
    @IBOutlet weak var viewIndicatorContainer1: UIView!
    @IBOutlet weak var viewIndicator2: UIView!
    @IBOutlet weak var viewIndicatorContainer2: UIView!
    @IBOutlet weak var viewIndicator3: UIView!
    @IBOutlet weak var viewIndicatorContainer3: UIView!
    @IBOutlet weak var buttonContinue: UIButton!

    var presenterImpl = TutorialPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    var currentPageIndex: Int = 0
    var pages: [TutorialPageView] = []

    var pageIndicatorIcons: [UIView] {
        return [viewIndicator1, viewIndicator2, viewIndicator3]
    }

    var pageIndicatorContainers: [UIView] {
        return [viewIndicatorContainer1, viewIndicatorContainer2, viewIndicatorContainer3]
    }

    override func navBarCustomStyle() -> NavBarCustomStyle {
        return NavBarCustomStyle(hasNavBar: false)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        currentPageIndex = 0
        setupScroll()
        setupButtons()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()

        // Setup (views thar require others to be resized first)
        setupTutorialPages()
        changePage(to: currentPageIndex, animated: false)
    }

    static func openThisView(_ caller: UIViewController?) {

        ViewControllerUtils.changeScreenPresented(caller: caller, storyboardName: "Tutorial", viewControllerIdentif: "Tutorial")
        caller?.navigationController?.popViewController(animated: true)
    }

    // MARK: Setup

    func setupScroll() {

        scrollView.delegate = self
    }

    func setupButtons() {

        buttonContinue.layer.cornerRadius = AppConfigs.CORNER_RADIUS_BUTTON
    }

    func setupTutorialPages() {

        pages = []
        for _ in 0 ... 2 {
            let page = TutorialPageView()
            pages.append(page)
        }

        // Force the scrollview to have the correct size first
        scrollView.refreshView()
        scrollView.contentSize = CGSize(width: scrollView.frame.width * CGFloat(pages.count), height: scrollView.frame.height)
        scrollView.isPagingEnabled = true

        for i in 0 ..< pages.count {
            pages[i].frame = CGRect(x: scrollView.frame.width * CGFloat(i), y: 0, width: scrollView.frame.width, height: scrollView.frame.height)
            scrollView.addSubview(pages[i])
            pages[i].config(index: i)
            pageIndicatorContainers[i].addOnClickListener(action: actionPages[i])
        }

        view.refreshView()
    }

    // MARK: Buttons
    @IBAction func tappedButtonContinue(_ sender: Any) {
        presenterImpl.tappedRegisterButton()
    }
    
    func tappedButtonAction(for view: TutorialPageView, buttonIndex: Int) {

        if currentPageIndex + 1 < pages.count {
            changePage(to: currentPageIndex + 1, animated: true)
        } else if buttonIndex == 0 {
            presenterImpl.tappedRegisterButton()
        } else if buttonIndex == 1 {
            presenterImpl.tappedLoginButton()
        }
    }

    lazy var actionPages = [actionPage1, actionPage2, actionPage3]

    lazy var actionPage1 = SelectorAction(action: { [weak self] in
        self?.changePage(to: 0, animated: true)
    })

    lazy var actionPage2 = SelectorAction(action: { [weak self] in
        self?.changePage(to: 1, animated: true)
    })

    lazy var actionPage3 = SelectorAction(action: { [weak self] in
        self?.changePage(to: 2, animated: true)
    })

    // MARK: Scrollview

    func changePage(to index: Int, animated: Bool) {

        self.currentPageIndex = index
        let nextPage = pages[index]
        scrollView.scrollRectToVisible(nextPage.frame, animated: animated)

        for i in 0 ..< pages.count {
            pageIndicatorIcons[i].backgroundColor = index != i ? .appGrey : .appRed
        }
    }

    func scrollViewDidScroll(_ scrollView: UIScrollView) {
        let pageIndex = Int(round(scrollView.contentOffset.x/view.frame.width))
//         pageControl.currentPage = Int(pageIndex)
        for i in 0 ..< pages.count {
            pageIndicatorIcons[i].backgroundColor = pageIndex != i ? .appGrey : .appRed
        }
    }

    // MARK: Screens

    func changeScreenToRegister() {
        _ = app_mayPerformSegue(withIdentifier: "RegisterSegue", sender: self)
    }

    func changeScreenToLogin() {
        _ = app_mayPerformSegue(withIdentifier: "LoginSegue", sender: self)
    }
}
