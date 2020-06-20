/*
 * Copyright (c) 2020 Richard Allwood
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package au.com.rma.micronaut.jms.jms;

import au.com.rma.micronaut.jms.annotation.JmsDestinationType;
import au.com.rma.micronaut.jms.aop.JmsServerException;
import io.micronaut.messaging.exceptions.MessagingException;
import io.micronaut.scheduling.TaskExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.jms.*;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Singleton
public class JmsHelper {
  private static Logger logger = LoggerFactory.getLogger(JmsHelper.class);

  private List<NamedConnectionFactory> connectionFactories;
  private ExecutorService executorService;

  public JmsHelper(List<NamedConnectionFactory> connectionFactories, @Named(TaskExecutors.IO) ExecutorService executorService) {
    this.connectionFactories = connectionFactories;
    this.executorService = executorService;
  }

  public ConnectionFactory getNamedFactory(String name) {
    return connectionFactories.stream()
        .filter(factory -> factory.getName().equals(name))
        .findFirst()
        .orElseThrow(() -> new MessagingException("Unable to find JMS connection factory named " + name));
  }

  public void withClient(String name, ContextHandler handler) {
    JMSContext context = getNamedFactory(name).createContext();

    try {
      handler.usingContext(context);
    } catch (JMSException exception) {
      logger.error("Failed handle client", exception);
    }
  }

  public AutoCloseable withListener(String name, String queue, ContextMessageHandler consumer) {
    JmsListenerWrapper wrapper = new JmsListenerWrapper(name, queue, consumer);
    executorService.submit(wrapper);

    return wrapper;
  }

  public Destination destinationFor(String name, JmsDestinationType destinationType, JMSContext context) {
    switch(destinationType) {
      case TOPIC:
        return context.createTopic(name);
      default:
        return context.createQueue(name);
    }
  }

  @FunctionalInterface
  public interface ContextHandler {
    void usingContext(JMSContext context) throws JMSException;
  }

  @FunctionalInterface
  public interface ContextMessageHandler {
    void usingContext(JMSContext context, Message message) throws JMSException;
  }

  private class JmsListenerWrapper implements Runnable, ExceptionListener, AutoCloseable {
    private String name;
    private String queue;
    private ContextMessageHandler messageConsumer;

    private Thread thread;
    private JMSContext context;

    private JmsListenerWrapper(String name, String queue, ContextMessageHandler messageConsumer) {
      this.name = name;
      this.queue = queue;
      this.messageConsumer = messageConsumer;
    }

    @Override
    public void run() {
      thread = Thread.currentThread();
      while (!thread.isInterrupted()) {
        try {
          context = getNamedFactory(name).createContext();
          context.setExceptionListener(this);

          Queue queue = context.createQueue(this.queue);
          JMSConsumer consumer = context.createConsumer(queue);

          consumer.setMessageListener(message -> {
            try {
              messageConsumer.usingContext(context, message);
            } catch (JMSException exception) {
              throw new JmsServerException("Unable to process message", exception);
            }
          });
          return;
        } catch (Exception e) {
          logger.error("Encountered an error initialising the listener", e);
          try {
            Thread.sleep(5000);
          } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
          }
        }
      }
    }

    @Override
    synchronized public void onException(JMSException exception) {
      close();
      executorService.submit(this);
    }

    @Override
    public void close() {
      context.close();
      thread.interrupt();
    }
  }

}
