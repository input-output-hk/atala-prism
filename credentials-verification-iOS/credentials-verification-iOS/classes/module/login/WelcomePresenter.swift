//

class WelcomePresenter: BasePresenter {

    var viewImpl: WelcomeViewController? {
        return view as? WelcomeViewController
    }

    func tappedContinueButton() {
        viewImpl?.changeScreenToTutorial()
    }
}
