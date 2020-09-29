package com.sun.squawk.translator;
import java.io.*;
import java.util.*;
import sunlabs.brazil.server.*;

public class BrazilHandler implements Handler {
    Server server;

    /**
     * The base prefix to form requests for this handler.
     */
    String prefix;

    /**
     * Prefix used to form "get suite for class name" requests.
     */
    static final String forNamePrefix = "/forName/";

    /**
     * Prefix used to stop the server
     */
    static final String terminatePrefix = "/terminate";

    /**
     * This is the options that are applied to each translation request.
     */
    String[] vmArgs;

    /**
     * The handler constructor called by the enclosing Brazil server object.
     */
    public boolean init(Server server, String prefix) {
        this.server = server;
        this.prefix = prefix;

        String options  = server.props.getProperty(prefix + "options", "").replace('+', ' ');

System.out.println("options='"+options+"'");

        // Convert the translator options into a String[], leaving space for the root class name of each request.
        StringTokenizer st = new StringTokenizer(options, " ");
        int size = st.countTokens();
        vmArgs = new String[size + 1];
        for (int i = 0; i != size; i++) {
            vmArgs[i] = st.nextToken();
        }
        log("initialized Translator" + prefix);
        return true;
    }

    /**
     * respond
     */
    public boolean respond(Request request) throws IOException {
        if (!request.method.equals("GET")) {
            log("Skipping request, only GET's allowed" + request.url);
            return false;
        }

        String requestUrl = request.url;
System.out.println("GET "+ requestUrl);

        if (requestUrl.startsWith(terminatePrefix)) {
            System.out.println("Server exiting...");
            System.exit(1);
        }
        if (requestUrl.startsWith(forNamePrefix)) {
            Hashtable queryData = request.getQueryData();
            String forceLoad = (String)queryData.get("forceLoad");
            requestUrl = requestUrl.substring(forNamePrefix.length());
            return service_forName(request, requestUrl);
        }
        log("Not my prefix: " + requestUrl);
        return false;
    }

    /**
     * Service a "forName" request.
     */
    public boolean service_forName(Request request, String name) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(5000);
            vmArgs[vmArgs.length - 1] = name;
            com.sun.squawk.translator.main.Main.main(vmArgs);
            baos.close();
            request.sendResponse(baos.toByteArray(), "text/plain");
        } catch (Exception ex) {
ex.printStackTrace();
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1000);
            ex.printStackTrace(new PrintStream(baos));
            try {
                baos.close();
                request.sendResponse("<p><hr><blockquote><pre>" + new String(baos.toByteArray()) + "</pre></blockquote><hr>", "text/html", 404);
            } catch (IOException ioe) {
                request.sendError(404, ioe.toString());
            }
        }
        return true;
    }

    /**
     * log
     */
    protected void log(String message) {
        //System.out.println("\n\n\n\n"+message+"\n\n\n\n");
        server.log(Server.LOG_INFORMATIONAL, prefix, message);
    }
}

