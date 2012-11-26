package org.nicolasgramlich.toolbox.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;

/**
 * @author Nicolas Gramlich
 * @since Nov 21, 2012
 */
public class Util {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	public static final int getBitLength(final int pNumber) {
		return Integer.SIZE - Integer.numberOfLeadingZeros(pNumber);
	}

	public static String[] readLines(final File pFile) throws IOException {
		return Util.readLines(new FileInputStream(pFile));
	}

	public static String[] readLines(final InputStream pInputStream) throws IOException {
		return Util.readLines(new InputStreamReader(pInputStream));
	}

	public static String[] readLines(final Reader pReader) throws IOException {
		final BufferedReader reader = new BufferedReader(pReader);

		final ArrayList<String> lines = new ArrayList<String>();

		String line = null;
		while((line = reader.readLine()) != null) {
			lines.add(line);
		}

		return lines.toArray(new String[lines.size()]);
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
