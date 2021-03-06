/*
 * Copyright 2015 Fizzed, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fizzed.crux.uri;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/**
 * Helps to build a URI.  Why another one?  Unlike Java's URI this one allows
 * modification after it's been created. This one has a simple fluent style
 * to help build uris.
 */
public class MutableUri extends Uri {

    public MutableUri() {
        // empty
    }
    
    public MutableUri(String uri) {
        this(URI.create(uri));
    }
    
    public MutableUri(URI uri) {
        this.apply(uri);
    }
    
    public MutableUri(Uri uri) {
        this.scheme = uri.scheme;
        this.userInfo = uri.userInfo;
        this.host = uri.host;
        this.port = uri.port;
        this.path = uri.path;
        this.query = uri.query;
        this.fragment = uri.fragment;
    }
    
    public Uri toImmutable() {
        return new Uri(this.scheme, this.userInfo, this.host, this.port, this.path, copy(this.query), this.fragment);
    }
    
    public URI toURI() {
        // only way to correctly set query string
        return URI.create(toString());
    }
    
    public MutableUri scheme(String scheme) {
        this.scheme = scheme;
        return this;
    }
    
    public MutableUri userInfo(String userInfo) {
        this.userInfo = userInfo;
        return this;
    }
    
    public MutableUri host(String host) {
        this.host = host;
        return this;
    }
    
    public MutableUri port(Integer port) {
        this.port = port;
        return this;
    }
    
    public MutableUri path(String path) {
        this.path = path;
        return this;
    }
    
    public MutableUri query(String name, String value) {
        List<String> values = getQueryValues(name);
        
        values.add(value);

        return this;
    }
    
    public MutableUri setQuery(String name, String value) {
        List<String> values = getQueryValues(name);
        
        values.clear();
        values.add(value);

        return this;
    }
    
    private List<String> getQueryValues(String name) {
        Objects.requireNonNull(name, "name cannot be null");
        
        if (this.query == null) {
            this.query = new LinkedHashMap<>(); // order of insertion important
        }
        
        return this.query.computeIfAbsent(name, (key) -> new ArrayList<>());
    }
    
    public MutableUri fragment(String fragment) {
        this.fragment = fragment;
        return this;
    }
    
    private String encodedQueryString() {
        if (this.query == null || this.query.isEmpty()) {
            return null;
        }
        
        StringBuilder s = new StringBuilder();
        
        this.query.forEach((key, values) -> {
            values.forEach((value) -> {
                if (s.length() != 0) {
                    s.append("&");
                }
                s.append(key);
                if (value != null) {
                    s.append("=");
                    s.append(encode(value));
                }
            });
        });
        
        return s.toString();
    }
    
    private MutableUri apply(URI uri) {
        if (uri.getScheme() != null) {
            this.scheme = uri.getScheme();
        }
        
        if (uri.getRawUserInfo() != null) {
            this.userInfo = decode(uri.getRawUserInfo());
        }
        
        if (uri.getHost() != null) {
            this.host = uri.getHost();
        }
        
        if (uri.getPort() >= 0) {
            this.port = uri.getPort();
        }
        
        String rawPath = uri.getRawPath();
        if (rawPath != null && rawPath.length() > 0) {
            // TODO: should we decode it here?
            this.path = uri.getRawPath();
        }
        
        if (uri.getRawQuery() != null) {
            // get rid of map to rebuild
            this.query = null;
            
            // split on ampersand...
            String[] pairs = uri.getRawQuery().split("&");
            for (String pair : pairs) {
                String[] nv = pair.split("=");
                switch (nv.length) {
                    case 1:
                        this.query(decode(nv[0]), null);
                        break;
                    case 2:
                        this.query(decode(nv[0]), decode(nv[1]));
                        break;
                    default:
                        throw new IllegalArgumentException("Name value pair [" + pair + "] in query [" + uri.getRawQuery() + "] missing = char");
                }
            }
        }
        
        if (uri.getRawFragment() != null) {
            this.fragment = decode(uri.getRawFragment());
        }
        
        return this;
    }
    
    @Override
    public String toString() {
        // Note this code is essentially a copy of 'java.net.URI.defineString',
        // which is private. We cannot use the 'new URI( scheme, userInfo, ... )' or
        // 'new URI( scheme, authority, ... )' constructors because they double
        // encode the query string using 'java.net.URI.quote'
        StringBuilder uri = new StringBuilder();
        
        if (this.scheme != null) {
            uri.append(this.scheme);
            uri.append(':');
        }
        
        if (this.host != null) {
            uri.append("//");
            
            if (this.userInfo != null) {
                uri.append(encode(this.userInfo));
                uri.append('@');
            }
            
            uri.append(this.host);
            
            if (this.port != null) {
                uri.append(':');
                uri.append(this.port);
            }
            
        }
        
        if (this.path != null) {
            uri.append(this.path);
        }

        if (this.query != null && !this.query.isEmpty()) {
            uri.append('?');
            uri.append(encodedQueryString());
        }
        
        if (this.fragment != null) {
            uri.append('#');
            uri.append(encode(this.fragment));
        }
       
        return uri.toString();
    }
    
    /*
    static public MutableUri of(String uri, Object... parameters) {
        String s = format(uri, parameters);
        return new MutableUri(s);
    }
    
    static public URI uri(String uri, Object... parameters) {
        String s = format(uri, parameters);
        return new MutableUri(s).toURI();
    }
    */
    
    static private String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }
    
    static private String decode(String value) {
        try {
            return java.net.URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }
    
    /**
     * Builds a String that accepts place holders and replaces them with URL encoded
     * values.
     * @param uri A string with place holders (e.g. "http://localhost:{}/path?a={}&b={}", 80, "valForA", "valForB")
     * @param parameters
     * @return 
     */
    /*
    static private String format(String uri, Object... parameters) {
        if (parameters == null || parameters.length == 0 || !uri.contains("{}")) {
            return uri;
        }
        
        List<String> tokens = splitter(uri, "{}");
        
        // there should be tokens.length - 1 parameters supplied
        if (tokens.size() - 1 != parameters.length) {
            throw new IllegalArgumentException("Incorrect number of parameters (expected " + (tokens.size() - 1)
                + "; actual " + parameters.length + ")");
        }
        
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < tokens.size(); i++) {
            if (i > 0) {
                String v = parameters[i-1].toString();
                sb.append(encode(v));
            }
            sb.append(tokens.get(i));
        }
        
        return sb.toString();
    }
    
    static private List<String> splitter(String s, String delimiter) {
        List<String> tokens = new ArrayList<>();
        
        int index = 0;
        int matches = 0;
        while (index < s.length()) {
            int pos = s.indexOf(delimiter, index);
            
            tokens.add(s.substring(index, (pos < 0 ? s.length() : pos)));
            
            if (pos < 0) {
                break;
            }
            
            matches++;
            index = pos + delimiter.length();
        }
        
        // add an empty value at end if needed
        if (matches + 1 != tokens.size()) {
            tokens.add("");
        }
        
        return tokens;
    }
    */
    
    
}
