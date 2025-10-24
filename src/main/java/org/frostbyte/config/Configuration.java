package org.frostbyte.config;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Represents the structure of the configuration XML.
 *
 * This class maps directly to your config.xml file and can be
 * used to deserialize decrypted configuration data using JAXB.
 */
@XmlRootElement(name = "configuration")
public class Configuration {

    @XmlElement(name = "server")
    public Server server;

    @XmlElement(name = "dataSource")
    public DataSource dataSource;

    @XmlElement(name = "connectionPool")
    public ConnectionPool connectionPool;

    // --- Nested Classes ---

    @XmlRootElement(name = "server")
    public static class Server {
        @XmlElement public String host;
        @XmlElement public int port;
        @XmlElement public int ioThreads;
        @XmlElement public int workerThreads;
        @XmlElement public String basePath;
    }

    @XmlRootElement(name = "dataSource")
    public static class DataSource {
        @XmlElement public String driverClassName;
        @XmlElement public String jdbcUrl;
        @XmlElement public String user;
        @XmlElement public String password;
        @XmlElement public boolean encrypt;
        @XmlElement public boolean trustServerCertificate;
    }

    @XmlRootElement(name = "connectionPool")
    public static class ConnectionPool {
        @XmlElement public int maximumPoolSize;
        @XmlElement public int minimumIdle;
        @XmlElement public int idleTimeout;
        @XmlElement public long connectionTimeout;
        @XmlElement public long maxLifetime;
    }
}



/**
 *
 * ------USAGE--------
 import jakarta.xml.bind.JAXBContext;
 import jakarta.xml.bind.Unmarshaller;
 import java.io.File;

 Configuration config = JAXBContext.newInstance(Configuration.class)
         .createUnmarshaller()
         .unmarshal(new File("config.dec.xml"), Configuration.class)
         .getValue();

 System.out.println("DB URL: " + config.dataSource.jdbcUrl);

 * */
