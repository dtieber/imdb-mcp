package controllers

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.test.Helpers._
import play.api.test.FakeRequest
import play.api.libs.json.Json

class HomeControllerSpec extends AnyWordSpec with GuiceOneAppPerSuite with Matchers {

  "HomeController GET /" should {
    "return 200 OK with empty JSON object" in {
      val controller = app.injector.instanceOf[HomeController]
      val request = FakeRequest(GET, "/")
      val result = controller.index().apply(request)

      status(result) shouldBe OK
      contentType(result) shouldBe Some("application/json")
      contentAsJson(result) shouldBe Json.obj()
    }
  }
}
