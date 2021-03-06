package nl.tudelft.watchdog.intellij.logic.ui.listeners.staticanalysis;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import nl.tudelft.watchdog.core.logic.ui.listeners.staticanalysis.StaticAnalysisMessageClassifier;
import nl.tudelft.watchdog.core.util.WatchDogLogger;
import org.infernus.idea.checkstyle.CheckStylePlugin;
import org.infernus.idea.checkstyle.CheckstyleClassLoader;
import org.infernus.idea.checkstyle.CheckstylePluginApi;
import org.infernus.idea.checkstyle.CheckstyleProjectService;
import org.infernus.idea.checkstyle.csapi.ConfigurationModule;
import org.infernus.idea.checkstyle.model.ConfigurationLocation;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static nl.tudelft.watchdog.core.logic.ui.listeners.staticanalysis.CheckStyleChecksMessagesFetcher.addCheckStyleMessagesToBundle;
import static nl.tudelft.watchdog.core.logic.ui.listeners.staticanalysis.CheckStyleChecksMessagesFetcher.addMessageToCheckstyleBundle;

/**
 * Component that is lazily initiated, only if the CheckStyle-IDEA plugin is actually available in the editor.
 * It loads the default configured messages from CheckStyle (in their `messages.properties` files) and
 * traverses the currently activated configuration for any custom messages.
 */
public class CheckStyleStartup implements ProjectComponent {

    private final Project project;

    public CheckStyleStartup(@NotNull final Project project) {
        this.project = project;
    }

    @Override
    public void initComponent() {
        try {
            // Older versions of the CheckstylePlugin do not have the API and we still have to classloader magic.
            // We know they will keep on working, but for newer versions we do want to use the API, because
            // that guarantees backwards compatibility
            try {
                initComponentWithCheckstylePluginAPI();
            } catch (Exception e) {
                CheckstyleProjectService service = ServiceManager.getService(this.project, CheckstyleProjectService.class);

                addCheckStyleMessagesToBundle(getPluginCreatedClassLoaderFromService(service));
                addMessagesForActiveConfiguration(service);
            }

            StaticAnalysisMessageClassifier.CHECKSTYLE_BUNDLE.sortList();
        } catch (Exception e) {
            WatchDogLogger.getInstance().logSevere("Could not initialize the CheckStyle plugin. This is likely an issue with an outdated version of the CheckStyle plugin: " + e);
        }
    }

    private void initComponentWithCheckstylePluginAPI() throws Exception {
        CheckstylePluginApi pluginApi = ServiceManager.getService(this.project, CheckstylePluginApi.class);

        addCheckStyleMessagesToBundle(pluginApi.currentCheckstyleClassLoader());
        pluginApi.visitCurrentConfiguration(this::addMessagesForActiveConfiguration);
    }

    // TODO (TimvdLippe): update the messages when the configuration changes
    private void addMessagesForActiveConfiguration(CheckstyleProjectService service) {
        final CheckStylePlugin checkStylePlugin = project.getComponent(CheckStylePlugin.class);
        final ConfigurationLocation activeConfigLocation = checkStylePlugin.configurationManager().getCurrent().getActiveLocation();

        if (activeConfigLocation == null) {
            return;
        }

        service.getCheckstyleInstance().peruseConfiguration(
                service.getCheckstyleInstance()
                        .loadConfiguration(activeConfigLocation, true, new HashMap<>()),
                module -> addMessagesForActiveConfiguration(activeConfigLocation.getDescription(), module)
        );
    }

    private void addMessagesForActiveConfiguration(String description, ConfigurationModule module) {
        String moduleKey = description + "." + module.getName() + ".";

        for (Map.Entry<String, String> message : module.getMessages().entrySet()) {
            addMessageToCheckstyleBundle(moduleKey + message.getKey(), message.getValue());
        }
    }

    // A bunch of reflection magic, as the plugin does not expose its internal classLoader
    private ClassLoader getPluginCreatedClassLoaderFromService(CheckstyleProjectService service) throws NoSuchFieldException, IllegalAccessException {
        Field checkstyleClassLoaderField = service.getClass().getDeclaredField("checkstyleClassLoader");
        checkstyleClassLoaderField.setAccessible(true);

        CheckstyleClassLoader loader = ((CheckstyleClassLoader) checkstyleClassLoaderField.get(service));

        Field classLoaderField = loader.getClass().getDeclaredField("classLoader");
        classLoaderField.setAccessible(true);

        return (ClassLoader) classLoaderField.get(loader);
    }

}
