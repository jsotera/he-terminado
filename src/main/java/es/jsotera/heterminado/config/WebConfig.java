package es.jsotera.heterminado.config;

import freemarker.template.Configuration;

import io.javalin.config.JavalinConfig;
import io.javalin.http.staticfiles.StaticFileConfig;
import io.javalin.rendering.template.JavalinFreemarker;

import java.util.Map;
import java.util.function.Consumer;

/** Configura Javalin mediante una clase que implementa su interfaz de configuración. */
public class WebConfig implements Consumer<JavalinConfig> {
    @Override
    public void accept(JavalinConfig config) {
        Configuration templates = new Configuration(Configuration.VERSION_2_3_32);
        templates.setClassForTemplateLoading(WebConfig.class, "/");
        templates.setDefaultEncoding("UTF-8");

        config.staticFiles.add(new StaticResourcesConfig());
        config.fileRenderer(new JavalinFreemarker(templates));
        config.http.maxRequestSize = 16_384L;
        HttpsConfig.configure(config);
    }

    private static class StaticResourcesConfig implements Consumer<StaticFileConfig> {
        @Override
        public void accept(StaticFileConfig files) {
            files.directory = "/public";
            files.headers = Map.of("Cache-Control", "no-store");
            files.mimeTypes.add("application/manifest+json", "webmanifest");
        }
    }
}
