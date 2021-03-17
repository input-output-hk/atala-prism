//
//  IntdemoTutorialViewController.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 01/03/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import UIKit

class IntdemoTutorialViewController: BaseViewController, UIScrollViewDelegate {

    @IBOutlet weak var scrollView: UIScrollView!
    @IBOutlet weak var viewIndicator1: UIView!
    @IBOutlet weak var viewIndicatorContainer1: UIView!
    @IBOutlet weak var viewIndicator2: UIView!
    @IBOutlet weak var viewIndicatorContainer2: UIView!
    @IBOutlet weak var viewIndicator3: UIView!
    @IBOutlet weak var viewIndicatorContainer3: UIView!
    @IBOutlet weak var buttonNext: UIButton!

    var presenterImpl = IntdemoTutorialPresenter()
    override var presenter: BasePresenter { return presenterImpl }

    var currentPageIndex: Int = 0
    var pages: [IntdemoTutorialPageView] = []

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
    }

    static func openThisView(_ caller: UIViewController?) {

        ViewControllerUtils.changeScreenPresented(caller: caller, storyboardName: "IntdemoTutorial",
                                                  viewControllerIdentif: "IntdemoTutorial")
        caller?.navigationController?.popViewController(animated: true)
    }

    // MARK: Setup

    func setupScroll() {

        scrollView.delegate = self
    }

    func setupButtons() {

        buttonNext.addRoundCorners(radius: AppConfigs.CORNER_RADIUS_BUTTON)
    }

    func setupTutorialPages() {

        pages = []
        for _ in 0 ... 2 {
            let page = IntdemoTutorialPageView()
            pages.append(page)
        }

        // Force the scrollview to have the correct size first
        scrollView.refreshView()
        scrollView.contentSize = CGSize(width: scrollView.frame.width * CGFloat(pages.count),
                                        height: scrollView.frame.height)
        scrollView.isPagingEnabled = true

        for pos in 0 ..< pages.count {
            pages[pos].frame = CGRect(x: scrollView.frame.width * CGFloat(pos), y: 0,
                                    width: scrollView.frame.width, height: scrollView.frame.height)
            scrollView.addSubview(pages[pos])
            pages[pos].config(index: pos)
            pageIndicatorContainers[pos].addOnClickListener(action: actionPages[pos])
        }

        view.refreshView()
    }

    // MARK: Buttons
    @IBAction func tappedButtonNext(_ sender: Any) {
        if currentPageIndex == 2 {
            dismiss(animated: true, completion: nil)
        } else {
            changePage(to: currentPageIndex + 1, animated: true)
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

        for pos in 0 ..< pages.count {
            pageIndicatorIcons[pos].backgroundColor = index != pos ? .appGrey : .appRed
        }
    }

    func scrollViewDidScroll(_ scrollView: UIScrollView) {
        currentPageIndex = Int(round(scrollView.contentOffset.x/view.frame.width))
        for pos in 0 ..< pages.count {
            pageIndicatorIcons[pos].backgroundColor = currentPageIndex != pos ? .appGrey : .appRed
        }
        buttonNext.setTitle(currentPageIndex == 2 ? "back".localize() : "next".localize(),
                                for: .normal)
    }

}
