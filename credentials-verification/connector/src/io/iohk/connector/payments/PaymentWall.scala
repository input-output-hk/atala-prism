package io.iohk.connector.payments

import com.paymentwall.java._
import io.iohk.connector.model.ParticipantId

class PaymentWall {

  def generatePaymentUrl(userId: ParticipantId): String = {
    val amount = 1
    val currency = "USD"
    val productId = "single_connection"
    val productName = "Single Connection"
    val product = newProduct(amount = amount, currency = currency, productId = productId, productName = productName)
    val widget = newWidget(product, userId.id.toString)
    widget.getUrl
  }

  private def newProduct(amount: Double, currency: String, productId: String, productName: String): Product = {
    val productBuilder = new ProductBuilder(productId)
    productBuilder.setAmount(amount);
    productBuilder.setCurrencyCode(currency);
    productBuilder.setName(productName);
    productBuilder.setProductType(Messages.TYPE_FIXED);
    productBuilder.build()
  }

  private def newWidget(product: Product, userId: String): Widget = {
    val widgetBuilder = new WidgetBuilder(userId, "p1_1");
    widgetBuilder.setProduct(product)

    val params = new java.util.LinkedHashMap[String, String]()
    //  params.put("email", "YOUR_CUSTOMER_EMAIL")
    //  params.put("history[registration_date]", "REGISTRATION_DATE");
    params.put("ps", "test"); // "all" shows all methods

    widgetBuilder.setExtraParams(params)
    widgetBuilder.build();
  }
}

object PaymentWall {
  case class Config(publicKey: String, privateKey: String)

  /**
    * This must be called exactly once, PaymentWall has a global config
    */
  def initialize(config: Config): Unit = {
    val innerConfig = com.paymentwall.java.Config.getInstance()
    innerConfig.setLocalApiType(com.paymentwall.java.Config.API_GOODS)
    innerConfig.setPublicKey(config.publicKey)
    innerConfig.setPrivateKey(config.privateKey)
  }
}
