# ConfigEncryptor Project

## 📁 Project Structure

```
Configuration-SETUP-JAVA/
│
├── pom.xml                               # Maven build descriptor
├── README.md                             # This file
├── config.xml                            # Plaintext configuration file
├── config.enc.xml                        # Encrypted configuration file (generated)
├── config.dec.xml                        # Decrypted configuration file (generated)
│
├── src/
│   └── main/
│       ├── java/
│       │   └── org/
│       │       └── frostyte/
│       │           └── config/
│       │               ├── ConfigEncryptor.java      # Encryption/Decryption logic
│       │               ├── Configuration.java        # JAXB model for reading decrypted XML
│       │               └── KeyProvider.java          # Loads encryption key securely
│       └── resources/
│           └── logs/
```

---

## 🔐 Overview

The **ConfigEncryptor Project** is a standalone Java utility designed to:

1. Encrypt sensitive configuration values in XML files.
2. Decrypt those configurations at runtime.
3. Securely manage encryption keys through environment variables or external secret storage.
4. Deserialize the decrypted configuration using JAXB or XPath for runtime access.

---

## ⚙️ Workflow Summary

### 1️⃣ Encryption Phase

* Command:

  ```bash
  encrypt --in config.xml --out config.enc.xml --pass MyStrongKey123
  ```
* Reads each element in `config.xml` with `mode="TEXT"`.
* Uses **AES-256-GCM** with **PBKDF2-HMAC-SHA256** to encrypt its value.
* Converts each such element into:

  ```xml
  <password mode="ENCRYPTED">v1:gcm:pbkdf2:<salt>:<iv>:<ciphertext></password>
  ```
* Writes the encrypted version to `config.enc.xml`.

### 2️⃣ Decryption Phase

* Command:

  ```bash
  decrypt --in config.enc.xml --out config.dec.xml --pass MyStrongKey123
  ```
* Finds every `mode="ENCRYPTED"` element, reverses encryption, and restores plaintext.
* Outputs `config.dec.xml`.

### 3️⃣ Runtime Loading

* Application retrieves encryption key from an environment variable:

  ```bash
  set CONFIG_MASTER_KEY=MyStrongKey123
  ```
* On startup, the project uses `KeyProvider` to fetch the key and `ConfigEncryptor` to decrypt `config.enc.xml` into a temporary XML document in memory.
* The decrypted document is then parsed into a `Configuration` object using JAXB or queried via XPath.

---

## 🧩 Key Classes

### `ConfigEncryptor.java`

Responsible for parsing XML, encrypting and decrypting text nodes, and updating `mode` attributes. Uses AES-256-GCM and PBKDF2 key derivation for robust security.

### `Configuration.java`

A JAXB-mapped model class representing the XML structure, making it simple to load configuration objects post-decryption.

### `KeyProvider.java`

Handles key retrieval securely:

```java
package org.frostyte.config;

public class KeyProvider {
    public static String getEncryptionKey() {
        String key = System.getenv("CONFIG_MASTER_KEY");
        if (key == null || key.isEmpty()) {
            throw new IllegalStateException("CONFIG_MASTER_KEY environment variable not set.");
        }
        return key;
    }
}
```

---

## 🧠 Runtime Decryption + Configuration Load Example

```java
package org.frostyte.config;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class ConfigLoader {
    public static void main(String[] args) throws Exception {
        String key = KeyProvider.getEncryptionKey();
        Document decryptedDoc = ConfigEncryptor.decryptInMemory("config.enc.xml", key);

        // XPath usage example
        XPath xPath = XPathFactory.newInstance().newXPath();
        Node dbUser = (Node) xPath.evaluate("/configuration/dataSource/user", decryptedDoc, XPathConstants.NODE);
        System.out.println("Database User: " + dbUser.getTextContent());
    }
}
```

---

## 🔑 Key Management Best Practices

* Store `CONFIG_MASTER_KEY` in:

    * OS environment variables
    * Docker secrets
    * AWS Secrets Manager / Azure Key Vault / HashiCorp Vault
* Never commit or log the key.
* Rotate keys periodically and re-encrypt configuration files.

---

## 🚀 How to Run

### Encrypt Configuration

```
mvn compile exec:java -Dexec.mainClass="org.frostyte.config.ConfigEncryptor" -Dexec.args="encrypt --in config.xml --out config.enc.xml --pass $CONFIG_MASTER_KEY"
```

### Decrypt Configuration

```
mvn compile exec:java -Dexec.mainClass="org.frostyte.config.ConfigEncryptor" -Dexec.args="decrypt --in config.enc.xml --out config.dec.xml --pass $CONFIG_MASTER_KEY"
```

### Load Config at Runtime

```
java -cp target/classes org.frostyte.config.ConfigLoader
```

---

## 🧱 Summary

This project provides a clean, enterprise-grade configuration security model:

* 🔒 Encryption with AES-256-GCM.
* 🔑 Key-based decryption at runtime.
* 🧰 JAXB-mapped configuration parsing.
* ⚙️ Simple IntelliJ / Maven setup.

It can be dropped into any modern Java system that needs to **protect sensitive configuration data while keeping configuration management flexible and transparent.**
