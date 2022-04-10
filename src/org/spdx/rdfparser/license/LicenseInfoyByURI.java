package org.spdx.rdfparser.license;

import org.apache.jena.graph.Node;
import org.spdx.rdfparser.IModelContainer;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SpdxRdfConstants;

public class LicenseInfoyByURI  implements LicenseStrategy {

	IModelContainer modelContainer;
	Node node;
	LicenseInfoyByURI(IModelContainer modelContainer, Node node) {
		this.modelContainer = modelContainer;
		this.node = node;
	}
	@Override
	public AnyLicenseInfo getLicense() throws InvalidSPDXAnalysisException {
		if (!node.isURI()) {
			return null;
		}
		if (node.getURI().equals(SpdxRdfConstants.SPDX_NAMESPACE+SpdxRdfConstants.TERM_LICENSE_NONE)) {
			return new SpdxNoneLicense(this.modelContainer, this.node);
		} else if (node.getURI().equals(SpdxRdfConstants.SPDX_NAMESPACE+SpdxRdfConstants.TERM_LICENSE_NOASSERTION)) {
			return new SpdxNoAssertionLicense(this.modelContainer, this.node);
		} else if (node.getURI().startsWith(ListedLicenses.LISTED_LICENSE_ID_URL)) {
			// try to fetch the listed license from the model
			try {
				return ListedLicenses.getListedLicenses().getLicenseFromStdLicModel(this.modelContainer, this.node);
			} catch (Exception ex) {
				//logger.warn("Unable to get license from SPDX listed license model for "+node.getURI());
				return null;
			}
		} else {
			return null;
		}
	}
}
