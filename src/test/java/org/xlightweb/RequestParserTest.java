/*
 *  Copyright (c) xlightweb.org, 2008 - 2010. All rights reserved.
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



import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;


import org.junit.Assert;
import org.junit.Test;
import org.xlightweb.HttpResponse;
import org.xlightweb.IHttpExchange;
import org.xlightweb.IHttpRequestHandler;
import org.xlightweb.client.HttpClient;
import org.xlightweb.server.HttpServer;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IBlockingConnection;
import org.xsocket.connection.IServer;


/**
*
* @author grro@xlightweb.org
*/
public final class RequestParserTest {


    
            
    @Test
    public void testQueryString() throws Exception {
      
        RequestHandler rh = new RequestHandler();
        IServer server = new HttpServer(rh);
        server.start();
        

        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
        con.write("GET /picture/?21,32 HTTP/1.1\r\n" +
                  "Host: localhost\r\n" +
                  "User-Agent: me\r\n" +
                  "\r\n");
        
        String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        int contentLength = QAUtil.readContentLength(header);
        String body = con.readStringByLength(contentLength);
        
        Assert.assertTrue(header.indexOf("200") != -1);
        Assert.assertEquals("OK", body);

        
        IHttpRequest request = rh.getRequest();
        Assert.assertEquals("21,32", request.getQueryString());
        
        Assert.assertEquals("localhost", request.getHost());
        Assert.assertEquals("me", request.getUserAgent());
        Assert.assertEquals(0, request.getParameterNameSet().size());
        
        request.setParameter("test1", "value1");

        Assert.assertEquals("21,32", request.getQueryString());
        Assert.assertEquals("value1", request.getParameter("test1"));


        con.close();
        server.close();
    }      
    
    
    
    @Test
    public void testQueryString2() throws Exception {
        
        RequestHandler rh = new RequestHandler();
        IServer server = new HttpServer(rh);
        server.start();

        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
        con.write("GET /picture/?name=value HTTP/1.1\r\n" +
                  "Host: localhost\r\n" +
                  "User-Agent: me\r\n" +
                  "\r\n");
        
        String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        int contentLength = QAUtil.readContentLength(header);
        String body = con.readStringByLength(contentLength);
        
        Assert.assertTrue(header.indexOf("200") != -1);
        Assert.assertEquals("OK", body);

        
        IHttpRequest request = rh.getRequest();
        Assert.assertEquals("name=value", request.getQueryString());
        Assert.assertEquals("value", request.getParameter("name"));

        con.close();
        server.close();
    }      
        
    
 
    
    @Test
    public void testQueryString3() throws Exception {
        
        RequestHandler rh = new RequestHandler();
        IServer server = new HttpServer(rh);
        server.start();

        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
        con.write("GET /picture/?name= HTTP/1.1\r\n" +
                  "Host: localhost\r\n" +
                  "User-Agent: me\r\n" +
                  "\r\n");
        
        String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        int contentLength = QAUtil.readContentLength(header);
        String body = con.readStringByLength(contentLength);
        
        Assert.assertTrue(header.indexOf("200") != -1);
        Assert.assertEquals("OK", body);

        
        IHttpRequest request = rh.getRequest();
        Assert.assertEquals("name=", request.getQueryString());
        Assert.assertEquals("", request.getParameter("name"));

        con.close();
        server.close();
    }      
    
    
    @Test
    public void testQueryString4() throws Exception {
        
        RequestHandler rh = new RequestHandler();
        IServer server = new HttpServer(rh);
        server.start();

        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
        con.write("GET /picture/?name=value&name2= HTTP/1.1\r\n" +
                  "Host: localhost\r\n" +
                  "User-Agent: me\r\n" +
                  "\r\n");
        
        String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        int contentLength = QAUtil.readContentLength(header);
        String body = con.readStringByLength(contentLength);
        
        Assert.assertTrue(header.indexOf("200") != -1);
        Assert.assertEquals("OK", body);

        
        IHttpRequest request = rh.getRequest();
        Assert.assertEquals("name=value&name2=", request.getQueryString());
        Assert.assertEquals("value", request.getParameter("name"));
        Assert.assertEquals("", request.getParameter("name2"));

        con.close();
        server.close();
    }      
    
    
    @Test
    public void testQueryString5() throws Exception {
        
        RequestHandler rh = new RequestHandler();
        IServer server = new HttpServer(rh);
        server.start();

        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
        con.write("GET /picture/?name=value& HTTP/1.1\r\n" +
                  "Host: localhost\r\n" +
                  "User-Agent: me\r\n" +
                  "\r\n");
        
        String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        int contentLength = QAUtil.readContentLength(header);
        String body = con.readStringByLength(contentLength);
        
        Assert.assertTrue(header.indexOf("200") != -1);
        Assert.assertEquals("OK", body);

        
        IHttpRequest request = rh.getRequest();
        Assert.assertEquals("name=value", request.getQueryString());
        Assert.assertEquals("value", request.getParameter("name"));

        con.close();
        server.close();
    }      
    
    
    @Test
    public void testQueryString6() throws Exception {
        
        RequestHandler rh = new RequestHandler();
        IServer server = new HttpServer(rh);
        server.start();

        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
        con.write("GET /picture/?blablabla HTTP/1.1\r\n" +
                  "Host: localhost\r\n" +
                  "User-Agent: me\r\n" +
                  "\r\n");
        
        String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        int contentLength = QAUtil.readContentLength(header);
        String body = con.readStringByLength(contentLength);
        
        Assert.assertTrue(header.indexOf("200") != -1);
        Assert.assertEquals("OK", body);

        
        IHttpRequest request = rh.getRequest();
        Assert.assertEquals("blablabla", request.getQueryString());
        Assert.assertEquals(0, request.getParameterNameSet().size());

        con.close();
        server.close();
    }      
    
    @Test
    public void testQueryString7() throws Exception {
        
        RequestHandler rh = new RequestHandler();
        IServer server = new HttpServer(rh);
        server.start();

        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
        con.write("GET /picture/?blablabla&werwer HTTP/1.1\r\n" +
                  "Host: localhost\r\n" +
                  "User-Agent: me\r\n" +
                  "\r\n");
        
        String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        int contentLength = QAUtil.readContentLength(header);
        String body = con.readStringByLength(contentLength);
        
        Assert.assertTrue(header.indexOf("200") != -1);
        Assert.assertEquals("OK", body);

        
        IHttpRequest request = rh.getRequest();
        Assert.assertEquals("blablabla&werwer", request.getQueryString());
        Assert.assertEquals(0, request.getParameterNameSet().size());

        con.close();
        server.close();
    }      
    
    
	@Test
	public void testGodHttp11Request() throws Exception {
		
		RequestHandler rh = new RequestHandler();
		IServer server = new HttpServer(rh);
		server.start();
		

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		con.write("GET / HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");
		
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
		String body = con.readStringByLength(contentLength);
		
		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertEquals("OK", body);
		
		Set<String> headerNames = rh.getRequest().getHeaderNameSet();
		Assert.assertTrue(headerNames.remove("User-Agent"));
		Assert.assertTrue(headerNames.remove("Host"));
		Assert.assertTrue(headerNames.isEmpty());

		con.close();
		server.close();
	}
	
	
    @Test
    public void testEncoding() throws Exception {
        
        RequestHandler rh = new RequestHandler();
        IServer server = new HttpServer(rh);
        server.start();
        

        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
        con.write("POST / HTTP/1.1\r\n" +
                  "Host: localhost\r\n" +
                  "User-Agent: me\r\n" +
                  "Content-Length: 5\r\n" +
                  "Content-Type: text/plain;charset=iso-8859-1\r\n" +
                  "\r\n" +
                  "12345");
        
        String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        int contentLength = QAUtil.readContentLength(header);
        String body = con.readStringByLength(contentLength);
        
        Assert.assertTrue(header.indexOf("200") != -1);
        Assert.assertEquals("OK", body);
        
        Set<String> headerNames = rh.getRequest().getHeaderNameSet();
        Assert.assertTrue(headerNames.remove("User-Agent"));
        Assert.assertTrue(headerNames.remove("Host"));
        Assert.assertEquals("iso-8859-1", rh.getRequest().getCharacterEncoding());
        
        Assert.assertEquals("12345", rh.getRequest().getBody().toString());


        con.close();
        server.close();
    }
	

    @Test
    public void testQuotedEncoding() throws Exception {
        
        RequestHandler rh = new RequestHandler();
        IServer server = new HttpServer(rh);
        server.start();
        

        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
        con.write("POST / HTTP/1.1\r\n" +
                  "Host: localhost\r\n" +
                  "User-Agent: me\r\n" +
                  "Content-Length: 5\r\n" +
                  "Content-Type: text/plain;charset=\"iso-8859-1\"\r\n" +
                  "\r\n" +
                  "12345");
        
        String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
        int contentLength = QAUtil.readContentLength(header);
        String body = con.readStringByLength(contentLength);
        
        Assert.assertTrue(header.indexOf("200") != -1);
        Assert.assertEquals("OK", body);
        
        Set<String> headerNames = rh.getRequest().getHeaderNameSet();
        Assert.assertTrue(headerNames.remove("User-Agent"));
        Assert.assertTrue(headerNames.remove("Host"));
        Assert.assertEquals("iso-8859-1", rh.getRequest().getCharacterEncoding());
        
        Assert.assertEquals("12345", rh.getRequest().getBody().toString());

        con.close();
        server.close();
    }
    
    
	@Test
	public void testGodHttp11RequestWithQuery() throws Exception {
	    
		RequestHandler rh = new RequestHandler();
		IServer server = new HttpServer(rh);
		server.start();
		
		HttpClient httpClient = new HttpClient();
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test/?param1=value1&param2=value2"));

		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("value1", rh.getRequest().getParameter("param1"));
		Assert.assertEquals("value2", rh.getRequest().getParameter("param2"));
		
		Set<String> headerNames = rh.getRequest().getHeaderNameSet();
		Assert.assertTrue(headerNames.remove("User-Agent"));
		Assert.assertTrue(headerNames.remove("Host"));
		Assert.assertTrue(headerNames.isEmpty());

		httpClient.close();
		server.close();
	}

	
	@Test
	public void testGodHttp11RequestWithQuery0() throws Exception {
		
		RequestHandler rh = new RequestHandler();
		IServer server = new HttpServer(rh);
		server.start();
		
		HttpClient httpClient = new HttpClient();
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test?test=12&test=two"));

		Assert.assertEquals(200, response.getStatus());
		List<String> values = Arrays.asList(rh.getRequest().getParameterValues("test"));
		Assert.assertTrue(values.contains("12"));
		Assert.assertTrue(values.contains("two"));
		
		Set<String> headerNames = rh.getRequest().getHeaderNameSet();
		Assert.assertTrue(headerNames.remove("User-Agent"));
		Assert.assertTrue(headerNames.remove("Host"));
		Assert.assertTrue(headerNames.isEmpty());

		httpClient.close();
		server.close();
	}
	
	
	@Test
	public void testGodHttp11RequestWithQuery2() throws Exception {
		
		RequestHandler rh = new RequestHandler();
		IServer server = new HttpServer(rh);
		server.start();
		
		HttpClient httpClient = new HttpClient();
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test/?param1=value1&param2="));

		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("value1", rh.getRequest().getParameter("param1"));
		Assert.assertEquals("", rh.getRequest().getParameter("param2"));

		httpClient.close();
		server.close();
	}

	
	@Test
	public void testGodHttp11RequestWithQuery3() throws Exception {
		
		RequestHandler rh = new RequestHandler();
		IServer server = new HttpServer(rh);
		server.start();
		
		HttpClient httpClient = new HttpClient();
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test/?param1=value1"));

		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("value1", rh.getRequest().getParameter("param1"));

		httpClient.close();
		server.close();
	}


	
	@Test
	public void testGodHttp11RequestWithQuery4() throws Exception {
		
		RequestHandler rh = new RequestHandler();
		IServer server = new HttpServer(rh);
		server.start();
		
		HttpClient httpClient = new HttpClient();
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test/?param1="));

		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("", rh.getRequest().getParameter("param1"));

		httpClient.close();
		server.close();
	}

	
	@Test
	public void testGodHttp11RequestWithQuery5() throws Exception {
		
		RequestHandler rh = new RequestHandler();
		IServer server = new HttpServer(rh);
		server.start();
		
		HttpClient httpClient = new HttpClient();
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test?filename=C%3A%5CDOKUME%7E1%5Cgrro%5CLOKALE%7E1%5CTemp%5CxSocketTest23878.html"));

		Assert.assertEquals(200, response.getStatus());
		
		String filename = rh.getRequest().getParameter("filename");
		Assert.assertEquals("C:\\DOKUME~1\\grro\\LOKALE~1\\Temp\\xSocketTest23878.html", filename);
		
		Set<String> headerNames = rh.getRequest().getHeaderNameSet();
		Assert.assertTrue(headerNames.remove("User-Agent"));
		Assert.assertTrue(headerNames.remove("Host"));
		Assert.assertTrue(headerNames.isEmpty());

		httpClient.close();
		server.close();
	}
	
	
	@Test
	public void testGodHttp11RequestWithQuery6() throws Exception {
		
		RequestHandler rh = new RequestHandler();
		IServer server = new HttpServer(rh);
		server.start();
		
		HttpClient httpClient = new HttpClient();
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test/?param1=&param2=2"));

		Assert.assertEquals(200, response.getStatus());
		Assert.assertEquals("", rh.getRequest().getParameter("param1"));
		Assert.assertEquals("2", rh.getRequest().getParameter("param2"));

		httpClient.close();
		server.close();
	}

	@Test
	public void testGodHttp11RequestWithQuery7() throws Exception {
		
		RequestHandler rh = new RequestHandler();
		IServer server = new HttpServer(rh);
		server.start();
		
		HttpClient httpClient = new HttpClient();
		IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test/?param1=&param2=2&param1=3"));

		Assert.assertEquals(200, response.getStatus());
		List<String> params = Arrays.asList(rh.getRequest().getParameterValues("param1"));
		Assert.assertTrue(params.contains(""));
		Assert.assertTrue(params.contains("3"));
		Assert.assertEquals("2", rh.getRequest().getParameter("param2"));

		httpClient.close();
		server.close();
	}
	
	

    @Test
    public void testGodHttp11RequestWithQuery8() throws Exception {
        
        RequestHandler rh = new RequestHandler();
        IServer server = new HttpServer(rh);
        server.start();
        
        HttpClient httpClient = new HttpClient();
        
        MultivalueMap entity = new MultivalueMap("UTF-8", new NameValuePair("param1", ""), new NameValuePair("param2", " 2  "), new NameValuePair("param1", "3 "));
        IHttpResponse response = httpClient.call(new GetRequest("http://localhost:" + server.getLocalPort() + "/test/?" + entity.toString()));

        Assert.assertEquals(200, response.getStatus());
        List<String> params = Arrays.asList(rh.getRequest().getParameterValues("param1"));
        Assert.assertTrue(params.contains(""));
        Assert.assertTrue(params.contains("3 "));
        Assert.assertEquals(" 2  ", rh.getRequest().getParameter("param2"));

        httpClient.close();
        server.close();
    }
    
	
	@Test
	public void testGodHttp10Request() throws Exception {
		
		IServer server = new HttpServer(new RequestHandler());
		server.start();
		

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		con.write("GET / HTTP/1.0\r\n" +
				  "\r\n");
		
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
		String body = con.readStringByLength(contentLength);
		
		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertEquals("OK", body);

		con.close();
		server.close();
	}
	
	
	@Test
	public void testGodHttp10Request_2() throws Exception {
		
		IServer server = new HttpServer(new RequestHandler());
		server.start();
		

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		con.write("GET / HTTP/1.0\n" +
				  "\n");
		
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
		String body = con.readStringByLength(contentLength);
		
		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertEquals("OK", body);

		con.close();
		server.close();
	}
	

	@Test
	public void testBadRequest() throws Exception {

		
		IServer server = new HttpServer(new RequestHandler());
		server.start();
		

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		con.write("GET /s/ ere6r" + "\r\n\r\n");

		
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
		String body = con.readStringByLength(contentLength);
		
		Assert.assertTrue(header.indexOf("400") != -1);
		Assert.assertTrue(body.indexOf("bad message") != -1);

		con.close();
		server.close();
	}

	
	@Test
	public void testHttp09Request() throws Exception {
		
		IServer server = new HttpServer(new RequestHandler());
		server.start();
		

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		con.write("GET /\r\n");
		
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
		String body = con.readStringByLength(contentLength);
		
		Assert.assertTrue(header.indexOf("400") != -1);
		Assert.assertTrue(body.indexOf("simple request messages are not supported") != -1);

		con.close();
		server.close();
	}
	
	


	@Test
	public void testHttp09Request_2() throws Exception {
		
		IServer server = new HttpServer(new RequestHandler());
		server.start();
		

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		con.write("GET /\n");
		
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
		String body = con.readStringByLength(contentLength);
		
		Assert.assertTrue(header.indexOf("400") != -1);
		Assert.assertTrue(body.indexOf("simple request messages are not supported") != -1);

		con.close();
		server.close();
	}
	
	
	@Test
	public void testHttp11RequestWithServeralSpaces() throws Exception {
		
		IServer server = new HttpServer(new RequestHandler());
		server.start();
		

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		con.write("GET    /    HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");
		
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
		String body = con.readStringByLength(contentLength);
		
		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertEquals("OK", body);

		con.close();
		server.close();
	}
	
	
	@Test
	public void testHttp11RequestWithServeralSpacesAndTabs() throws Exception {
		
		IServer server = new HttpServer(new RequestHandler());
		server.start();
		

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		con.write("GET    /  \t  HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");
		
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
		String body = con.readStringByLength(contentLength);
		
		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertEquals("OK", body);

		con.close();
		server.close();
	}
	
	
	@Test
	public void testHttp11RequestWithHeaderValueSpaces() throws Exception {
		
		RequestHandler srvHdl = new RequestHandler();
		IServer server = new HttpServer(srvHdl);
		server.start();
		

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		con.write("GET / HTTP/1.1\r\n" +
				  "Content-Length: 5    \r\n" +
				  "X-MyHeader: this is a text   \r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n" +
				  "12345");
		
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
		String body = con.readStringByLength(contentLength);
		
		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertEquals("OK", body);

		
		Assert.assertEquals("this is a text", srvHdl.getRequest().getHeader("X-MyHeader"));
		
		con.close();
		server.close();
	}
	
	@Test
	public void testHttp11RequestWithLeadingCRLF() throws Exception {
		
		IServer server = new HttpServer(new RequestHandler());
		server.start();
		

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		con.write("\r\n" +
				  "GET / HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");
		
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
		String body = con.readStringByLength(contentLength);
		
		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertEquals("OK", body);

		con.close();
		server.close();
	}
	
	
	@Test
	public void testHttp11RequestWithServeralLeadingCRLF() throws Exception {
		
		IServer server = new HttpServer(new RequestHandler());
		server.start();
		

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		con.write("\r\n" +
				  "\r\n" +
				  "GET / HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");
		
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
		String body = con.readStringByLength(contentLength);
		
		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertEquals("OK", body);

		con.close();
		server.close();
	}
	
	
	@Test
	public void testHttp11RequestWithServeralLeadingCRLFAndSpaces() throws Exception {
		
		IServer server = new HttpServer(new RequestHandler());
		server.start();
		

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		con.write("\r\n" +
				  "  \r\n" +
				  "GET / HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");
		
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
		String body = con.readStringByLength(contentLength);
		
		Assert.assertTrue(header.indexOf("400") != -1);
		Assert.assertTrue(body.indexOf("bad request") != -1);

		con.close();
		server.close();
	}
	
	
	@Test
	public void testLargeCookie() throws Exception {
		
		IServer server = new HttpServer(new RequestHandler());
		server.start();
		

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		con.write("GET / HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "Cookie: pA_c[p]=rZNBCsIwEEXvkhNkZpIa05VgFx5ANyJStWpRbKGgC9u7m1qo;\r\n" +
				  " pA_c[p340358058568d1f]=S7QysqoutjIytF;\r\n" +
				  " pA_c[p3403580662066ea]=S7QysqoutjIytFIqyChwTC;\r\n" +
				  "\r\n");
		
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
		String body = con.readStringByLength(contentLength);
		
		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertEquals("OK", body);

		con.close();
		server.close();
	}



	@Test
	public void testFragmentedPipeliningRequest() throws Exception {
		
		IServer server = new HttpServer(new RequestHandler());
		server.start();
		

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		con.write("GET / HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");
		
		con.write("GET / HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");


		
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
		String body = con.readStringByLength(contentLength);
		
		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertEquals("OK", body);


		header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		contentLength = QAUtil.readContentLength(header);
		body = con.readStringByLength(contentLength);
		
		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertEquals("OK", body);

		
		con.close();
		server.close();
	}
	
	
	

	@Test
	public void testFragmentedPipeliningRequest2() throws Exception {
		
		IServer server = new HttpServer(new RequestHandler());
		server.start();
		

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		con.write("GET / HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");

		QAUtil.sleep(200);
		
		con.write("GET / HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");


		
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
		String body = con.readStringByLength(contentLength);
		
		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertEquals("OK", body);


		header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		contentLength = QAUtil.readContentLength(header);
		body = con.readStringByLength(contentLength);
		
		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertEquals("OK", body);

		
		con.close();
		server.close();
	}
	



	@Test
	public void testFragmentedPipeliningRequest3() throws Exception {
		
		IServer server = new HttpServer(new RequestHandler());
		server.start();
		

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		con.write("GET / HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-");

		QAUtil.sleep(200);

		con.write("Agent: me\r\n" +
				  "\r\n");

		QAUtil.sleep(200);

		
		con.write("GET / HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");


		
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
		String body = con.readStringByLength(contentLength);
		
		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertEquals("OK", body);


		header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		contentLength = QAUtil.readContentLength(header);
		body = con.readStringByLength(contentLength);
		
		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertEquals("OK", body);

		
		con.close();
		server.close();
	}

	

	@Test
	public void testFragmentedPipeliningRequest4() throws Exception {
		
		IServer server = new HttpServer(new RequestHandler());
		server.start();
		

		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());

		con.write("GET / HTTP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-");

		QAUtil.sleep(200);

		con.write("Agent: me\r\n" +
				  "\r\nGET / HT");

		
		QAUtil.sleep(200);

		con.write("TP/1.1\r\n" +
				  "Host: localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");


		
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
		String body = con.readStringByLength(contentLength);
		
		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertEquals("OK", body);


		header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		contentLength = QAUtil.readContentLength(header);
		body = con.readStringByLength(contentLength);
		
		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertEquals("OK", body);

		
		con.close();
		server.close();
	}

	
	
	
	@Test
	public void testMissingBlank() throws Exception {
		
		RequestHandler rh = new RequestHandler();
		IServer server = new HttpServer(rh);
		server.start();
		
	
		IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
		con.write("GET / HTTP/1.1\r\n" +
				  "Host:localhost\r\n" +
				  "User-Agent: me\r\n" +
				  "\r\n");
		
		String header = con.readStringByDelimiter("\r\n\r\n") + "\r\n";
		int contentLength = QAUtil.readContentLength(header);
		String body = con.readStringByLength(contentLength);
		
		Assert.assertTrue(header.indexOf("200") != -1);
		Assert.assertEquals("OK", body);
		
		Assert.assertEquals("localhost", rh.getRequest().getHeader("Host"));
		
		Set<String> headerNames = rh.getRequest().getHeaderNameSet();
		Assert.assertTrue(headerNames.remove("User-Agent"));
		Assert.assertTrue(headerNames.remove("Host"));
		Assert.assertTrue(headerNames.isEmpty());
	
		con.close();
		server.close();
	}




	private static final class RequestHandler implements IHttpRequestHandler {
		
		private IHttpRequest request = null;
		
		public void onRequest(IHttpExchange exchange) throws IOException {
			this.request = exchange.getRequest();
			
			exchange.send(new HttpResponse(200, "text/plain", "OK"));
		}
		
		
		public IHttpRequest getRequest() {
			return request;
		}
	}
}
