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
import au.com.rma.micronaut.jms.binding.binders.JmsBodyAnnotationBinder;
import au.com.rma.micronaut.jms.binding.binders.JmsPropertyAnnotationBinder;
import au.com.rma.micronaut.jms.conversion.ConversionHelper;
import io.micronaut.core.bind.ArgumentBinder;
import io.micronaut.core.bind.ArgumentBinderRegistry;
import io.micronaut.core.bind.annotation.Bindable;
import io.micronaut.core.type.Argument;
import io.micronaut.messaging.annotation.Body;

import javax.inject.Singleton;
import javax.jms.Message;
import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Singleton
public class MessageBinderRegistry implements ArgumentBinderRegistry<Message> {

  private final Map<Class<? extends Annotation>, ArgumentBinder<Object, Message>> byAnnotation = new LinkedHashMap<>();

  public MessageBinderRegistry(ConversionHelper conversionHelper) {
    byAnnotation.put(JmsProperty.class, new JmsPropertyAnnotationBinder(conversionHelper));
    byAnnotation.put(Body.class, new JmsBodyAnnotationBinder(conversionHelper));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Optional<ArgumentBinder<T, Message>> findArgumentBinder(Argument<T> argument, Message source) {
    return Optional.ofNullable((ArgumentBinder<T, Message>)argument.getAnnotationMetadata().getAnnotationTypeByStereotype(Bindable.class)
        .map(byAnnotation::get)
        .orElse(null));
  }
}
