//
//  AboutViewController.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 26/02/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import UIKit
import WebKit

class AboutViewController: BaseViewController {
    
    var presenterImpl = AboutPresenter()
    override var presenter: BasePresenter { return presenterImpl }
    
    @IBOutlet weak var labelVersion: UILabel!
    @IBOutlet weak var labelTesting: UILabel!
    @IBOutlet weak var labelBuiltBy: UILabel!
    @IBOutlet weak var viewLegalContainer: UIView!
    @IBOutlet weak var viewLegalBg: UIView!
    @IBOutlet weak var viewLegalBody: UIView!
    @IBOutlet weak var webViewLegal: WKWebView!
    @IBOutlet weak var buttonLegalClose: WKWebView!
    
    override func navBarCustomStyle() -> NavBarCustomStyle {
        return NavBarCustomStyle(hasNavBar: true, hasBackButton: true)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Setup
        setupLabels()
        setupDecorators()
        setupWebView()
    }
    
    // MARK: Setup
    func setupLabels() {
        
        let version = Bundle.main.infoDictionary!["CFBundleShortVersionString"] as? String
        labelVersion.text = version
        
        labelTesting.isHidden = Env.isProduction()
        
        let attrs = [NSAttributedString.Key.font : UIFont.boldSystemFont(ofSize: 13)]
        let attributedString = NSMutableAttributedString(string:"about_atala_prism".localize(), attributes:attrs)
        attributedString.append(NSMutableAttributedString(string:"about_powered_by".localize()))
        attributedString.append(NSMutableAttributedString(string:"about_cardano".localize(), attributes:attrs))
        labelBuiltBy.attributedText = attributedString


    }
    
    func setupDecorators() {
        
        viewLegalBody.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_REGULAR, onlyTops: true)
    }
    
    func setupWebView() {
        
        webViewLegal.allowsBackForwardNavigationGestures = false
    }
    
    // MARK: Buttons
    
    @IBAction func actionLegalCloseButton(_ sender: Any) {
        presenterImpl.tappedLegalCloseButton()
    }
    
    @IBAction func actionOpenTermsButton(_ sender: Any) {
        presenterImpl.tappedOpenTerms()
    }
    
    @IBAction func actionOpenPrivacyButton(_ sender: Any) {
        presenterImpl.tappedOpenPrivacy()
    }
    
    // MARK: Config
    
    func showLegalView(doShow: Bool, urlStr: String?) {
        
        viewLegalContainer.isHidden = !doShow
        // TODO: Add animation
        
        // Load page
        let urlStr = urlStr ?? "about:blank"
        let requestObj = URLRequest(url: URL(string: urlStr)!, cachePolicy: .reloadIgnoringLocalAndRemoteCacheData)
        webViewLegal.load(requestObj)
    }
    
}
