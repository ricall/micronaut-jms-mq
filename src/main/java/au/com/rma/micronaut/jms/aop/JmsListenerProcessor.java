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
package au.com.rma.micronaut.jms.aop;

import au.com.rma.micronaut.jms.annotation.JmsDestination;
import au.com.rma.micronaut.jms.annotation.JmsListener;
import au.com.rma.micronaut.jms.binding.MessageBinderRegistry;
import au.com.rma.micronaut.jms.conversion.ConversionHelper;
import au.com.rma.micronaut.jms.jms.JmsHelper;
import com.ibm.msg.client.jms.JmsMessage;
import io.micronaut.context.BeanContext;
import io.micronaut.context.processor.ExecutableMethodProcessor;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.bind.BoundExecutable;
import io.micronaut.core.bind.DefaultExecutableBinder;
import io.micronaut.core.bind.ExecutableBinder;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.inject.qualifiers.Qualifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.inject.Qualifier;
import javax.inject.Singleton;
import javax.jms.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * A {@link ExecutableMethodProcessor} that will process all beans annotated with {@link JmsListener}
 * creating a Jms Listener that will forward all method calls to
 */
@Singleton
public class JmsListenerProcessor implements ExecutableMethodProcessor<JmsListener>, AutoCloseable {
  private static final Logger logger = LoggerFactory.getLogger(JmsListenerProcessor.class);

  private BeanContext beanContext;
  private ConversionHelper conversionHelper;
  private JmsHelper jmsHelper;
  private MessageBinderRegistry messageBinderRegistry;
  private List<AutoCloseable> listeners = new ArrayList<AutoCloseable>();

  public JmsListenerProcessor(
      BeanContext beanContext,
      ConversionHelper conversionHelper,
      JmsHelper jmsHelper,
      MessageBinderRegistry messageBinderRegistry) {
    this.beanContext = beanContext;
    this.conversionHelper = conversionHelper;
    this.jmsHelper = jmsHelper;
    this.messageBinderRegistry = messageBinderRegistry;
  }

  @Override
  public void process(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
    AnnotationValue<JmsDestination> destinationAnnotation = method.getAnnotation(JmsDestination.class);
    if (destinationAnnotation == null) {
      return;
    }

    String destination = destinationAnnotation.stringValue()
        .orElseThrow(() -> new JmsServerException("@Destination must contain a connection name on method: " + method));
    String name = method.findAnnotation(JmsListener.class)
        .orElseThrow(() -> new IllegalArgumentException("No @JmsListener annotation on method: " + method))
        .stringValue()
        .orElseThrow(() -> new IllegalArgumentException("@JmsListener must contain a connection name on method: " + method));

    io.micronaut.context.Qualifier<Object> qualifier = beanDefinition.getAnnotationTypeByStereotype(Qualifier.class)
        .map(type -> Qualifiers.byAnnotation(beanDefinition, type))
        .orElse(null);
    Class<Object> beanType = (Class<Object>)beanDefinition.getBeanType();
    Object bean = beanContext.getBean(beanType, qualifier);
    ExecutableBinder<Message> binder = new DefaultExecutableBinder<>();

    listeners.add(jmsHelper.withListener(name, destination, (context, message) -> {
        BoundExecutable boundExecutable = binder.bind(method, messageBinderRegistry, message);

        Object returnValue = boundExecutable.invoke(bean);

        if (message.getJMSReplyTo() != null) {
          JMSProducer producer = context.createProducer();

          TextMessage reply = context.createTextMessage(conversionHelper.convertObjectToString(returnValue));
          reply.setJMSCorrelationID(message.getJMSCorrelationID());
          producer.send(message.getJMSReplyTo(), reply);
        }
    }));
  }

  @SuppressWarnings("unchecked")
  void receiveMessage(JMSContext context, Message message, ExecutableMethod<?, ?> method) {
    try {
      logger.info("Received message");

      logger.info("*******************************************************************");
      logger.info("MessageType: {}", message.getClass().getName());
      if (message instanceof JmsMessage) {
        logger.info("JMSMessageID: {}", message.getJMSMessageID());
        logger.info("JMSTimestamp: {}", message.getJMSTimestamp());
        logger.info("JMSCorrelationId: {}", message.getJMSCorrelationID());
        logger.info("JMSReplyTo: {}", message.getJMSReplyTo());
        logger.info("JMSDestination: {}", message.getJMSDestination());
        logger.info("JMSDeliveryMode: {}", message.getJMSDeliveryMode());
        logger.info("JMSRedelivered: {}", message.getJMSRedelivered());
        logger.info("JMSType: ${}", message.getJMSType());
        logger.info("JMSExpiration: {}", message.getJMSExpiration());
        logger.info("JMSDeliveryTime: {}", message.getJMSDeliveryTime());
        logger.info("JMSPriority: {}", message.getJMSPriority());

        Enumeration<String> properties = message.getPropertyNames();
        while (properties.hasMoreElements()) {
          String property = properties.nextElement();
          logger.info("Property {} = {}", property, message.getObjectProperty(property));
        }
      }
      if (message instanceof TextMessage){
        logger.info("<TEXT>");
        logger.info(((TextMessage) message).getText());
      }
      if (message instanceof BytesMessage){
        BytesMessage mess = (BytesMessage)message;
        byte[] data = new byte[(int)mess.getBodyLength()];
        mess.readBytes(data);

        logger.info("<BYTES>");
        logger.info(new String(data));
      }

      // Call the method with the correctly bound arguments



    } catch (Exception e) {
      logger.error("Error receiving message", e);
    }
  }


  @Override
  @PreDestroy
  public void close() throws Exception {
    listeners.forEach(listener -> {
      try {
        listener.close();
      } catch (Exception e) {
        logger.error("Error closing listener", e);
      }
    });
  }
}
