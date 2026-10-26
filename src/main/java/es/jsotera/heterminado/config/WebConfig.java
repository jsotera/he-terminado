package es.jsotera.heterminado.config;

import freemarker.template.Configuration;

import io.javalin.config.JavalinConfig;
import io.javalin.rendering.template.JavalinFreemarker;

import java.util.function.Consumer;

/** Configura Javalin mediante una clase que implementa su interfaz de configuración. */
public class WebConfig implements Consumer<JavalinConfig> {
    @Override
    public void accept(JavalinConfig config) {
        Configuration templates = new Configuration(Configuration.VERSION_2_3_32);
        templates.setClassForTemplateLoading(WebConfig.class, "/");
        templates.setDefaultEncoding("UTF-8");

        config.staticFiles.add("/public");
        config.fileRenderer(new JavalinFreemarker(templates));
        config.http.maxRequestSize = 16_384L;
    }
}
