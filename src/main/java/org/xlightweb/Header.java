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


import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;





/**
 * Implementation base for a header
 * 
 * @author grro@xlightweb.org
 */
public class Header implements IHeader, Cloneable {
    
    private static final Logger LOG = Logger.getLogger(Header.class.getName());
    
	
	static final int MAX_HEADER_SIZE = 8192;
	

	static final String CONTENT_TYPE = "Content-Type";
	static final String CONTENT_DISPOSITION = "Content-Disposition";
    static final String TRANSFER_ENCODING = "Transfer-Encoding";

	
	
	private static final int MAX_HEADER_LINE_LENGTH = 250; // (RFC2882 998 -> see http://rfc.net/rfc2822.html#s2.1.1.)
	private static final int MIN_HEADER_LINE_LENGTH_THRESHOLD = 15;
	
	
	
	private ArrayList<HeaderEntry> headers = new ArrayList<HeaderEntry>();

	private String defaultEncoding = IHttpMessage.DEFAULT_ENCODING;
	private String characterEncoding = null;


	
	   // caching
    private String contentType;
    private String contentDisposition;
    private String contentDispositionType;
    private Map<String, String> contentDispositionParams;
    private String transferEncoding;


    /**
     * constructor
     */
    public Header() {
	}

    
    /**
     * constructor
     * 
     * @param contentType the contentType
     */
    public Header(String contentType) {
    	setContentType(contentType);
	}
    

    
    void setBodyDefaultEncoding(String defaultEncoding) {
        this.defaultEncoding = defaultEncoding;
    }
	
	
	/**
	 * adds a header line
	 * 
	 * @param line the header line 
	 */
	public final void addHeaderLine(String line) {
		String[] kvp = line.split(":");
		addHeader(kvp[0], kvp[1].trim());
	}
	
	
	/**
     * {@inheritDoc}
     */
	public final void addHeaderlines(String... lines) {
        for (String headerline : lines) {
            addHeaderLine(headerline);
        }
	}



	/**
	 * {@inheritDoc}
	 */
	public final void setHeader(String headername, String headervalue) {
		removeHeader(headername);
		addHeader(headername, headervalue);
	}

	
	
	/**
	 * {@inheritDoc}
	 */
	public final void addHeader(String headername, String headervalue) {
		boolean isHandled = onHeaderAdded(headername, headervalue);
		
		if (!isHandled) {
			HeaderEntry entry = new HeaderEntry(headername, headervalue);
			headers.add(entry);
		}
	}
	
	
	boolean onHeaderAdded(String headername, String headervalue) {
	    
        if (headername.equalsIgnoreCase(CONTENT_TYPE)) {
            contentType = headervalue;
            return true;
        }
        
        if (headername.equalsIgnoreCase(TRANSFER_ENCODING)) {
            transferEncoding = headervalue;
            return true;
        }
        
		return false;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public final void removeHeader(String headername) {
		boolean isHandled = onHeaderRemoved(headername);
		
		if (!isHandled) {
			List<HeaderEntry> removeList = new ArrayList<HeaderEntry>();
			
			for (HeaderEntry headerEntry : headers) {
				if (headerEntry.isNameEquals(headername)) {
					removeList.add(headerEntry);
				}
			}
			
			headers.removeAll(removeList);
		}
	}
	
	
	

	boolean onHeaderRemoved(String headername) {
	    
	    if (headername.equalsIgnoreCase(CONTENT_TYPE)) {
	        contentType = null;
	        return true;
	    }
	    
	    if (headername.equalsIgnoreCase(CONTENT_DISPOSITION)) {
            contentDisposition = null;
            contentDispositionType = null;
            contentDispositionParams = null;
            return true;
        }

	    if (headername.equalsIgnoreCase(TRANSFER_ENCODING)) {
	        transferEncoding = null;
	        return true;
	    }
	        
		return false;
	}
	
	
	
	/**
	 * {@inheritDoc}
	 */
	public boolean containsHeader(String headername) {
	    
	    if ((contentType != null) && headername.equalsIgnoreCase(CONTENT_TYPE)) {
	        return true;
	    }
	    
	    if ((transferEncoding != null) && headername.equalsIgnoreCase(TRANSFER_ENCODING)) {
	        return true;
	    }

		
		for (HeaderEntry entry : headers) {
			if (entry.isNameEquals(headername)) {
				return true;
			}
		}
		
		return false;
	}

	

	
	/**
	 * {@inheritDoc}
	 */
	public Set<String> getHeaderNameSet() {
	    
	    HashSet<String> headerNames = new HashSet<String>();
	    
	    if (contentType != null) {
	        headerNames.add(CONTENT_TYPE);
	    }
	    
	    if (transferEncoding != null) {
	        headerNames.add(TRANSFER_ENCODING);
	    }
	        
		for (HeaderEntry entry : headers) {
		    headerNames.add(entry.getName());
		}
		return headerNames;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public final Enumeration getHeaderNames() {
		return Collections.enumeration(getHeaderNameSet());
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public List<String> getHeaderList(String headername) {
	    
	       
        if (headername.equalsIgnoreCase(TRANSFER_ENCODING)) {
            if (transferEncoding == null) {
                return null;
            }
            List<String> result = new ArrayList<String>();
            result.add(transferEncoding);
            return Collections.unmodifiableList(result); 
        } 

        if (headername.equalsIgnoreCase(CONTENT_TYPE)) {
            if (contentType == null) {
                return null;
            }
            List<String> result = new ArrayList<String>();
            result.add(contentType);
            return Collections.unmodifiableList(result); 
        } 

        
		List<String> values = new ArrayList<String>();
		
		for (HeaderEntry entry : headers) {
			if (entry.isNameEquals(headername)) {
				values.add(entry.getValue());
			}
		}
		
		return Collections.unmodifiableList(values);
	}
	
	
	

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public final Enumeration getHeaders(String headername) {
		return Collections.enumeration(getHeaderList(headername));
	}


	/**
	 * {@inheritDoc}
	 */
	public String getHeader(String headername) {
	    
	    if (headername.equalsIgnoreCase(TRANSFER_ENCODING)) {
	        if (transferEncoding == null) {
	            return null;
	        }
	        return transferEncoding;
	    } 


	    if (headername.equalsIgnoreCase(CONTENT_TYPE)) {
            if (contentType == null) {
                return null;
            }
            return contentType;
        }
        
	    
		for (HeaderEntry entry : headers) {
			if (entry.isNameEquals(headername)) {
				return entry.getValue();
			}
		}
	
		return null;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	public String getHeader(String headername, String dflt) {
	    String value = getHeader(headername);
	    if (value == null) {
	        return dflt;
	    } else {
	        return value;
	    }
	}   
	    
	    


	
	/**
     * {@inheritDoc}
     */
    public final void setContentType(String contentType) {
        this.contentType = contentType;
        
        // contains charset encoding?
        String encoding = HttpUtils.parseEncodingWithDefault(contentType, defaultEncoding);
        if (encoding != null) {
            characterEncoding = encoding;
        } 
    }
	

    /**
     * {@inheritDoc}
     */
    public final String getCharacterEncoding() {
        if ((characterEncoding == null) && (contentType != null)) {
            characterEncoding = HttpUtils.parseEncodingWithDefault(contentType, defaultEncoding);
        }
        
        if (characterEncoding == null) {
            characterEncoding = defaultEncoding;
        }
        
        return characterEncoding; 
    }

    
    final void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding; 
    }
    
    /**
     * {@inheritDoc}
     */
    public final String getContentType() {
        return contentType;
    }


	/**
	 * {@inheritDoc}
	 */
	public final List<ContentType> getAccept() {
		List<ContentType> result = new ArrayList<ContentType>();
	
		String acceptHeader = getHeader("Accept");
		
		if (acceptHeader != null) {
			if (acceptHeader.indexOf(",") == -1) {
				result.add(new ContentType(acceptHeader));
			} else {
				String[] acceptList = acceptHeader.split(",");
				List<WeightContentType> weightContentTypes = new ArrayList<WeightContentType>();
				for (String acceptString : acceptList) {
					weightContentTypes.add(new WeightContentType(acceptString));
				}
				Collections.sort(weightContentTypes);
				for (WeightContentType weightContentType : weightContentTypes) {
					result.add(weightContentType.contentType);
				}
			}
		}
		
		return result;
	}


	private static final class WeightContentType implements Comparable<WeightContentType> {
		private final ContentType contentType;
		private final double weight;
		
		
		WeightContentType(String acceptString) {
			int idx = acceptString.indexOf(";q=");
			if (idx == -1) {
				contentType = new ContentType(acceptString.trim());
				weight = 1;
				
			} else {
				contentType = new ContentType(acceptString.substring(0, idx).trim());
				String acceptParams = acceptString.substring(idx + 3, acceptString.length());
				acceptParams = acceptParams.trim();
				if (acceptParams.endsWith(";")) {
					acceptParams = acceptParams.substring(0, acceptParams.length() - 1).trim();
				}
				idx = acceptParams.indexOf(";");
				if (idx != -1) {
					acceptParams = acceptParams.substring(0, idx).trim();
				}
				weight = Double.parseDouble(acceptParams);
			}
		}
		
		
		public int compareTo(WeightContentType other) {
			if (this.weight > other.weight) {
				return -1;
				
			} else if (this.weight < other.weight) {
				return 1;
				
			} else {
				if (this.contentType.getPrimaryType().equals("*")) {
					return 1;
				} 
				if (other.contentType.getPrimaryType().equals("*")) {
					return -1;
				}
				
				if (this.contentType.getPrimaryType().equalsIgnoreCase(other.contentType.getPrimaryType())) {
					if (this.contentType.getSubType().equals("*")) {
						return 1;
					}
					if (other.contentType.getSubType().equals("*")) {
						return -1;
					}
					
					if (this.contentType.getSubType().equalsIgnoreCase(other.contentType.getSubType())) {
						if (this.contentType.toString().length() > other.contentType.toString().length()) {
							return -1;
						} 
						if (this.contentType.toString().length() < other.contentType.toString().length()) {
							return 1;
						}
					}
				}
				return 0;
			}
		}
		
		
		@Override
		public boolean equals(Object other) {
			
			if (other instanceof WeightContentType) {
				WeightContentType otherWeightContentType = (WeightContentType) other;
				if ((otherWeightContentType.weight == this.weight) && 
				    (otherWeightContentType.contentType.equals(this.contentType))) {
					return true;
				}
			}
			
			return false;
		}
		
		
		@Override
		public int hashCode() {
			return (contentType.toString() + Double.toString(weight)).hashCode();
		}

		
		@Override
		public String toString() {
			return contentType.toString() + " [q=" + weight + "]";
		}
	}
	

    /**
     * {@inheritDoc}
     */
    public final String getDisposition() {
        if (contentDisposition == null) {
            contentDisposition = getHeader(CONTENT_DISPOSITION);
        }
        
        return contentDisposition;
    }
    
    
    /**
     * {@inheritDoc}
     */    
    public final String getDispositionType() {
        if (contentDispositionType == null) {
            resolveContentDisposition();
        }
        
        return contentDispositionType;
    }
    
    

    /**
     * {@inheritDoc}
     */    
    public final String getDispositionParam(String name) {
        if (contentDispositionParams == null) {
            resolveContentDisposition();
        }
        
        if (contentDispositionParams != null) {
            return contentDispositionParams.get(name.toUpperCase(Locale.US));
        } else {
            return null;
        }
    }
   
    
    private void resolveContentDisposition() {
        
        String cd = getDisposition();
        
        if (cd != null) {
            contentDispositionParams = new HashMap<String, String>(); 
                
            String[] parts = cd.split(";");
            
            for (int i = 0; i < parts.length; i++) {
                parts[i] = parts[i].trim();
            
                if (i == 0) {
                    contentDispositionType = parts[i];
                
                } else {
                    int idx = parts[i].indexOf("=");
                    if (idx != -1) {
                        String name = parts[i].substring(0, idx).toUpperCase(Locale.US);
                        String value = removeQuote(parts[i].substring(idx +1, parts[i].length()));
                        contentDispositionParams.put(name, value);
                        
                    } else {
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.fine("error occured by parsing content disposition param " + parts[i]);
                        }
                    }
                }
            }
            
        }
    }
    
    private String removeQuote(String txt) {
        if (txt.charAt(0) == '"') {
            txt = txt.substring(1, txt.length());
        }
        
        if (txt.charAt(txt.length() - 1) == '"') {
            txt = txt.substring(0, txt.length() - 1);
        }
        
        return txt;
    }
    
    

    /**
     * {@inheritDoc}
     */
    public final String getTransferEncoding() {
        return transferEncoding;
    }

    
    /**
     * {@inheritDoc}
     */
    public final void setTransferEncoding(String transferEncoding) {
        this.transferEncoding = transferEncoding;
    }


    
    
	void writeHeadersTo(StringBuilder sb) {
		
	    if (contentType != null) {
	        sb.append("Content-Type: ");
	        sb.append(contentType);
	        sb.append("\r\n");
	    }
	    
	    
        if (transferEncoding != null) {
            sb.append("Transfer-Encoding: ");
            sb.append(transferEncoding);
            sb.append("\r\n");
        }
        
	    
		for (HeaderEntry entry : headers) {
			entry.writeTo(sb);
			sb.append("\r\n");
		}
	}
	
	

    
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Object clone() throws CloneNotSupportedException {
        Header copy = (Header) super.clone();
        
        copy.headers = (ArrayList<HeaderEntry>) this.headers.clone();        
        return copy;
    }
    
	

    
	private static class HeaderEntry {
		
		private String name = null;
		private String value = null;
		
		public HeaderEntry(String name, String value) {
			this.name = name;
			this.value = value;
		}
		
		String getName() {
			return name;
		}
		
		String getValue() {
			return value;
		}
		
		boolean isNameEquals(String name) {
			return this.name.equalsIgnoreCase(name);
		}
		
		
		@Override
		public String toString() {
			return name + ": " + value;
		}
		
		
		void writeTo(StringBuilder sb) {
			sb.append(name);
			sb.append(": ");
			sb.append(value);
		}
		
		void writeFoldedTo(StringBuilder sb) {
			sb.append(name);
			
			if (value.length() > MAX_HEADER_LINE_LENGTH) {
				sb.append(":");
				sb.append(foldLine(value));
			} else {
				sb.append(": ");
				sb.append(value);
			}
		}
		
		
		private String foldLine(String line) {
			StringBuffer foldedLine = new StringBuffer();
			
			
			String[] tokens = line.split(" ");
			int currentLineLength = 0;
			
			for (String token : tokens) {
				if (((token.length() + currentLineLength) > MAX_HEADER_LINE_LENGTH) || (token.length() > MIN_HEADER_LINE_LENGTH_THRESHOLD)) {
					foldedLine.append("\r\n ");
					foldedLine.append(token);
					currentLineLength = token.length();
					
				} else {
					foldedLine.append(" ");
					foldedLine.append(token);
					currentLineLength += token.length();
				}
			}
			
			
			return foldedLine.toString();
		}
	}
	
	@Override
	public String toString() {
	    StringBuilder sb = new StringBuilder();
	    writeHeadersTo(sb);
	    return sb.toString();
	}
}
