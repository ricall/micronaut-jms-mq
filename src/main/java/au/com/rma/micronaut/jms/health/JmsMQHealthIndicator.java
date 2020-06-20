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
package au.com.rma.micronaut.jms.health;

import au.com.rma.micronaut.jms.jms.NamedConnectionFactory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.health.HealthStatus;
import io.micronaut.management.endpoint.health.HealthEndpoint;
import io.micronaut.management.health.indicator.AbstractHealthIndicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
@Requires(property = HealthEndpoint.PREFIX + "jms-mq.enabled", notEquals = "false")
@Requires(beans = HealthEndpoint.class)
public class JmsMQHealthIndicator extends AbstractHealthIndicator<Map<String, String>> {
  private static final Logger logger = LoggerFactory.getLogger(JmsMQHealthIndicator.class);
  private List<NamedConnectionFactory> connectionFactories;

  public JmsMQHealthIndicator(List<NamedConnectionFactory> connectionFactories) {
    this.connectionFactories = connectionFactories;
  }

  @Override
  protected Map<String, String> getHealthInformation() {
    logger.info("Checking Health");
    healthStatus = HealthStatus.UP;
    return connectionFactories.stream()
        .collect(Collectors.toMap(
            NamedConnectionFactory::getName,
            this::getConnectionStatus));
  }

  private String getConnectionStatus(NamedConnectionFactory cf) {
    try {
      cf.createContext();
      return "UP";
    } catch (Exception exception) {
      healthStatus = HealthStatus.DOWN;
      logger.error("Connection to MQ server failed", exception);
      return "DOWN";
    }
  }

  @Override
  protected String getName() {
    return "micronaut-jms-mq";
  }
}
