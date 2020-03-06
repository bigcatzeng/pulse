package com.trxs.pulse.controller;

import com.alibaba.fastjson.JSON;
import com.trxs.commons.io.FileTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.yaml.snakeyaml.Yaml;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

@RestController
public class SwaggerController
{
    protected static Logger log = LoggerFactory.getLogger(SwaggerController.class.getName());

    @Value("${server.port}")
    private String serverPort;

    @RequestMapping(value = "/swagger/doc/{name:.+}", method = RequestMethod.GET)
    public Object swagger(HttpServletRequest request, HttpServletResponse response, @PathVariable String name)
    {
        String source = String.join("","/", name );

        log.info("Client IP : {}", getClientIp(request));

        return getResponseEntity(yml2Json(source).getBytes());
    }

    private ResponseEntity<byte[]> getResponseEntity(byte []bytes )
    {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type","text/html;charset=UTF-8");
        HttpStatus statusCode = HttpStatus.OK;
        return new ResponseEntity<byte[]>(bytes, headers, statusCode);
    }

    private String yml2Json(String source)
    {
        Yaml yaml = new Yaml();
        Object obj = yaml.load(String.join("\n", FileTools.getInstance().readStaticResource(source)));

        ((Map)obj).put("host", getHost());

        return JSON.toJSONString(obj);
    }

    private String getHost()
    {
        InetAddress inetAddress = getLocalHost();
        String ip = ( inetAddress == null ? "127.0.0.1" : inetAddress.getHostAddress().toString() );
        if ( serverPort == null ) serverPort = "80";
        int port = Integer.valueOf(serverPort);
        if ( port != 80 ) return String.join(":", ip, Integer.valueOf(port).toString() );
        return ip;
    }

    public static InetAddress getLocalHost()
    {
        InetAddress inetAddress = null;
        try
        {
            inetAddress = InetAddress.getLocalHost();
        }
        catch (UnknownHostException e)
        {
            e.printStackTrace();
        }
        return inetAddress;
    }

    public static String getClientIp(HttpServletRequest request)
    {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip) || "null".equalsIgnoreCase(ip))
        {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip) || "null".equalsIgnoreCase(ip))
        {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip) || "null".equalsIgnoreCase(ip))
        {
            ip = request.getRemoteAddr();
        }

        return getFirstIp(ip);
    }

    //对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
    public static String getFirstIp(String ip)
    {
        int index = ip.indexOf(",");
        if( ip!=null && index > 0 )
        {
            if (ip.indexOf(",") > 0)
            {
                return ip.substring(0, index);
            }
        }
        return ip;
    }
} 
 
