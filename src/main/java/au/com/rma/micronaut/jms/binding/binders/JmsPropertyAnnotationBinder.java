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
package au.com.rma.micronaut.jms.binding.binders;

import au.com.rma.micronaut.jms.annotation.JmsProperty;
import au.com.rma.micronaut.jms.conversion.ConversionHelper;
import au.com.rma.micronaut.jms.aop.JmsServerException;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.bind.ArgumentBinder;
import io.micronaut.core.convert.ArgumentConversionContext;

import javax.jms.JMSException;
import javax.jms.Message;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class JmsPropertyAnnotationBinder implements ArgumentBinder<Object, Message> {

  private Map<String, FunctionWithException<Message, Object, JMSException>> propertyMap = new LinkedHashMap<>();
  private ConversionHelper conversionHelper;

  public JmsPropertyAnnotationBinder(ConversionHelper conversionHelper) {
    this.conversionHelper = conversionHelper;

    propertyMap.put("messageId", Message::getJMSMessageID);
    propertyMap.put("timestamp", Message::getJMSTimestamp);
    propertyMap.put("correlationId", Message::getJMSCorrelationID);
    propertyMap.put("replyTo", Message::getJMSReplyTo);
    propertyMap.put("destination", Message::getJMSDestination);
    propertyMap.put("deliveryMode", Message::getJMSDeliveryMode);
    propertyMap.put("redelivered", Message::getJMSRedelivered);
    propertyMap.put("type", Message::getJMSType);
    propertyMap.put("expiration", Message::getJMSExpiration);
    propertyMap.put("deliveryTime", Message::getJMSDeliveryTime);
    propertyMap.put("priority", Message::getJMSPriority);
  }

  @FunctionalInterface
  private interface FunctionWithException<T, R, E extends Exception> {
    R invoke(T value) throws E;
  }

  @Override
  public BindingResult<Object> bind(ArgumentConversionContext<Object> context, Message source) {
    AnnotationValue<JmsProperty> annotation = context.getAnnotation(JmsProperty.class);

    String property = annotation.stringValue("name")
        .orElseThrow(() -> new JmsServerException("@JmsProperty annotation missing required name"));
    FunctionWithException<Message, Object, JMSException> function = propertyMap.get(property);
    if (function == null) {
      throw new JmsServerException("Unable to find a bindable property for " + property);
    }

    Class<?> argumentType = context.getArgument().getType();
    return () -> {
      try {
        return Optional.of(conversionHelper.convert(function.invoke(source), argumentType));
      } catch (JMSException jmse) {
        throw new JmsServerException("Unable to read property " + property + " from message");
      }
    };
  }
}
