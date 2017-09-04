/* 
 * Copyright 2012-2015 Aerospike, Inc.
 *
 * Portions may be licensed to Aerospike, Inc. under one or more contributor
 * license agreements.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package net.helipilot50.graphql.export;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;

public class NoCaseInputStream extends ANTLRInputStream {
	
	

	public NoCaseInputStream(InputStream input) throws IOException {
		super(input);
	}


	public NoCaseInputStream() {
		super();
	}


	public NoCaseInputStream(char[] data, int numberOfActualCharsInArray) {
		super(data, numberOfActualCharsInArray);
	}


	public NoCaseInputStream(InputStream input, int initialSize,
			int readChunkSize) throws IOException {
		super(input, initialSize, readChunkSize);
	}


	public NoCaseInputStream(InputStream input, int initialSize)
			throws IOException {
		super(input, initialSize);
	}


	public NoCaseInputStream(Reader r, int initialSize, int readChunkSize)
			throws IOException {
		super(r, initialSize, readChunkSize);
	}


	public NoCaseInputStream(Reader r, int initialSize) throws IOException {
		super(r, initialSize);
	}


	public NoCaseInputStream(Reader r) throws IOException {
		super(r);
	}


	public NoCaseInputStream(String input) {
		super(input);
	}


	public int LA(int i) {
        if ( i==0 ) {
            return 0; // undefined
        }
        if ( i<0 ) {
            i++; // e.g., translate LA(-1) to use offset 0
        }

        if ( (p+i-1) >= n ) {

            return CharStream.EOF;
        }
        return Character.toLowerCase(data[p+i-1]);
    }

}
