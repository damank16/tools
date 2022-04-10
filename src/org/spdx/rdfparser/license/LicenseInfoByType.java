package org.spdx.rdfparser.license;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.spdx.rdfparser.IModelContainer;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.SpdxRdfConstants;

public class LicenseInfoByType implements LicenseStrategy{

	IModelContainer modelContainer;
	Node node;
	LicenseInfoByType(IModelContainer modelContainer, Node node) {
		this.modelContainer = modelContainer;
		this.node = node;
	}
	@Override
	public AnyLicenseInfo getLicense() throws InvalidSPDXAnalysisException {
			Node rdfTypePredicate = modelContainer.getModel().getProperty(SpdxRdfConstants.RDF_NAMESPACE,
					SpdxRdfConstants.RDF_PROP_TYPE).asNode();
			Triple m = Triple.createMatch(node, rdfTypePredicate, null);
			ExtendedIterator<Triple> tripleIter = modelContainer.getModel().getGraph().find(m);	// find the type(s)
			if (tripleIter.hasNext()) {
				Triple triple = tripleIter.next();
				if (tripleIter.hasNext()) {
					throw(new InvalidSPDXAnalysisException("More than one type associated with a licenseInfo"));
				}
				Node typeNode = triple.getObject();
				if (!typeNode.isURI()) {
					throw(new InvalidSPDXAnalysisException("Invalid type for licenseInfo - not a URI"));
				}
				// need to parse the URI
				String typeUri = typeNode.getURI();
				if (!typeUri.startsWith(SpdxRdfConstants.SPDX_NAMESPACE)) {
					throw(new InvalidSPDXAnalysisException("Invalid type for licenseInfo - not an SPDX type"));
				}
				String type = typeUri.substring(SpdxRdfConstants.SPDX_NAMESPACE.length());
				if (type.equals(SpdxRdfConstants.CLASS_SPDX_CONJUNCTIVE_LICENSE_SET)) {
					return new ConjunctiveLicenseSet(modelContainer, node);
				} else if (type.equals(SpdxRdfConstants.CLASS_SPDX_DISJUNCTIVE_LICENSE_SET)) {
					return new DisjunctiveLicenseSet(modelContainer, node);
				} else if (type.equals(SpdxRdfConstants.CLASS_SPDX_EXTRACTED_LICENSING_INFO)) {
					return new ExtractedLicenseInfo(modelContainer, node);
				} else if (type.equals(SpdxRdfConstants.CLASS_SPDX_LICENSE)) {
					return new SpdxListedLicense(modelContainer, node);
				} else if (type.equals(SpdxRdfConstants.CLASS_OR_LATER_OPERATOR)) {
					return new OrLaterOperator(modelContainer, node);
				} else if (type.equals(SpdxRdfConstants.CLASS_WITH_EXCEPTION_OPERATOR)) {
					return new WithExceptionOperator(modelContainer, node);
				} else {
					throw(new InvalidSPDXAnalysisException("Invalid type for licenseInfo '"+type+"'"));
				}
			} else {
				return null;
			}
		}
}
