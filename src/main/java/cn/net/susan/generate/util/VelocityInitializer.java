package cn.net.susan.generate.util;

import java.util.Properties;
import org.apache.velocity.app.Velocity;

public class VelocityInitializer {

    private static final String UTF8 = "UTF8";
    private static volatile boolean initialized = false;

    public static void initVelocity() {
        if (initialized) return;
        synchronized (VelocityInitializer.class) {
            if (initialized) return;
            Properties p = new Properties();
            try {
                p.setProperty("file.resource.loader.class",
                        "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
                p.setProperty(Velocity.ENCODING_DEFAULT, UTF8);
                p.setProperty(Velocity.OUTPUT_ENCODING, UTF8);
                Velocity.init(p);
                initialized = true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
