package org.frostbyte;

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Minimal example: reads config.xml and extracts only the dataSource user and password.
 */
public class Main {
    public static void main(String[] args) {
        try {
            String configPath = "config.xml"; // adjust to absolute path if needed

            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(configPath);
            doc.getDocumentElement().normalize();

            Element dataSource = (Element) doc.getElementsByTagName("dataSource").item(0);
            if (dataSource == null) {
                System.err.println("dataSource element not found in config.xml");
                return;
            }

            String user = getElementText(dataSource, "user");
            String password = getElementText(dataSource, "password");

            // Variables available for further use:
            System.out.println("User: " + user);
            System.out.println("Password: " + password);

            // (Now you can pass `user` and `password` to DB connectors, services, etc.)

        } catch (Exception e) {
            System.err.println("Failed to read config.xml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String getElementText(Element parent, String tagName) {
        if (parent.getElementsByTagName(tagName).getLength() == 0) return null;
        String text = parent.getElementsByTagName(tagName).item(0).getTextContent();
        return text == null ? null : text.trim();
    }
}
