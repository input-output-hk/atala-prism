//

class MainNavViewController: BaseNavigationController {

    var ownPresenter = MainNavPresenter()
    override var presenter: BasePresenter { return ownPresenter }

    override func viewDidLoad() {
        super.viewDidLoad()

        // If its not logged, finish now
        if !UserUtils.isLogged(sharedMemory) {
            UserUtils.logout(sharedMemory)
            exitToLogin()
            return
        }
    }

    static func openThisView() {
        // ViewControllerUtils.changeScreenPresented(caller: caller, storyboardName: "Main", viewControllerIdentif: "MainFlow")
        ViewControllerUtils.changeScreenRooted(storyboardName: "Main", viewControllerIdentif: "MainFlow")
    }
}
