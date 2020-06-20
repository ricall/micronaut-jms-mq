package au.com.rma.micronaut.jms.support

import au.com.rma.micronaut.jms.annotation.JmsClient
import au.com.rma.micronaut.jms.annotation.JmsDestination

@JmsClient("admin")
interface StringSender {
  @JmsDestination("DEV.QUEUE.1")
  void sendMessage(String message)
}