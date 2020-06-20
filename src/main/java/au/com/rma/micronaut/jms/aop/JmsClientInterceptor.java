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

import au.com.rma.micronaut.jms.annotation.JmsClient;
import au.com.rma.micronaut.jms.annotation.JmsDestination;
import au.com.rma.micronaut.jms.annotation.JmsDestinationType;
import au.com.rma.micronaut.jms.annotation.JmsReplyDestination;
import au.com.rma.micronaut.jms.binding.JmsClientPropertyBinder;
import au.com.rma.micronaut.jms.conversion.ConversionHelper;
import au.com.rma.micronaut.jms.jms.JmsHelper;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.bind.annotation.Bindable;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.ReturnType;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.messaging.annotation.Body;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.jms.*;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class JmsClientInterceptor implements MethodInterceptor<Object, Object> {
  private static final Logger logger = LoggerFactory.getLogger(JmsClientInterceptor.class);

  private final ConversionHelper conversionHelper;
  private final JmsHelper jmsHelper;
  private final JmsClientPropertyBinder propertyBinder;

  public JmsClientInterceptor(
      ConversionHelper conversionHelper,
      JmsHelper jmsHelper,
      JmsClientPropertyBinder propertyBinder) {
    this.conversionHelper = conversionHelper;
    this.jmsHelper = jmsHelper;
    this.propertyBinder = propertyBinder;
  }

  @Override
  public Object intercept(MethodInvocationContext<Object, Object> context) {
    if (!context.hasAnnotation(JmsClient.class)) {
      return context.proceed();
    }

    ExecutableMethod<Object, Object> method = context.getExecutableMethod();
    logger.trace("Intercepting {} with parameters {}", context.getMethodName(), context.getArguments());

    String connectionName = getConnectionName(method);
    AnnotationValue<JmsDestination> destinationAnnotation = getDestinationAnnotation(method);
    String destinationName = destinationAnnotation.stringValue()
        .orElseThrow(() -> new IllegalArgumentException("@JmsDestination must contain the queue to send on method: " + method));
    JmsDestinationType destinationType = destinationAnnotation.enumValue("type", JmsDestinationType.class)
        .orElse(JmsDestinationType.QUEUE);
    Optional<AnnotationValue<JmsReplyDestination>> replyToAnnotation = getReplyToAnnotation(method);

    String message = getBody(context);

    JmsClientPropertyBinder.JmsClientBinder binder = propertyBinder.binder(context);
    ReturnType<?> returnType = method.getReturnType();

    AtomicReference<Object> response = new AtomicReference<>();
    jmsHelper.withClient(connectionName, ctx -> {
      JMSProducer producer = ctx.createProducer();

      TextMessage textMessage = ctx.createTextMessage(message);
      Destination destination = jmsHelper.destinationFor(destinationName, destinationType, ctx);
      Destination replyToDestination = replyToAnnotation.map(annotation -> jmsHelper.destinationFor(
          annotation.stringValue().orElseThrow(() -> new JmsClientException("@JmsReplyDestination is missing a destination")),
          annotation.enumValue(JmsDestinationType.class).orElse(JmsDestinationType.QUEUE),
          ctx)).orElse(null);

      if (replyToAnnotation.isPresent()) {
        textMessage.setJMSReplyTo(replyToDestination);
      }
      textMessage.setJMSCorrelationID(UUID.randomUUID().toString());
      binder.bind(producer, textMessage);

      producer.send(destination, textMessage);

      if (replyToAnnotation.isPresent()) {
        String jmsCorrelationID = textMessage.getJMSCorrelationID();
        JMSConsumer consumer = ctx.createConsumer(replyToDestination, "JMSCorrelationID='" + jmsCorrelationID + "'");
        Message reply = consumer.receive(replyToAnnotation.get().longValue("timeout").orElse(5_000));

        if (reply == null) {
          throw new JmsClientException("Timeout receiving response for JMSCorrelationID=" + jmsCorrelationID);
        }
        String replyText = reply.getBody(String.class);
        response.set(conversionHelper.convertMessageToObject(replyText, returnType.getType()));
      }
    });
    return response.get();
  }

  private String getConnectionName(ExecutableMethod<Object, Object> method) {
    return method.findAnnotation(JmsClient.class)
        .orElseThrow(() -> new IllegalArgumentException("No @JmsClient annotation on method: " + method))
        .stringValue()
        .orElseThrow(() -> new IllegalArgumentException("@JmsClient must contain a connection name on method: " + method));
  }

  private AnnotationValue<JmsDestination> getDestinationAnnotation(ExecutableMethod<Object, Object> method) {
    return method.findAnnotation(JmsDestination.class)
        .orElseThrow(() -> new IllegalArgumentException("No @JmsDestination annotation on method: " + method));
  }

  private Optional<AnnotationValue<JmsReplyDestination>> getReplyToAnnotation(ExecutableMethod<Object, Object> method) {
    return method.findAnnotation(JmsReplyDestination.class);
  }

  private String getBody(MethodInvocationContext<Object, Object> context) {
    Argument<?> bodyArgument = findBodyArgument(context.getArguments())
        .orElseThrow(() -> new JmsClientException("No valid body argument found on method: " + context.getMethodName()));
    Object message = context.getParameters().get(bodyArgument.getName()).getValue();

    return conversionHelper.convertObjectToString(message);
  }

  private Optional<Argument<?>> findBodyArgument(Argument[] arguments) {
    if (arguments.length == 1) {
      return Optional.of(arguments[0]);
    }
    return Optional.ofNullable(Arrays.stream(arguments)
        .filter(this::isBodyArgument)
        .findFirst()
        .orElse(null));
  }

  private boolean isBodyArgument(Argument argument) {
    AnnotationMetadata metadata = argument.getAnnotationMetadata();

    return metadata.hasAnnotation(Body.class) || metadata.hasAnnotation(Bindable.class);
  }
}
