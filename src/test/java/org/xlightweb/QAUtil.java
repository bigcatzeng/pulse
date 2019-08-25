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


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Random;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assert;
import org.xlightweb.BodyDataSource;
import org.xsocket.DataConverter;


/**
*
* @author grro@xlightweb.org
*/
public final class QAUtil {
	
	private static String testMail = 
		  "Received: from localhost (localhost [127.0.0.1])\r\n" 
		+ "by Semta.de with ESMTP id 881588961.1153334034540.1900236652.1\r\n" 
		+ "for feki@semta.de; Mi, 19 Jul 2006 20:34:00 +0200\r\n" 
		+ "Message-ID: <24807938.01153334039898.JavaMail.grro@127.0.0.1>\r\n" 
		+ "Date: Wed, 19 Jul 2006 20:33:59 +0200 (CEST)\r\n" 
		+ "From: feki2 <fekete99@web.de>\r\n" 
		+ "To: festi flow <feki@semta.de>\r\n" 
		+ "Subject: Test mail\r\n" 
		+ "MIME-Version: 1.0\r\n" 
		+ "Content-Type: multipart/mixed;\r\n" 
		+ "boundary=\"----=_Part_1_14867177.1153334039707\"\r\n" 
		+ "\r\n" 
		+ "This is a multi-part message in MIME format.\r\n"
		+ "------=_Part_1_14867177.1153334039707\r\n" 
		+ "Content-Type: multipart/mixed;\r\n" 
		+ "boundary=\"----=_Part_0_14158819.1153334039687\"\r\n" 
		+ "\r\n" 
		+ "------=_Part_0_14158819.1153334039687\r\n" 
		+ "Content-Type: text/plain; charset=us-ascii\r\n" 
		+ "Content-Transfer-Encoding: 7bit\r\n" 
		+ "\r\n" 
		+ "Halli Hallo\r\n" 
		+ "------=_Part_0_14158819.1153334039687\r\n" 
		+ "------=_Part_1_14867177.1153334039707--";

	

	private static final int OFFSET = 48;
	
	
	private QAUtil() { }
	


	public static int readContentLength(String header) {
		int start = header.indexOf("Content-Length:");
		int end = header.indexOf("\r\n", start);
		
		if (end > 0) {
			return Integer.parseInt(header.substring(start + "Content-Length:".length(), end).trim());
		} else {
			return Integer.parseInt(header.substring(start + "Content-Length:".length(), header.length()).trim());
		}
	}
	
	
	public static String getFilepath(String name) {
		String filename = null;
		
		int pos = name.lastIndexOf(File.separator);
		URL url = SSLTestContextFactory.class.getResource(name.substring(pos, name.length()));
	
		if ((url != null) && (new File(url.getFile()).exists())) {
			filename = url.getFile();
		} else {
			filename = new File("src" + File.separator + "test" + File.separator 
					           + "resources" + File.separator + name).getAbsolutePath();
		}
		
		return filename;
	}
	
	private static File createTempfile(InputStream is, String suffix) throws IOException {
        File file = File.createTempFile("xSocketTest", suffix);
        file.deleteOnExit();

        FileOutputStream fos = new FileOutputStream(file);
        
        
        byte[] transfer = new byte[4096];
        int read = 0;
        do {
            read = is.read(transfer);
            if (read > 0) {
                fos.write(transfer, 0, read);
            }
        } while (read > 0); 
        
        fos.close();
        
        return file;      
    }
	
	
    public static File copyToTempfile(String filename) throws IOException {
        String suffix = "bin";
        int idx = filename.lastIndexOf(".");
        if (idx != -1) {
            suffix = filename.substring(idx, filename.length());
        }
         File file = File.createTempFile(filename, suffix);
         file.deleteOnExit();

         FileOutputStream fos = new FileOutputStream(file);
         
         InputStream is = newInputStream(filename);
         
         byte[] transfer = new byte[4096];
         int read = 0;
         do {
             read = is.read(transfer);
             if (read > 0) {
                 fos.write(transfer, 0, read);
             }
         } while (read > 0); 
         
         fos.close();
         is.close();
         
         return file;      
     }
	    
	
	
	

    public static InputStream newInputStream(String filename) {
        URL url = QAUtil.class.getResource(filename);
        
        try {
            if (url != null) {
                InputStream is = url.openConnection().getInputStream();
                return is;
            
            } else {
                filename = filename.replace("\"", File.separator);
                filename = filename.replace("/", File.separator);
                
                filename = new File("src" + File.separator + "test" + File.separator + 
                                    "resources" + File.separator + "org" + File.separator +
                                    "xlightweb" + File.separator + filename).getAbsolutePath();
                FileInputStream fis = new FileInputStream(filename);
                return fis;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

	
	public static File createTempfile() throws IOException {
        return createTempfile(null);
    }

	public static File createTempfile(String suffix) throws IOException {
        return File.createTempFile("xSocketTest", suffix);
    }
	
	
	public static File createTestfile_40M() {
	    try {
	        InputStream is = newInputStream("Testfile_40m.html");
	        File file = createTempfile(is, ".html");
	        is.close();
	        
	        return file;
	    } catch (IOException e) {
	        throw new RuntimeException(e);
	    }       	    
    }
    
    
	
	

	
	
	
	public static File createTestfile_4000k() {
        try {
            InputStream is = newInputStream("Testfile_4000k.html");
            File file = createTempfile(is, ".html");
            is.close();
                
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }        
	}
	
	
	
	public static File createTestfile_400k() {
	    try {
	        InputStream is = newInputStream("Testfile_400k.html");
	        File file = createTempfile(is, ".html");
	        is.close();
	                
	        return file;
	    } catch (IOException e) {
	        throw new RuntimeException(e);
	    }        
	}
	
	
	public static File createTestfile_40k() {
	    try {
            InputStream is = newInputStream("Testfile_40k.html");
            File file = createTempfile(is, ".html");
            is.close();
                    
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }     
	}
	
	
	public static File createTestfile_40k(String ext) {
        try {
            InputStream is = newInputStream("Testfile_40k.html");
            File file = createTempfile(is, "." + ext);
            is.close();
                    
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }     
    }	

	public static File createTestfile() {
	    try {
            InputStream is = newInputStream("Testfile.xml");
            File file = createTempfile(is, ".xml");
            is.close();
                    
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }     
	}
	
	
	
	public static File createTestfile_80byte() {
        try {
            InputStream is = newInputStream("Testfile_80byte.html");
            File file = createTempfile(is, ".html");
            is.close();
                    
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }     
	}


    public static File createTestfile_130byte() {
        try {
            InputStream is = newInputStream("Testfile_130byte.html");
            File file = createTempfile(is, ".html");
            is.close();
                    
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }     
    }
    
    
    public static File createTestfile_650byte() {
        try {
            InputStream is = newInputStream("Testfile_650byte.html");
            File file = createTempfile(is, ".html");
            is.close();
                    
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }     
    }    
	
    public static File createTestfile_75byte() {
        try {
            InputStream is = newInputStream("Testfile_75byte.html");
            File file = createTempfile(is, ".html");
            is.close();
                    
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }     
    }
	

    
    public static File createTestfile_50byte() {
        try {
            InputStream is = newInputStream("Testfile_50byte.html");
            File file = createTempfile(is, ".html");
            is.close();
                    
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }     
    }
	
	

	public static File createBinaryTestfile_1k() {
	    try {
	        InputStream is = newInputStream("Testfile_1k.bin");
	        File file = createTempfile(is, ".bin");
	        is.close();
	                    
	        return file;
	    } catch (IOException e) {
	        throw new RuntimeException(e);
	    }
	}



	public static File createTestfile_4k() {
        try {
            InputStream is = newInputStream("Testfile_4k.html");
            File file = createTempfile(is, ".html");
            is.close();
                        
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }	    
	}


    public static File createTestfile_utf8WithBOM() {
        try {
            InputStream is = newInputStream("utf8WithBOM.txt");
            File file = createTempfile(is, ".txt");
            is.close();
                        
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }       
    }

	
	
	
	public static ByteBuffer getAsByteBuffer() {
		try {
			Charset charset = Charset.forName("ISO-8859-1");
		    CharsetEncoder encoder = charset.newEncoder();
		    ByteBuffer buf = encoder.encode(CharBuffer.wrap(testMail.toCharArray()));
		    return buf;
		} catch (Exception e) {
			throw new RuntimeException(e.toString());
		}
	}


	public static ByteBuffer generateByteBuffer(int length) {
		ByteBuffer buffer = ByteBuffer.wrap(generateByteArray(length));
		return buffer;
	}
	
	public static ByteBuffer generateDirectByteBuffer(int length) {
		byte[] bytes = generateByteArray(length);
		ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
		buffer.put(bytes);
		buffer.flip();
		return buffer;
	}
	
	public static ByteBuffer[] generateDirectByteBufferArray(int elements, int length) {
		ByteBuffer[] byteBufferArray = new ByteBuffer[elements];
		for (int i = 0; i < elements; i++) {
			byteBufferArray[i] = generateDirectByteBuffer(length);
		}
		
		return byteBufferArray;
	}
	
	
	public static byte[] generateRandomByteArray(int length) {
		Random random = new Random();
		
		byte[] bytes = new byte[length];
		random.nextBytes(bytes);

		return bytes;
	}
	
	
	
	public static byte[] generateByteArray(int length) {
		
		byte[] bytes = new byte[length];
		
		int item = OFFSET;
		
		for (int i = 0; i < length; i++) {
			bytes[i] = (byte) item;
			
			item++;
			if (item > (OFFSET + 9)) {
				item = OFFSET;
			}
		}
		
		return bytes;
	}
	
	
	public static byte[] generateByteArray(int length, String delimiter) {
		byte[] del = delimiter.getBytes();
		byte[] data = generateByteArray(length);
		
		byte[] result = new byte[del.length + data.length];
		System.arraycopy(data, 0, result, 0, data.length);
		System.arraycopy(del, 0, result, data.length, del.length);
		return result;
	}
	
	
	
	public static boolean isEquals(File file, String text) throws IOException {
		return isEquals(file, "UTF-8", text);
	}
	
	public static boolean isEquals(File file, String encoding, String text) throws IOException {
	    ByteBuffer buf = DataConverter.toByteBuffer(text, encoding);
	    return isEquals(file, new ByteBuffer[] { buf } );
	}


	
    public static boolean isEquals(File file, File file2) throws IOException {
        
    	RandomAccessFile raf = new RandomAccessFile(file2, "r");
        FileChannel fc = raf.getChannel();
        ByteBuffer buf = ByteBuffer.allocate((int) fc.size());
        fc.read(buf);
        buf.flip();
        
        boolean isEquals = isEquals(file, new ByteBuffer[] { buf });
        
        fc.close();
        raf.close();
        
        return isEquals;
    }
    	
    
    public static boolean isEquals(File file, byte[] data) throws IOException {
    	return isEquals(file, new ByteBuffer[] { DataConverter.toByteBuffer(data) });
    }
    
	
	public static boolean isEquals(File file, ByteBuffer[] buffers) throws IOException {
		int length = 0;
		for (ByteBuffer byteBuffer : buffers) {
			length += byteBuffer.remaining();
		}

		RandomAccessFile raf = new RandomAccessFile(file, "r");
		FileChannel fc = raf.getChannel();
		ByteBuffer buf = ByteBuffer.allocate(length);
		fc.read(buf);
		buf.flip();
		
		boolean isEquals = isEquals(buf, buffers);
		
		fc.close();
		raf.close();
		
		return isEquals;
	}
	
	public static boolean isEquals(byte[] b1, byte[] b2) {
		if (b1.length != b2.length) {
			return false;
		}
		
		for (int i = 0; i < b1.length; i++) {
			if (b1[i] != b2[i]) {
				return false;
			}
		}
		
		return true;
	}
	
	
	public static boolean isEquals(InputStream is, BodyDataSource ds, int size) throws IOException {
		
		for (int i = 0; i < size; i++) {
			byte j = (byte) is.read();
			byte k = ds.readByte();
			
			if (j != k) {
				return false;
			}
		}

		return true;
	}
	
	
	public static boolean isEquals(ByteBuffer[] b1, ByteBuffer[] b2) {
		return isEquals(DataConverter.toByteBuffer(b1), DataConverter.toByteBuffer(b2));
	}
	
	public static boolean isEquals(ByteBuffer b1, ByteBuffer[] b2) {
		return isEquals(b1, DataConverter.toByteBuffer(b2));
	}
	
	public static boolean isEquals(ByteBuffer b1, ByteBuffer b2) {
		if (b1.remaining() != b2.remaining()) {
		    System.out.println("different length l1=" + b1.remaining() + ", l2=" + b2.remaining());
			return false;
		}

		int b1Pos = b1.position();
		int b2Pos = b2.position();
		int length = b1.remaining();
		
		for (int i = 0; i < length; i++) {
			if (b1.get(b1Pos) != b2.get(b2Pos)) {
				return false;
			}
			b1Pos++;
			b2Pos++;
		}
		
		return true;
	}
	
	
	
	public static void sleep(int sleepTime) {
		if (sleepTime <= 0) {
			return;
		}
		
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException ie) { 
			// Restore the interrupted status
            Thread.currentThread().interrupt();
		}
	}
	
	
	public static byte[] mergeByteArrays(byte[] b1, byte[] b2) {
		byte[] result = new byte[b1.length + b2.length];
		System.arraycopy(b1, 0, result, 0, b1.length);
		System.arraycopy(b2, 0, result, b1.length, b2.length);
		
		return result;
	}
	
	
	public static byte[] toArray(ByteBuffer buffer) {

		byte[] array = new byte[buffer.limit() - buffer.position()];

		if (buffer.hasArray()) {
			int offset = buffer.arrayOffset();
			byte[] bufferArray = buffer.array();
			System.arraycopy(bufferArray, offset, array, 0, array.length);

			return array;
		} else {
			buffer.get(array);
			return array;
		}
	}

	
	
	public static void setLogLevel(String level) {		
		setLogLevel("org.xlightweb", Level.parse(level));
	}

	
	public static void setLogLevel(Level level) {		
		setLogLevel("org.xlightweb", level);
	}

	
	public static void setLogLevel(String namespace, String level) {
		setLogLevel(namespace, Level.parse(level));
	}

	
	public static void setLogLevel(String namespace, Level level) {
		Logger logger = Logger.getLogger(namespace);
		logger.setLevel(level);

		ConsoleHandler ch = new ConsoleHandler();
		ch.setLevel(level);
		ch.setFormatter(new LogFormatter());
		logger.addHandler(ch);		
	}
		
	
	public static void assertTimeout(long elapsed, long expected, long min, long max) {
		System.out.println("elapsed time " + elapsed + " (expected=" + expected + ", min=" + min + ", max=" + max + ")");
		Assert.assertTrue("elapsed time " + elapsed + " out of range (expected=" + expected + ", min=" + min + ", max=" + max + ")"
				          , (elapsed >= min) && (elapsed <= max));
	}

	
	public static InetAddress getRandomLocalAddress() throws IOException {
		String hostname = InetAddress.getLocalHost().getHostName();
		InetAddress[] addresses = InetAddress.getAllByName(hostname);
	
		int i = new Random().nextInt();
		if (i < 0) {
			i = 0 - i;
		}
		
		i = i % addresses.length;
		
		return addresses[i];
	}
}
