package io.iohk.atala.prism.credentials

import io.iohk.atala.prism.credentials.content.CredentialContent._

package object content {

  /**
    * Syntax allowing to use credential content fields like [[Tuple]].
    *
    * Example: {{
    *   Fields("field" -> Values(1, 2, "test")) == IndexedSeq(Field("field", Seq(IntValue(1), IntValue(2), StringValue("test"))))
    * }}
    */
  object syntax {
    implicit def shortcutToIntValue(value: Int): IntValue = IntValue(value)
    implicit def shortcutToStringValue(value: String): StringValue = StringValue(value)
    implicit def shortcutToStringField(s: (String, String)): Field = Field(s._1, StringValue(s._2))
    implicit def shortcutToIntField(s: (String, Int)): Field = Field(s._1, IntValue(s._2))
    implicit def shortcutToBooleanField(s: (String, Boolean)): Field = Field(s._1, BooleanValue(s._2))
    implicit def shortcutToSeqField(s: (String, Values)): Field = Field(s._1, SeqValue(s._2))
    implicit def shortcutToSubFields(s: (String, Fields)): Field = Field(s._1, SubFields(s._2))
  }

}
