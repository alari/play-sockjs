package infra.sockjs

import java.util.concurrent.{TimeoutException, TimeUnit}

import akka.actor.ActorSystem
import play.api.Plugin

import scala.concurrent.duration.Duration

/**
 * @author alari
 * @since 12/13/13
 */
object SockJs {
  def plugin = play.api.Play.current.plugin[SockJs].getOrElse(throw new IllegalStateException("SockJs plugin is not enabled!"))

  implicit def system = plugin.system

  implicit val Timeout = akka.util.Timeout(1, TimeUnit.SECONDS)

  abstract class ClosableLazy[T <: AnyRef] {

    /**
     * The type of resource to close when `close()` is called. May be different
     * from the type of T.
     */
    protected type ResourceToClose <: AnyRef
    /**
     * The result of calling the `create()` method.
     *
     * @param value The value that has been initialized.
     * @param resourceToClose A resource that has been allocated. This resource will be
     * passed to `close(ResourceToClose)`  when `close()` is called.
     */
    protected case class CreateResult(value: T, resourceToClose: ResourceToClose)

    @volatile
    private var value: AnyRef = null
    private var resourceToClose: AnyRef = null
    private var hasBeenClosed: Boolean = false

    /**
     * Get the value. Calling this method may allocate resources, such as a thread pool.
     *
     * Calling this method after the `close()` method has been called will result in an
     * IllegalStateException.
     */
    final def get(): T = {
      val currentValue = value
      if (currentValue != null) return currentValue.asInstanceOf[T]
      synchronized {
        if (hasBeenClosed) throw new IllegalStateException("Can't get ClosableLazy value after it has been closed")
        if (value == null) {
          val result = create()
          if (result.value == null) throw new IllegalStateException("Can't initialize ClosableLazy to null value")
          value = result.value
          resourceToClose = result.resourceToClose
        }
        value.asInstanceOf[T]
      }
    }

    /**
     * Close the value. Calling this method is safe, but does nothing, if the value
     * has not been initialized.
     */
    final def close(): Unit = {
      synchronized {
        if (!hasBeenClosed && value != null) {
          val cachedCloseInfo = resourceToClose.asInstanceOf[ResourceToClose]
          value = null
          hasBeenClosed = true
          resourceToClose = null
          close(cachedCloseInfo)
        }
      }

    }

    /**
     * Called when the lazy value is first initialized. Returns the value, and the
     * resource to close when `close()` is called.
     */
    protected def create(): CreateResult

    /**
     * Called when `close()` is called. Passed the resource that was originally
     * returned when `create()` was called.
     */
    protected def close(resourceToClose: ResourceToClose)
  }
}

class SockJs(app: play.api.Application) extends Plugin {

  private val lazySystem = new SockJs.ClosableLazy[ActorSystem] {

    protected type ResourceToClose = ActorSystem

    protected def create(): CreateResult = {
      val system = ActorSystem("sockjs", app.configuration.underlying, app.classloader)
      play.api.Logger.info("Starting SockJs default Akka system.")
      CreateResult(system, system)
    }

    protected def close(systemToClose: ActorSystem) = {
      play.api.Logger.info("Shutdown SockJs default Akka system.")
      systemToClose.shutdown()

      app.configuration.getMilliseconds("sockjs.akka.shutdown-timeout") match {
        case Some(timeout) =>
          try {
            systemToClose.awaitTermination(Duration(timeout, TimeUnit.MILLISECONDS))
          } catch {
            case te: TimeoutException =>
              // oh well.  We tried to be nice.
              play.api.Logger.info(s"Could not shutdown the SockJs Akka system in $timeout milliseconds.  Giving up.")
          }
        case None =>
          // wait until it is shutdown
          systemToClose.awaitTermination()
      }
    }
  }

  def system: ActorSystem = lazySystem.get()

  override def onStop() {
    lazySystem.close()
  }


}