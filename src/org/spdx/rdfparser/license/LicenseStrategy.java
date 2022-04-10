package org.spdx.rdfparser.license;

import org.apache.jena.graph.Node;
import org.spdx.rdfparser.IModelContainer;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;

public interface LicenseStrategy {

	public AnyLicenseInfo getLicense() throws InvalidSPDXAnalysisException;
}
