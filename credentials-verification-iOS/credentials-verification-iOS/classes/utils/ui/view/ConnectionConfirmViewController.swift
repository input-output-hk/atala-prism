//
import Presentr
import UIKit

protocol ConnectionConfirmPresenterDelegate: class {

    func tappedDeclineAction(for: ConnectionConfirmViewController)
    func tappedConfirmAction(for: ConnectionConfirmViewController)
}

class ConnectionConfirmViewController: UIViewController, PresentrDelegate {

    @IBOutlet weak var labelLead: UILabel!
    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var imageLogo: UIImageView!
    @IBOutlet weak var buttonConfirm: UIButton!
    @IBOutlet weak var buttonDecline: UIButton!
    @IBOutlet weak var viewBg: UIView!

    weak var delegate: ConnectionConfirmPresenterDelegate?

    var isDuplicated: Bool = false

    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.

        viewBg.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR)
        buttonDecline.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON,
                                      borderWidth: 3, borderColor: UIColor.appRed.cgColor)
        buttonConfirm.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON,
                                      borderWidth: 3, borderColor: UIColor.appRed.cgColor)
    }

    static func makeThisView() -> ConnectionConfirmViewController {
        let storyboard = UIStoryboard(name: "ConnectionConfirm", bundle: nil)
        if let viewcontroller = storyboard.instantiateViewController(withIdentifier: "ConnectionConfirm")
                                    as? ConnectionConfirmViewController {
            return viewcontroller
        }
        return ConnectionConfirmViewController()
    }

    let presentr: Presentr = {

        let presenter = Presentr(presentationType: .alert)
        presenter.transitionType = TransitionType.coverHorizontalFromRight
        presenter.dismissOnSwipe = false

        let width = ModalSize.sideMargin(value: 21.0)
        let height = ModalSize.custom(size: 204)
        let center = ModalCenterPosition.center
        presenter.presentationType = .custom(width: width, height: height, center: center)
        presenter.transitionType = nil
        presenter.dismissTransitionType = nil
        presenter.dismissAnimated = true

        return presenter
    }()

    func config(delegate: ConnectionConfirmPresenterDelegate?, lead: String?, title: String?,
                logoData: Data?, placeholderNamed: String?, isDuplicated: Bool) {

        self.delegate = delegate
        labelLead.text = lead
        labelTitle.text = title
        if isDuplicated {
            buttonConfirm.setTitle("connections_scan_qr_confirm_duplicated_button".localize(), for: .normal)
        }
        imageLogo.applyDataImage(data: logoData, placeholderNamed: placeholderNamed)
    }

    // MARK: Component delegates

    @IBAction func actionConfirmButtonTapped(_ sender: Any) {
        self.dismiss(animated: true, completion: nil)
        self.delegate?.tappedConfirmAction(for: self)
    }

    @IBAction func actionDeclineButtonTapped(_ sender: Any) {
        self.dismiss(animated: true, completion: nil)
        self.delegate?.tappedDeclineAction(for: self)
    }

    // MARK: Presentr Delegate

    func presentrShouldDismiss(keyboardShowing: Bool) -> Bool {
        self.delegate?.tappedDeclineAction(for: self)
        return true
    }
}
