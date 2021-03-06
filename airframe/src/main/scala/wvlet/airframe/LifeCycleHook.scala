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

import wvlet.log.LogSupport
import wvlet.obj.{ObjectMethod, ObjectType}

trait LifeCycleHook {
  def tpe : ObjectType
  def execute: Unit
}

case class EventHookHolder[A](tpe: ObjectType, obj: A, hook: A => Any) extends LifeCycleHook with LogSupport {
  override def toString : String = s"hook for [$tpe]: $hook"
  def execute {
    hook(obj)
  }
}
case class ObjectMethodCall(tpe: ObjectType, obj: AnyRef, method: ObjectMethod) extends LifeCycleHook {
  override def toString : String = s"method call hook for [$tpe]: $obj, $method"
  def execute {
    method.invoke(obj)
  }
}


