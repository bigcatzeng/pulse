/*
 *  Copyright (c) xsocket.org, 2006 - 2009. All rights reserved.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * Please refer to the LGPL license at: http://www.gnu.org/copyleft/lesser.txt
 * The latest copy of this software may be found on http://www.xlightweb.org/
 */
package org.xlightweb;





import java.io.File;
import java.io.FileWriter;


import org.junit.Assert;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.FileSystemResource;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.IServer;




/**
*
* @author grro@xlightweb.org
*/
public final class SpringTest {
	
	
	private static final String SPRING_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\r" +
											 "<beans xmlns=\"http://www.springframework.org/schema/beans\"" +
											 " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
											 " xsi:schemaLocation=\"http://www.springframework.org/schema/beans" +
											 " http://www.springframework.org/schema/beans/spring-beans-2.0.xsd\">\r\n" +
									  		 " <bean id=\"server\" class=\"org.xlightweb.server.HttpServer\" init-method=\"start\" destroy-method=\"close\" scope=\"singleton\">\n\r" +
									  		 "  <constructor-arg type=\"int\" value=\"0\"/>\n\r" +
									  		 "  <constructor-arg type=\"org.xlightweb.IWebHandler\" ref=\"handler\"/>\n\r" +
									  		 "\n\r" +
										  	 " </bean>\n\r" +
										  	 " <bean id=\"handler\" class=\"org.xlightweb.HeaderInfoServerHandler\" scope=\"prototype\"/>\n\r" +
										  	 "</beans>";

	
	
	private static final String SPRING_XML2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\r" +
 											 "<beans xmlns=\"http://www.springframework.org/schema/beans\"" +
											 " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
											 " xsi:schemaLocation=\"http://www.springframework.org/schema/beans" +
											 " http://www.springframework.org/schema/beans/spring-beans-2.0.xsd\">\r\n" +
											 " <bean id=\"server\" class=\"org.xlightweb.server.HttpServer\" init-method=\"start\" destroy-method=\"close\" scope=\"singleton\">\n\r" +
											 "  <constructor-arg value=\"0\"/>\n\r" +
											 "  <constructor-arg ref=\"chain\"/>\n\r" +
											 "\n\r" +
										 	 " </bean>\n\r" +
										 	 " <bean id=\"chain\" class=\"org.xlightweb.RequestHandlerChain\" scope=\"prototype\">\n\r" +
										 	 "  <constructor-arg>\r\n" +
										 	 "   <list>\r\n" +
										   	 "     <ref bean=\"authFilter\"/>\r\n" +
										 	 "     <ref bean=\"handler\"/>\r\n" + 
										 	 "   </list>\r\n" +
 										 	 "  </constructor-arg>\r\n" +
											 "\n\r" +
										 	 " </bean>\n\r" +
										 	 " <bean id=\"authFilter\" class=\"org.xlightweb.AuthFilter\" scope=\"prototype\"/>\n\r" +
										 	 " <bean id=\"handler\" class=\"org.xlightweb.HeaderInfoServerHandler\" scope=\"prototype\"/>\n\r" +
										 	 "</beans>";
	
	private static final String SPRING_XML3 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\r" +
											 "<beans xmlns=\"http://www.springframework.org/schema/beans\"" +
											 " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
											 " xsi:schemaLocation=\"http://www.springframework.org/schema/beans" +
											 " http://www.springframework.org/schema/beans/spring-beans-2.0.xsd\">\r\n" +
											 " <bean id=\"server\" class=\"org.xlightweb.server.HttpServer\" init-method=\"start\" destroy-method=\"close\" scope=\"singleton\">\n\r" +
											 "  <constructor-arg value=\"0\"/>\n\r" +
											 "  <constructor-arg ref=\"context\"/>\n\r" +
											 "\n\r" +
											 " </bean>\n\r" +
											 " <bean id=\"context\" class=\"org.xlightweb.Context\" scope=\"prototype\">\n\r" +
									  		 "  <constructor-arg value=\"\"/>\n\r" +
									  		 "  <constructor-arg>\n\r" +
											 "   <map>\r\n" +
											 "     <entry key=\"/info/*\">\r\n" +
											 "       <ref bean=\"infoHandler\"/>\r\n" +
											 "     </entry>\r\n" +
											 "     <entry key=\"/itWorks/*\">\r\n" +
											 "       <ref bean=\"itWorksHandler\"/>\r\n" +
											 "     </entry>\r\n" +											 
											 "   </map>\r\n" +
										 	 "  </constructor-arg>\r\n" +
											 "\n\r" +
											 " </bean>\n\r" +
											 " <bean id=\"infoHandler\" class=\"org.xlightweb.HeaderInfoServerHandler\" scope=\"prototype\"/>\n\r" +
											 " <bean id=\"itWorksHandler\" class=\"org.xlightweb.ItWorksServerHandler\" scope=\"prototype\"/>\n\r" +
											 "</beans>";	
	
	private static final String SPRING_XML4 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\r" +
											 "<beans xmlns=\"http://www.springframework.org/schema/beans\"" +
											 " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
											 " xsi:schemaLocation=\"http://www.springframework.org/schema/beans" +
											 " http://www.springframework.org/schema/beans/spring-beans-2.0.xsd\">\r\n" +
											 " <bean id=\"httpClient\" class=\"org.xlightweb.client.HttpClient\" scope=\"prototype\">\n\r" +
											 " <property name=\"maxIdle\">\r\n" +
											 "   <value>30</value>\r\n" +
											 " </property>\r\n" +
											 " <property name=\"responseTimeoutMillis\">\r\n" +
											 "   <value>60000</value>\r\n" +
											 " </property>\r\n" +											 
										 	 " </bean>\n\r" +
										 	 "</beans>";
	

	@Test 
	public void testServer() throws Exception {	
		
		File file = QAUtil.createTempfile();
		FileWriter fw = new FileWriter(file);
		fw.write(SPRING_XML);
		fw.close();
		
		BeanFactory beanFactory = new XmlBeanFactory(new FileSystemResource(file));
		IServer server = (IServer) beanFactory.getBean("server");

		if (!server.isOpen()) {
			System.out.println("server should be open");
			Assert.fail("server should be open");
		}
		

		HttpClient httpClient = new HttpClient();
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test?parm1=1"));
		
		Assert.assertEquals(200, response.getStatus());

		file.delete();
		httpClient.close();
		server.close();
	}
	
	
/*
	@Test 
	public void testServerSSL() throws Exception {	
		
		File file = File.createTempFile("test", null);
		file.deleteOnExit();
		FileWriter fw = new FileWriter(file);
		fw.write(SPRING_XML1);
		fw.close();
		
		BeanFactory beanFactory = new XmlBeanFactory(new FileSystemResource(file));
		IServer server = (IServer) beanFactory.getBean("server");

		if (!server.isOpen()) {
			System.out.println("server should be open");
			Assert.fail("server should be open");
		}
		

		HttpClient httpClient = new HttpClient(SSLContext.getDefault());
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test?parm1=1"));
		
		Assert.assertEquals(200, response.getStatus());
		
		httpClient.close();
		server.close();
	}
	*/
	
	@Test 
	public void testChain() throws Exception {	
		
		File file = QAUtil.createTempfile();
		FileWriter fw = new FileWriter(file);
		fw.write(SPRING_XML2);
		fw.close();

		BeanFactory beanFactory = new XmlBeanFactory(new FileSystemResource(file));
		IServer server = (IServer) beanFactory.getBean("server");
		
		if (!server.isOpen()) {
			System.out.println("server should be open");
			Assert.fail("server should be open");
		}


		HttpClient httpClient = new HttpClient();
		
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test?parm1=1"));
		
		if (response.getStatus() != 401) {
			System.out.println("401 response expected");
			Assert.fail("401 response expected");
		}
		
		GetRequest request2 = new GetRequest("http://localhost:" + server.getLocalPort() + "/test?parm1=1");
		request2.setHeader("Authorization", "Basic dGVzdDp0ZXN0");
		response = httpClient.call(request2);
		if (response.getStatus() != 200) {
			System.out.println("200 response expected");
			Assert.fail("200 response expected");
		}

		file.delete();		
		httpClient.close();
		server.close();
	}
	
	
	@Test 
	public void testContext() throws Exception {	
				
		File file = QAUtil.createTempfile();
		FileWriter fw = new FileWriter(file);
		fw.write(SPRING_XML3);
		fw.close();


		
		BeanFactory beanFactory = new XmlBeanFactory(new FileSystemResource(file));
		IServer server = (IServer) beanFactory.getBean("server");

		if (!server.isOpen()) {
			System.out.println("server should be open");
			Assert.fail("server should be open");
		}

		
		HttpClient httpClient = new HttpClient();
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/info/test"));
		Assert.assertEquals(200, response.getStatus());
		Assert.assertTrue(response.getBody().readString().indexOf("method= GET") != -1);
		
		
		GetRequest request2 = new GetRequest("http://localhost:" + server.getLocalPort() + "/itWorks/test");
		response = httpClient.call(request2);
		Assert.assertEquals(200, response.getStatus());
		Assert.assertTrue(response.getBody().readString().indexOf("it works") != -1);
		
		file.delete();
		httpClient.close();
		server.close();
	}
	
	
	@Test 
	public void testHttpClient() throws Exception {

		HttpServer server = new HttpServer(new ItWorksServerHandler());
		server.start();
		
		File file = QAUtil.createTempfile();
		FileWriter fw = new FileWriter(file);
		fw.write(SPRING_XML4);
		fw.close();


		
		BeanFactory beanFactory = new XmlBeanFactory(new FileSystemResource(file));
		HttpClient httpClient = (HttpClient) beanFactory.getBean("httpClient");
		Assert.assertEquals(30, httpClient.getMaxIdle());
		Assert.assertEquals(60000, httpClient.getResponseTimeoutMillis());


		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test?parm1=1"));
		
		Assert.assertEquals(200, response.getStatus());
		Assert.assertTrue(response.getBody().readString().indexOf("it works") != -1);
		
		file.delete();
		httpClient.close();
		server.close();
	}
	
}
