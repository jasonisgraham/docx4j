/*
 *  Copyright 2007-2008, Plutext Pty Ltd.
 *   
 *  This file is part of docx4j.

    docx4j is licensed under the Apache License, Version 2.0 (the "License"); 
    you may not use this file except in compliance with the License. 

    You may obtain a copy of the License at 

        http://www.apache.org/licenses/LICENSE-2.0 

    Unless required by applicable law or agreed to in writing, software 
    distributed under the License is distributed on an "AS IS" BASIS, 
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
    See the License for the specific language governing permissions and 
    limitations under the License.

 */

package org.docx4j.samples;

import java.io.FileInputStream;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

import org.docx4j.XmlUtils;
import org.docx4j.convert.out.html.HtmlExporterNG;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class CreateXmlCss {
	
	public final static String HTML_TOP = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">"
		+"\n<html>"
		+"\n<head>"
		+"\n  <style type=\"text/css\">";
	
	public final static String HTML_MIDDLE =  "</style>"
		+ "\n</head>\n<body>";

	public final static String HTML_TAIL =  "\n  </body>" 
			+ "\n  <script type=\"text/javascript\" src=\"wml_fix.js\"></script>"
			+ "\n</html>";
	
	    public static void main(String[] args) 
	            throws Exception {

	    	boolean save = true;	    	
	    	    	
//			String inputfilepath = System.getProperty("user.dir") + "/sample-docs/numbering-multilevel.docx";
	    	//String inputfilepath = System.getProperty("user.dir") + "/test3.docx";
//			 String inputfilepath = "/home/dev/workspace/docx4all/sample-docs/docx4all-CurrentDocxFeatures.docx";
			 String inputfilepath = "/home/dev/workspace/docx4j/sample-docs/StyleResolution.xml";
	    	
			System.out.println(inputfilepath);
			WordprocessingMLPackage wordMLPackage;
			if (inputfilepath.endsWith(".xml")) {
				
				JAXBContext jc = Context.jcXmlPackage;
				Unmarshaller u = jc.createUnmarshaller();
				u.setEventHandler(new org.docx4j.jaxb.JaxbValidationEventHandler());

				org.docx4j.xmlPackage.Package wmlPackageEl = (org.docx4j.xmlPackage.Package)((JAXBElement)u.unmarshal(
						new javax.xml.transform.stream.StreamSource(new FileInputStream(inputfilepath)))).getValue(); 

				org.docx4j.convert.in.XmlPackageImporter xmlPackage = new org.docx4j.convert.in.XmlPackageImporter( wmlPackageEl); 

				wordMLPackage = (WordprocessingMLPackage)xmlPackage.get(); 
			
			} else {
				wordMLPackage = WordprocessingMLPackage.load(new java.io.File(inputfilepath));
			}
			
			// Convert it to HTML (ie no self-closing tags)
			Object o = wordMLPackage.getMainDocumentPart().getJaxbElement();
			Document doc = XmlUtils.marshaltoW3CDomDocument(o);
			StringBuffer buff = new StringBuffer();
			serialise(doc, buff, "  " );
			
			// Generate CSS   	
			String css = HtmlExporterNG.getCssForStyles(wordMLPackage); 			
			
			// Join it all together
			String result = HTML_TOP + css + HTML_MIDDLE + buff.toString() + HTML_TAIL;
						
						
			OutputStream os; 
			if (save) {
				os = new java.io.FileOutputStream(inputfilepath + ".html");
			} else {
				os = System.out;
			}
			os.write(result.getBytes());
			os.close();
			if (save) {
				System.out.println("Saved: " + inputfilepath + ".html ");
			} 
	        	        
	    }
	    
		public static void serialise( Node sourceNode, StringBuffer result, String indent ) {
		    	
	    	//log.debug("node type" + sourceNode.getNodeType());
		    	
            switch (sourceNode.getNodeType() ) {

            	case Node.DOCUMENT_NODE: // type 9
            
                    // recurse on each child
                    NodeList nodes = sourceNode.getChildNodes();
                    if (nodes != null) {
                        for (int i=0; i<nodes.getLength(); i++) {
                        	serialise((Node)nodes.item(i), result, indent);
                        }
                    }
                    break;
                case Node.ELEMENT_NODE:
                    
                    // Copy of the node itself
                	result.append("\n" + indent + "<" + sourceNode.getNodeName() );                    
            		
            		// .. its attributes
                	NamedNodeMap atts = sourceNode.getAttributes();
                	for (int i = 0 ; i < atts.getLength() ; i++ ) {
                		Attr attr = (Attr)atts.item(i);
                		
//		                		if (attr.getNamespaceURI()!=null
//		                				&& attr.getNamespaceURI().equals("http://www.w3.org/2000/xmlns/")) {
//			                		result.append(" " + attr.getLocalName() + "=\"" + attr.getValue() + "\"");
//		                		} else {
//			                		result.append(" " + attr.getName() + "=\"" + attr.getValue() + "\"");
//		                		}
                		result.append(" " + attr.getName() + "=\"" + attr.getValue() + "\"");
                	}

                	result.append(">");
                	
                	if (sourceNode.getChildNodes() == null
                			|| sourceNode.getChildNodes().getLength() ==0) {
                		// this is where we force tags to be
                		// non self-closing
                		result.append("</" + sourceNode.getNodeName() + ">" );
                	} else {
	                    // recurse on each child
                		NodeList children = sourceNode.getChildNodes();
                		boolean hasNonTextChild = false;
	                    if (children != null) {
	                        for (int i=0; i<children.getLength(); i++) {
	                        	//treeCopy( (DTMNodeProxy)children.item(i), newChild);
	                        	if (children.item(i).getNodeType() != Node.TEXT_NODE) {
	                        		hasNonTextChild = true;
	                        	}
	                        	serialise( (Node)children.item(i), result, indent+ "  ");
	                        }
	                    }
	                    if (hasNonTextChild) {
	                    	result.append("\n" + indent);
	                    }
	                    result.append("</" + sourceNode.getNodeName() + ">" );
                	}
                    break;

                case Node.TEXT_NODE:
                	//Node textNode = destParent.getOwnerDocument().createTextNode(sourceNode.getNodeValue());       
                	result.append(sourceNode.getNodeValue());
                    break;

//                case Node.CDATA_SECTION_NODE:
//                    writer.write("<![CDATA[" +
//                                 node.getNodeValue() + "]]>");
//                    break;
//
//                case Node.COMMENT_NODE:
//                    writer.write(indentLevel + "<!-- " +
//                                 node.getNodeValue() + " -->");
//                    writer.write(lineSeparator);
//                    break;
//
//                case Node.PROCESSING_INSTRUCTION_NODE:
//                    writer.write("<?" + node.getNodeName() +
//                                 " " + node.getNodeValue() +
//                                 "?>");
//                    writer.write(lineSeparator);
//                    break;
//
//                case Node.ENTITY_REFERENCE_NODE:
//                    writer.write("&" + node.getNodeName() + ";");
//                    break;
//
//                case Node.DOCUMENT_TYPE_NODE:
//                    DocumentType docType = (DocumentType)node;
//                    writer.write("<!DOCTYPE " + docType.getName());
//                    if (docType.getPublicId() != null)  {
//                        System.out.print(" PUBLIC \"" +
//                            docType.getPublicId() + "\" ");
//                    } else {
//                        writer.write(" SYSTEM ");
//                    }
//                    writer.write("\"" + docType.getSystemId() + "\">");
//                    writer.write(lineSeparator);
//                    break;
            }
        }	

	    
	}