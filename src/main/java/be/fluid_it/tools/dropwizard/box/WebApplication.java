package be.fluid_it.tools.dropwizard.box;

import be.fluid_it.tools.dropwizard.box.config.ClasspathConfigurationSourceProvider;
import be.fluid_it.tools.dropwizard.box.config.ConfigurationBridge;
import be.fluid_it.tools.dropwizard.box.config.BridgedConfigurationFactoryFactory;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import javax.servlet.*;

import org.eclipse.jetty.server.Server;

import java.util.LinkedList;
import java.util.List;

/**
 * A WebApplication decorates a (DropWizard) Application (GoF)
 * @param <C> Dropwizard Configuration class
 */
public abstract class WebApplication<C extends Configuration> extends Application<C> implements ServletContextListener, ServletContextAttributeListener {
    private static ServletContext theServletContext;
    private final List<Destroyable> destroyables = new LinkedList<Destroyable>();
    protected final Application<C> dropwizardApplication;
    protected final String[] args;
    private ConfigurationBridge configurationBridge;

    public static ServletContext servletContext() {
        return theServletContext;
    }

    public WebApplication(Application<C> dropwizardApplication, String configurationFileLocation) {
        this(dropwizardApplication, new String[]{"server", configurationFileLocation});
    }

    public WebApplication(Application<C> dropwizardApplication, String[] args) {
        this.dropwizardApplication = dropwizardApplication;
        this.args = args;
    }

    public void setConfigurationBridge(ConfigurationBridge configurationBridge) {
        this.configurationBridge = configurationBridge;
    }

    public ConfigurationBridge getConfigurationBridge() {
        return configurationBridge;
    }

    @Override
    public void initialize(Bootstrap<C> bootstrap) {
        if (configurationBridge != null) {
            bootstrap.setConfigurationFactoryFactory(new BridgedConfigurationFactoryFactory<C>(configurationBridge));
        }
        // Swaps the default FileConfigurationSourceProvider
        bootstrap.setConfigurationSourceProvider(new ClasspathConfigurationSourceProvider());
        dropwizardApplication.initialize(bootstrap);

    }

    @Override
    public String getName() {
        return dropwizardApplication.getName() + "-war";
    }

    @Override
    public void run(C configuration,
                    Environment environment) throws Exception {
        dropwizardApplication.run(configuration, environment);
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        if (theServletContext != null) {
            throw new IllegalStateException("Multple WebListeners extending WebApplication detected. Only one is allowed!");
        }
        theServletContext = sce.getServletContext();
        try {
            run(args);
        } catch (Exception e) {
            throw new RuntimeException("Initialization of Dropwizard failed ...", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        synchronized (destroyables) {
            for (Destroyable destroyable : destroyables) {
                destroyable.destroy();
            }
        }
        Server server = (Server)theServletContext.getAttribute("fakeJettyServer");
        if (server != null) {
            try {
                server.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        theServletContext = null;
    }

    @Override
    public void attributeAdded(ServletContextAttributeEvent event) {
        System.out.println(event.getName());
    }

    @Override
    public void attributeRemoved(ServletContextAttributeEvent event) {
        System.out.println(event.getName());
    }

    @Override
    public void attributeReplaced(ServletContextAttributeEvent event) {
        System.out.println(event.getName());
    }

    public void registerDestroyable(Destroyable destroyable) {
        synchronized (destroyables) {
            destroyables.add(destroyable);
        }
    }
}