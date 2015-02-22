/**
 * Copyright (c) 2011 Source Auditor Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.spdx.tools;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.model.Annotation;
import org.spdx.rdfparser.model.Relationship;
import org.spdx.rdfparser.model.SpdxDocument;
import org.spdx.rdfparser.model.SpdxElement;
import org.spdx.rdfparser.model.SpdxPackage;
import org.spdx.rdfparser.license.ExtractedLicenseInfo;
import org.spdx.rdfparser.SPDXDocumentFactory;
import org.spdx.rdfparser.model.SpdxFile;
import org.spdx.rdfparser.SPDXReview;
import org.spdx.rdfparser.SpdxRdfConstants;
import org.spdx.spdxspreadsheet.AnnotationsSheet;
import org.spdx.spdxspreadsheet.NonStandardLicensesSheet;
import org.spdx.spdxspreadsheet.OriginsSheet;
import org.spdx.spdxspreadsheet.PackageInfoSheet;
import org.spdx.spdxspreadsheet.PerFileSheet;
import org.spdx.spdxspreadsheet.RelationshipsSheet;
import org.spdx.spdxspreadsheet.ReviewersSheet;
import org.spdx.spdxspreadsheet.SPDXSpreadsheet;
import org.spdx.spdxspreadsheet.SpreadsheetException;

/**
 * Translates an RDF XML file to a SPDX Spreadsheet format
 * Usage: RdfToSpreadsheet rdfxmlfile.rdf spreadsheetfile.xls
 * where rdfxmlfile.rdf is a valid SPDX RDF XML file and spreadsheetfile.xls is 
 * the output SPDX spreadsheeet file.
 * @author Gary O'Neall
 *
 */
public class RdfToSpreadsheet {

	static final int MIN_ARGS = 2;
	static final int MAX_ARGS = 2;
	static Pattern datePattern = Pattern.compile(".. ... \\d\\d\\d\\d \\d\\d:\\d\\d:\\d\\d GMT$");
	public static final String NOT_SUPPORTED_STRING = "This field is not yet supported by SPDX";
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < MIN_ARGS) {
			usage();
			return;
		}
		if (args.length > MAX_ARGS) {
			usage();
			return;
		}
		File spdxRdfFile = new File(args[0]);
		if (!spdxRdfFile.exists()) {
			System.out.printf("Error: File %1$s does not exist.\n", args[0]);
			return;
		}
		File spdxSpreadsheetFile = new File(args[1]);
		if (spdxSpreadsheetFile.exists()) {
			System.out.println("Spreadsheet file already exists - please specify a new file.");
			return;
		}
		SpdxDocument doc = null;
		try {
			doc = SPDXDocumentFactory.createSpdxDocument(args[0]);
		} catch (InvalidSPDXAnalysisException ex) {
			System.out.print("Error creating SPDX Document: "+ex.getMessage());
			return;
		} catch (IOException e) {
			System.out.print("Unable to open file :"+args[0]+", "+e.getMessage());
		}
		SPDXSpreadsheet ss = null;
		try {
			ss = new SPDXSpreadsheet(spdxSpreadsheetFile, true, false);
			copyRdfXmlToSpreadsheet(doc, ss);
			ArrayList<String> verify = doc.verify();
			if (verify.size() > 0) {
				System.out.println("Warning: The following verifications failed for the resultant SPDX RDF file:");
				for (int i = 0; i < verify.size(); i++) {
					System.out.println("\t"+verify.get(i));
				}
			}
		} catch (SpreadsheetException e) {
			System.out.println("Error opening or writing to spreadsheet: "+e.getMessage());
		} catch (InvalidSPDXAnalysisException e) {
			System.out.println("Error translating the RDF file: "+e.getMessage());
		} catch (Exception ex) {
			System.out.println("Unexpected error translating the RDF to spreadsheet: "+ex.getMessage());
		} finally {
			if (ss != null) {
				try {
					ss.close();
				} catch (SpreadsheetException e) {
					System.out.println("Error closing spreadsheet: "+e.getMessage());
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	public static void copyRdfXmlToSpreadsheet(SpdxDocument doc,
			SPDXSpreadsheet ss) throws InvalidSPDXAnalysisException, SpreadsheetException {
		if (doc == null) {
			System.out.println("Warning: No document to copy");
			return;
		}
		copyOrigins(doc, ss.getOriginsSheet());
		HashMap<String, String> fileIdToPackageId = copyPackageInfo(doc.getDocumentContainer().findAllPackages(), ss.getPackageInfoSheet());
		copyNonStdLicenses(doc.getExtractedLicenseInfos(), ss.getNonStandardLicensesSheet());
		copyPerFileInfo(doc.getDocumentContainer().findAllFiles(), ss.getPerFileSheet(), fileIdToPackageId);
		List<Relationship> allRelationships = new ArrayList<Relationship>();
		List<Annotation> allAnnotations = new ArrayList<Annotation>();
		List<SpdxElement> allElements = doc.getDocumentContainer().findAllElements();
		allRelationships.addAll(Arrays.asList(doc.getRelationships()));
		allAnnotations.addAll(Arrays.asList(doc.getAnnotations()));
		for (SpdxElement element:allElements) {
			allRelationships.addAll(Arrays.asList(element.getRelationships()));
			allAnnotations.addAll(Arrays.asList(element.getAnnotations()));
		}
		copyRelationships(allRelationships, ss.getRelationshipsSheet());
		copyAnnotations(allAnnotations, ss.getAnnotationsSheet());
		copyReviewerInfo(doc.getReviewers(), ss.getReviewersSheet());
		ss.resizeRow();
	}

	/**
	 * @param annotations
	 * @param annotationsSheet
	 */
	private static void copyAnnotations(List<Annotation> annotations,
			AnnotationsSheet annotationsSheet) {
		Collections.sort(annotations);
		for (Annotation annotation:annotations) {
			annotationsSheet.add(annotation);
		}
	}

	/**
	 * @param relationships
	 * @param relationshipsSheet
	 */
	private static void copyRelationships(List<Relationship> relationships,
			RelationshipsSheet relationshipsSheet) {
		Collections.sort(relationships);
		for (Relationship relationship:relationships) {
			relationshipsSheet.add(relationship);
		}
	}

	private static void copyReviewerInfo(SPDXReview[] reviewers,
			ReviewersSheet reviewersSheet) throws InvalidSPDXAnalysisException {
		DateFormat dateFormat = new SimpleDateFormat(SpdxRdfConstants.SPDX_DATE_FORMAT);	
		for (int i = 0; i < reviewers.length; i++) {
			String reviewerName = reviewers[i].getReviewer();
			Date reviewDate = null;
			String dateString = reviewers[i].getReviewDate();
			if (dateString != null && !dateString.isEmpty()) {
				try {
					dateString = dateString.trim();
					reviewDate = dateFormat.parse(dateString);
				} catch (Exception ex) {
					throw(new InvalidSPDXAnalysisException("Invalid reviewer date format for reviewer "+reviewers[i]));
				}
			}
			reviewersSheet.addReviewer(reviewerName, reviewDate, reviewers[i].getComment());
		}
	}

	private static void copyPerFileInfo(List<SpdxFile> fileList,
			PerFileSheet perFileSheet, HashMap<String, String> fileIdToPackageId) {            
            Collections.sort(fileList);
            /* Print out sorted files */            
		for (SpdxFile file : fileList) {
			perFileSheet.add(file, fileIdToPackageId.get(file.getId()));
		}
	}

	private static void copyNonStdLicenses(ExtractedLicenseInfo[] nonStandardLicenses,
			NonStandardLicensesSheet nonStandardLicensesSheet) {
		for(int i = 0; i < nonStandardLicenses.length; i++) {
			nonStandardLicensesSheet.add(nonStandardLicenses[i].getLicenseId(), nonStandardLicenses[i].getExtractedText(), 
					nonStandardLicenses[i].getName(),
					nonStandardLicenses[i].getSeeAlso(),
					nonStandardLicenses[i].getComment());
		}
	}

	private static HashMap<String, String> copyPackageInfo(List<SpdxPackage> packages,
			PackageInfoSheet packageInfoSheet) throws InvalidSPDXAnalysisException {
		HashMap<String, String> fileIdToPkgId = new HashMap<String, String>();
		Collections.sort(packages);
		Iterator<SpdxPackage> iter = packages.iterator();
		while (iter.hasNext()) {
			SpdxPackage pkg = iter.next();
			String pkgId = pkg.getId();
			SpdxFile[] files = pkg.getFiles();
			for (int i = 0; i < files.length; i++) {
				String fileId = files[i].getId();
				String pkgIdsForFile = fileIdToPkgId.get(fileId);
				if (pkgIdsForFile == null) {
					pkgIdsForFile = pkgId;
				} else {
					pkgIdsForFile = pkgIdsForFile + ", " + pkgId;
				}
				fileIdToPkgId.put(fileId, pkgIdsForFile);
			}
			packageInfoSheet.add(pkg);
		}
		return fileIdToPkgId;
	}

	private static void copyOrigins(SpdxDocument doc, OriginsSheet originsSheet) throws SpreadsheetException {
		originsSheet.addDocument(doc);
	}

	private static void usage() {
		System.out.println("Usage: RdfToSpreadsheet rdfxmlfile.rdf spreadsheetfile.xls\n"+
				"where rdfxmlfile.rdf is a valid SPDX RDF XML file and spreadsheetfile.xls is\n"+
				"the output SPDX spreadsheeet file.");
	}
}
