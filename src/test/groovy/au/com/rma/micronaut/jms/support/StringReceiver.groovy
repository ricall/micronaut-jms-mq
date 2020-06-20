package au.com.rma.micronaut.jms.support

import au.com.rma.micronaut.jms.annotation.JmsDestination
import au.com.rma.micronaut.jms.annotation.JmsListener
import io.micronaut.messaging.annotation.Body

import java.util.concurrent.CountDownLatch

@JmsListener("admin")
class StringReceiver {
  private latch
  private message

  @JmsDestination("DEV.QUEUE.1")
  void onMessage(@Body String message) {
    this.message = message
    latch?.countDown()
  }

  String getMessage() {
    latch = new CountDownLatch(1)
    latch.await()
    return message
  }
}