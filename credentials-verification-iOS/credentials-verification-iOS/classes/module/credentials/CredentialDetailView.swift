//
//  CredentialDetailView.swift
//  credentials-verification-iOS
//
//  Created by Leandro Pardo on 09/09/2020.
//  Copyright © 2020 iohk. All rights reserved.
//

import UIKit
import WebKit

class CredentialDetailView: BaseNibLoadingView, WKNavigationDelegate {

    @IBOutlet weak var credentialWv: WKWebView!
    @IBOutlet weak var wvHeightCtrt: NSLayoutConstraint!
    @IBOutlet weak var dateLbl: UILabel!
    @IBOutlet weak var dateBg: UIView!

    override func commonInit() {
        super.commonInit()

        dateBg.addRoundCorners(radius: 4)
        credentialWv.navigationDelegate = self
    }

    func config(credential: Credential) {

        credentialWv.loadHTMLString(credential.htmlView, baseURL: nil)
        dateLbl.text = String.init(format: "credentials_detail_date".localize(),
                                   credential.dateReceived.dateTimeString())

        self.refreshView()
    }

    func clearWebView() {

        credentialWv.loadHTMLString("", baseURL: nil)

        self.refreshView()
    }

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {

        // Set to initial value of webview
        var frame: CGRect = webView.frame
        frame.size.height = 1.0
        webView.frame = frame
        // Calculated height of webview
        webView.evaluateJavaScript("document.body.scrollHeight") { data, _ in
            if let height = data as? CGFloat {
                self.wvHeightCtrt.constant = height + 10
            }
        }

    }

}
