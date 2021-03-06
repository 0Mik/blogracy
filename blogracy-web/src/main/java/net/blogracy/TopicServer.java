package net.blogracy;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Logger;

import net.blogracy.config.Configurations;
import net.blogracy.controller.ActivitiesController;
import net.blogracy.controller.ChatController;
import net.blogracy.controller.ChatTopicController;
import net.blogracy.controller.DistributedHashTable;
import net.blogracy.controller.FileSharing;
import net.blogracy.controller.PeerTopicController;
import net.blogracy.model.users.User;
import net.blogracy.web.FileUpload;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;

public class TopicServer {

    static final DateFormat ISO_DATE_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'");

    static final int TOTAL_WAIT = 5 * 60 * 1000; // 5 minutes

    public static void main(String[] args) throws Exception {
    	ISO_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));

        int randomWait = (int) (TOTAL_WAIT * Math.random());
        Logger log = Logger.getLogger("net.blogracy.webserver");
        log.info("Topic server: waiting for " + (randomWait / 1000)
                + " secs before starting");
        Thread.currentThread().sleep(randomWait);

        String webDir = WebServer.class.getClassLoader().getResource("webapp")
                .toExternalForm();
        WebAppContext context = new WebAppContext();
        context.setResourceBase(webDir);
        // context.setDescriptor(webDir + "/WEB-INF/web.xml");
        // context.setContextPath("/");
        // context.setParentLoaderPriority(true);
        
        Server server = new Server(8181);
        server.setHandler(context);
        server.start();
        // server.join();
        
     	String firstArg = null;
    	if (args.length > 0) {
    	    try {
    	        firstArg = args[0];
    	    } catch(Exception e) {
    	        System.err.println("Argument must be entered by -DargumentA=channel_name");
    	        System.exit(1);
    	    }
    	}
    	PeerTopicController.getSingleton().setChannelListener(firstArg);
        PeerTopicController.getSingleton().sendingAggregatorInfo();
        
        
        
    }
}

