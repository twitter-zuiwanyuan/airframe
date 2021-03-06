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

import java.util.UUID

import wvlet.airframe.AirframeException.CYCLIC_DEPENDENCY
import wvlet.log.LogSupport
import wvlet.obj.ObjectType
import wvlet.airframe.AirframeMacros._

import scala.language.experimental.macros
import scala.reflect.runtime.{universe => ru}

object Binder {
  sealed trait Binding {
    def forSingleton: Boolean = false
    def from: ObjectType

  }
  case class ClassBinding(from: ObjectType, to: ObjectType) extends Binding {
    if(from == to) {
      throw new CYCLIC_DEPENDENCY(Set(to))
    }
  }
  case class SingletonBinding(from: ObjectType, to: ObjectType, isEager: Boolean) extends Binding {
    override def forSingleton: Boolean = true
  }
  case class ProviderBinding(factory: DependencyFactory, provideSingleton: Boolean, eager: Boolean)
    extends Binding {
    assert(!eager || (eager && provideSingleton))
    def from: ObjectType = factory.from
    override def forSingleton: Boolean = provideSingleton

    private val uuid : UUID = UUID.randomUUID()

    override def hashCode(): Int = { uuid.hashCode()  }
    override def equals(other: Any): Boolean = {
      other match {
        case that: ProviderBinding =>
          // Scala 2.12 generates Lambda for Function0, and the class might be generated every time, so
            // comparing functionClasses doesn't work
            (that canEqual this) && this.uuid == that.uuid
        case _ => false
      }
    }
  }

  case class DependencyFactory(from: ObjectType,
                               dependencyTypes: Seq[ObjectType],
                               factory: Any) {
    def create(args: Seq[Any]): Any = {
      require(args.length == dependencyTypes.length)
      args.length match {
        case 0 =>
          // We need to copy the F0 instance in order to make Design immutable
          factory.asInstanceOf[LazyF0[_]].copy.eval
        case 1 =>
          factory.asInstanceOf[Any => Any](args(0))
        case 2 =>
          factory.asInstanceOf[(Any, Any) => Any](args(0), args(1))
        case 3 =>
          factory.asInstanceOf[(Any, Any, Any) => Any](args(0), args(1), args(2))
        case 4 =>
          factory.asInstanceOf[(Any, Any, Any, Any) => Any](args(0), args(1), args(2), args(3))
        case 5 =>
          factory.asInstanceOf[(Any, Any, Any, Any, Any) => Any](args(0), args(1), args(2), args(3), args(4))
        case other =>
          throw new IllegalStateException("Should never reach")
      }
    }
  }

  /**
    * To provide an access to internal Binder methods
    */
  implicit class BinderAccess[A](binder:Binder[A]) {
    def toProviderD1[D1: ru.TypeTag](factory: (D1) => A, singleton: Boolean, eager: Boolean) : Design =
      binder.toProviderD1[D1](factory, singleton, eager)

    def toProviderD2[D1: ru.TypeTag, D2: ru.TypeTag](factory: (D1, D2) => A, singleton: Boolean, eager: Boolean) : Design =
      binder.toProviderD2[D1, D2](factory, singleton, eager)

    def toProviderD3[D1: ru.TypeTag, D2: ru.TypeTag, D3: ru.TypeTag]
    (factory: (D1, D2, D3) => A, singleton: Boolean, eager: Boolean): Design =
      binder.toProviderD3[D1, D2, D3](factory, singleton, eager)

    def toProviderD4[D1: ru.TypeTag, D2: ru.TypeTag, D3: ru.TypeTag, D4: ru.TypeTag]
    (factory: (D1, D2, D3, D4) => A, singleton: Boolean, eager: Boolean): Design =
      binder.toProviderD4[D1, D2, D3, D4](factory, singleton, eager)

    def toProviderD5[D1: ru.TypeTag, D2: ru.TypeTag, D3: ru.TypeTag, D4: ru.TypeTag, D5: ru.TypeTag]
    (factory: (D1, D2, D3, D4, D5) => A, singleton: Boolean, eager: Boolean): Design =
      binder.toProviderD5[D1, D2, D3, D4, D5](factory, singleton, eager)
  }
}

import wvlet.airframe.Binder._

/**
  *
  */
class Binder[A](val design: Design, val from: ObjectType) extends LogSupport {

  def to[B <: A : ru.TypeTag]: Design = macro binderToImpl[B]

  /**
    * Bind the type to a given instance. The instance will be instantiated as an eager singleton when creating a session.
    * Note that as you create a new session, new instance will be generated.
    *
    * @param any
    * @return
    */
  def toInstance(any: => A): Design = {
    design.addBinding(ProviderBinding(DependencyFactory(from, Seq.empty, LazyF0(any).asInstanceOf[Any]), true, true))
  }

  def toSingletonOf[B <: A : ru.TypeTag]: Design = macro binderToSingletonOfImpl[B]

  def toEagerSingletonOf[B <: A : ru.TypeTag]: Design = macro binderToEagerSingletonOfImpl[B]

  def toSingleton: Design = {
    design.addBinding(SingletonBinding(from, from, false))
  }

  def toEagerSingleton: Design = {
    design.addBinding(SingletonBinding(from, from, true))
  }

  def toProvider[D1: ru.TypeTag]
  (factory: D1 => A): Design = macro bindToProvider1[D1]
  def toProvider[D1: ru.TypeTag, D2: ru.TypeTag]
  (factory: (D1, D2) => A): Design = macro bindToProvider2[D1, D2]
  def toProvider[D1: ru.TypeTag, D2: ru.TypeTag, D3: ru.TypeTag]
  (factory: (D1, D2, D3) => A): Design = macro bindToProvider3[D1, D2, D3]
  def toProvider[D1: ru.TypeTag, D2: ru.TypeTag, D3: ru.TypeTag, D4: ru.TypeTag]
  (factory: (D1, D2, D3, D4) => A): Design = macro bindToProvider4[D1, D2, D3, D4]
  def toProvider[D1: ru.TypeTag, D2: ru.TypeTag, D3: ru.TypeTag, D4: ru.TypeTag, D5: ru.TypeTag]
  (factory: (D1, D2, D3, D4, D5) => A): Design = macro bindToProvider5[D1, D2, D3, D4, D5]

  def toSingletonProvider[D1: ru.TypeTag]
  (factory: D1 => A): Design = macro bindToSingletonProvider1[D1]
  def toSingletonProvider[D1: ru.TypeTag, D2: ru.TypeTag]
  (factory: (D1, D2) => A): Design = macro bindToSingletonProvider2[D1, D2]
  def toSingletonProvider[D1: ru.TypeTag, D2: ru.TypeTag, D3: ru.TypeTag]
  (factory: (D1, D2, D3) => A): Design = macro bindToSingletonProvider3[D1, D2, D3]
  def toSingletonProvider[D1: ru.TypeTag, D2: ru.TypeTag, D3: ru.TypeTag, D4: ru.TypeTag]
  (factory: (D1, D2, D3, D4) => A): Design = macro bindToSingletonProvider4[D1, D2, D3, D4]
  def toSingletonProvider[D1: ru.TypeTag, D2: ru.TypeTag, D3: ru.TypeTag, D4: ru.TypeTag, D5: ru.TypeTag]
  (factory: (D1, D2, D3, D4, D5) => A): Design = macro bindToSingletonProvider5[D1, D2, D3, D4, D5]

  def toEagerSingletonProvider[D1: ru.TypeTag]
  (factory: D1 => A): Design = macro bindToEagerSingletonProvider1[D1]
  def toEagerSingletonProvider[D1: ru.TypeTag, D2: ru.TypeTag]
  (factory: (D1, D2) => A): Design = macro bindToEagerSingletonProvider2[D1, D2]
  def toEagerSingletonProvider[D1: ru.TypeTag, D2: ru.TypeTag, D3: ru.TypeTag]
  (factory: (D1, D2, D3) => A): Design = macro bindToEagerSingletonProvider3[D1, D2, D3]
  def toEagerSingletonProvider[D1: ru.TypeTag, D2: ru.TypeTag, D3: ru.TypeTag, D4: ru.TypeTag]
  (factory: (D1, D2, D3, D4) => A): Design = macro bindToEagerSingletonProvider4[D1, D2, D3, D4]
  def toEagerSingletonProvider[D1: ru.TypeTag, D2: ru.TypeTag, D3: ru.TypeTag, D4: ru.TypeTag, D5: ru.TypeTag]
  (factory: (D1, D2, D3, D4, D5) => A) : Design = macro bindToEagerSingletonProvider5[D1, D2, D3, D4, D5]


  private[airframe] def toProviderD1[D1: ru.TypeTag]
  (factory: D1 => A, singleton: Boolean, eager: Boolean): Design = {
    design.addBinding(ProviderBinding(
      DependencyFactory(
        from,
        Seq(ObjectType.of[D1]),
        factory),
      singleton,
      eager
    ))
  }

  private[airframe] def toProviderD2[D1: ru.TypeTag, D2: ru.TypeTag]
  (factory: (D1, D2) => A, singleton: Boolean, eager: Boolean): Design = {
    design.addBinding(ProviderBinding(
      DependencyFactory(
        from,
        Seq(
          ObjectType.of[D1],
          ObjectType.of[D2]),
        factory),
      singleton,
      eager
    ))
  }

  private[airframe] def toProviderD3[D1: ru.TypeTag, D2: ru.TypeTag, D3: ru.TypeTag]
  (factory: (D1, D2, D3) => A, singleton: Boolean, eager: Boolean): Design = {
    design.addBinding(ProviderBinding(
      DependencyFactory(
        from,
        Seq(
          ObjectType.of[D1],
          ObjectType.of[D2],
          ObjectType.of[D3]),
        factory),
      singleton,
      eager
    ))
  }

  private[airframe] def toProviderD4[D1: ru.TypeTag, D2: ru.TypeTag, D3: ru.TypeTag, D4: ru.TypeTag]
  (factory: (D1, D2, D3, D4) => A, singleton: Boolean, eager: Boolean): Design = {
    design.addBinding(ProviderBinding(
      DependencyFactory(
        from,
        Seq(
          ObjectType.of[D1],
          ObjectType.of[D2],
          ObjectType.of[D3],
          ObjectType.of[D4]),
        factory),
      singleton,
      eager
    ))
  }

  private[airframe] def toProviderD5[D1: ru.TypeTag, D2: ru.TypeTag, D3: ru.TypeTag, D4: ru.TypeTag, D5: ru.TypeTag]
  (factory: (D1, D2, D3, D4, D5) => A, singleton: Boolean, eager: Boolean): Design = {
    design.addBinding(ProviderBinding(
      DependencyFactory(
        from,
        Seq(
          ObjectType.of[D1],
          ObjectType.of[D2],
          ObjectType.of[D3],
          ObjectType.of[D4],
          ObjectType.of[D5]),
        factory),
      singleton,
      eager
    ))
  }
}

