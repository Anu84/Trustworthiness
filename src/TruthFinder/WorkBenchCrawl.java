/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package TruthFinder;

/**
 *
 * @author Asus
 */
import java.awt.*;
import java.io.*;
import java.net.URL;
import websphinx.*;
import rcm.awt.Constrain;
import rcm.awt.PopupDialog;
import rcm.awt.TabPanel;
import rcm.awt.BorderPanel;
import rcm.awt.ClosableFrame;
import websphinx.workbench.*;


public class WorkBenchCrawl extends Panel implements CrawlListener {

    Crawler crawler;
    String currentFilename = "";
    // panel wrappers
    Panel workbenchPanel; // contains menuPanel, configPanel, buttonPanel
    GridBagConstraints workbenchConstraints;
    WorkbenchVizPanel vizPanel; // contains graph, outline, and statistics
    GridBagConstraints vizConstraints;
    // GUI event listeners
    WebGraph graph;
    WebOutline outline;
    Statistics statistics;
    EventLog logger;
    // menu bar (for frame mode)
    MenuBar menubar;
    Menu fileMenu;
    MenuItem newCrawlerItem;
    MenuItem openCrawlerItem;
    MenuItem saveCrawlerItem;
    MenuItem createCrawlerItem;
    MenuItem exitItem;
    // menu panel (for container mode)
    Panel menuPanel;
    Button newCrawlerButton;
    Button openCrawlerButton;
    Button saveCrawlerButton;
    Button createCrawlerButton;
 WorkbenchTabPanel configPanel;
    Panel simplePanel;
    Panel crawlPanel;
    Panel limitsPanel;
    Panel classifiersPanel;
    Panel linksPanel;
    Panel actionPanel;
    CrawlerEditor2 crawlerEditor;
    ClassifierListEditor classifierListEditor;
    DownloadParametersEditor downloadParametersEditor;
    LinkPredicateEditor linkPredicateEditor;
    PagePredicateEditor pagePredicateEditor;
    ActionEditor2 actionEditor;
    SimpleCrawlerEditor2 simpleCrawlerEditor;
    boolean advancedMode = false;
    boolean tornOff = false;
    Button startButton, pauseButton, stopButton, clearButton;
    boolean allowExit;
    // Frames
 Panel workbenchFrame;
    Frame vizFrame;
    static final int MARGIN = 8;  // pixel border around configPanel

    public WorkBenchCrawl() {
        this(makeDefaultCrawler());
        return;
    }

    private static Crawler makeDefaultCrawler() {
        Crawler c = new Crawler();
        c.setDomain(Crawler.SUBTREE);
        return c;
    }

    public WorkBenchCrawl(String filename) throws Exception {
        //#ifdef JDK1.1
        this(loadCrawler(new FileInputStream(filename)));
        //#endif JDK1.1
        /*
         * #ifdef JDK1.0 throw new RuntimeException ("Crawler load/save not
         * supported under Java 1.0"); #endif JDK1.0
         */
    }

    public WorkBenchCrawl(URL url) throws Exception {
        //#ifdef JDK1.1
        this(loadCrawler(url.openStream())); // FIX: Netscape 4 refuses to load off local disk
        //#endif JDK1.1
        /*
         * #ifdef JDK1.0 throw new RuntimeException ("Crawler load/save not
         * supported under Java 1.0"); #endif JDK1.0
         */
    }

    public WorkBenchCrawl(Crawler _crawler) {
        Browser browser = Context.getBrowser();

        setLayout(new BorderLayout());
        setBackground(new java.awt.Color(240, 240, 240));

        setLayout(new GridLayout(2, 1));

        add(workbenchPanel = new Panel());
        workbenchPanel.setLayout(new GridBagLayout());

        // menu buttons panel
     
        configPanel = new WorkbenchTabPanel();
        Constrain.add(workbenchPanel, configPanel, Constrain.areaLike(0, 0));
        simplePanel = makeSimplePanel();
        crawlPanel = makeCrawlPanel();
        linksPanel = makeLinksPanel();
        actionPanel = makeActionPanel();
        classifiersPanel = makeClassifiersPanel();
        limitsPanel = makeLimitsPanel();

        // start/pause/stop button panel
        Constrain.add(workbenchPanel, makeButtonPanel(), Constrain.fieldLike(0, 1));

        // visualization panel
        add(vizPanel = new WorkbenchVizPanel(this));

        // graph visualization
        graph = new WebGraph();
        graph.setBackground(Color.white);
        if (browser != null) {
            graph.addLinkViewListener(browser);
        }
        vizPanel.addTabPanel("Graph", true, graph);

        // outline visualization
        outline = new WebOutline();
       // **********outline.setBackground(Color.white);
        if (browser != null) {
            outline.addLinkViewListener(browser);
        }
        vizPanel.addTabPanel("Outline", true, outline);

//        // statistics visualization
//        statistics = new Statistics();
//        Panel p = new Panel();
//        p.setLayout(new FlowLayout());
//        p.add(statistics);
//        vizPanel.addTabPanel("Statistics", true, p);

        // event logger (sends to System.err -- no GUI presence)
        logger = new EventLog();

        // now that the GUI is set up, we can initialize it with the
        // crawler
        setCrawler(_crawler);
    }

    public Panel makeFrame() {
        if (workbenchFrame == null) {
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            workbenchFrame = new Panel();
            workbenchFrame.setForeground(getForeground());
            workbenchFrame.setBackground(getBackground());
            workbenchFrame.setFont(getFont());
//            workbenchFrame.setTitle("Crawler Workbench: "
//                    + (crawler != null ? crawler.getName() : ""));
            workbenchFrame.setLayout(new GridLayout(1, 1));
            workbenchFrame.add(this);
//            workbenchPanel.remove(menuPanel);
//            workbenchFrame.setMenuBar(menubar);
            workbenchFrame.reshape(0, 0,
                    1003,
                    530);
        }
        return workbenchFrame;
    }

    public void setAllowExit(boolean yes) {
        allowExit = yes;
    }

    public boolean getAllowExit() {
        return allowExit;
    }

    public synchronized void setAdvancedMode(boolean adv) {
        if (advancedMode == adv) {
            return;
        }

        configureCrawler();  // write current mode's settings back to crawler
        advancedMode = adv;
        setCrawler(crawler); // read new mode's settings from crawler

        configPanel.advancedButton.setLabel(advancedMode
                ? "<< Simple" : ">> Advanced");
        validate();
    }

    public boolean getAdvancedMode() {
        return advancedMode;
    }

    /*
     * * * * * * * * * * * * * * * * * * * * * * *
     * GUI Construction * * * * * * * * * * * * * * * * * * * * *
     */
    static void setVisible(Component comp, boolean visible) {
        if (visible) {
            comp.show();
        } else {
            comp.hide();
        }
    }

    static void setEnabled(Component comp, boolean enabled) {
        if (enabled) {
            comp.enable();
        } else {
            comp.disable();
        }
    }

    static void setEnabled(MenuItem item, boolean enabled) {
        if (enabled) {
            item.enable();
        } else {
            item.disable();
        }
    }

   
    private Panel makeSimplePanel() {
        return simpleCrawlerEditor = new SimpleCrawlerEditor2();
    }

    // FIX: add onlyHyperLinks, synchronous, ignoreVisitedLinks
    private Panel makeCrawlPanel() {
        return crawlerEditor = new CrawlerEditor2();
    }

    private Panel makeLinksPanel() {
        Panel panel = new Panel();
        panel.setLayout(new GridBagLayout());

        Constrain.add(panel, new Label("Follow:"), Constrain.labelLike(0, 0));
        Constrain.add(panel, linkPredicateEditor = new LinkPredicateEditor(),
                Constrain.areaLike(1, 0));

        return panel;
    }

    private Panel makeActionPanel() {
        Panel panel = new Panel();
        panel.setLayout(new GridBagLayout());

        Constrain.add(panel, new Label("Action:"), Constrain.labelLike(0, 0));
        Constrain.add(panel, actionEditor = new ActionEditor2(), Constrain.areaLike(1, 0));

        Constrain.add(panel, new Label("on pages:"), Constrain.labelLike(0, 1));
        Constrain.add(panel, pagePredicateEditor = new PagePredicateEditor(),
                Constrain.areaLike(1, 1));
        return panel;
    }

    private Panel makeClassifiersPanel() {
        classifierListEditor = new ClassifierListEditor();
        return classifierListEditor;
    }

    private Panel makeLimitsPanel() {
        downloadParametersEditor = new DownloadParametersEditor();
        return downloadParametersEditor;
    }

    private Panel makeButtonPanel() {
        Panel panel = new Panel();
        panel.setLayout(new FlowLayout());

        panel.add(startButton = new Button("Start"));
        panel.add(pauseButton = new Button("Pause"));
        panel.add(stopButton = new Button("Stop"));
        panel.add(clearButton = new Button("Clear"));
        enableButtons(true, false, false, false);
        return panel;
    }

    String getCrawlerClassName(String label) {
        String className = label;
        if (className != null) {
            if (className.equals("Crawler")) {
                className = "websphinx.Crawler";
            } else if (className.equals("Load Class...")) {
                className = null;
            }
        }
        return className;
    }

    public boolean handleEvent(Event event) {
        if (doEvent(event)) {
            return true;
        } else {
            return super.handleEvent(event);
        }
    }

    boolean doEvent(Event event) {
        if (event.id == Event.ACTION_EVENT) {
            if (event.target instanceof MenuItem) {
                MenuItem item = (MenuItem) event.target;

                if (item == newCrawlerItem) {
                    newCrawler();
                } //#ifdef JDK1.1
                else if (item == openCrawlerItem) {
                    openCrawler();
                } else if (item == saveCrawlerItem) {
                    saveCrawler();
                } //#endif JDK1.1
                else if (item == createCrawlerItem) {
                    createCrawler(null);
                } else if (item == exitItem) {
                    close();
                } else {
                    return false;
                }
            } else if (event.target == newCrawlerButton) {
                newCrawler();
            } //#ifdef JDK1.1
            else if (event.target == openCrawlerButton) {
                openCrawler();
            } else if (event.target == saveCrawlerButton) {
                saveCrawler();
            } //#endif JDK1.1
            else if (event.target == createCrawlerButton) {
                createCrawler(null);
            } else if (event.target == configPanel.advancedButton) {
                setAdvancedMode(!advancedMode);
            }  else if (event.target == startButton) {
                start();
            } else if (event.target == pauseButton) {
                pause();
            } else if (event.target == stopButton) {
                stop();
            } else if (event.target == clearButton) {
                clear();
            } else {
                return false;
            }
        } else {
            return false;
        }

        return true;
    }

    /*
     * * * * * * * * * * * * * * * * * * * * * * *
     * Command handling * * * * * * * * * * * * * * * * * * * * *
     */
    protected void finalize() {
        // FIX: dispose of frames
    }

    void close() {
        if (!allowExit) {
            return;
        }

        // FIX: dispose of frames

        if (Context.isApplication()) {
            //#ifdef JDK1.1
            Runtime.runFinalizersOnExit(true);
            //#endif JDK1.1
            System.exit(0);
        }
    }

    public void refresh() {
        graph.updateClosure(crawler.getCrawledRoots());
        outline.updateClosure(crawler.getCrawledRoots());
    }

    void connectVisualization(Crawler crawler, Object viz, boolean linksToo) {
        if (viz instanceof CrawlListener) {
            crawler.addCrawlListener((CrawlListener) viz);
        }
        if (linksToo && viz instanceof LinkListener) {
            crawler.addLinkListener((LinkListener) viz);
        }
    }

    void disconnectVisualization(Crawler crawler, Object viz, boolean linksToo) {
        if (viz instanceof CrawlListener) {
            crawler.removeCrawlListener((CrawlListener) viz);
        }
        if (linksToo && viz instanceof LinkListener) {
            crawler.removeLinkListener((LinkListener) viz);
        }
    }

    void showVisualization(Object viz) {
        if (viz == graph) {
            graph.start();
        }
    }

    void hideVisualization(Object viz) {
        if (viz == graph) {
            graph.stop();
        }
    }

    void tearoffVisualizations() {
        if (tornOff) {
            return;
        }

        if (vizFrame == null) {
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            vizFrame = new WorkbenchVizFrame(this);
            vizFrame.setForeground(getForeground());
            vizFrame.setBackground(getBackground());
            vizFrame.setFont(getFont());
            vizFrame.setTitle("Visualization: "
                    + (crawler != null ? crawler.getName() : ""));
            vizFrame.setLayout(new GridLayout(1, 1));
            vizFrame.reshape(0, 0,
                    Math.min(550, screen.width),
                    screen.height / 2);
        }

        remove(vizPanel);
        setLayout(new GridLayout(1, 1));
        validate();

        vizFrame.add(vizPanel);
        setVisible(vizFrame, true);

        vizPanel.tearoffButton.setLabel("Glue Back");

        tornOff = true;
    }

    void dockVisualizations() {
        if (!tornOff) {
            return;
        }

        setVisible(vizFrame, false);
        vizFrame.remove(vizPanel);

        setLayout(new GridLayout(2, 1));
        add(vizPanel);
        validate();

        vizPanel.tearoffButton.setLabel("Tear Off");

        tornOff = false;
    }

    void newCrawler() {
        setCrawler(makeDefaultCrawler());
        currentFilename = "";
    }

    void createCrawler(String className) {
        if (className == null || className.length() == 0) {
            className = PopupDialog.ask(workbenchPanel,
                    "New Crawler",
                    "Create a Crawler of class:",
                    crawler.getClass().getName());
            if (className == null) {
                return;
            }
        }

        try {
            Class crawlerClass = (Class) Class.forName(className);
            Crawler newCrawler = (Crawler) crawlerClass.newInstance();

            setCrawler(newCrawler);
            currentFilename = "";
        } catch (Exception e) {
            PopupDialog.warn(workbenchPanel,
                    "Error",
                    e.toString());
            return;
        }
    }

    //#ifdef JDK1.1
    void openCrawler() {
        String fn = PopupDialog.askFilename(workbenchPanel, "Open Crawler", "", true);
        if (fn != null) {
            openCrawler(fn);
        }
    }

    void openCrawler(String filename) {
        try {
            setCrawler(loadCrawler(Access.getAccess().readFile(new File(filename))));
            currentFilename = filename;
        } catch (Exception e) {
            PopupDialog.warn(workbenchPanel,
                    "Error",
                    e.toString());
        }
    }

    void openCrawler(URL url) {
        try {
            setCrawler(loadCrawler(Access.getAccess().openConnection(url).getInputStream()));
            currentFilename = "";
        } catch (Exception e) {
            PopupDialog.warn(workbenchPanel,
                    "Error",
                    e.toString());
        }
    }

    static Crawler loadCrawler(InputStream stream) throws Exception {
        ObjectInputStream in = new ObjectInputStream(stream);
        Crawler loadedCrawler = (Crawler) in.readObject();
        in.close();
        return loadedCrawler;
    }

    void saveCrawler() {
        String fn = PopupDialog.askFilename(workbenchPanel, "Save Crawler As", currentFilename, true);
        if (fn != null) {
            saveCrawler(fn);
        }
    }

    void saveCrawler(String filename) {
        configureCrawler();

        try {
            ObjectOutputStream out =
                    new ObjectOutputStream(Access.getAccess().writeFile(new File(filename), false));
            out.writeObject((Object) crawler);
            out.close();

            currentFilename = filename;
        } catch (Exception e) {
            PopupDialog.warn(workbenchPanel,
                    "Error",
                    e.toString());
        }
    }
    //#endif JDK1.1

    void configureCrawler() {
        if (advancedMode) {
            crawlerEditor.getCrawler();
            classifierListEditor.getCrawler();
            crawler.setDownloadParameters(downloadParametersEditor.getDownloadParameters());
            if (advancedMode) {
                crawler.setLinkPredicate(linkPredicateEditor.getLinkPredicate());
                crawler.setPagePredicate(pagePredicateEditor.getPagePredicate());
                crawler.setAction(actionEditor.getAction());
            }
        } else {
            simpleCrawlerEditor.getCrawler();
        }
    }

    void enableButtons(boolean fStart, boolean fPause, boolean fStop, boolean fClear) {
        setEnabled(startButton, fStart);
        setEnabled(pauseButton, fPause);
        setEnabled(stopButton, fStop);
        setEnabled(clearButton, fClear);
    }

    /*
     * * * * * * * * * * * * * * * * * * * * * * *
     * Changing the crawler * * * * * * * * * * * * * * * * * * * * *
     */
    public void setCrawler(Crawler _crawler) {
        if (crawler != _crawler) {
            if (crawler != null) {
                clear();
                disconnectVisualization(crawler, this, false);
                disconnectVisualization(crawler, graph, true);
                disconnectVisualization(crawler, outline, true);
                disconnectVisualization(crawler, statistics, false);
                disconnectVisualization(crawler, logger, true);
            }

            connectVisualization(_crawler, this, false);
            connectVisualization(_crawler, graph, true);
            connectVisualization(_crawler, outline, true);
            connectVisualization(_crawler, statistics, false);
            connectVisualization(_crawler, logger, true);
        }

        crawler = _crawler;

        // set all window titles
        String name = crawler.getName();
        if (workbenchFrame != null) {
          //  workbenchFrame.setTitle("Crawler Workbench: " + name);
        }
        if (vizFrame != null) {
            vizFrame.setTitle("Visualization: " + name);
        }

        // set configuration

        if (advancedMode) {
            crawlerEditor.setCrawler(crawler);
            classifierListEditor.setCrawler(crawler);
            downloadParametersEditor.setDownloadParameters(crawler.getDownloadParameters());
            if (advancedMode) {
                linkPredicateEditor.setLinkPredicate(crawler.getLinkPredicate());
                pagePredicateEditor.setPagePredicate(crawler.getPagePredicate());
                actionEditor.setAction(crawler.getAction());
            }
        } else {
            simpleCrawlerEditor.setCrawler(crawler);
        }

        if (advancedMode) {
            showAdvancedTabs();
        } else {
            showSimpleTabs();
        }
    }

    public Crawler getCrawler() {
        return crawler;
    }

    private void showAdvancedTabs() {
        if (configPanel.countTabs() != 5) {
            configPanel.removeAllTabPanels();
            configPanel.addTabPanel("Crawl", true, crawlPanel);
            configPanel.addTabPanel("Links", true, linksPanel);
            configPanel.addTabPanel("Pages", true, actionPanel);
            configPanel.addTabPanel("Classifiers", true, classifiersPanel);
            configPanel.addTabPanel("Limits", true, limitsPanel);
        }
    }

    private void showSimpleTabs() {
        if (configPanel.countTabs() != 1) {
            configPanel.removeAllTabPanels();
            configPanel.addTabPanel("Truth Finder Web Crawler:", true, simplePanel);
        }
    }

    /*
     * * * * * * * * * * * * * * * * * * * * * * *
     * Running the crawler * * * * * * * * * * * * * * * * * * * * *
     */
    public void start() {
        configureCrawler();

        if (crawler.getState() == CrawlEvent.STOPPED) {
            crawler.clear();
        }

        Thread thread = new Thread(crawler, crawler.getName());
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        crawler.stop();
    }

    public void pause() {
        crawler.pause();
    }

    public void clear() {
        crawler.clear();
    }

    /**
     * Notify that the crawler started
     */
    public void started(CrawlEvent event) {
        enableButtons(false, true, true, false);
    }

    /**
     * Notify that the crawler ran out of links to crawl
     */
    public void stopped(CrawlEvent event) {
        enableButtons(true, false, false, true);
    }

    /**
     * Notify that the crawler's state was cleared.
     */
    public void cleared(CrawlEvent event) {
        enableButtons(true, false, false, false);
    }

    /**
     * Notify that the crawler timed out.
     */
    public void timedOut(CrawlEvent event) {
        enableButtons(true, false, false, true);
    }

    /**
     * Notify that the crawler was paused.
     */
    public void paused(CrawlEvent event) {
        enableButtons(true, false, true, true);
    }
//
//    public static void main(String[] args) throws Exception {
//        WorkBenchCrawl w = (args.length == 0)
//                ? new WorkBenchCrawl()
//                : new WorkBenchCrawl(args[0]);
//        w.setAllowExit(true);
//
//        Frame f = w.makeFrame();
//        f.show();
//    }
}
class WorkbenchFrame extends ClosableFrame {

   WorkBenchCrawl workbench;

    public WorkbenchFrame(WorkBenchCrawl workbench) {
        super();
        this.workbench = workbench;
    }

    public void close() {
        workbench.close();
    }

    public boolean handleEvent(Event event) {
        if (workbench.doEvent(event)) {
            return true;
        } else {
            return super.handleEvent(event);
        }
    }
}

class WorkbenchVizFrame extends ClosableFrame {

    WorkBenchCrawl workbench;

    public WorkbenchVizFrame(WorkBenchCrawl workbench) {
        super(true);
        this.workbench = workbench;
    }

    public void close() {
        workbench.dockVisualizations();
        super.close();
    }

    public boolean handleEvent(Event event) {
        if (workbench.doEvent(event)) {
            return true;
        } else {
            return super.handleEvent(event);
        }
    }
}

class WorkbenchTabPanel extends TabPanel {

    Button advancedButton;

    public WorkbenchTabPanel() {
        super();
        //add(advancedButton = new Button("Advanced >>"));
    }
}

class WorkbenchVizPanel extends TabPanel {

     WorkBenchCrawl workbench;
    Button optionsButton;
    Button tearoffButton;

    public WorkbenchVizPanel(WorkBenchCrawl workbench) {
        this.workbench = workbench;
//        add(optionsButton = new Button("Options..."));
//        add(tearoffButton = new Button("Tear Off"));
    }

    public void select(int num) {
        Component prior = getSelectedComponent();

        super.select(num);

        Component now = getSelectedComponent();

        if (prior == now) {
            return;
        }

        if (prior != null) {
            workbench.hideVisualization(prior);
        }

        if (now != null) {
            workbench.showVisualization(now);
            now.requestFocus();
        }
    }
}
