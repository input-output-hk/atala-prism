//
import Braintree
import BraintreeDropIn

class PaymentUtils {

    /// Calls the payment view.
    /// success: In case no error arises. Nonce string will be the param.
    /// error: In case of an error or in case of a cancellation. Bool param will be true in the former case.
    static func showPaymentView(_ view: BaseViewController?, token: String,
                                success: @escaping (String) -> Void,
                                error errorCallback: @escaping (Bool, Error?) -> Void) {

        let request = BTDropInRequest()
        let dropIn = BTDropInController(authorization: token, request: request) { controller, result, error in
            controller.dismiss(animated: true, completion: {
                DispatchQueue.main.async {
                    if error != nil {
                        errorCallback(false, error!)
                    } else if result?.isCancelled == true {
                        errorCallback(true, nil)
                    } else if let result = result, let nonce = result.paymentMethod?.nonce {
                        success(nonce)
                    } else {
                        errorCallback(false, SimpleLocalizedError("Payment nonce failed to retrieve."))
                    }
                }
            })
        }
        view?.present(dropIn!, animated: true, completion: nil)
    }
}
