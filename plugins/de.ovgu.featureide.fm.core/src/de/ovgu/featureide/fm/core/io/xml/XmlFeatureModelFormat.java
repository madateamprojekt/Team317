/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2017  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 *
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://featureide.cs.ovgu.de/ for further information.
 */
package de.ovgu.featureide.fm.core.io.xml;

import static de.ovgu.featureide.fm.core.localization.StringTable.ABSTRACT;
import static de.ovgu.featureide.fm.core.localization.StringTable.CALCULATIONS;
import static de.ovgu.featureide.fm.core.localization.StringTable.COMMENTS;
import static de.ovgu.featureide.fm.core.localization.StringTable.HIDDEN;
import static de.ovgu.featureide.fm.core.localization.StringTable.MANDATORY;
import static de.ovgu.featureide.fm.core.localization.StringTable.NOT;
import static de.ovgu.featureide.fm.core.localization.StringTable.WRONG_SYNTAX;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.prop4j.And;
import org.prop4j.AtMost;
import org.prop4j.Equals;
import org.prop4j.Implies;
import org.prop4j.Literal;
import org.prop4j.Not;
import org.prop4j.Or;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import de.ovgu.featureide.fm.core.PluginID;
import de.ovgu.featureide.fm.core.base.FeatureUtils;
import de.ovgu.featureide.fm.core.base.IConstraint;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.IFeatureModelFactory;
import de.ovgu.featureide.fm.core.base.IPropertyContainer.Entry;
import de.ovgu.featureide.fm.core.base.IPropertyContainer.Type;
import de.ovgu.featureide.fm.core.base.impl.FMFactoryManager;
import de.ovgu.featureide.fm.core.base.impl.FeatureAttribute;
import de.ovgu.featureide.fm.core.base.impl.FeatureAttributeInherited;
import de.ovgu.featureide.fm.core.io.IFeatureModelFormat;
import de.ovgu.featureide.fm.core.io.Problem;
import de.ovgu.featureide.fm.core.io.UnsupportedModelException;
import de.ovgu.featureide.fm.core.io.xml.XmlPropertyLoader.PropertiesParser;

/**
 * Reads / Writes a feature model in the FeatureIDE XML format
 *
 * @author Jens Meinicke
 * @author Marcus Pinnecke
 * @author Sebastian Krieter
 */
public class XmlFeatureModelFormat extends AXMLFormat<IFeatureModel> implements IFeatureModelFormat {

	public static final String ID = PluginID.PLUGIN_ID + ".format.fm." + XmlFeatureModelFormat.class.getSimpleName();

	private static final Pattern CONTENT_REGEX = Pattern.compile("\\A\\s*(<[?]xml\\s.*[?]>\\s*)?<featureModel[\\s>]");

	private IFeatureModelFactory factory;

	@Override
	public boolean supportsRead() {
		return true;
	}

	@Override
	public boolean supportsWrite() {
		return true;
	}

	@Override
	protected void readDocument(Document doc, List<Problem> warnings) throws UnsupportedModelException {
		object.reset();

		factory = FMFactoryManager.getFactory(object);

		final Collection<PropertiesParser> customProperties = new ArrayList<>();

		for (final Element e : getElements(doc.getElementsByTagName(FEATURE_MODEL))) {
			parseStruct(e.getElementsByTagName(STRUCT));
			parseConstraints(e.getElementsByTagName(CONSTRAINTS));
			parseCalculations(e.getElementsByTagName(CALCULATIONS));
			parseComments(e.getElementsByTagName(COMMENTS));
			parseFeatureOrder(e.getElementsByTagName(FEATURE_ORDER));

			final XmlPropertyLoader propertyLoader = new XmlPropertyLoader(e.getElementsByTagName(PROPERTIES));
			customProperties.addAll(propertyLoader.parseProperties());
		}

		if (object.getStructure().getRoot() == null) {
			throw new UnsupportedModelException(WRONG_SYNTAX, 1);
		}

		importCustomProperties(customProperties, object);
	}

	@Override
	protected void writeDocument(Document doc) {
		final Element root = doc.createElement(FEATURE_MODEL);
		final Element struct = doc.createElement(STRUCT);
		final Element properties = doc.createElement(PROPERTIES);
		final Element constraints = doc.createElement(CONSTRAINTS);
		final Element calculations = doc.createElement(CALCULATIONS);
		final Element comments = doc.createElement(COMMENTS);
		final Element order = doc.createElement(FEATURE_ORDER);

		doc.appendChild(root);
		root.appendChild(properties);
		createXmlPropertiesPart(doc, properties, object);

		root.appendChild(struct);
		createXmlDocRec(doc, struct, FeatureUtils.getRoot(object));

		root.appendChild(constraints);
		for (int i = 0; i < object.getConstraints().size(); i++) {
			Element rule;
			rule = doc.createElement(RULE);

			constraints.appendChild(rule);
			createPropositionalConstraints(doc, rule, object.getConstraints().get(i).getNode());
		}

		root.appendChild(calculations);
		calculations.setAttribute(CALCULATE_AUTO, "" + object.getAnalyser().runCalculationAutomatically);
		calculations.setAttribute(CALCULATE_FEATURES, "" + object.getAnalyser().calculateFeatures);
		calculations.setAttribute(CALCULATE_CONSTRAINTS, "" + object.getAnalyser().calculateConstraints);
		calculations.setAttribute(CALCULATE_REDUNDANT, "" + object.getAnalyser().calculateRedundantConstraints);
		calculations.setAttribute(CALCULATE_TAUTOLOGY, "" + object.getAnalyser().calculateTautologyConstraints);

		root.appendChild(comments);
		for (final String comment : object.getProperty().getComments()) {
			final Element c = doc.createElement(C);
			comments.appendChild(c);
			final Text text = doc.createTextNode(comment);
			c.appendChild(text);
		}
		order.setAttribute(USER_DEFINED, Boolean.toString(object.isFeatureOrderUserDefined()));
		root.appendChild(order);

		if (object.isFeatureOrderUserDefined()) {
			Collection<String> featureOrderList = object.getFeatureOrderList();

			if (featureOrderList.isEmpty()) {
				featureOrderList = FeatureUtils.extractConcreteFeaturesAsStringList(object);
			}

			for (final String featureName : featureOrderList) {
				final Element feature = doc.createElement(FEATURE);
				feature.setAttribute(NAME, featureName);
				order.appendChild(feature);
				writeRealAttribute(doc, feature, object.getFeature(featureName));
			}
		}
	}

	protected void addDescription(Document doc, IFeature feat, Element fnod) {
		final String description = feat.getProperty().getDescription();
		if ((description != null) && !description.trim().isEmpty()) {
			final Element descr = doc.createElement(DESCRIPTION);
			descr.setTextContent("\n" + description.replace("\r", "") + "\n");
			fnod.appendChild(descr);
		}
	}

	/**
	 *
	 * @param attribute an attribute
	 * @param attributeListRecursive List of Recursive Elements
	 *
	 *        If an Attribute has less than 3 parameters, this method will check if it is an inherited attribute. If there is no corresponding Attribute, the
	 *        name is empty, there already is an inherited attribute with that name, or there is another parameter than "name" or "value" an error will be
	 *        shown. Otherwise the attribute will be added to the list.
	 *
	 */

	private void addToInheritedList(Element e, LinkedList<FeatureAttribute> attributeListRecursive,
			LinkedList<FeatureAttributeInherited> inheritedAttributeList) throws UnsupportedModelException {

		final FeatureAttributeInherited inherited = new FeatureAttributeInherited();

		final NamedNodeMap nodeMap = e.getAttributes();
		for (int i = 0; i < nodeMap.getLength(); i++) {
			final org.w3c.dom.Node node = nodeMap.item(i);
			final String nodeName = node.getNodeName();
			final String attributeValue = node.getNodeValue().trim();

			if (nodeName.equals(NAME)) {
				if (attributeValue.isEmpty()) {
					throwError("Empty Name " + nodeMap.item(0), e);
				} else {
					if (!isNameInInheretedList(inheritedAttributeList, attributeValue)) {
						final String attributeName = attributeValue.toLowerCase();
						for (final FeatureAttribute fa : attributeListRecursive) {
							if (fa.getName().toLowerCase().equals(attributeName)) {
								inherited.setParent(fa);
							}
						}
					} else {
						throwError("Attribute already defined: " + nodeMap.item(0), e);
					}
				}
			} else if (nodeName.equals(VALUE)) {
				inherited.setValue(attributeValue);
			} else {
				throwError("Wrong parameters in: " + inherited, e);
			}
		}
		if (inherited.getParent() == null) {
			throwError("No type for attribute: " + nodeMap.item(0), e);
		}
		inheritedAttributeList.add(inherited);
	}

	/**
	 * @param attributeList
	 * @param listElementName
	 * @return true if an attributeName is already in the list
	 */
	private boolean checkAttributeList(String attributeName, LinkedList<FeatureAttribute> attributeList) {
		for (int i = 0; i < attributeList.size(); i++) {
			if (attributeList.get(i).getName().toLowerCase().equals(attributeName)) {
				return true;
			}
		}
		return false;
	}

	private Node createFeaturePropertyContainerNode(Document doc, String featureName, Set<Entry<String, Type, Object>> propertyEntries) {
		final Element result = doc.createElement(FEATURE);
		result.setAttribute(NAME, featureName);
		for (final Entry<String, Type, Object> entry : propertyEntries) {
			result.appendChild(createPropertyEntriesNode(doc, entry));
		}
		return result;
	}

	private Node createPropertyEntriesNode(Document doc, Entry<String, Type, Object> entry) {
		final Element propertyElement = doc.createElement(XmlPropertyLoader.PROPERTY);
		propertyElement.setAttribute(XmlPropertyLoader.KEY, entry.getKey());
		propertyElement.setAttribute(XmlPropertyLoader.VALUE, entry.getValue().toString());
		propertyElement.setAttribute(XmlPropertyLoader.TYPE, entry.getType().toString());
		return propertyElement;
	}

	/**
	 * Inserts the tags concerning propositional constraints into the DOM document representation
	 *
	 * @param doc
	 * @param FeatMod Parent node for the propositional nodes
	 */
	private void createPropositionalConstraints(Document doc, Element xmlNode, org.prop4j.Node node) {
		if (node == null) {
			return;
		}

		Element op;
		if (node instanceof Literal) {
			Literal literal = (Literal) node;
			if (literal.positive) {
				op = doc.createElement(VAR);
				xmlNode.appendChild(op);
				op.appendChild(doc.createTextNode(node.toString()));
			} else {
				op = doc.createElement(NOT);
				xmlNode.appendChild(op);
				literal = literal.clone();
				literal.positive = true;
				createPropositionalConstraints(doc, op, literal);
			}
			return;
		}

		if (node instanceof And) {
			op = doc.createElement(CONJ);
			xmlNode.appendChild(op);
		} else if (node instanceof Or) {
			op = doc.createElement(DISJ);
			xmlNode.appendChild(op);
		} else if (node instanceof Not) {
			op = doc.createElement(NOT);
			xmlNode.appendChild(op);
		} else if (node instanceof Equals) {
			op = doc.createElement(EQ);
			xmlNode.appendChild(op);
		} else if (node instanceof Implies) {
			op = doc.createElement(IMP);
			xmlNode.appendChild(op);
		} else if (node instanceof AtMost) {
			op = doc.createElement(ATMOST1);
			xmlNode.appendChild(op);
		} else {
			op = doc.createElement(UNKNOWN);
			xmlNode.appendChild(op);
		}

		final org.prop4j.Node[] children = node.getChildren();

		for (int i = 0; i < children.length; i++) {
			createPropositionalConstraints(doc, op, children[i]);
		}
	}

	/**
	 * Creates document based on feature model step by step
	 *
	 * @param doc document to write
	 * @param node parent node
	 * @param feat current feature
	 */
	private void createXmlDocRec(Document doc, Element node, IFeature feat) {
		if (feat == null) {
			return;
		}

		final List<IFeature> children = FeatureUtils.convertToFeatureList(feat.getStructure().getChildren());

		final Element fnod;
		if (children.isEmpty()) {
			fnod = doc.createElement(FEATURE);
			addDescription(doc, feat, fnod);
			writeAttributes(node, fnod, feat);
			writeRealAttribute(doc, fnod, feat);

		} else {
			if (feat.getStructure().isAnd()) {
				fnod = doc.createElement(AND);
			} else if (feat.getStructure().isOr()) {
				fnod = doc.createElement(OR);
			} else if (feat.getStructure().isAlternative()) {
				fnod = doc.createElement(ALT);
			} else {
				fnod = doc.createElement(UNKNOWN);// Logger.logInfo("creatXMlDockRec: Unexpected error!");
			}

			addDescription(doc, feat, fnod);
			writeAttributes(node, fnod, feat);
			writeRealAttribute(doc, fnod, feat);

			for (final IFeature feature : children) {
				createXmlDocRec(doc, fnod, feature);
			}

		}

	}

	private void createXmlPropertiesPart(Document doc, Element propertiesNode, IFeatureModel featureModel) {

		if ((featureModel == null) || (propertiesNode == null)) {
			throw new RuntimeException();
		}

		// Store per-feature properties
		for (final IFeature feature : featureModel.getFeatures()) {
			final String featureName = feature.getName();
			final Set<Entry<String, Type, Object>> propertyEntries = feature.getCustomProperties().entrySet();
			if (!propertyEntries.isEmpty()) {
				propertiesNode.appendChild(createFeaturePropertyContainerNode(doc, featureName, propertyEntries));
			}
		}

		// TODO: Add here other property container, e.g., feature model
		// ...
	}

	private void importCustomProperties(Collection<PropertiesParser> customProperties, IFeatureModel object) {
		for (final PropertiesParser parser : customProperties) {
			switch (parser.getType()) {
			case FEATURE_PROPERTIES_PARSER: {
				for (final String featureName : parser.getIdentifier()) {
					object.getFeature(featureName).getCustomProperties().setEntrySet(parser.getPropertyEntries(featureName));
				}
			}
				break;
			default:
				throw new UnsupportedOperationException("Unkown property container parser type " + parser.getType());
			}
		}
	}

	/**
	 * @param fa
	 * @param inherited
	 * @return true if Attribute is already in InheritedList
	 */
	private boolean isAttributeInInheritedList(FeatureAttribute fa, LinkedList<FeatureAttributeInherited> inherited) {
		for (final FeatureAttributeInherited fai : inherited) {
			if (fai.getParent().equals(fa)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param attributeList
	 * @param attributeName
	 * @return true if Name is already in AttributeList
	 */
	private boolean isNameInAttributeList(LinkedList<FeatureAttribute> attributeList, String attributeName) {
		for (final FeatureAttribute fai : attributeList) {
			if (fai.getName().toLowerCase().equals(attributeName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param attributeValue
	 * @param inheritedAttributeList
	 * @return
	 */
	private boolean isNameInInheretedList(LinkedList<FeatureAttributeInherited> inheritedAttributeList, String attributeValue) {
		attributeValue = attributeValue.toLowerCase();
		for (final FeatureAttributeInherited fai : inheritedAttributeList) {
			if (fai.getName().toLowerCase().equals(attributeValue)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param e
	 * @param attributeList
	 * @param recursiveList
	 * @param inheritedList
	 * @throws UnsupportedModelException
	 */
	private void parseAttribute(Element e, LinkedList<FeatureAttribute> attributeList, LinkedList<FeatureAttribute> recursiveList,
			LinkedList<FeatureAttributeInherited> inheritedList) throws UnsupportedModelException {

		if (e.hasAttributes()) {
			final NamedNodeMap nodeMap = e.getAttributes();
			if (nodeMap.getLength() > 2) {
				final FeatureAttribute attribute = new FeatureAttribute();
				for (int i = 0; i < nodeMap.getLength(); i++) {
					final org.w3c.dom.Node node = nodeMap.item(i);
					final String nodeName = node.getNodeName();
					final String attributeValue = node.getNodeValue().trim();
					if (nodeName.equals(NAME)) {
						if (!attributeValue.isEmpty()) {
							attribute.setName(attributeValue);
						} else {
							throwError("No Name for attribute: " + attribute.toString(), e);
						}
					} else if (nodeName.equals(TYPE)) {
						if (attributeValue.isEmpty()) {
							throwError("Type empty in attribute: " + attribute.toString(), e);
						}
						attribute.setTypeFromString(attributeValue);
						if (attribute.getType() == null) {
							throwError("Wrong type for attribute : " + attribute.toString(), e);
						}
					} else if (nodeName.equals(UNIT)) {
						attribute.setUnit(attributeValue);
					} else if (nodeName.equals(RECURSIVE)) {
						attribute.setRecursive(attributeValue.equals(TRUE));
					} else if (nodeName.equals(CONFIGURABLE)) {
						attribute.setConfigurable(attributeValue.equals(TRUE));
					} else if (nodeName.equals(VALUE)) {
						attribute.setValue(attributeValue);
					} else {
						throwError("Unknown attribute: " + nodeName, e);
					}

				}

				if (isNameInAttributeList(recursiveList, attribute.getName().toLowerCase())) {
					throwError("Too many parameters in this inherited attribute: " + attribute.toString(), e);
				}

				if (checkAttributeList(attribute.getName().toLowerCase(), attributeList)) {
					throwError("Duplicate name for attribute in this feature: " + attribute.toString(), e);
				} else {
					attributeList.add(attribute);
					if (attribute.getRecursive() == true) {
						recursiveList.add(attribute);
					}
				}
			} else {
				addToInheritedList(e, recursiveList, inheritedList);
			}
		}
	}

	/**
	 * Parses the calculations.
	 */
	private void parseCalculations(NodeList nodeList) throws UnsupportedModelException {
		for (final Element e : getElements(nodeList)) {
			if (e.hasAttributes()) {
				final NamedNodeMap nodeMap = e.getAttributes();
				for (int i = 0; i < nodeMap.getLength(); i++) {
					final org.w3c.dom.Node node = nodeMap.item(i);
					final String nodeName = node.getNodeName();
					final boolean value = node.getNodeValue().equals(TRUE);
					if (nodeName.equals(CALCULATE_AUTO)) {
						object.getAnalyser().runCalculationAutomatically = value;
					} else if (nodeName.equals(CALCULATE_CONSTRAINTS)) {
						object.getAnalyser().calculateConstraints = value;
					} else if (nodeName.equals(CALCULATE_REDUNDANT)) {
						object.getAnalyser().calculateRedundantConstraints = value;
					} else if (nodeName.equals(CALCULATE_FEATURES)) {
						object.getAnalyser().calculateFeatures = value;
					} else if (nodeName.equals(CALCULATE_TAUTOLOGY)) {
						object.getAnalyser().calculateTautologyConstraints = value;
					} else {
						throwError("Unknown calculations attribute: " + nodeName, e);
					}

				}
			}
		}
	}

	/**
	 * Parses the comment section.
	 */
	private void parseComments(NodeList nodeList) throws UnsupportedModelException {
		for (final Element e : getElements(nodeList)) {
			if (e.hasChildNodes()) {
				parseComments2(e.getChildNodes());
			}
		}
	}

	private void parseComments2(NodeList nodeList) throws UnsupportedModelException {
		for (final Element e : getElements(nodeList)) {
			if (e.getNodeName().equals(C)) {
				object.getProperty().addComment(e.getTextContent());
			} else {
				throwError("Unknown comment attribute: " + e.getNodeName(), e);
			}
		}
	}

	/**
	 * Parses the constraint section.
	 */
	private void parseConstraints(NodeList nodeList) throws UnsupportedModelException {
		for (final Element e : getElements(nodeList)) {
			for (final Element child : getElements(e.getChildNodes())) {
				final String nodeName = child.getNodeName();
				if (nodeName.equals(RULE)) {
					final IConstraint c = factory.createConstraint(object, parseConstraints2(child.getChildNodes()).getFirst());
					if (child.hasAttributes()) {
						final NamedNodeMap nodeMap = child.getAttributes();
						for (int i = 0; i < nodeMap.getLength(); i++) {
							final org.w3c.dom.Node node = nodeMap.item(i);
							final String attributeName = node.getNodeName();
							if (attributeName.equals(COORDINATES)) {
								// Legacy case, for backwards compatibility
							} else {
								throwError("Unknown constraint attribute: " + attributeName, node);
							}
						}
					}
					object.addConstraint(c);
				} else {
					throwError("Unknown constraint node: " + nodeName, child);
				}
			}
		}
	}

	private LinkedList<org.prop4j.Node> parseConstraints2(NodeList nodeList) throws UnsupportedModelException {
		final LinkedList<org.prop4j.Node> nodes = new LinkedList<>();
		LinkedList<org.prop4j.Node> children;
		for (final Element e : getElements(nodeList)) {
			final String nodeName = e.getNodeName();
			if (nodeName.equals(DISJ)) {
				nodes.add(new Or(parseConstraints2(e.getChildNodes())));
			} else if (nodeName.equals(CONJ)) {
				nodes.add(new And(parseConstraints2(e.getChildNodes())));
			} else if (nodeName.equals(EQ)) {
				children = parseConstraints2(e.getChildNodes());
				nodes.add(new Equals(children.get(0), children.get(1)));
			} else if (nodeName.equals(IMP)) {
				children = parseConstraints2(e.getChildNodes());
				nodes.add(new Implies(children.get(0), children.get(1)));
			} else if (nodeName.equals(NOT)) {
				nodes.add(new Not((parseConstraints2(e.getChildNodes())).getFirst()));
			} else if (nodeName.equals(ATMOST1)) {
				nodes.add(new AtMost(1, parseConstraints2(e.getChildNodes())));
			} else if (nodeName.equals(VAR)) {
				final String featureName = e.getTextContent();
				if (object.getFeature(featureName) != null) {
					nodes.add(new Literal(featureName));
				} else {
					throwError("Feature \"" + featureName + "\" does not exists", e);
				}
			} else {
				throwError("Unknown constraint type: " + nodeName, e);
			}
		}
		return nodes;
	}

	/**
	 * Parses the feature order section.
	 */
	private void parseFeatureOrder(NodeList nodeList) throws UnsupportedModelException {
		final ArrayList<String> order = new ArrayList<>(object.getNumberOfFeatures());
		for (final Element e : getElements(nodeList)) {
			if (e.hasAttributes()) {
				final NamedNodeMap nodeMap = e.getAttributes();
				for (int i = 0; i < nodeMap.getLength(); i++) {
					final org.w3c.dom.Node node = nodeMap.item(i);
					final String attributeName = node.getNodeName();
					final String attributeValue = node.getNodeValue();
					if (attributeName.equals(USER_DEFINED)) {
						object.setFeatureOrderUserDefined(attributeValue.equals(TRUE));
					} else if (attributeName.equals(NAME)) {
						if (object.getFeature(attributeValue) != null) {
							order.add(attributeValue);
						} else {
							throwError("Feature \"" + attributeValue + "\" does not exists", e);
						}
					} else {
						throwError("Unknown feature order attribute: " + attributeName, e);
					}

				}
			}
			if (e.hasChildNodes()) {
				parseFeatureOrder(e.getChildNodes());
			}
		}
		if (!order.isEmpty()) {
			object.setFeatureOrderList(order);
		}
	}

	private void parseFeatures(NodeList nodeList, IFeature parent, LinkedList<FeatureAttribute> parentList) throws UnsupportedModelException {

		final LinkedList<FeatureAttribute> attributeList = new LinkedList<>();
		final LinkedList<FeatureAttributeInherited> inheritedList = new LinkedList<>();

		final LinkedList<FeatureAttribute> attributeListRecursive = new LinkedList<FeatureAttribute>();

		if (parent != null) {
			for (final FeatureAttribute fa : parentList) {
				attributeListRecursive.addLast(fa);
			}
		}

		for (final Element e : getElements(nodeList)) {
			final String nodeName = e.getNodeName();
			if (nodeName.equals(DESCRIPTION)) {
				/* case: description */
				String nodeValue = e.getFirstChild().getNodeValue();
				if ((nodeValue != null) && !nodeValue.isEmpty()) {
					nodeValue = nodeValue.replace("\t", "");
					nodeValue = nodeValue.substring(1, nodeValue.length() - 1);
					nodeValue = nodeValue.trim();
				}
				parent.getProperty().setDescription(nodeValue);
				continue;
			}

			if (nodeName.equals(ATTRIBUTE)) {
				parseAttribute(e, attributeList, attributeListRecursive, inheritedList);
				parent.getStructure().setAttributeList(attributeList);
				parent.getStructure().setAttributeListInherited(inheritedList);
				continue;
			}

			boolean mandatory = false;
			boolean _abstract = false;
			boolean hidden = false;
			String name = "";
			// FMPoint featureLocation = null;
			if (e.hasAttributes()) {
				final NamedNodeMap nodeMap = e.getAttributes();
				for (int i = 0; i < nodeMap.getLength(); i++) {
					final org.w3c.dom.Node node = nodeMap.item(i);
					final String attributeName = node.getNodeName();
					final String attributeValue = node.getNodeValue();
					if (attributeName.equals(ABSTRACT)) {
						_abstract = attributeValue.equals(TRUE);
					} else if (attributeName.equals(MANDATORY)) {
						mandatory = attributeValue.equals(TRUE);
					} else if (attributeName.equals(NAME)) {
						name = attributeValue;
					} else if (attributeName.equals(HIDDEN)) {
						hidden = attributeValue.equals(TRUE);
					} else if (attributeName.equals(COORDINATES)) {
						// Legacy case, for backwards compatibility
					} else {
						throwError("Unknown feature attribute: " + attributeName, e);
					}

				}
			}

			if (object.getFeature(name) != null) {
				throwError("Duplicate entry for feature: " + name, e);
			}
			// TODO Consider feature name validity in all readers
			// if (!object.getFMComposerExtension().isValidFeatureName(name)) {
			// throwError(name + IS_NO_VALID_FEATURE_NAME, e);
			// }
			final IFeature f = factory.createFeature(object, name);
			f.getStructure().setMandatory(true);
			if (nodeName.equals(AND)) {
				f.getStructure().setAnd();
			} else if (nodeName.equals(ALT)) {
				f.getStructure().setAlternative();
			} else if (nodeName.equals(OR)) {
				f.getStructure().setOr();
			} else if (nodeName.equals(FEATURE)) {

			} else {
				throwError("Unknown feature type: " + nodeName, e);
			}
			f.getStructure().setAttributeListInherited(inheritedList);

			f.getStructure().setAbstract(_abstract);
			f.getStructure().setMandatory(mandatory);
			f.getStructure().setHidden(hidden);

			object.addFeature(f);
			if (parent == null) {
				object.getStructure().setRoot(f.getStructure());
			} else {
				parent.getStructure().addChild(f.getStructure());
			}
			if (e.hasChildNodes()) {
				parseFeatures(e.getChildNodes(), f, attributeListRecursive);
			}
		}

		// Check that there are only OR connections when the parent has more than one feature
		for (final IFeature f : object.getFeatures()) {
			if (f.getStructure().isOr() && (f.getStructure().getChildrenCount() <= 1)) {
				f.getStructure().setAnd();
			}
		}
	}

	/**
	 * Parse the struct section to add features to the model.
	 */
	private void parseStruct(NodeList struct) throws UnsupportedModelException {
		for (final Element e : getElements(struct)) {
			parseFeatures(e.getChildNodes(), null, null);
		}
	}

	/**
	 * Throws an error that will be used for error markers
	 *
	 * @param message The error message
	 * @param tempNode The node that causes the error. this node is used for positioning.
	 */
	private void throwError(String message, org.w3c.dom.Node node) throws UnsupportedModelException {
		throw new UnsupportedModelException(message, Integer.parseInt(node.getUserData(PositionalXMLHandler.LINE_NUMBER_KEY_NAME).toString()));
	}

	// TODO implement warnings
	@SuppressWarnings("unused")
	private void throwWarning(String message, org.w3c.dom.Node node) throws UnsupportedModelException {
		throw new UnsupportedModelException(message, Integer.parseInt(node.getUserData(PositionalXMLHandler.LINE_NUMBER_KEY_NAME).toString()));
	}

	private void writeAttributes(Element node, Element fnod, IFeature feat) {
		fnod.setAttribute(NAME, feat.getName());
		if (feat.getStructure().isHidden()) {
			fnod.setAttribute(HIDDEN, TRUE);
		}
		if (feat.getStructure().isMandatory()) {
			if ((feat.getStructure().getParent() != null) && feat.getStructure().getParent().isAnd()) {
				fnod.setAttribute(MANDATORY, TRUE);
			} else if (feat.getStructure().getParent() == null) {
				fnod.setAttribute(MANDATORY, TRUE);
			}
		}
		if (feat.getStructure().isAbstract()) {
			fnod.setAttribute(ABSTRACT, TRUE);
		}

		node.appendChild(fnod);
	}

	/**
	 * @param fnod Current feature node
	 * @param doc xml document
	 * @param feat IFeature
	 *
	 *        Write the XML file from FeatureModel. Checks if parent has Attributes that must be inherited. Optional parameters won't be written if empty or
	 *        false.
	 *
	 */

	private void writeRealAttribute(Document doc, Element fnod, IFeature feat) {

		final LinkedList<FeatureAttributeInherited> inheritedList = feat.getStructure().getAttributeListInherited();
		final LinkedList<FeatureAttribute> attributeList = feat.getStructure().getAttributeList();

		for (final FeatureAttribute fa : attributeList) {
			final Element attribute;
			attribute = doc.createElement(ATTRIBUTE);
			attribute.setAttribute(NAME, fa.getName());
			attribute.setAttribute(TYPE, fa.getType().toString());
			if (!fa.getValue().isEmpty()) {
				attribute.setAttribute(VALUE, fa.getValue());
			}
			if (!fa.getUnit().isEmpty()) {
				attribute.setAttribute(UNIT, fa.getUnit());
			}
			if (fa.getRecursive()) {
				attribute.setAttribute(RECURSIVE, String.valueOf(fa.getRecursive()));
			}
			if (fa.getConfigurable()) {
				attribute.setAttribute(CONFIGURABLE, String.valueOf(fa.getConfigurable()));
			}
			fnod.appendChild(attribute);
		}

		if (feat.getStructure().getParent() != null) {
			for (final FeatureAttribute fa : feat.getStructure().getParent().getFeature().getStructure().getAttributeList()) {
				if (fa.getRecursive()) {
					if (!isAttributeInInheritedList(fa, inheritedList)) {
						final FeatureAttributeInherited fai = new FeatureAttributeInherited(fa);
						inheritedList.add(fai);
					}
				}
			}
			for (final FeatureAttributeInherited fai : feat.getStructure().getParent().getFeature().getStructure().getAttributeListInherited()) {
				if (!isAttributeInInheritedList(fai.getParent(), inheritedList)) {
					final FeatureAttributeInherited featureInherited = new FeatureAttributeInherited();
					featureInherited.setParent(fai.getParent());
					inheritedList.add(featureInherited);
				}
			}
		}

		for (final FeatureAttributeInherited fai : inheritedList) {
			final Element element;
			element = doc.createElement(ATTRIBUTE);
			element.setAttribute(NAME, fai.getName());
			element.setAttribute(VALUE, fai.getValue());
			if (!element.getAttribute(VALUE).isEmpty()) {
				fnod.appendChild(element);
			}
		}
	}

	@Override
	public XmlFeatureModelFormat getInstance() {
		return new XmlFeatureModelFormat();
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public boolean supportsContent(CharSequence content) {
		return supportsRead() && CONTENT_REGEX.matcher(content).find();
	}

	@Override
	public String getName() {
		return "FeatureIDE";
	}

}
