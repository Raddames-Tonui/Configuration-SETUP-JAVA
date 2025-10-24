package org.frostbyte.config;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.xpath.*;
import org.w3c.dom.*;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;


/**
 * TO ENCRYPT:  Add to parameters
 *    encrypt --in config.xml --out config.enc.xml --pass MyStrongKey123
 * TO DECRYPT: Add to Parameters
 *    decrypt --in config.enc.xml --out config.dec.xml --pass MyStrongKey123
 * */
public class ConfigEncryptor {

    // --- Crypto params ---
    private static final int PBKDF2_ITERATIONS = 200_000; // defines how many iterations the PBKDF2 key derivation algorithm performs when turning your password (like MyStrongKey123) into a strong 256-bit AES key.
    private static final int KEY_BITS = 256;                 // requires JCE unlimited (default on modern JDKs)
    private static final int SALT_BYTES = 16;
    private static final int IV_BYTES = 12;                  // GCM recommended
    private static final int GCM_TAG_BITS = 128;             // 16 byte tag
    private static final SecureRandom RNG = new SecureRandom();

    // Payload format: v1:gcm:pbkdf2:<b64(salt)>:<b64(iv)>:<b64(ciphertext)>
    private static final String PREFIX = "v1:gcm:pbkdf2:";

    public static void main(String[] args) throws Exception {
        if (args.length < 6) {
            System.err.println("Usage: java ConfigEncryptor <encrypt|decrypt> --in <infile> --out <outfile> --pass <password>");
            System.exit(1);
        }
        String mode = args[0];
        String in = null, out = null, pass = null;

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--in":  in = args[++i]; break;
                case "--out": out = args[++i]; break;
                case "--pass": pass = args[++i]; break;
                default: /* ignore */ ;
            }
        }

        if (in == null || out == null || pass == null) {
            System.err.println("Missing required args. Example: encrypt --in config.xml --out config.enc.xml --pass \"secret\"");
            System.exit(2);
        }

        Document doc = readXml(in);
        if ("encrypt".equalsIgnoreCase(mode)) {
            transformValues(doc, pass, true);
        } else if ("decrypt".equalsIgnoreCase(mode)) {
            transformValues(doc, pass, false);
        } else {
            System.err.println("First arg must be 'encrypt' or 'decrypt'");
            System.exit(3);
        }
        writeXml(doc, out);
        System.out.println("Done → " + out);
    }

    /** Core transformation: encrypt nodes with mode="TEXT" or decrypt nodes with mode="ENCRYPTED" */
    private static void transformValues(Document doc, String password, boolean encrypt) throws Exception {
        XPath xPath = XPathFactory.newInstance().newXPath();
        // Select all elements that have a @mode attribute
        NodeList nodes = (NodeList) xPath.evaluate("//*[@mode]", doc, XPathConstants.NODESET);

        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            String mode = el.getAttribute("mode").trim();
            String value = textContentTrimmed(el);

            if (encrypt && "TEXT".equalsIgnoreCase(mode)) {
                if (!value.isEmpty()) {
                    String sealed = seal(password, value);
                    setText(el, sealed);
                    el.setAttribute("mode", "ENCRYPTED");
                }
            } else if (!encrypt && "ENCRYPTED".equalsIgnoreCase(mode)) {
                if (!value.isEmpty()) {
                    String opened = open(password, value);
                    setText(el, opened);
                    el.setAttribute("mode", "TEXT");
                }
            }
        }
    }

    private static String textContentTrimmed(Element el) {
        String s = el.getTextContent();
        return s == null ? "" : s.trim();
    }

    private static void setText(Element el, String newText) {
        // Replace all children with a single text node (keeps formatting simple & deterministic)
        while (el.hasChildNodes()) el.removeChild(el.getFirstChild());
        el.appendChild(el.getOwnerDocument().createTextNode(newText));
    }

    // --- XML IO ---
    private static Document readXml(String path) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();   // Configure XML parsing
        dbf.setNamespaceAware(false);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); // prevent XXE
        DocumentBuilder db = dbf.newDocumentBuilder();
        try (InputStream is = new FileInputStream(path)) {
            return db.parse(is);
        }
    }

    private static void writeXml(Document doc, String path) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t = tf.newTransformer();
        t.setOutputProperty("indent", "yes");
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        try (OutputStream os = new FileOutputStream(path)) {
            t.transform(new DOMSource(doc), new StreamResult(os));
        }
    }

    // --- Crypto helpers ---
    private static String seal(String password, String plaintext) throws Exception {
        byte[] salt = randomBytes(SALT_BYTES);
        SecretKey key = deriveKey(password, salt);

        byte[] iv = randomBytes(IV_BYTES);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        return PREFIX
                + b64(salt) + ":"
                + b64(iv)   + ":"
                + b64(ct);
    }

    private static String open(String password, String payload) throws Exception {
        if (!payload.startsWith(PREFIX)) throw new IllegalArgumentException("Unsupported payload format");
        String[] parts = payload.substring(PREFIX.length()).split(":");
        if (parts.length != 3) throw new IllegalArgumentException("Malformed payload");

        byte[] salt = b64d(parts[0]);
        byte[] iv   = b64d(parts[1]);
        byte[] ct   = b64d(parts[2]);

        SecretKey key = deriveKey(password, salt);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] pt = cipher.doFinal(ct);
        return new String(pt, StandardCharsets.UTF_8);
    }

    private static SecretKey deriveKey(String password, byte[] salt) throws Exception {
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_BITS);
        byte[] keyBytes = skf.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    private static byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        RNG.nextBytes(b);
        return b;
    }

    private static String b64(byte[] in) {
        return Base64.getEncoder().withoutPadding().encodeToString(in);
    }
    private static byte[] b64d(String s) {
        return Base64.getDecoder().decode(s);
    }
}
