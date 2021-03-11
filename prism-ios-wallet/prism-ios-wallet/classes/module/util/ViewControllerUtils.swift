//

class ViewControllerUtils {

    // MARK: Lifecycle

    static func viewDidLoad(view: UIViewController, presenter: BasePresenter?) {

        Logger.d("View's viewDidLoad: \(String(describing: view))")
        presenter?.viewDidLoad()

        // Setups
        NavBarCustom.config(view: view)
    }

    static func viewDidAppear(view: UIViewController, presenter: BasePresenter?, _ animated: Bool) {

        Logger.d("View's viewDidAppear: \(String(describing: view))")
        presenter?.viewDidAppear()
    }

    static func viewDidDisappear(view: UIViewController, presenter: BasePresenter?, _ animated: Bool) {

        Logger.d("View's viewDidDisappear: \(String(describing: view))")
        presenter?.viewDidDisappear()
    }

    static func viewWillAppear(view: UIViewController, presenter: BasePresenter?, _ animated: Bool) {

        Logger.d("View's viewWillAppear: \(String(describing: view))")
        presenter?.viewWillAppear()
    }

    static func viewWillDisappear(view: UIViewController, presenter: BasePresenter?, _ animated: Bool) {

        Logger.d("View's viewWillDisappear: \(String(describing: view))")
        presenter?.viewWillDisappear()
    }

    static func didEnterForeground(view: UIViewController, presenter: BasePresenter?) {

        Logger.d("View's didEnterForeground: \(String(describing: view))")
        presenter?.didEnterForeground()
    }

    static func willEnterForeground(view: UIViewController, presenter: BasePresenter?) {

        Logger.d("View's willEnterForeground: \(String(describing: view))")
        presenter?.willEnterForeground()
    }

    static func didEnterBackground(view: UIViewController, presenter: BasePresenter?) {

        Logger.d("View's didEnterBackground: \(String(describing: view))")
        presenter?.didEnterBackground()
    }

    static func willEnterBackground(view: UIViewController, presenter: BasePresenter?) {

        Logger.d("View's willEnterBackground: \(String(describing: view))")
        presenter?.willEnterBackground()
    }

    static func setupNotificationsForBackgroundCalls(view: UIViewController, didEnterForeground: Selector,
                                                     willEnterForeground: Selector,
                                                     didEnterBackground: Selector,
                                                     willEnterBackground: Selector) {

        NotificationCenter.default.addObserver(view, selector: didEnterForeground,
                                               name: UIApplication.didBecomeActiveNotification, object: nil)
        NotificationCenter.default.addObserver(view, selector: willEnterForeground,
                                               name: UIApplication.willEnterForegroundNotification, object: nil)
        NotificationCenter.default.addObserver(view, selector: didEnterBackground,
                                               name: UIApplication.didEnterBackgroundNotification, object: nil)
        NotificationCenter.default.addObserver(view, selector: willEnterBackground,
                                               name: UIApplication.willResignActiveNotification, object: nil)
    }

    static func didReceiveMemoryWarning(view: UIViewController) {
        // Dispose of any resources that can be recreated.
    }

    // MARK: Navigation Bar

    static func defaultBackPressed(view: UIViewController) {

        // Go back to the previous ViewController
        _ = view.navigationController?.popViewController(animated: true)
        view.dismiss(animated: true, completion: nil)
    }

    // MARK: Keyboard

    public static func addTapToDismissKeyboard(view: UIViewController) {

        let tapGesture = UITapGestureRecognizer(target: view, action: #selector(view.hideKeyboard))
        tapGesture.cancelsTouchesInView = false
        view.view.addGestureRecognizer(tapGesture)
    }

    public static func addShiftKeyboardListeners(view: UIViewController) {

        NotificationCenter.default.addObserver(view, selector: #selector(view.shiftKeyboardOnWillShow),
                                               name: UIResponder.keyboardWillShowNotification, object: nil)
        NotificationCenter.default.addObserver(view, selector: #selector(view.shiftKeyboardOnWillHide),
                                               name: UIResponder.keyboardWillHideNotification, object: nil)
    }

    // MARK: Screen navigation

    static func exitToLogin(view: UIViewController) {
        UserUtils.exitToLogin(caller: view)
    }

    static func prepare(for segue: UIStoryboardSegue, caller: UIViewController, sender: Any?) {

        if segue.identifier == caller.sharedMemory.changeScreenSegueIdentif && segue.identifier != nil {
            if let destinationVC = segue.destination as? SegueableScreen {
                destinationVC.configScreenFromSegue(params: caller.sharedMemory.changeScreenParams)
                caller.sharedMemory.changeScreenSegueIdentif = nil
                caller.sharedMemory.changeScreenParams = nil
            }
        }

        if segue.identifier == caller.sharedMemory.notifPushSegueIdentif && segue.identifier != nil {
            if let destinationVC = segue.destination as? SegueableScreen {
                destinationVC.configScreenFromSegue(params: caller.sharedMemory.notifPushParams)
                caller.sharedMemory.notifPushSegueIdentif = nil
                caller.sharedMemory.notifPushParams = nil
            }
        }
    }

    static func changeScreenSegued(caller: UIViewController?, segue: String, params: [Any?]?) {

        caller?.sharedMemory.changeScreenParams = params
        caller?.sharedMemory.changeScreenSegueIdentif = segue
        caller?.performSegue(withIdentifier: segue, sender: caller)
    }

    static func changeScreenRooted(storyboardName: String, viewControllerIdentif: String) {

        let storyboard = UIStoryboard(name: storyboardName, bundle: nil)
        let viewcontroller = storyboard.instantiateViewController(withIdentifier: viewControllerIdentif)
        UIApplication.shared.keyWindow!.replaceRootViewControllerWith(viewcontroller, animated: true, completion: nil)
    }

    static func changeScreenPresented(caller: UIViewController?,
                                      storyboardName: String,
                                      viewControllerIdentif: String? = nil,
                                      params: [Any?]? = nil, animated: Bool = true) {

        let storyboard = UIStoryboard(name: storyboardName, bundle: nil)
        let viewcontroller = storyboard.instantiateViewController(withIdentifier:
                                                                viewControllerIdentif ?? storyboardName)
        if let segueableViewController = viewcontroller as? SegueableScreen {
            segueableViewController.configScreenFromSegue(params: params)
        }
        // If its a navigation controller, use it, otherwise create one and set view controller as its root
        var navController = viewcontroller as? UINavigationController
        if navController == nil {
            navController = UINavigationController(rootViewController: viewcontroller)
        }
        navController?.modalPresentationStyle = .fullScreen
        viewcontroller.modalPresentationStyle = .fullScreen
        caller?.present(navController!, animated: animated, completion: nil)
    }
}
