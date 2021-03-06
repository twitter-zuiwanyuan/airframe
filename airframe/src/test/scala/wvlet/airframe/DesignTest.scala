/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet.airframe

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import wvlet.airframe.Alias.{HelloRef, StringHello}
import wvlet.obj.ObjectType
import wvlet.obj.tag.@@

trait Message
case class Hello(message: String) extends Message
trait Production
trait Development

object Alias {
  trait Hello[A] {
    def hello : A
  }

  class StringHello extends Hello[String] {
    def hello = "hello world"
  }

  type HelloRef = Hello[String]
}
/**
  *
  */
class DesignTest extends AirframeSpec {

  val d0 = Design.blanc
  lazy val d1 =
    d0
    .bind[Message].to[Hello]
    .bind[Hello].toInstance(Hello("world"))
    .bind[Message].toSingleton
    .bind[Message].toEagerSingleton
    .bind[Message].toEagerSingletonOf[Hello]
    .bind[Message].toSingletonOf[Hello]
    .bind[Message @@ Production].toInstance(Hello("production"))
    .bind[Message @@ Development].toInstance(Hello("development"))

  "Design" should {
    "be immutable" in {
      d0 shouldEqual Design.blanc

      val d2 = d1.bind[Hello].toInstance(Hello("airframe"))
      d2 should not equal(d1)
    }

    "remove binding" in {
      val dd = d1.remove[Message]

      def hasMessage(d:Design) : Boolean =
        d.binding.exists(_.from == ObjectType.of[Message])
      def hasProductionMessage(d:Design) : Boolean =
        d.binding.exists(_.from == ObjectType.of[Message @@ Production])

      hasMessage(d1) shouldBe true
      hasMessage(dd) shouldBe false

      hasProductionMessage(d1) shouldBe true
      hasProductionMessage(dd) shouldBe true
    }


    "be serializable" taggedAs("ser") in {
      val b = d1.serialize
      val d1s = Design.deserialize(b)
      d1s shouldBe (d1)
    }

    "serialize instance binding" taggedAs("ser1") in {
      val d = Design.blanc.bind[Message].toInstance(Hello("world"))
      val b = d.serialize
      val ds = Design.deserialize(b)
      ds shouldBe (d)
    }

    "bind providers" in {
      val d = newDesign
              .bind[Hello].toProvider{ (m : String @@ Production) => Hello(m) }
              .bind[String@@Production].toInstance("hello production")

      val h = d.newSession.build[Hello]
      h.message shouldBe "hello production"
    }

    "bind type aliases" taggedAs("alias") in {
      val d = newDesign
              .bind[HelloRef].toInstance(new StringHello)

      val h = d.newSession.build[HelloRef]
      h.hello shouldBe "hello world"
    }
  }
}
