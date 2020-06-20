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

import au.com.rma.micronaut.jms.conversion.ConversionHelper;
import au.com.rma.micronaut.jms.aop.JmsServerException;
import io.micronaut.core.bind.ArgumentBinder;
import io.micronaut.core.convert.ArgumentConversionContext;

import javax.jms.JMSException;
import javax.jms.Message;
import java.util.Optional;

public class JmsBodyAnnotationBinder implements ArgumentBinder<Object, Message> {

  private ConversionHelper conversionHelper;

  public JmsBodyAnnotationBinder(ConversionHelper conversionHelper) {
    this.conversionHelper = conversionHelper;
  }

  @Override
  public BindingResult<Object> bind(ArgumentConversionContext<Object> context, Message source) {
    try {
      String body = source.getBody(String.class);
      Class<Object> type = context.getArgument().getType();
      return () -> Optional.of(conversionHelper.convertMessageToObject(body, type));
    } catch (JMSException jmse) {
      throw new JmsServerException("Failed to convert body to " + context.getArgument());
    }
  }
}
