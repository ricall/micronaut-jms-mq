# micronaut-jms-mq library

This library allows you to write simple JMS applications using https://micronaut.io/ and IBM MQ

## Quickstart
The `micronaut-jms-mq` library requires configuration telling micronaut where the MQ servers are:
```yaml
mq-server:
  ibm-mq:
    host: "${mq.server:localhost}"
    port: "${mq.port:1414}"
    queueManager: QM1
    channel: DEV.ADMIN.SVRCONN
    username: admin
    password: passw0rd
```
To send messages you simply need to create a client interface:
```java
@JmsClient("ibm-mq")
public interface MessageService {
  @JmsDestination(value = "///DEV.QUEUE.MESSAGE")
  @JmsReplyDestination(value = "///DEV.QUEUE.REPLY", timeout = 5_000)
  String sendMessage(String text);
}
```

To listen to messages you create a listener interface:
```java
@Infrastructure
@JmsListener("ibm-mq")
public class MessageServiceListener {
  private static Logger logger = LoggerFactory.getLogger(MessageServiceListener.class);

  @JmsDestination("///DEV.QUEUE.MESSAGE")
  public String handleMessage(@Body String text) {
    logger.info("message received {}", text);
    return text;
  }
}
```

## Supported Features

* Simple JMS Client annotations allow you to create an interface for sending messages
  * Includes support for topics
  * Includes support for send/receive style messaging
* Simple JMS Listener annotations allow you to create a bean that receives messages from MQ
* Uses docker-compose to start the application.

## TODO

* Add an example using Topics 
  * Simple chat client using Server Send Events (SSE)
* Add support for different message types
* Add @Transactional support

# Authors
* **Richard Allwood** - Initial Version

## Licence
This product is licensed under the MIT License

## Acknowledgements
The project was inspired from the [Micronaut RabbitMQ project](https://micronaut-projects.github.io/micronaut-rabbitmq/latest/guide/)
 