package org.spdx.rdfparser.license;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.spdx.rdfparser.IModelContainer;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SpdxRdfConstants;

public class LicenseInfoById implements LicenseStrategy {

	IModelContainer modelContainer;
	Node node;
	LicenseInfoById(IModelContainer modelContainer, Node node) {
		this.modelContainer = modelContainer;
		this.node = node;
	}
	@Override
	public AnyLicenseInfo getLicense() throws InvalidSPDXAnalysisException {
		Node licenseIdPredicate = modelContainer.getModel().getProperty(SpdxRdfConstants.SPDX_NAMESPACE, SpdxRdfConstants.PROP_LICENSE_ID).asNode();
		Triple m = Triple.createMatch(node, licenseIdPredicate, null);
		ExtendedIterator<Triple> tripleIter = modelContainer.getModel().getGraph().find(m);
		if (tripleIter.hasNext()) {
			Triple triple = tripleIter.next();
			String id = triple.getObject().toString(false);
			if (tripleIter.hasNext()) {
				throw(new InvalidSPDXAnalysisException("More than one ID associated with license "+id));
			}
			if (isSpdxListedLicenseID(id)) {
				return new SpdxListedLicense(modelContainer, node);
			} else if (id.startsWith(SpdxRdfConstants.NON_STD_LICENSE_ID_PRENUM)) {
				return new ExtractedLicenseInfo(modelContainer, node);
			} else {
				// could not determine the type from the ID
				// could be a conjunctive or disjunctive license ID
				return null;
			}
		} else {
			throw(new InvalidSPDXAnalysisException("No ID associated with a license"));
		}
	}

	public static boolean isSpdxListedLicenseID(String licenseID)  {
		return ListedLicenses.getListedLicenses().isSpdxListedLicenseID(licenseID);
	}

}
