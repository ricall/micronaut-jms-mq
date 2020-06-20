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

import edu.umd.cs.findbugs.annotations.NonNull;
import io.micronaut.core.naming.Named;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;

public class NamedConnectionFactory implements ConnectionFactory, Named {

  private String name;
  private ConnectionFactory connectionFactory;

  public NamedConnectionFactory(String name, ConnectionFactory connectionFactory) {
    this.name = name;
    this.connectionFactory = connectionFactory;
  }

  @NonNull
  public String getName() {
    return name;
  }

  @Override
  public Connection createConnection() throws JMSException {
    return connectionFactory.createConnection();
  }

  @Override
  public Connection createConnection(String userName, String password) throws JMSException {
    return connectionFactory.createConnection(userName, password);
  }

  @Override
  public JMSContext createContext() {
    return connectionFactory.createContext();
  }

  @Override
  public JMSContext createContext(String userName, String password) {
    return connectionFactory.createContext(userName, password);
  }

  @Override
  public JMSContext createContext(String userName, String password, int sessionMode) {
    return connectionFactory.createContext(userName, password, sessionMode);
  }

  @Override
  public JMSContext createContext(int sessionMode) {
    return connectionFactory.createContext(sessionMode);
  }
}
