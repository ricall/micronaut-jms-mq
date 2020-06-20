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
package au.com.rma.micronaut.jms.conversion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.core.convert.ConversionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

@Singleton
public class ConversionHelper {
  private static Logger logger = LoggerFactory.getLogger(ConversionHelper.class);

  private ConversionService conversionService;
  private ObjectMapper objectMapper;

  public ConversionHelper(ConversionService conversionService, ObjectMapper objectMapper) {
    this.conversionService = conversionService;
    this.objectMapper = objectMapper;
  }

  @SuppressWarnings("unchecked")
  public <T,R> R convert(T source, Class<R> clazz) {
    if (source == null) {
      return null;
    }
    return (R)conversionService.convert(source, clazz).get();
  }

  public <T,R> String convertObjectToString(T source) {
    if (source == null) {
      return null;
    }
    if (source.getClass().equals(String.class)) {
      return (String)source;
    }
    try {
      return objectMapper.writeValueAsString(source);
    } catch (JsonProcessingException jpe) {
      throw new ConversionException("Failed to convert source to String value", jpe);
    }
  }

  public Object convertMessageToObject(Object reply, Class<?> returnClass) {
    Class replyClass = reply == null ? String.class : reply.getClass();

    if (conversionService.canConvert(replyClass, returnClass)) {
      return conversionService.convert(reply, returnClass).get();
    }

    try {
      String json = reply.toString();
      return objectMapper.readValue(json, returnClass);
    } catch (JsonProcessingException jpe) {
      throw new ConversionException("Failed to convert response to " + returnClass, jpe);
    }
  }
}
