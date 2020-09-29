package com.sun.squawk.traceviewer;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.tree.*;
import javax.swing.text.*;
import javax.microedition.io.*;
import sunlabs.brazil.util.regexp.*;
import com.sun.squawk.io.connections.*;

/**
 * Show a tree based, foldable representation of a VM trace. A trace may include
 * trace lines and non-trace lines. Trace lines have the following format:
 *
 *   "*TRACE*" thread_id ":" level ":" method_id ":" preamble_size ":" relative_ip ":" stuff
 */
public class TraceViewer extends JFrame implements WindowListener, ComponentListener {


    static final int EXPANSION_LIMIT = 100;
    static final String FRAME_TITLE_PREFIX = "Squawk trace viewer";
    static final String TRACE_PREFIX = "*TRACE*";

    static MethodMap       map;

    /**
     * Regular expression matching a stack trace line. The captured groups are:
     *   1 - the fully qualified method name (e.g. "java.lang.Object.wait")
     *   2 - the source file name (e.g. "Object.java")
     *   3 - the source line number (e.g. "234")
     */
    static final String STACK_TRACE_RE = "([A-Za-z_][A-Za-z0-9_\\.\\$]*)\\((.*\\.java):([1-9][0-9]*)\\)";


    JTree                  threads;
    DefaultTreeModel       model;
    DefaultMutableTreeNode root;
    ClasspathConnection    sourcePath;
    Hashtable              sourceFiles;
    JScrollPane            sourceView;

    /**
     * Get the top level thread node for a given thread ID, creating it first if necessary.
     * @param threadID A thread's ID.
     */
    ThreadNode getThreadNode(String threadID, ThreadNode currentThread) {

        // Go through the thread's back backwards to get the last slice of
        // the thread.
        int count = root.getChildCount();
        ThreadNode thread = null;
        while (--count >= 0) {
            ThreadNode childThread = (ThreadNode)root.getChildAt(count);
            if (childThread.name.equals(threadID)) {
                thread = childThread;
                break;
            }
        }
        if (thread == null) {
            thread = new ThreadNode(threadID, 0);
            root.add(thread);
        } else if (thread != currentThread) {
            thread = thread.nextSlice();
            root.add(thread);
        }
        return thread;
    }

    /**
     * Set the source path.
     */
    void setSourcePath(String path) {
        if (path == null) {
            return;
        }
        try {
            path = path.replace(':', File.pathSeparatorChar);
            sourcePath = (ClasspathConnection)Connector.open("classpath://"+path);
            sourceFiles = new Hashtable();
        } catch (IOException ioe) {
            System.err.println("Couldn't open sourcepath:");
            ioe.printStackTrace();
        }
    }

    static String getMatch(int i, String str, int[] indices) {
        return str.substring(indices[i*2], indices[(i*2)+1]);
    }

    static int toInt(String s, String name) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException nfe) {
            BaseFunctions.fatalError("Bad number format for "+name);
        }
        return -1;
    }

    /**
     * Parse a trace file and build the corresponding tree.
     */
    void parseTraceFile(InputStream is, int truncateDepth, String match) {
        try {
            Regexp stackTraceRE     = new Regexp(STACK_TRACE_RE);
            int[] indices           = new int[20];
            BufferedReader br       = new BufferedReader(new InputStreamReader(is));
            ThreadNode thread       = getThreadNode("<startup>", null);
            int inputLineNumber     = 0;
            ThreadNode lastThread   = thread;
            TraceInfo lastTraceInfo = null;
            String line;

            while ((line = br.readLine()) != null) {
            try {
                inputLineNumber++;

                if ((inputLineNumber % 10000) == 0) {
                    System.err.println("Read " +inputLineNumber + " lines of trace input");
                }

                // Ignore blank lines
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }

                // Look for trace line prefix
                if (line.startsWith(TRACE_PREFIX)) {
                    TraceInfo trace;
                    String traceLine = line.substring(TRACE_PREFIX.length());
                    try {
                        // "*TRACE*" thread_id ":" indent_level ":" method_id ":" preamble_size ":" relative_ip ":" stuff
                        trace = new TraceInfo(traceLine);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        continue;
                    }

                    thread = getThreadNode(trace.threadId, lastThread);
                    boolean threadSwitch = (thread != lastThread);

                    // Is this a method entry ?
                    if (thread.currentMethod == null || thread.currentMethod.level < trace.level) {
                        thread.addMethodNode(new MethodNode(trace), true, truncateDepth);
                    // Is this a method exit ?
                    } else if (!threadSwitch) {
                        if (lastTraceInfo.level > trace.level) {
                            thread.addMethodNode(new MethodNode(trace), false, truncateDepth);
                        }
                    }

                    if (match == null || trace.trace.indexOf(match) != -1) {
                        thread.addInstructionNode(trace);
                    }

                    lastTraceInfo = trace;
                    lastThread    = thread;
                } else {
                    if (stackTraceRE.match(line, indices)) {
                        String name       = getMatch(1, line, indices);
                        String fileName   = getMatch(2, line, indices);
                        String lineNumber = getMatch(3, line, indices);

                        // Strip off class name and method name
                        try {
                            name = name.substring(0, name.lastIndexOf('.'));
                            name = name.substring(0, name.lastIndexOf('.'));

                            // Convert package name to path
                            name = name.replace('.', '/');
                            thread.addStackTraceNode(line, name+"/"+fileName, Integer.parseInt(lineNumber));
                            continue;
                        } catch (NumberFormatException nfe) {
                        } catch (StringIndexOutOfBoundsException se) {
                        }
                    }

                    boolean matched = false;
                    if (match == null) {
                        thread.addTraceLineNode(line);
                    } else if (line.indexOf(match) != -1) {
                        if (lastTraceInfo != null) {
                            lastTraceInfo.trace = line;
                            thread.addInstructionNode(lastTraceInfo);
                        } else {
                            thread.addTraceLineNode(line);
                        }
                    }
                }
            } catch (OutOfMemoryError ome) {
                threads = null;
                throw new RuntimeException("Ran out of memory after reading " + inputLineNumber + " lines");
            }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Find the line number for a given method's declaration. This method
     * builds a regular expression based on the given method's signature
     * and searches the source file for a match.
     */
    static void setMethodLineNo(JTextArea source, MethodNode method) {
        MethodMapEntry entry = TraceViewer.map.lookup(method.id);

        StringBuffer sb = new StringBuffer(100);
        sb.append(entry.name);
        sb.append("\\(");
        int argCount = entry.argCount;
        while (argCount != 0) {
            sb.append("[^,)]*");
            if (argCount != 1) {
                sb.append(',');
            }
            argCount--;
        }
        sb.append("\\)");

        String reString = sb.toString();
        try {
            Regexp re = new Regexp(reString);
            int lineCount = source.getLineCount();
            for (int i = 0; i != lineCount; i++) {
                try {
                    int start   = source.getLineStartOffset(i);
                    int length  = source.getLineEndOffset(i) - start;
                    if (length < 1) {
                        continue;
                    }
                    String line = source.getText(start, length);
                    if (re.match(line) != null) {
                        method.lineNumber = i + 1;
                        return;
                    }
                } catch (BadLocationException ble) {
                    break;
                }
            }
        } catch (IllegalArgumentException iae) {
        }

        method.lineNumber = 1;
    }


    /**
     * Get a JTextArea component holding the source file corresponding to a
     * given path.
     * @param path
     * @return
     */
    JTextArea getSourceFile(String path) {
        JTextArea text = (JTextArea)sourceFiles.get(path);
        if (text == null) {
            text = new JTextArea();
            text.setEditable(false);
            text.setFont(new Font("monospaced", Font.PLAIN, 12));
            try {
                InputStream is = sourcePath.openInputStream(path);
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                StringBuffer sb = new StringBuffer(is.available() * 2);
                String line = br.readLine();
                int lineNo = 1;
                while (line != null) {
                    sb.append(lineNo++);
                    sb.append(":\t");
                    sb.append(line);
                    sb.append("\n");
                    line = br.readLine();
                }
                text.setText(sb.toString());
                sourceFiles.put(path, text);
            } catch (IOException ioe) {
//                ioe.printStackTrace();
                text.setText("An exception occurred while reading "+path+":\n\t"+ioe);
                JViewport view = sourceView.getViewport();
                view.setView(text);
                return null;
            }
        }
        return text;
    }

    /**
     * Get the next node in a traversal of the tree from a given node.
     * @param node The node to start at.
     * @param backwards If true, get the node immediately preceding 'node'.
     * @param descend If true, then descend into nodes that have children.
     * @return the next node or null if at the end of a traversal.
     */
    DefaultMutableTreeNode getNext(DefaultMutableTreeNode node, boolean backwards, boolean descend) {
        if (backwards) {
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode)node.getParent();
            if (parent == null) {
                return null;
            } else {
                DefaultMutableTreeNode prev = (DefaultMutableTreeNode)parent.getChildBefore(node);
                if (prev == null) {
                    return parent;
                } else {
                    if (prev.isLeaf() || !descend) {
                        return prev;
                    } else {
                        while (!prev.isLeaf()) {
                            prev = (DefaultMutableTreeNode)prev.getLastChild();
                        }
                        return prev;
                    }
                }
            }
        } else {
            if (node.isLeaf() || !descend) {
                DefaultMutableTreeNode parent = (DefaultMutableTreeNode)node.getParent();
                if (parent == null) {
                    return null;
                } else {
                    DefaultMutableTreeNode next = (DefaultMutableTreeNode)parent.getChildAfter(node);
                    if (next == null) {
                        return getNext(parent, false, false);
                    } else {
                        return next;
                    }
                }
            } else {
                return (DefaultMutableTreeNode)node.getFirstChild();
            }
        }
    }

    /**
     * Search for a node based on some given search options. If found, the set
     * the found node to be the currently selected node and scroll to it if
     * necessary.
     * @param search
     */
    void search(SearchOptions search) {

        DefaultMutableTreeNode start = (DefaultMutableTreeNode)threads.getLastSelectedPathComponent();
        if (search.searchFromTop) {
            start = root;
        } else {
            start = getNext(start, search.backwards, true);
        }

        String find = search.text;
        if (!search.caseSensitive) {
            find = find.toUpperCase();
        }

        Regexp re = null;
        if (search.regex) {
            re = new Regexp(find);
        }

        while (start != null) {
            String text = (String)start.getUserObject();
            if (text != null) {
                if (!search.caseSensitive) {
                    text = text.toUpperCase();
                }
                if (search.regex) {
                    if (re.match(text) != null) {
                        break;
                    }
                } else {
                    if (text.indexOf(find) != -1) {
                        break;
                    }
                }
            }
            start = getNext(start, search.backwards, true);
        }

        if (start == null) {
            JOptionPane.showMessageDialog(null, "Finished searching trace");
        } else {
            TreePath path = new TreePath(start.getPath());
            threads.setSelectionPath(path);
            threads.scrollPathToVisible(path);
            threads.setSelectionPath(path);
        }
    }

    /**
     * Constructor.
     */
    TraceViewer(InputStream is, String sPath, int truncateDepth, String match) {

        setSourcePath(sPath);
        root    = new DefaultMutableTreeNode("All threads");
        model   = new DefaultTreeModel(root);

        threads = new JTree(model);
        threads.putClientProperty("JTree.lineStyle", "Angled");
        threads.setShowsRootHandles(true);
        threads.setCellRenderer(new TraceRenderer());
        threads.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        threads.setScrollsOnExpand(false);

        parseTraceFile(is, truncateDepth, match);

        // Create the listener that displays the source for a given line or method
        // when its corresponding tree node is selected
        if (sourceFiles != null) {

            threads.addTreeSelectionListener(new TreeSelectionListener() {

            public void valueChanged(TreeSelectionEvent e) {
                if (sourceView == null) {
                    return;
                }

                DefaultMutableTreeNode node = (DefaultMutableTreeNode)threads.getLastSelectedPathComponent();
                /*
                 * Source cannot be shown when:
                 *    i) there is no selected node
                 *   ii) the node does not map to a source file
                 */
                if (node == null || !(node instanceof SourcePathItem)) {
                    return;
                }

                MethodNode method = null;
                if (node instanceof MethodNode) {
                    method = (MethodNode)node;
                    // Don't show source for methods that are collapsed and have children.
                    if (method.nestedInstructionCount != 0 && threads.isCollapsed(new TreePath(node.getPath()))) {
                        return;
                    }
                }

                SourcePathItem item = (SourcePathItem)node;
                String path = item.getSourcePath();
                if (path == null) {
                    return;
                }

                // Update the frame title based on the source path
                JTextArea text = getSourceFile(path);
                if (text == null) {
                    setTitle(FRAME_TITLE_PREFIX + " - ??/" + path + ":" + item.getLineNumber());
                    return;
                }

                if (method != null && method.getLineNumber() == -1) {
                    setMethodLineNo(text, method);
                }

                JViewport view = sourceView.getViewport();
                view.setView(text);
                try {
                    final int lineNo   = item.getLineNumber();
                    final int startPos = text.getLineStartOffset(lineNo - 1);
                    final int endPos   = text.getLineEndOffset(lineNo - 1);
                    text.setCaretPosition(endPos);
                    text.moveCaretPosition(startPos);
                    text.getCaret().setSelectionVisible(true);

                    setTitle(FRAME_TITLE_PREFIX + " - " + path + ":" + lineNo);

                    final JTextArea textArea = text;

                    // Scroll so that the highlighted text is in the center
                    // if is not already visible on the screen. The delayed
                    // invocation is necessary as the view for the text
                    // area will not have been computed yet.
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            // Compute the desired area of the text to show.
                            Rectangle textScrollRect = new Rectangle();
                            textScrollRect.y = (lineNo - 1) * textArea.getFontMetrics(textArea.getFont()).getHeight();
                            Rectangle visible = textArea.getVisibleRect();

                            textScrollRect.height = visible.height;
                            textScrollRect.y -= (visible.height >> 1);

                            // Compute the upper and lower bounds of what
                            // is acceptable.
                            int upper = visible.y + (visible.height >> 2);
                            int lower = visible.y - (visible.height >> 1);

                            // See if we really should scroll the text area.
                            if ((textScrollRect.y < lower) ||
                                (textScrollRect.y > upper)) {
                                // Check that we're not scrolling past the
                                // end of the text.
                                int newbottom = textScrollRect.y +
                                    textScrollRect.height;
                                int textheight = textArea.getHeight();
                                if (newbottom > textheight) {
                                    textScrollRect.y -= (newbottom - textheight);
                                }
                                // Perform the text area scroll.
                                textArea.scrollRectToVisible(textScrollRect);
                            }
                        }
                    });

                } catch (BadLocationException ble) {
                } catch (IllegalArgumentException iae) {
                }
            }
        });
        }

        TreePath lastPointOfExecution = null;

        try {
            // Compute nested instruction counts
            for (Enumeration e = root.children(); e.hasMoreElements(); ) {
                ThreadNode thread = (ThreadNode)e.nextElement();
                if (thread.entryMethod != null) {
                    thread.entryMethod.computeNestedInstructionCount();
                }
            }

            // Expand the path to the first and last instruction in each thread
            for (Enumeration e = root.children(); e.hasMoreElements(); ) {
                ThreadNode thread = (ThreadNode)e.nextElement();

                DefaultMutableTreeNode first = thread.getFirstLeaf();
                if (first instanceof TraceLineNode) {
                    DefaultMutableTreeNode parent = (DefaultMutableTreeNode)first.getParent();
                    TreePath path = new TreePath(parent.getPath());
                    threads.expandPath(path);
                }

                if (thread.last != null) {
                    thread.executionPath.addElement(thread.last);
                }
                for (Enumeration p = thread.executionPath.elements(); p.hasMoreElements(); ) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode)p.nextElement();
                    if (node.isLeaf()) {
                        node = (DefaultMutableTreeNode)node.getParent();
                    }
                    if (node != null) {
                        lastPointOfExecution = new TreePath(node.getPath());
                        threads.expandPath(lastPointOfExecution);
                        threads.collapsePath(new TreePath(thread.getPath()));
                    }
                }
            }
        } catch (Exception e) {
            // Don't want this to prevent viewing of trace built up after a potentially long execution
            e.printStackTrace();
        }

        // If filtering is on, remove all method nodes that have no children
        if (match != null) {
            boolean atLeastOneRemoved = false;
            do {
                Vector toRemove = new Vector();
                for (Enumeration e = root.depthFirstEnumeration();
                     e.hasMoreElements(); ) {
                    Object obj = e.nextElement();
                    if (obj instanceof MethodNode) {
                        MethodNode method = (MethodNode)obj;
                        if (method.isLeaf()) {
                            toRemove.addElement(method);
                        }
                    }
                }
                atLeastOneRemoved = !toRemove.isEmpty();
                if (atLeastOneRemoved) {
                    for (Enumeration e = toRemove.elements(); e.hasMoreElements(); ) {
                        MethodNode method = (MethodNode)e.nextElement();
                        MutableTreeNode parent = (MutableTreeNode)(method.
                            getParent());
                        if (parent != null) {
                            model.removeNodeFromParent(method);
                        }
                    }
                }
            } while (atLeastOneRemoved);
        }

        // Add the listener that will cause a selection event when a method node is expanded.
        threads.addTreeExpansionListener(new TreeExpansionListener() {
            public void treeCollapsed(TreeExpansionEvent e) {}
            public void treeExpanded(TreeExpansionEvent e) {
                TreePath path = e.getPath();
                threads.setSelectionPath(path);
                threads.scrollPathToVisible(path);
            }
        });

        /*
         * Initialise the GUI
         */
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        // Put the tree in a scrollable pane
        JScrollPane treeScrollPane = new JScrollPane(threads);

        // Place holder until a node is selected
        JPanel noSourcePanel = new JPanel(new GridBagLayout());
        noSourcePanel.add(new JLabel("No source file selected/available"));
        sourceView = new JScrollPane(noSourcePanel);

        // Create search panel
        SearchPanel searchPanel = new SearchPanel(this);
        mainPanel.add("North", searchPanel);

        // Only create a source view panel if a valid source path was provided
        if (sourceFiles != null) {
            JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, treeScrollPane, sourceView);
            splitPane.setDividerLocation(500);
            mainPanel.add("Center", splitPane);
        }
        else {
            mainPanel.add("Center", treeScrollPane);
        }

        mainPanel.setPreferredSize(new Dimension(1275, 970));
        setTitle(FRAME_TITLE_PREFIX);
        getContentPane().add(mainPanel);
        addWindowListener(this);
        addComponentListener(this);
        validate();
        pack();

        if (lastPointOfExecution != null) {
            threads.expandPath(lastPointOfExecution);
            threads.setSelectionPath(lastPointOfExecution);
            threads.scrollPathToVisible(lastPointOfExecution);
        }

    }

    /**
     * WindowListener implementation.
     */
    public void windowClosing(WindowEvent e) {
        System.exit(0);
    }
    public void windowClosed(WindowEvent e) {}
    public void windowActivated(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowOpened(WindowEvent e) {}

    /**
     * ComponentListener implementation.
     */
    public void componentHidden(ComponentEvent e) {}
    public void componentMoved(ComponentEvent e) {}
    public void componentShown(ComponentEvent e) {}
    public void componentResized(ComponentEvent e) {
        validate();
    }

    /**
     * Print usage message.
     * @param errMsg An error message or null.
     */
    static void usage(String errMsg) {
        PrintStream out = System.out;
        if (errMsg != null) {
            out.println(errMsg);
        }
        out.println("Usage: TraceViewer [-options] [ tracefile ] ");
        out.println("where options include:");
        out.println("    -map <file>         map file containing method meta info");
        out.println("    -sp -sourcepath <path>  where to find source files");
        out.println("    -port <port>        port to open for receiving trace input");
        out.println("    -truncate <level>   truncate subtree of methods that completed to be <depth> deep.");
        out.println("                        The value -1 indicates no truncation (default = -1)");
        out.println("    -match <string>     filter for trace leaf nodes");
        out.println("    -help               show this message and exit");
        out.println();
        out.println("Either a tracefile must be provided or a -port option must be given");
        out.println();
    }

    /**
     * Command line entrance point.
     */
    public static void main(String[] args) {
        String sourcePath  = null;
        int port           = -1;
        String traceFile   = null;
        int truncateDepth  = -1;
        String mapFile     = null;
        String match       = null;

        int i = 0;
        for (; i < args.length ; i++) {
            if (args[i].charAt(0) != '-') {
                break;
            }
            String arg = args[i];
            if (arg.equals("-sourcepath") || arg.equals("-sp")) {
                sourcePath = args[++i];
            } else if (arg.equals("-map")) {
                mapFile = args[++i];
            } else if (arg.equals("-match")) {
                match = args[++i];
            } else if (arg.equals("-port")) {
                port = toInt(args[++i], " port number");
            } else if (arg.equals("-truncate")) {
                truncateDepth = toInt(args[++i], " truncation depth");
            } else {
                usage("Bad switch: "+arg);
                return;
            }
        }
        if (port == -1) {
            if (i >= args.length) {
                usage("Missing tracefile");
                return;
            } else {
                traceFile = args[i];
            }
        }

        InputStream is = null;
        try {

            if (mapFile != null) {
                map = new MethodMap(new FileInputStream(mapFile), mapFile);
            } else {
                map = new MethodMap();
            }

            if (port != -1) {
                StreamConnectionNotifier ssocket = (StreamConnectionNotifier)Connector.open("serversocket://:"+port);
                System.out.println("listening on port " + port + " for trace input...");
                is = ssocket.acceptAndOpen().openInputStream();
                System.out.println("reading trace...");
            }
            else {
                is = new FileInputStream(traceFile);
            }
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
            return;
        }

        TraceViewer instance = new TraceViewer(is, sourcePath, truncateDepth, match);
        instance.setVisible(true);

    }

}

/*---------------------------------------------------------------------------*\
 *                          Method map file classes                          *
\*---------------------------------------------------------------------------*/

class MethodMap extends BaseFunctions {

    MethodMapEntry[] methods;
    MethodMapEntry nullEntry;

    MethodMap() {
        nullEntry = new MethodMapEntry();
    }

    MethodMap(InputStream in, String mapFileName) {
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        nullEntry = new MethodMapEntry();
        try {
            String line = br.readLine();
            Vector entries = new Vector();
            while (line != null) {
                // Read map
                int id = Integer.parseInt(line);
                assume(id == entries.size());
                entries.addElement(new MethodMapEntry(id, br));
                line = br.readLine();
            }
            methods = new MethodMapEntry[entries.size()];
            entries.copyInto(methods);
        } catch (IOException ex) {
            ex.printStackTrace();
            fatalError("Error while parsing map file: "+mapFileName);
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
            fatalError("Error while parsing map file: "+mapFileName);
        }
    }

    Hashtable badMethodIds = new Hashtable();
    MethodMapEntry lookup(int methodId) {
        if (methods == null || methodId == -1) {
            return nullEntry;
        }
        if (methodId >= methods.length) {
            Integer id = new Integer(methodId);
            MethodMapEntry badEntry = (MethodMapEntry)badMethodIds.get(id);
            if (badEntry == null) {
                badMethodIds.put(id, badEntry = new MethodMapEntry(methodId));
            }
            return badEntry;
        }

//        assume(methods.length > methodId, "methodId="+methodId+" methods.length="+methods.length);
        return methods[methodId];
    }
}

class MethodMapEntry extends BaseFunctions {
    int       id;
    String    file;
    String    signature;
    private int[] startPCs;
    private int[] lineNumbers;
    String    name;
    int       argCount;

    MethodMapEntry() {
    }

    MethodMapEntry(int badId) {
        this.id = badId;
        this.name = this.signature = "<unknown method>";
    }

    MethodMapEntry(int id, BufferedReader br) throws IOException {
        this.id        = id;
        this.file      = br.readLine();
        this.signature = br.readLine();

        // Parse the signature to extract the (unqualified) name of the
        // method and its parameter count. Method signatures are assumed
        // to be fully qualified.
        int bracket = signature.indexOf('(');
        int start = signature.lastIndexOf('.', bracket);
        name = signature.substring(start+1, bracket);
        while (signature.charAt(++bracket) != ')') {
            if (signature.charAt(bracket) == ',') {
                argCount++;
            } else if (signature.charAt(bracket) != ' ' && argCount == 0) {
                argCount = 1;
            }
        }

        // Build a plausible path out of the signature if file is empty.
        if (file.length() == 0) {
            int space = signature.lastIndexOf(' ', bracket);
            file = signature.substring(space + 1, bracket);

            // Remove method name
            file = file.substring(0, file.lastIndexOf('.'));

            // Convert fully qualified class name to path
            file = file.replace('.', '/') + ".java";
        }

        // Parse the IP table
        String line = br.readLine();
        if (line.length() != 0) {
            StringTokenizer st = new StringTokenizer(line);
            int count = st.countTokens();
            assume((count % 2) == 0);
            count /= 2;
            startPCs = new int[count];
            lineNumbers = new int[count];
            for (int i = 0; i != count; i++) {
                startPCs[i] = Integer.parseInt(st.nextToken());
                lineNumbers[i] = Integer.parseInt(st.nextToken());
            }
        } else {
            startPCs = new int[0];
            lineNumbers = new int[0];
        }
    }

    /**
     * Get the source line number for a given instruction address.
     *
     * @param pc an instruction address
     * @return the source line number for 'pc' or -1 if there is no
     * source line number recorded for this address.
     */
    int getSourceLine(int pc) {
        int index = 0;
        int lineNumber = -1;
        for (int i = 0; i != startPCs.length; ++i) {
            int startPc = startPCs[i];
            if (pc < startPc) {
                break;
            }
            lineNumber = lineNumbers[i];
        }
        return lineNumber;
    }
}


/*---------------------------------------------------------------------------*\
 *                     TraceInfo class                                       *
\*---------------------------------------------------------------------------*/

class TraceInfo extends BaseFunctions {
    String  threadId;
    int     level;
    int     methodId;
    int     ip;
    String  trace;

    /**
     * Parse a trace line.
     * @param line a trace line with the "*TRACE*" prefix removed.
     */
    TraceInfo(String line) {
        StringTokenizer st = new StringTokenizer(line, ":");
//        try {
            threadId     = st.nextToken();
            level        = Integer.parseInt(st.nextToken());
            methodId     = Integer.parseInt(st.nextToken());
            ip           = Integer.parseInt(st.nextToken());
            trace        = expand(st.nextToken("").substring(1));
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
    }

    String expand(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0 ; i < s.length() ;) {
            int ch = s.charAt(i++);
            if (ch == '~') {
                ch = s.charAt(i++);
                if (ch != '{') {
                    sb.append('~');
                    sb.append((char)ch);
                } else {
                    int n = 0;
                    for (;;) {
                        ch = s.charAt(i++);
                        if (ch < '0' || ch > '9') {
                            break;
                        }
                        n = (n * 10) + (ch - '0');
                    }
                    while (sb.length() < n) {
                        sb.append(' ');
                    }
                }

            } else {
                sb.append((char)ch);
            }
        }
        return sb.toString();
    }
}


/*---------------------------------------------------------------------------*\
 *                     Tree node classes                                     *
\*---------------------------------------------------------------------------*/

interface SourcePathItem {
    String getSourcePath();
    int    getLineNumber();
}

/**
 * Represents a line of trace info.
 */
abstract class TraceLineNode extends DefaultMutableTreeNode {
    String label;
    TraceLineNode(String label) {
        setLabel(label);
    }
    void setLabel(String label) {
        this.label = label;
        setUserObject(label);
    }
}

class CommentNode extends TraceLineNode {
    CommentNode(String label) {
        super(label);
    }
}

class MethodNode extends TraceLineNode implements SourcePathItem {
    int    id;
    int    level;
    int    lineNumber = -1;
    int    nestedInstructionCount;
    InstructionNode firstInstruction;

    MethodNode(TraceInfo trace) {
        super(TraceViewer.map.lookup(trace.methodId).signature + " (id="+trace.methodId+")");
        this.level       = trace.level;
        this.id          = trace.methodId;
        this.lineNumber  = TraceViewer.map.lookup(id).getSourceLine(trace.ip < 0 ? 0 : trace.ip);
    }

    MethodNode(int level) {
        super("??");
        this.level   = level;
        this.id      = -1;
    }

    void updateFrom(MethodNode other) {
        this.id      = other.id;
        setLabel(TraceViewer.map.lookup(id).signature);
    }

    public String getSourcePath() {
        if (id != -1) {
            return TraceViewer.map.lookup(id).file;
        } else {
            return null;
        }
    }

    public int getLineNumber() {
        if (firstInstruction != null) {
            return firstInstruction.getLineNumber();
        }
        return lineNumber;
    }

    int computeNestedInstructionCount() {
        nestedInstructionCount = 0;
        for (Enumeration e = children(); e.hasMoreElements(); ) {
            Object child = e.nextElement();
            if (child instanceof InstructionNode) {
                nestedInstructionCount++;
            }
            else
            if (child instanceof MethodNode) {
                nestedInstructionCount += ((MethodNode)child).computeNestedInstructionCount();
            }
        }
        return nestedInstructionCount;
    }
}

class StackTraceNode extends CommentNode implements SourcePathItem {
    final String filePath;
    final int    lineNumber;
    StackTraceNode(String label, String filePath, int lineNumber) {
        super(label);
        this.filePath   = filePath;
        this.lineNumber = lineNumber;
    }
    public String getSourcePath() {
        return filePath;
    }
    public int getLineNumber() {
        return lineNumber;
    }
}

class InstructionNode extends TraceLineNode  implements SourcePathItem {

    int    methodId;
    int    ip;

    InstructionNode(TraceInfo trace, MethodNode parent) {
        super(fmt(trace.ip, trace.trace));
        this.ip       = trace.ip;
        this.methodId = trace.methodId;

        if (ip < 0) {
            ip = 0;
        }
        if (parent.firstInstruction == null) {
            parent.firstInstruction = this;
        }
    }

    static String fmt(int ip, String str) {
        StringBuffer sb = new StringBuffer();
        sb.append(""+ip);
        sb.append(": ");
        while (sb.length() < 5) {
             sb.append(" ");
        }
        sb.append(str);
        return sb.toString();
    }

    public String getSourcePath() {
        return TraceViewer.map.lookup(methodId).file;
    }

    public int getLineNumber() {
        return TraceViewer.map.lookup(methodId).getSourceLine(ip);
    }
}

class ThreadNode extends DefaultMutableTreeNode {
    MethodNode      entryMethod;
    MethodNode      currentMethod;
    String          name;
    String          sourceFile;
    TraceLineNode   last;
    Vector          executionPath = new Vector();
    int             slice;


    ThreadNode(String name, int slice) {
        this.slice = slice;
        this.name  = name;
        setUserObject("Thread-"+name+":"+slice);
    }

    ThreadNode nextSlice() {
        ThreadNode next = new ThreadNode(name, slice+1);

        TraceLineNode node = entryMethod;
        while (!node.isLeaf()) {
            MethodNode mnode = (MethodNode)node;
            MethodNode m = new MethodNode(mnode.level);
            m.updateFrom(mnode);
            next.addMethodNode(m, true, -1);
            node = (TraceLineNode)node.getLastChild();
        }

        return next;
    }

    void setEntryMethod(MethodNode node, boolean entering) {
        // Create dummy methods if necessary
        if (node.level != 0) {
            int level = 0;
            currentMethod = entryMethod = new MethodNode(level);
            while ((++level) != node.level) {
                MethodNode dummy = new MethodNode(level);
                currentMethod.add(dummy);
                currentMethod = dummy;
            }
            if (entering) {
                currentMethod.add(node);
                currentMethod = node;
            }
        }
        else {
            entryMethod = node;
            currentMethod = node;
        }
        add(entryMethod);
    }

    static void removeAllChildren(DefaultMutableTreeNode node, int depth) {
        if (depth == 0) {
            node.removeAllChildren();
            return;
        }
        for (Enumeration children = node.children(); children.hasMoreElements();) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode)children.nextElement();
            if (!child.isLeaf()) {
                removeAllChildren(child, depth - 1);
            }
        }
    }

    void addMethodNode(MethodNode node, boolean entering, int truncateDepth) {
        if (entering) {
            if (entryMethod == null) {
                setEntryMethod(node, true);
            }
            else {
                addChildNode(node);
                currentMethod = node;
            }
        }
        else {
            if (entryMethod == null || entryMethod.level > node.level) {
                setEntryMethod(node, false);
                return;
            }
            int leavingLevel = currentMethod.level;
            int currentLevel  = leavingLevel;
            MethodNode leavingMethod = currentMethod;
            while (currentLevel != node.level) {
                BaseFunctions.assume(currentLevel > 0);
                currentLevel--;
                currentMethod = (MethodNode)currentMethod.getParent();
            }
            if (leavingLevel != (node.level+1)) {
                CommentNode theThrow = new CommentNode("throw to exception handler in "+currentMethod.label);
                leavingMethod.add(theThrow);
                executionPath.addElement(theThrow);
            }
            else {
                if (currentMethod.id == -1) {
                    currentMethod.updateFrom(node);
                }
                else {
                    if (truncateDepth >= 0) {
                       removeAllChildren(leavingMethod, truncateDepth);
                    }
                }
            }
        }
        last = currentMethod;
    }

    void addInstructionNode(TraceInfo trace) {
        addChildNode(new InstructionNode(trace, currentMethod));
    }

    void addChildNode(TraceLineNode node) {
        if (currentMethod != null) {
            currentMethod.add(node);
        } else {
            this.add(node);
        }
        last = node;
    }

    void addStackTraceNode(String line, String filePath, int lineNumber) {
        addChildNode(new StackTraceNode(line, filePath, lineNumber));
    }
    void addTraceLineNode(String line) {
        addChildNode(new CommentNode(line));
    }
}

abstract class BaseFunctions {
    static void assume(boolean c) {
        if (!c) {
            fatalError("assume failure");
        }
    }
    static void assume(boolean c, String msg) {
        if (!c) {
            fatalError("assume failure: "+msg);
        }
    }

    static void fatalError(String msg) {
        throw new RuntimeException(msg);
    }
}

class TraceRenderer extends DefaultTreeCellRenderer {

    Font plain;
    Font italic;
    Font bold;
    Font fixed;

    Object v;

    TraceRenderer () {
        plain  = new Font(null, Font.PLAIN, 12);
        italic = plain.deriveFont(Font.ITALIC);
        bold   = plain.deriveFont(Font.BOLD);
        fixed  = new Font("monospaced", Font.PLAIN, 12);
    }
    public Component getTreeCellRendererComponent(
                        JTree tree,
                        Object value,
                        boolean sel,
                        boolean expanded,
                        boolean leaf,
                        int row,
                        boolean hasFocus) {

        super.getTreeCellRendererComponent(
                        tree, value, sel,
                        expanded, leaf, row,
                        hasFocus);
        if (value instanceof CommentNode) {
            setIcon(null);
            setFont(italic);
        } else if (value instanceof MethodNode) {
            setFont(bold);
            if (!expanded) {
                int nestedCount = ((MethodNode)value).nestedInstructionCount;
                setText("["+nestedCount+"] "+getText());
            }
        } else if (value instanceof InstructionNode) {
            setFont(fixed);
        } else {
            setFont(plain);
        }
        v = value;
        return this;
    }
}

class SearchOptions {
    final String text;
    final boolean searchFromTop;
    final boolean regex;
    final boolean caseSensitive;
    final boolean backwards;
    SearchOptions(String text, boolean searchFromTop, boolean regex, boolean caseSensitive, boolean backwards) {
        this.text          = text;
        this.searchFromTop = searchFromTop;
        this.regex         = regex;
        this.caseSensitive = caseSensitive;
        this.backwards     = backwards;
    }
}

class SearchPanel extends JPanel {

    JTextField textToFind;

    JCheckBox searchFromTop;
    JCheckBox regex;
    JCheckBox caseSensitive;
    JCheckBox backwards;

    JButton find;

    SearchPanel(final TraceViewer viewer) {

        FlowLayout layout = new FlowLayout(FlowLayout.LEADING);
        setLayout(layout);

        // Search text panel
        textToFind = new JTextField(30);
        add(new JLabel("Text to find: "));
        add(textToFind);

        // Options
        searchFromTop = new JCheckBox("Search from top of tree");
        regex         = new JCheckBox("Regular expression search");
        caseSensitive = new JCheckBox("Case sensitive");
        backwards     = new JCheckBox("Search backwards");

        add(searchFromTop);
        add(regex);
        add(caseSensitive);
        add(backwards);

        // Buttons
        find   = new JButton("Find");
        add(find);

        // Actions for buttons
        ActionListener buttonListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                viewer.search(new SearchOptions(
                        textToFind.getText(),
                        searchFromTop.isSelected(),
                        regex.isSelected(),
                        caseSensitive.isSelected(),
                        backwards.isSelected()));
            }
        };
        find.addActionListener(buttonListener);
    }

}
