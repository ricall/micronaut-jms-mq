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

import au.com.rma.micronaut.jms.configuration.ServerConfiguration;
import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;

import static com.ibm.msg.client.jms.JmsConstants.WMQ_PROVIDER;
import static com.ibm.msg.client.wmq.common.CommonConstants.*;

@Factory
public class ConnectionFactoryFactory {
  private static final Logger logger = LoggerFactory.getLogger(ConnectionFactoryFactory.class);

  @EachBean(ServerConfiguration.class)
  public NamedConnectionFactory createConnectionFactory(ServerConfiguration config) throws JMSException {
    JmsFactoryFactory factory = JmsFactoryFactory.getInstance(WMQ_PROVIDER);
    JmsConnectionFactory connectionFactory = factory.createConnectionFactory();

    connectionFactory.setStringProperty(WMQ_HOST_NAME, config.getHost());
    connectionFactory.setIntProperty(WMQ_PORT, config.getPort());
    if (config.getChannel() != null) {
      connectionFactory.setStringProperty(WMQ_CHANNEL, config.getChannel());
    }
    connectionFactory.setIntProperty(WMQ_CONNECTION_MODE, WMQ_CM_CLIENT);
    if (config.getQueueManager() != null) {
      connectionFactory.setStringProperty(WMQ_QUEUE_MANAGER, config.getQueueManager());
    }
    if (config.getApplicationName() != null) {
      connectionFactory.setStringProperty(WMQ_APPLICATIONNAME, config.getApplicationName());
    }
    if (config.getUsername() != null) {
      connectionFactory.setBooleanProperty(USER_AUTHENTICATION_MQCSP, true);
      connectionFactory.setStringProperty(USERID, config.getUsername());
      connectionFactory.setStringProperty(PASSWORD, config.getPassword());
    }
    if (config.getCipherSuite() != null) {
      connectionFactory.setStringProperty(WMQ_SSL_CIPHER_SUITE, config.getCipherSuite());
    }
    return new NamedConnectionFactory(config.getName(), connectionFactory);
  }
}
