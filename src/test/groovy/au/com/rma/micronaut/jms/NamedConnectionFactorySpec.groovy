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
package au.com.rma.micronaut.jms

import au.com.rma.micronaut.jms.jms.NamedConnectionFactory
import spock.lang.Specification

import javax.jms.Connection
import javax.jms.ConnectionFactory
import javax.jms.JMSContext

import static java.lang.Integer.MAX_VALUE

class NamedConnectionFactorySpec extends Specification {
  private ConnectionFactory connectionFactory
  private NamedConnectionFactory factory

  def setup() {
    connectionFactory = Mock(ConnectionFactory)
    factory = new NamedConnectionFactory("foo", connectionFactory)
  }

  def "verify getName works as expected"() {
    when:
    def name = factory.name

    then:
    name == "foo"
  }

  def "verify createConnection works as expected"() {
    given:
    def connection = Mock(Connection)

    when:
    def returnedConnection = factory.createConnection();

    then:
    1 * connectionFactory.createConnection() >> connection
    returnedConnection == connection
  }

  def "verify createConnection(username, password) works as expected"() {
    given:
    def connection = Mock(Connection)

    when:
    def returnedConnection = factory.createConnection("user", "pass")

    then:
    1 * connectionFactory.createConnection("user", "pass") >> connection
    returnedConnection == connection
  }

  def "verify createContext() works as expected"() {
    given:
    def context = Mock(JMSContext)

    when:
    def returnedContext = factory.createContext()

    then:
    1 * connectionFactory.createContext() >> context
    returnedContext == context
  }

  def "verify createContext(username, password) works as exepcted"() {
    given:
    def context = Mock(JMSContext)

    when:
    def returnedContext = factory.createContext("user", "pass")

    then:
    1 * connectionFactory.createContext("user", "pass") >> context
    returnedContext == context
  }

  def "verify createContext(username, password, sessionMode) works as expected"() {
    given:
    def context = Mock(JMSContext)

    when:
    def returnedContext = factory.createContext("user", "pass", MAX_VALUE)

    then:
    1 * connectionFactory.createContext("user", "pass", MAX_VALUE) >> context
    returnedContext == context
  }

  def "verify createContext(sessionMode) works as expected"() {
    given:
    def context = Mock(JMSContext)

    when:
    def returnedContext = factory.createContext(MAX_VALUE)

    then:
    1 * connectionFactory.createContext(MAX_VALUE) >> context
    returnedContext == context
  }
}
