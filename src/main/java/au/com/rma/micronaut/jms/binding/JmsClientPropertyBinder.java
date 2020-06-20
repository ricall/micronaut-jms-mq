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
package au.com.rma.micronaut.jms.binding;

import au.com.rma.micronaut.jms.annotation.JmsProperty;
import au.com.rma.micronaut.jms.conversion.ConversionHelper;
import au.com.rma.micronaut.jms.aop.JmsClientException;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.inject.ExecutableMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import java.util.*;
import java.util.function.BiConsumer;

import static java.util.Collections.reverse;

@Singleton
public class JmsClientPropertyBinder {
  private static final Logger logger = LoggerFactory.getLogger(JmsClientPropertyBinder.class);
  private final Map<String, BiConsumer<JMSProducer, Object>> producerCache = new HashMap<>();
  private final Map<String, BiConsumer<Message, Object>> messageCache = new HashMap<>();

  private final ConversionHelper conversionHelper;

  public JmsClientPropertyBinder(ConversionHelper conversionHelper) {
    this.conversionHelper = conversionHelper;

    registerProducerConversion("deliveryDelay", JMSProducer::setDeliveryDelay, Long.class);
    registerProducerConversion("disableMessageId", JMSProducer::setDisableMessageID, Boolean.class);
    registerProducerConversion("disableMessageTimestamp", JMSProducer::setDisableMessageTimestamp, Boolean.class);
    registerProducerConversion("jmsType", JMSProducer::setJMSType, String.class);
    registerProducerConversion("priority", JMSProducer::setPriority, Integer.class);
    registerMessageConversion("correlationId", Message::setJMSCorrelationID, String.class);
    registerMessageConversion("deliveryMode", Message::setJMSDeliveryMode, Integer.class);
    registerMessageConversion("expiration", Message::setJMSExpiration, Long.class);
    registerMessageConversion("messageId", Message::setJMSMessageID, String.class);
    registerMessageConversion("type", Message::setJMSType, String.class);
  }

  public JmsClientBinder binder(MethodInvocationContext<Object, Object> context) {
    ExecutableMethod<Object, Object> method = context.getExecutableMethod();

    Map<String, Object> properties = new LinkedHashMap<>();
    List<AnnotationValue<JmsProperty>> classAnnotations = method.getAnnotationValuesByType(JmsProperty.class);
    reverse(classAnnotations);
    classAnnotations.forEach( p ->
        properties.put(p.stringValue("name")
                .orElseThrow(() -> new JmsClientException("Unable to get the property name for @JmsProperty annotation on method: " + method)),
            p.stringValue().orElse(null)));
    Map<String, Object> parameterValueMap = context.getParameterValueMap();
    Arrays.stream(context.getArguments())
        .forEach( argument -> {
          AnnotationValue<JmsProperty> annotation = argument.getAnnotation(JmsProperty.class);
          if (annotation != null) {
            properties.put(annotation.stringValue("name")
                    .orElseThrow(() -> new JmsClientException("Unable to get the property name for @JmsProperty annotation on method: " + method + " argument: " + argument)),
                parameterValueMap.get(argument.getName()));
          }
        });

    return (producer, message) -> applyProperties(producer, message, properties);
  }


  private void applyProperties(JMSProducer producer, Message message, Map<String, Object> properties) {
    properties.entrySet().forEach(e -> {
      String property = e.getKey();
      Object value = e.getValue();
      boolean assigned = false;
      BiConsumer<JMSProducer, Object> producerAssignment = producerCache.get(property);
      if (producerAssignment != null) {
        producerAssignment.accept(producer, value);
        assigned = true;
      }

      BiConsumer<Message, Object> messageAssignment = messageCache.get(property);
      if (messageAssignment != null) {
        messageAssignment.accept(message, value);
        assigned = true;
      }

      if (!assigned) {
        throw new JmsClientException("Unsupported @JmsProperty annotation " + property + "=" + value);
      }
    });
  }

  @FunctionalInterface
  public interface JmsClientBinder {
    void bind(JMSProducer producer, Message message);
  }

  @FunctionalInterface
  public interface BiConsumerWithException<T, U, E extends Exception> {
    void accept(T t, U u) throws E;
  }

  private <T> void registerProducerConversion(String property, BiConsumer<JMSProducer, T> target, Class<T> clazz) {
    producerCache.put(property, (producer, value) -> {
      try {
        target.accept(producer, conversionHelper.convert(value, clazz));
      } catch (Exception e) {
        logger.warn("Failed to assign property " + property + " to " + target);
      }
    });
  }

  private <T> void registerMessageConversion(String property, BiConsumerWithException<Message, T, JMSException> target, Class<T> clazz) {
    messageCache.put(property, (producer, value) -> {
      try {
        target.accept(producer, conversionHelper.convert(value, clazz));
      } catch (Exception e) {
        logger.warn("Failed to assign property " + property + " to " + target);
      }
    });
  }

}
