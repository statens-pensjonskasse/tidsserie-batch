package no.spk.pensjon.faktura.tidsserie.batch.main;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;

/**
 * @author Snorre E. Brekke - Computas
 */
public class TemplateConfigurationFactory {
    static{
        System.setProperty("org.freemarker.loggerLibrary", "SLF4J");
    }

    public static Configuration create() {
        Configuration config = new Configuration(Configuration.VERSION_2_3_22);
        config.setClassLoaderForTemplateLoading(Thread.currentThread().getContextClassLoader(), "templates");
        config.setDefaultEncoding("utf-8");
        config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        return config;
    }
}
