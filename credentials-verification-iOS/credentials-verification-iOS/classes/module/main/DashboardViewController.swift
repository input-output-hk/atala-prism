//
import Pageboy
import Tabman
import UIKit

class DashboardViewController: BaseTabPagerViewController {

    var presenterImpl = DashboardPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    @IBOutlet weak var viewMidButtonContainer: UIView!
    @IBOutlet weak var viewMidButton: UIButton!

    @IBOutlet weak var viewTabbarBgTop: UIView!
    @IBOutlet weak var constraintTabbarDecoratorHeight: NSLayoutConstraint!
    private weak var viewTabbarView: UIView?

    public typealias MyBar = TMBar.TabBar
    let bar = MyBar()

    let viewControllersStoryboardIdentifiers = ["Credentials", "Connections", "Notifications", "Profile", "Settings"]
    let viewControllersIdentifiers = ["Credentials", "Connections", "Notifications", "Profile", "Settings"]
    let viewControllersTitles = ["tab_credentials", "tab_contacts", "", "tab_profile", "tab_settings"]
    let viewControllersIcons = ["tab_credentials", "tab_contacts", "tab_empty", "tab_profile", "tab_settings"]

    lazy var viewControllers = { makeAllChildViewControllers() }()

    // MARK: Lifecycle

    override func viewDidLoad() {
        super.viewDidLoad()

        // Setup
        setupBar()
        setupBarButtons()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)

        configTabbarDecorator()
        NotificationCenter.default.addObserver(self, selector: #selector(onShowCredentialsScreen), name: .showCredentialsScreen, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(onShowContactsScreen), name: .showContactsScreen, object: nil)
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        NotificationCenter.default.removeObserver(self)
    }

    // MARK: Setups

    func setupBar() {

        dataSource = self

        viewTabbarBgTop.addDropShadow(offset: CGSize(width: 0.0, height: 3.0))

        // Customization
        bar.layout.contentInset = UIEdgeInsets(top: 10.0, left: 0.0, bottom: 14.0, right: 0.0)
        bar.indicator.tintColor = .white
        bar.fadesContentEdges = true
        bar.backgroundView.style = .flat(color: .white)

        // Disable scroll
        isScrollEnabled = false

        // Add the bar to the view controller - wrapping it in a `TMSystemBar`.
        addBar(bar,
               dataSource: self,
               at: .bottom)

        // Save the tabbarview
        viewTabbarView = self.view.subviews[self.view.subviews.count - 1]

        // Reorder mid button to top
        self.view.insertSubview(viewMidButtonContainer, at: self.view.subviews.count)

        // Select the initial tab
        scrollToPage(Page.at(index: 2), animated: false)
        
    }

    func setupBarButtons() {

        bar.buttons.customize { button in
            button.selectedTintColor = UIColor.appRed
            button.tintColor = UIColor.appGreyBlue
            button.font = UIFont.boldSystemFont(ofSize: 10)
            button.imageViewSize = CGSize(width: 18, height: 18)
            button.imageContentMode = .bottom
            button.shrinksImageWhenUnselected = false
        }
        // bar.indicator.tintColor = tintColor
    }

    func setupBarButtonImage(index: Int) -> UIImage {
        return UIImage(named: viewControllersIcons[index])!.withRenderingMode(.alwaysTemplate)
    }

    func configTabbarDecorator() {

        view.layoutIfNeeded()
        let newHeight = (viewTabbarView?.height ?? 0) + 1
        constraintTabbarDecoratorHeight.constant = newHeight
    }

    // MARK: Central button

    @IBAction func centralButtonAction(_ sender: Any) {
        Logger.d("Central button tapped")
        scrollToPage(Page.at(index: 2), animated: true)
    }
    
    // MARK: Change tab notifications
    
    @objc func onShowCredentialsScreen() {
        scrollToPage(Page.at(index: 0), animated: true)
    }
    
    @objc func onShowContactsScreen() {
        scrollToPage(Page.at(index: 1), animated: true)
    }
    
    // MARK: Pager

    @objc func nextPage(_ sender: UIBarButtonItem) {
        scrollToPage(.next, animated: true)
    }

    @objc func previousPage(_ sender: UIBarButtonItem) {
        scrollToPage(.previous, animated: true)
    }

    @objc func insertPage(_ sender: UIBarButtonItem) {
        let index = viewControllers.count
        viewControllers.append(makeChildViewController(index: index))
        insertPage(at: index, then: .scrollToUpdate)
    }

    func makeAllChildViewControllers() -> [UIViewController] {
        var viewControllers = [UIViewController]()
        for i in 0 ..< viewControllersIdentifiers.count {
            viewControllers.append(makeChildViewController(index: i))
        }
        return viewControllers
    }

    func makeChildViewController(index: Int) -> UIViewController {
        let storyboard = UIStoryboard(name: viewControllersStoryboardIdentifiers[index], bundle: .main)
        return storyboard.instantiateViewController(withIdentifier: viewControllersIdentifiers[index])
    }

    @discardableResult
    override func scrollToPage(_ page: PageboyViewController.Page, animated: Bool, completion: PageboyViewController.PageScrollCompletion? = nil) -> Bool {
        let result = super.scrollToPage(page, animated: animated)

        // Check if is the central button
        var isCentral = false
        if case Page.at(index: 2) = page {
            isCentral = true
        }
        // Sets the image accordingly
        let image = UIImage(named: isCentral ? "tab_central_on" : "tab_central_off")
        viewMidButton.setImage(image, for: .normal)

        return result
    }
}

// MARK: PageboyViewControllerDataSource

extension DashboardViewController: PageboyViewControllerDataSource {

    func numberOfViewControllers(in pageboyViewController: PageboyViewController) -> Int {
        let count = viewControllers.count
        return count
    }

    func viewController(for pageboyViewController: PageboyViewController,
                        at index: PageboyViewController.PageIndex) -> UIViewController? {
        return viewControllers[index]
    }

    func defaultPage(for pageboyViewController: PageboyViewController) -> PageboyViewController.Page? {
        return nil
    }
}

extension DashboardViewController: TMBarDataSource {

    func barItem(for bar: TMBar, at index: Int) -> TMBarItemable {
        let item = TMBarItem(title: viewControllersTitles[index].localize(), image: setupBarButtonImage(index: index))
        return item
    }
}

// MARK: Custom notifications to show screens

extension Notification.Name {
    static let showCredentialsScreen = Notification.Name("showCredentialsScreen")
    static let showContactsScreen = Notification.Name("showContactsScreen")
}
