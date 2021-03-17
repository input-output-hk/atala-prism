//
//  IntdemoTutorialPageView.swift
//  prism-ios-wallet
//
//  Created by Leandro Pardo on 01/03/2021.
//  Copyright © 2021 iohk. All rights reserved.
//

import UIKit

class IntdemoTutorialPageView: BaseNibLoadingView {

    var pageIndex: Int = 0

    @IBOutlet weak var labelTitle: UILabel!
    @IBOutlet weak var labelSubtitle: UILabel!
    @IBOutlet weak var imageMain: UIImageView!
    @IBOutlet weak var imageSecondary: UIImageView!
    @IBOutlet weak var labelDescription: UILabel!

    func config(index: Int) {

        self.pageIndex = index

        switch index {
        case 0:
            labelTitle.text = "intdemotutorial_step_1_title".localize()
            labelSubtitle.text = "intdemotutorial_step_1_subtitle".localize()
            imageMain.image = nil
            imageSecondary.image = #imageLiteral(resourceName: "img_intdemo_step_1")
            labelDescription.text = ""
        case 1:
            labelTitle.text = "intdemotutorial_step_2_title".localize()
            labelSubtitle.text = "intdemotutorial_step_2_subtitle".localize()
            imageMain.image = #imageLiteral(resourceName: "img_intdemo_step_2")
            imageSecondary.image = nil
            let desc = NSMutableAttributedString(attributedString: "01.  ".colored(with: .appRed))
            desc.append("intdemotutorial_step_2_desc_1".localize().regular)
            desc.append("02.  ".colored(with: .appRed))
            desc.append("intdemotutorial_step_2_desc_2".localize().regular)
            desc.append("03.  ".colored(with: .appRed))
            desc.append("intdemotutorial_step_2_desc_3".localize().regular)
            labelDescription.attributedText = desc
        case 2:
            labelTitle.text = "intdemotutorial_step_3_title".localize()
            labelSubtitle.text = "intdemotutorial_step_3_subtitle".localize()
            imageMain.image = #imageLiteral(resourceName: "img_intdemo_step_3")
            imageSecondary.image = nil
            labelDescription.text = ""
        default:
            return
        }

        self.layoutIfNeeded()
    }

}
