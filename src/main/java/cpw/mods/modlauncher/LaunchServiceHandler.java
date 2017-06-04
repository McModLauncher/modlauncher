package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.ILaunchHandlerService;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static cpw.mods.modlauncher.Logging.launcherLog;

/**
 * Identifies the launch target and dispatches to it
 */
public class LaunchServiceHandler
{
    private final ServiceLoader<ILaunchHandlerService> launchHandlerServices;
    private final Map<String, LaunchServiceHandlerDecorator> launchHandlerLookup;

    public LaunchServiceHandler()
    {
        launchHandlerServices = ServiceLoader.load(ILaunchHandlerService.class);
        launcherLog.info("Found launch services {}", () -> ServiceLoaderStreamUtils.toList(launchHandlerServices));
        launchHandlerLookup = StreamSupport.stream(launchHandlerServices.spliterator(), false)
                .collect(Collectors.toMap(ILaunchHandlerService::name, LaunchServiceHandlerDecorator::new));
    }

    public void launch(String target, String[] arguments, ClassLoader classLoader) {
        launchHandlerLookup.get(target).launch(arguments, classLoader);
    }

    public void launch(ArgumentHandler argumentHandler, TransformingClassLoader classLoader)
    {
        String launchTarget = argumentHandler.getLaunchTarget();
        String[] args = argumentHandler.buildArgumentList();
        launch(launchTarget, args, classLoader);
    }
}
