//
import Alamofire
import AlamofireImage

extension UIImage {

    static func loadUrlImage(url: String, callback: @escaping (Image) -> Void) {
        Alamofire.request(url).responseImage { response in
            if let image = response.result.value {
                callback(image)
            }
        }
    }

    static func loadCircularImage(url: String, width: CGFloat, height: CGFloat, callback: @escaping (Image) -> Void) {
        loadUrlImage(url: url) { image in
            let circularImage = image.af_imageRoundedIntoCircle()
            let size = CGSize(width: width, height: height)
            let scaledImage = circularImage.af_imageScaled(to: size)
            callback(scaledImage)
        }
    }

    static func applyUrlImage(url: String?, imageView: UIImageView, isCircular: Bool = false, placeholderNamed: String? = nil, callback: ((Image) -> Void)? = nil) {

        // Apply the placeholder
        if placeholderNamed != nil {
            imageView.image = UIImage(named: placeholderNamed!)
        }

        // If the url is nil or empty return
        if url?.isEmpty ?? true {
            return
        }

        // Load remote image and apply it
        loadUrlImage(url: url!) { image in
            var scaledImage = image
            if isCircular && imageView.width > 0 && imageView.height > 0 {
                let circularImage = image.af_imageRoundedIntoCircle()
                let size = CGSize(width: imageView.width, height: imageView.height)
                scaledImage = circularImage.af_imageScaled(to: size)
            }
            imageView.image = scaledImage
            callback?(scaledImage)
        }
    }
}

extension UIImageView {

    func applyUrlImage(url: String?, isCircular: Bool = false, placeholderNamed: String? = nil, callback: ((Image) -> Void)? = nil) {
        UIImage.applyUrlImage(url: url, imageView: self, isCircular: isCircular, placeholderNamed: placeholderNamed, callback: callback)
    }
}
