package io.iohk.atala.prism.credentials.utils

import scala.util.parsing.input.Positional
import scala.util.parsing.combinator.JavaTokenParsers

import io.iohk.atala.prism.credentials.utils.Mustache._

/**
  * Subset of Mustache templates (https://mustache.github.io/) for internal usage in Prism SDK.
  *
  * Implemented specification follows https://mustache.github.io/mustache.5.html
  *
  * Currently supported tags:
  *   - variables: {{ variable }} or with path {{ variable.name }}
  *   - unescaped variables: {{& variable }}
  *   - comments with multiline support: {{! variable }}
  *
  * Usage:
  *
  * {{{
  *   val mustache = new Mustache
  *   val context: TemplateContext = (variable: String) => Some("\$variable content")
  *   mustache.render("Template with {{ variable }}.", context) == Right("Template with variable content".)
  * }}}
  */
class Mustache extends JavaTokenParsers {

  override def skipWhitespace = false

  val open: Parser[String] = "{{"
  val close: Parser[String] = "}}"

  val literal: Parser[Literal] =
    positioned(rep1(not(open | close) ~> ".|\r|\n".r) ^^ { l => Literal(l.mkString) })

  val identifier: Parser[String] =
    rep1sep(ident, ".") ^^ { _.mkString(".") }

  val variable: Parser[Variable] =
    mustache(pad(identifier) ^^ { Variable(_, escape = true) })

  val unescapedVariable: Parser[Variable] =
    mustache("&" ~> pad(identifier) ^^ { Variable(_, escape = false) })

  val comment: Parser[Comment] =
    mustache("!" ~> pad(literal) ^^ { l => Comment(l.content.trim) })

  val statements: Parser[MustacheNode] =
    variable | unescapedVariable | comment

  val template: Parser[Template] =
    phrase(rep(literal | statements)) ^^ { Template(_) }

  /**
    * Parse string into template tree.
    */
  def parse(content: String): Either[MustacheError, Template] = {
    parse(template, content) match {
      case Success(matched, _) => Right(matched)
      case Failure(msg, _) => Left(MustacheError(s"Template parsing failure: $msg"))
      case Error(msg, _) => Left(MustacheError(s"Template parsing error: $msg"))
    }
  }

  /**
    * Render given template with provided context.
    */
  def render(template: Template, context: TemplateContext): Either[MustacheError, String] = {
    Right(template.tags.map {
      case Literal(content) => content
      case _: Comment => ""
      case Variable(variable, escape) =>
        context(variable) match {
          case Some(value) if escape => Mustache.escapeHtml(value.toString)
          case Some(value) if escape == false => value.toString
          case None => "" // TODO: Create strict mode and retrun error if variable desn't exist.
        }
    }.mkString)
  }

  /**
    * Render given string template with provided context.
    */
  def render(content: String, context: TemplateContext): Either[MustacheError, String] =
    parse(content).flatMap(render(_, context))

  /**
    * Render given string template with provided context.
    *
    * @throws MustacheError
    */
  def unsafeRender(content: String, context: TemplateContext): String = {
    render(content, context) match {
      case Left(error) => throw error
      case Right(result) => result
    }
  }

  /**
    * Trim whitespaces.
    */
  private def pad[T](parser: Parser[T]): Parser[T] =
    opt(whiteSpace) ~> parser <~ opt(whiteSpace)

  /**
    * Combine parser into mustache tags.
    */
  private def mustache[T <: MustacheNode](parser: Parser[T]): Parser[T] =
    positioned(open ~> parser <~ close)
}

object Mustache {
  sealed trait MustacheNode extends Positional
  case class Literal(content: String) extends MustacheNode
  case class Comment(content: String) extends MustacheNode
  case class Variable(name: String, escape: Boolean) extends MustacheNode
  case class Template(tags: Seq[MustacheNode])

  case class MustacheError(message: String) extends Exception

  type TemplateContext = String => Option[Any]

  /**
    * Render given string template with provided context.
    */
  def render(content: String, context: TemplateContext): Either[MustacheError, String] =
    (new Mustache).render(content, context)

  /**
    * Determine if given string is a Mustache template.
    */
  def isMustacheTemplate(content: String): Boolean =
    "\\{\\{(?:(?!}}).)*\\}\\}".r.findFirstIn(content).isDefined

  /**
    * Escape html entities.
    * Solution from: https://github.com/janl/mustache.js/blob/master/mustache.js#L67-L76
    */
  def escapeHtml(html: String): String = {
    html.map {
      case '\'' => "&#39;"
      case '\"' => "&quot;"
      case '&' => "&amp;"
      case '<' => "&lt;"
      case '>' => "&gt;"
      case '/' => "&#x2F;"
      case '`' => "&#x60;"
      case '=' => "&#x3D;"
      case other => other
    }.mkString
  }

}
