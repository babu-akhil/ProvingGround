package provingground

import scala.scalajs.js
import org.scalajs.dom
import dom.html.{Map => DomMap, _}
import scalajs.js.annotation.JSExport
import scalatags.JsDom.all._
import scala.util.Try

import scala.concurrent._

import scala.concurrent.ExecutionContext.Implicits.global

import org.scalajs.dom.ext._

import HoTT._

object ProvingGroundJS extends js.JSApp {
  def main(): Unit = {
    val page = Try(dom.document.getElementById("page")).map (_.textContent).getOrElse("default")

    page match {
      case "andrews-curtis" => AndrewsCurtisJS.andrewscurtisJS()
      case "default" => JsTest.jstest()
    }

//      JsTest.jstest()
  }
  
  val jsDiv = dom.document.getElementById("jsdiv")
  
  def insertDiv(div: Div) = 
    jsDiv.appendChild(div)
  
  def insertDiv(futDiv: Future[Div]) = 
    futDiv.foreach(jsDiv.appendChild(_))
    
  // Newer approach  
    
  val jsElems = dom.document.getElementsByClassName("js-element") map (_.asInstanceOf[Element])
  
  val script : Map[String, Element] = Map()
  
  
  jsElems foreach((elem) => {
    elem.innerHTML = ""
    elem.appendChild(script(elem.getAttribute("data-script")))
  }
  )
}