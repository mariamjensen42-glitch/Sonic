package com.cycling.domain.lyrics.utils

import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.StringReader

internal data class XmlAttribute(
    val name: String,
    val value: String
)

internal data class XmlElement(
    val name: String,
    val attributes: List<XmlAttribute>,
    val children: List<XmlElement>,
    val text: String
)

/**
 * XML → [XmlElement] via the platform DOM parser (`javax.xml.parsers`).
 *
 * The tree keeps the text-node convention the lyrics parsers were written against: whitespace
 * between tags surfaces as a `#text` child in document order, so a walker can append it to the
 * preceding element (that is how spaces between word-by-word spans reach the syllables), and
 * [XmlElement.text] is the concatenation of the element's own trimmed text chunks.
 */
internal class DomXmlParser {
    fun parse(xml: String): XmlElement = runCatching { buildTree(xml) }
        .getOrElse { XmlElement("", emptyList(), emptyList(), "") }

    private fun buildTree(xml: String): XmlElement {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            isValidating = false
            // Input is network-sourced; if the platform rejects the feature, DOCTYPEs stay
            // possible but the parse itself must not start failing.
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        }
        val document = factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
        return document.documentElement.toXmlElement()
    }

    private fun Element.toXmlElement(): XmlElement {
        val attributes = (0 until this.attributes.length).map { i ->
            val attribute = attributes.item(i)
            XmlAttribute(attribute.nodeName, attribute.nodeValue ?: "")
        }
        val text = StringBuilder()
        val children = mutableListOf<XmlElement>()
        val childNodes = this.childNodes
        for (i in 0 until childNodes.length) {
            val node = childNodes.item(i)
            when (node.nodeType) {
                Node.ELEMENT_NODE -> children.add((node as Element).toXmlElement())
                Node.TEXT_NODE, Node.CDATA_SECTION_NODE -> {
                    val collapsed = node.nodeValue.replace(Regex("\\s+"), " ")
                    val trimmed = collapsed.trim()
                    text.append(trimmed)
                    val whitespace = collapsed.substringBefore(trimmed) + collapsed.substringAfter(trimmed)
                    if (whitespace.isNotEmpty()) {
                        children.add(XmlElement("#text", emptyList(), emptyList(), whitespace))
                    }
                }
            }
        }
        return XmlElement(nodeName, attributes, children, text.toString())
    }
}
