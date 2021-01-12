//
import Presentr
import UIKit

protocol ShareDialogPresenterDelegate: class {

    func tappedDeclineAction(for view: ShareDialogViewController)
    func tappedConfirmAction(for view: ShareDialogViewController)
    func shareItem(for view: ShareDialogViewController, at index: Int) -> Any?
    func shareItemCount(for view: ShareDialogViewController) -> Int
    func shareItemTapped(for cell: ShareDialogItemCollectionViewCell?, at index: Int, item: Any?)
    func shareItemConfig(for cell: ShareDialogItemCollectionViewCell?, at index: Int, item: Any?)
    func shareItemFilter(for view: ShareDialogViewController, searchText: String)
}

class ShareDialogViewController: UIViewController, PresentrDelegate, UITableViewDataSource,
                                    UITableViewDelegate, UISearchBarDelegate {

    @IBOutlet weak var collectionView: UITableView!
    @IBOutlet weak var buttonConfirm: UIButton!
    @IBOutlet weak var viewBg: UIView!
    @IBOutlet weak var viewBgRest: UIView!
    @IBOutlet weak var searchBar: UISearchBar!

    weak var delegate: ShareDialogPresenterDelegate?

    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.

        viewBg.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR, onlyTops: true)
        buttonConfirm.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON)
        registerNib()
        collectionView.delegate = self
        collectionView.dataSource = self
        viewBgRest.addOnClickListener(action: actionBgRestTap)

        searchBar.backgroundColor = .appWhite
        searchBar.setBackgroundImage(UIImage(), for: .any, barMetrics: .default)
        searchBar.isTranslucent = true
        searchBar.searchTextField.addRoundCorners(radius: 6, borderWidth: 1, borderColor: UIColor.appGreyMid.cgColor)
        searchBar.searchTextField.backgroundColor = .appWhite
        searchBar.placeholder = "credentials_detail_share_contact_name".localize()
        searchBar.delegate = self

        ViewControllerUtils.addTapToDismissKeyboard(view: self)
        ViewControllerUtils.addShiftKeyboardListeners(view: self)
    }

    static func makeThisView() -> ShareDialogViewController {
        let storyboard = UIStoryboard(name: "ShareDialog", bundle: nil)
        if let viewcontroller = storyboard.instantiateViewController(withIdentifier: "ShareDialog")
                                as? ShareDialogViewController {
            return viewcontroller
        }
        return ShareDialogViewController()
    }

    let presentr: Presentr = {

        let width = ModalSize.full
        let height = ModalSize.full
        let center = ModalCenterPosition.center
        let presenter = Presentr(presentationType: .custom(width: width, height: height, center: center))
        presenter.transitionType = TransitionType.coverVertical
        presenter.dismissOnSwipe = false

        presenter.transitionType = nil
        presenter.dismissTransitionType = nil
        presenter.dismissAnimated = true

        return presenter
    }()

    func config(delegate: ShareDialogPresenterDelegate?, parentVc: UIViewController) {

        self.delegate = delegate
        self.collectionView.reloadData()
    }

    func config(enableButton: Bool) {

        buttonConfirm.isEnabled = enableButton
        buttonConfirm.backgroundColor = enableButton ? .appRed : .appGreyMid
    }

    // MARK: Component delegates

    @IBAction func actionConfirmButtonTapped(_ sender: Any) {
        self.dismiss(animated: true, completion: nil)
        self.delegate?.tappedConfirmAction(for: self)
    }

    lazy var actionBgRestTap = SelectorAction(action: { [weak self] in
        self?.dismiss(animated: true, completion: nil)
        self?.delegate?.tappedDeclineAction(for: self!)
    })

    // MARK: Presentr Delegate

    func presentrShouldDismiss(keyboardShowing: Bool) -> Bool {
        self.delegate?.tappedDeclineAction(for: self)
        return true
    }

    // MARK: Collection

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if let count = self.delegate?.shareItemCount(for: self), count != 0 {
            return count
        }
        return 1
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {

        if let count = self.delegate?.shareItemCount(for: self), count != 0,
        let cell = collectionView.dequeueReusableCell(withIdentifier:
                                                            ShareDialogItemCollectionViewCell.reuseIdentifier,
                                                         for: indexPath) as? ShareDialogItemCollectionViewCell {

            let item = self.delegate?.shareItem(for: self, at: indexPath.row)
            cell.config(index: indexPath.row, item: item, delegate: self)
            delegate?.shareItemConfig(for: cell, at: indexPath.row, item: item)

            return cell
        }
        if let cell = collectionView.dequeueReusableCell(withIdentifier:
                                                            NoResultsViewCell.reuseIdentifier,
                                                         for: indexPath) as? NoResultsViewCell {
            return cell
        }
        return UITableViewCell()
    }

    func registerNib() {
        let nib = UINib(nibName: ShareDialogItemCollectionViewCell.nibName, bundle: nil)
        collectionView?.register(nib, forCellReuseIdentifier: ShareDialogItemCollectionViewCell.reuseIdentifier)

        let emptyNib = UINib(nibName: NoResultsViewCell.default_NibName(), bundle: nil)
        collectionView?.register(emptyNib, forCellReuseIdentifier: NoResultsViewCell.reuseIdentifier)
    }

    // MARK: Search

    func searchBar(_ searchBar: UISearchBar, textDidChange searchText: String) {

        delegate?.shareItemFilter(for: self, searchText: searchText)
        collectionView.reloadData()
    }
}

class ShareDialogItemCollectionViewCell: UITableViewCell {

    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var imageLogo: UIImageView!
    @IBOutlet weak var viewTick: UIImageView!
    @IBOutlet weak var viewBg: UIView!

    weak var delegate: ShareDialogViewController?
    var index: Int?
    var item: Any?

    class var reuseIdentifier: String {
        return "ShareItem"
    }

    class var nibName: String {
        return "ShareItemViewCell"
    }

    func config(index: Int, item: Any?, delegate: ShareDialogViewController?) {

        self.index = index
        self.item = item
        self.delegate = delegate
    }

    func config(name: String?, logoData: Data?, placeholderNamed: String, isSelected: Bool) {

        self.labelTitle.text = name
        self.imageLogo.applyDataImage(data: logoData, placeholderNamed: placeholderNamed)
        self.imageLogo.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR)
        self.viewTick.image = isSelected ? #imageLiteral(resourceName: "ico_share_tick") : #imageLiteral(resourceName: "ico_share_empty")
        self.viewBg.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR,
                                    borderWidth: isSelected ? 2 : 0, borderColor: UIColor.appRed.cgColor)
        self.viewBg.addShadowLayer(radius: 4)
        self.viewTick.addDropShadow()
    }

    @IBAction func actionItemTapped(_ sender: Any) {
        delegate?.delegate?.shareItemTapped(for: self, at: index!, item: item)
    }
}
