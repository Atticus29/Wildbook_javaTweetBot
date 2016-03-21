package org.ecocean.rest;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.ecocean.Global;
import org.ecocean.security.User;
import org.ecocean.security.UserService;
import org.ecocean.util.LogBuilder;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

import com.samsix.database.ConnectionInfo;
import com.samsix.util.reflect.SimpleMethodInvoker;

//
// TODO: How can this be configurable? I can't figure that out. Seems like it's not possible.
// Hard-coding our animalus search path here. Also pom.xml has hard-coded path to our extra jar
// file since I couldn't figure out how to make that configurable on the command-line at build time.
// There are some things about Spring I hate.
//
// same as @Configuration @EnableAutoConfiguration @ComponentScan
//@SpringBootApplication(scanBasePackages="org.ecocean.rest, com.animalus.wildbook.rest")
@SpringBootApplication(scanBasePackages="org.ecocean.rest")
public class RestApplication extends SpringBootServletInitializer {
    private static Logger logger = LoggerFactory.getLogger(RestApplication.class);

    /**
     *  This method should allow you to start up the rest service from a compiled jar rather
     *  than having to make a war and stick it in tomcat.
     */
    public static void main(final String[] args) {
        SpringApplication.run(RestApplication.class, args);
    }

    @Override
    protected final SpringApplicationBuilder configure(final SpringApplicationBuilder application) {
        return application.sources(RestApplication.class);
    }


    @Override
    public void onStartup(final ServletContext servletContext) throws ServletException
    {
        //
        // WARN: DO NOT REMOVE THIS LINE
        //
        super.onStartup(servletContext);

        //
        // TODO: I could not for the life of me get tomcat (on my mac dev machine anyway)
        // to read this file from anywhere despite the fact that I tried to put it in all the
        // places that are supposed to be on the classpath. So instead I had to resort
        // to loading it by direct file input.
        //
//      initResources = new ResourceReaderImpl(servletContext.getContextPath() + "_init");
        Path overridingProps = Paths.get(System.getProperty("catalina.base"),
                               "conf",
                               servletContext.getContextPath() + "_init.properties");
        Path overridingVars = Paths.get(System.getProperty("catalina.base"),
                              "conf",
                              servletContext.getContextPath() + "_vars.properties");

        if (logger.isDebugEnabled()) {
            new LogBuilder().appendVar("overridingProps", overridingProps)
                .appendVar("overridingVars", overridingVars).debug(logger);
        }
        Global.INST.initResources(overridingProps, overridingVars);

        //
        // Old code to initialize stormpath. If, for some reason we decide to make it
        // an optional user admin option, you will have to make it configurable to turn
        // it on but not required.
        //
//        try {
//            ResourceReaderImpl secrets = new ResourceReaderImpl("secrets");
//            Stormpath.init(secrets.getString("auth.stormpath.apikey.id"),
//                           secrets.getString("auth.stormpath.apikey.secret"),
//                           appResources.getString("auth.stormpath.appname", cust));
//        } catch (Throwable ex) {
//            logger.error("Trouble initializing stormpath.", ex);
//        }

        //
        // Uses default location of db/migration on classpath. You will find it in the src/main/resources folder.
        // OutOfOrder = true makes it so that if two developers create SQL in a different order and one somehow
        // gets applied to the database, the other's should too. This should only be an issue on development
        // databases and allows me to get other developers sql patches even if I've applied a newer one myself
        // locally. In production, everything should be fine.
        //
        ConnectionInfo connectionInfo = Global.INST.getConnectionInfo();
        Flyway flyway = new Flyway();
        flyway.setOutOfOrder(true);
        //
        // Turn off the validate on migrate. Complicated issue.
        // Sometimes I find that you want to rewrite the
        // patch *after* it has been applied. For instance, it might have worked on your test db,
        // but then you find out that it doesn't work on someone else's db or even on production.
        // But you want to make sure that any future dbs that it is run on will run correctly.
        // There are two options. One is to run flyway.repair() which "repairs" the checksums, the
        // other is to ignore the checksums. The first just seems like another way to ignore them
        // but people complain of getting errors using this. Since I can't see the added benefit
        // of "repairing" and since it is apparently error prone itself, I am choosing to ignore them
        // by setting ValidateOnMigrate to false.
        // UPDATE: I also need this to be false because if, for instance, I am refactoring code and
        // it is code that is used in one of the java migrations, then that class file has to be updated
        // to match the refactor. This causes a checksum issue.
        //
        flyway.setValidateOnMigrate(false);

        flyway.setSqlMigrationPrefix("");
        flyway.setDataSource(connectionInfo.getUrl(), connectionInfo.getUserName(), connectionInfo.getPassword());
        flyway.migrate();

        Global.INST.initDBQueries();

        //
        //check for and inject a default user 'tomcat' if none exists
        //
        UserService service = Global.INST.getUserService();
        if (service.getNumUsers() == 0) {
            User newUser = User.create("tomcat", "Tomcat User", "tomcat@localhost", "tomcat123");
            logger.warn("Creating tomcat user account since no user account exists...");
            service.saveUser(newUser);
            service.addRole(newUser.getId().toString(), "context0", "admin");
        }

        //
        // Create a property such as ...
        //
        //      system.onstart.init.class = org.myorg.MyGlobal
        // ... and add a static method called "init" (or the result of the other property below)
        // and put any special code in there. Such as special event handling or whatever.
        //
        try {
            String initClass = Global.INST.getAppResources().getString("system.onstart.init.class", null);
            if (initClass != null) {
                String initMethod = Global.INST.getAppResources().getString("system.onstart.init.method", "init");
                SimpleMethodInvoker.invokeStaticMethod(initClass, initMethod, null, null);
            }
        } catch (Throwable ex) {
            logger.error("Can't run special init class", ex);
        }
    }

    @Bean
    public ServletRegistrationBean servletRegistrationBean(){
        return new ServletRegistrationBean(new MediaUploadServlet(),"/mediaupload");
    }
//
//    @Bean
//    @Primary
//    public ObjectMapper objectMapper() {
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.registerModule(new JavaTimeModule());
//        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
////        mapper.configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS
//        logger.error("Hey, here I am!");
//        return mapper;
//    }
}