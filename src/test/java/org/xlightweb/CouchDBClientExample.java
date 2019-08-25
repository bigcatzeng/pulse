/*
 *  Copyright (c) xlightweb.org, 2006 - 2009. All rights reserved.
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


import org.xlightweb.client.HttpClient;




/**
*
* @author grro@xlightweb.org
*/
public final class CouchDBClientExample  {


    public static void main(String[] args) throws IOException {
        HttpClient httpClient = new HttpClient();
        
        System.out.println("get greeting");
        IHttpResponse response = httpClient.call(new GetRequest("http://localhost:5984/"));
        System.out.println(response);
        
        
        System.out.println("get all dbs");
        response = httpClient.call(new GetRequest("http://localhost:5984/_all_dbs"));
        System.out.println(response);

        
        System.out.println("create test db");
        response = httpClient.call(new PutRequest("http://localhost:5984/test"));
        System.out.println(response);


        System.out.println("repeated create test db (should fail");
        response = httpClient.call(new PutRequest("http://localhost:5984/test"));
        System.out.println(response);

        
        System.out.println("get all dbs");
        response = httpClient.call(new GetRequest("http://localhost:5984/_all_dbs"));
        System.out.println(response);

        
        System.out.println("delete test db");
        response = httpClient.call(new DeleteRequest("http://localhost:5984/test"));
        System.out.println(response);
        
        httpClient.close();
    }
}