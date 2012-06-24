package util;

import net.jxta.document.StructuredDocument;
import net.jxta.document.XMLDocument;

public class AkkaUtil {

	public static XMLDocument structuredDocumentToXmldocument(StructuredDocument strDoc){
		return (XMLDocument)strDoc;
	}
	
}
