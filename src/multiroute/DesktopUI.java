package multiroute;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JTabbedPane;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.RowFilter;
import javax.swing.ToolTipManager;
import javax.swing.table.TableRowSorter;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JFileChooser;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UIDefaults;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.plaf.FontUIResource;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Native desktop front end for the same durable Manager used by the local API. */
public final class DesktopUI {
    private static final String[] GATEWAYS = {"192.168.1.1", "192.168.1.2", "192.168.1.3", "192.168.1.4"};
    private static final String SELECTION_POLICY = "all-default-v1";
    private static final String DIRECTORY_POLICY = "current-user-downloads-v1";
    private static final String[] HEADERS = {"Файл", "Состояние", "Прогресс", "Скорость", "Прошло", "Осталось", "Шлюзы"};
    private final Main.Manager manager;
    private final Runnable onClose;
    private final ExecutorService background = Executors.newCachedThreadPool(task -> {
        Thread thread = new Thread(task, "desktop-ui-background");
        thread.setDaemon(true);
        return thread;
    });
    private final ExecutorService settingsWriter = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "desktop-settings-writer");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean refreshing = new AtomicBoolean();
    private final Path settingsPath;
    @FunctionalInterface interface GatewayInspector {
        Map<String,Object> inspect(String gateway, String url) throws Exception;
    }
    @FunctionalInterface interface NetworkConfigurator {
        void configure(String virtualIp, List<String> gateways) throws IOException;
    }
    @FunctionalInterface interface RouteSpeedTester {
        Map<String,Object> run(Map<String,Object> route, String url) throws Exception;
    }
    @FunctionalInterface interface NetworkSettingsProvider {
        Map<String,Object> settings();
    }
    @FunctionalInterface interface HubInspector {
        Map<String,Object> inspect(String url, Map<String,Object> route) throws Exception;
    }
    @FunctionalInterface interface HubFileSelector {
        List<String> choose(JFrame owner, Map<String,Object> inspection);
    }
    @FunctionalInterface interface TorrentInspector {
        Map<String,Object> inspect(String source, Map<String,Object> route) throws Exception;
    }
    @FunctionalInterface interface TorrentFileSelector {
        List<String> choose(JFrame owner, Map<String,Object> inspection);
    }
    enum DeleteChoice { CANCEL, KEEP_FILES, DELETE_FILES }
    @FunctionalInterface interface JobDeletionConfirmation {
        DeleteChoice confirm(JFrame owner, Map<String,Object> job);
    }
    @FunctionalInterface interface JobDirectorySelector {
        String choose(JFrame owner, String currentDirectory);
    }
    private final GatewayInspector gatewayInspector;
    private final NetworkConfigurator networkConfigurator;
    private final RouteSpeedTester routeSpeedTester;
    private final NetworkSettingsProvider networkSettingsProvider;
    private final HubInspector hubInspector;
    private final HubFileSelector hubFileSelector;
    private final TorrentInspector torrentInspector;
    private final TorrentFileSelector torrentFileSelector;
    private final JobDeletionConfirmation jobDeletionConfirmation;
    private final JobDirectorySelector jobDirectorySelector;
    private final Object networkConfigurationLock = new Object();
    private volatile NetworkInput configuredNetwork;
    private final Map<String,Map<String,Object>> jobs = new LinkedHashMap<>();
    private final DefaultTableModel jobsModel = new DefaultTableModel(HEADERS, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
        @Override public Class<?> getColumnClass(int column) {
            return column == 2 ? DownloadProgress.class : column == 6 ? Integer.class : Object.class;
        }
    };

    final JFrame frame = new JFrame("MultiRoute Resume " + Main.VERSION);
    final JTextField urlField = new JTextField(96);
    final JTextField filenameField = new JTextField(24);
    final JTextField directoryField = new JTextField(defaultDownloadDirectory().toString(), 24);
    final JTextField shaField = new JTextField(24);
    final JTextField md5Field = new JTextField(24);
    final JTextField sha512Field = new JTextField(24);
    final JCheckBox additionalChecksumsCheck = translatedCheckBox("Показать дополнительные эталоны", false);
    final JTextField virtualIpField = new JTextField("192.168.1.70", 15);
    final JComboBox<String> languageCombo = new JComboBox<>(new String[]{"ru", "en", "de", "es", "fr"});
    final JButton logButton = translatedButton("Журнал…");
    final JButton exitButton = translatedButton("Выйти из приложения");
    private final DesktopTray tray;
    JDialog logDialog;
    JTextArea logArea;
    private LogView logView;
    private Timer logTimer;
    private String lastLogSnapshot = "";
    private long lastLogRevision = -1;
    private String lastLogLanguage = "";
    private Runnable logLanguageListener;
    private String lastLoggedStatus = "";
    final JButton applyNetworkButton = translatedButton("Проверить и применить");
    final JTextArea networkStatusLabel = translatedArea("Адрес ещё не проверен", 2, 34);
    final JSpinner chunkSpinner = new JSpinner(new SpinnerNumberModel(20, 1, 64, 1));
    final JButton pasteButton = translatedButton("Вставить URL");
    final JButton torrentFileButton = translatedButton("Открыть torrent…");
    final JButton browseButton = translatedButton("Обзор…");
    final JButton createButton = translatedButton("Начать загрузку");
    final JButton allRoutesButton = translatedButton("Выбрать все шлюзы");
    final JButton onlyDefaultButton = translatedButton("Снять выбор");
    final JButton findGatewaysButton = translatedButton("Проверить шлюзы");
    final JButton allSpeedButton = translatedButton("Проверить скорости");
    final List<RouteRow> routes = new ArrayList<>();
    final JPanel gatewayRows = new JPanel();
    final JButton addGatewayButton = translatedButton("Добавить шлюз…");
    final JButton virtualIpHelpButton = translatedButton("?");
    final JButton bulkButton = translatedButton("Список ссылок…");
    final JButton queueButton = translatedButton("Очередь и ограничения…");
    final JButton sourceAccessButton = translatedButton("Доступ к источнику…");
    final JButton optionsButton = translatedButton("Параметры загрузки…");
    final JButton openFolderButton = translatedButton("Открыть папку");
    final JTextField searchField = new JTextField(22);
    final JComboBox<String> jobFilter = new JComboBox<>(new String[]{"all","active","failed","complete"});
    final JCheckBox graphCheck = translatedCheckBox("График скорости",false);
    final TableRowSorter<DefaultTableModel> jobsSorter = new TableRowSorter<>(jobsModel);
    final SpeedGraph speedGraph = new SpeedGraph();
    final JTabbedPane mainTabs = new JTabbedPane();
    private JPanel networkEditor;
    private boolean profileChangeRunning;
    private String graphJobId = "";
    final JTable jobsTable = new JTable(jobsModel) {
        @Override public boolean getScrollableTracksViewportWidth() {
            if (getParent() instanceof JViewport viewport) {
                int minimumWidth = 0;
                for (int column = 0; column < getColumnCount(); column++)
                    minimumWidth += getColumnModel().getColumn(column).getMinWidth();
                return viewport.getWidth() >= minimumWidth;
            }
            return super.getScrollableTracksViewportWidth();
        }
    };
    final JScrollPane jobsScroll = new JScrollPane(jobsTable);
    final JProgressBar progressBar = new JProgressBar(0, 1000);
    final JTextArea detailsArea = new JTextArea();
    final JButton pauseButton = translatedButton("Пауза");
    final JButton resumeButton = translatedButton("Продолжить");
    final JButton changeGatewaysButton = translatedButton("Изменить шлюзы…");
    final JButton changeDirectoryButton = translatedButton("Изменить папку…");
    final JButton removeButton = translatedButton("Удалить задание");
    final JLabel statusLabel = translatedLabel("Готово к работе");
    final JLabel selectionLabel = translatedLabel("Выберите загрузку");
    final JTextArea transferMessageArea = new JTextArea(2, 32);
    final JLabel totalSpeedLabel = translatedLabel("Общая скорость: —");
    final JLabel elapsedLabel = translatedLabel("Прошло: —");
    final JLabel etaLabel = translatedLabel("Осталось: —");
    final JTextArea gatewaySpeedsArea = new JTextArea(4, 25);
    private final Timer refreshTimer;
    private boolean restoring;
    private boolean discoveryRunning;
    private int discoveryGeneration;
    private boolean speedRunning;
    private boolean createRunning;
    private boolean jobActionRunning;
    private long jobsGeneration;
    private boolean updatingJobs;
    private volatile boolean closing;
    private int speedUrlGeneration;
    private String selectedId;

    /** Called by the controller on any thread; returns only after the window is visible. */
    public static void open(Main.Manager manager, Runnable onClose) throws Exception {
        if (manager == null || onClose == null) throw new IllegalArgumentException("Manager and close callback required");
        Runnable show = () -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) { }
            installReadableFonts();
            DesktopUI ui = new DesktopUI(manager, onClose);
            ui.frame.setVisible(true);
            ui.tray.install();
            ui.refreshJobs();
        };
        if (SwingUtilities.isEventDispatchThread()) show.run();
        else SwingUtilities.invokeAndWait(show);
    }

    DesktopUI(Main.Manager manager, Runnable onClose) {
        this(manager, onClose, true);
    }

    /** The third argument is only for a hidden, synthetic UI test. */
    DesktopUI(Main.Manager manager, Runnable onClose, boolean autoDiscover) {
        this(manager, onClose, autoDiscover, GatewayStatus::inspect, manager::configureNetwork,
                SpeedTest::run, GatewayNetwork::settings);
    }

    /** Package-only inspection seam for a hidden test with synthetic routes. */
    DesktopUI(Main.Manager manager, Runnable onClose, boolean autoDiscover, GatewayInspector gatewayInspector) {
        this(manager, onClose, autoDiscover, gatewayInspector, (virtualIp, gateways) -> { });
    }

    /** Network configuration seam used only by hidden tests with owned local data. */
    DesktopUI(Main.Manager manager, Runnable onClose, boolean autoDiscover, GatewayInspector gatewayInspector,
            NetworkConfigurator networkConfigurator) {
        this(manager, onClose, autoDiscover, gatewayInspector, networkConfigurator, SpeedTest::run);
    }

    /** Tests may inspect native route dispatch without touching the host network. */
    DesktopUI(Main.Manager manager, Runnable onClose, boolean autoDiscover, GatewayInspector gatewayInspector,
            NetworkConfigurator networkConfigurator, RouteSpeedTester routeSpeedTester) {
        this(manager, onClose, autoDiscover, gatewayInspector, networkConfigurator, routeSpeedTester, null);
    }

    /** An injected provider also permits hidden API/UI synchronization tests. */
    DesktopUI(Main.Manager manager, Runnable onClose, boolean autoDiscover, GatewayInspector gatewayInspector,
            NetworkConfigurator networkConfigurator, RouteSpeedTester routeSpeedTester,
            NetworkSettingsProvider networkSettingsProvider) {
        this(manager, onClose, autoDiscover, gatewayInspector, networkConfigurator, routeSpeedTester,
                networkSettingsProvider, HubSource::inspect, HubFileDialog::choose);
    }

    /** Owned hidden tests inject metadata and selection without opening any dialog. */
    DesktopUI(Main.Manager manager, Runnable onClose, boolean autoDiscover, GatewayInspector gatewayInspector,
            NetworkConfigurator networkConfigurator, RouteSpeedTester routeSpeedTester,
            NetworkSettingsProvider networkSettingsProvider, HubInspector hubInspector, HubFileSelector hubFileSelector) {
        this(manager, onClose, autoDiscover, gatewayInspector, networkConfigurator, routeSpeedTester,
                networkSettingsProvider, hubInspector, hubFileSelector, TorrentSource::inspect, TorrentFileDialog::choose);
    }

    /** Torrent fixtures use owned metadata and cancellation without peer or user file access. */
    DesktopUI(Main.Manager manager, Runnable onClose, boolean autoDiscover, GatewayInspector gatewayInspector,
            NetworkConfigurator networkConfigurator, RouteSpeedTester routeSpeedTester,
            NetworkSettingsProvider networkSettingsProvider, HubInspector hubInspector, HubFileSelector hubFileSelector,
            TorrentInspector torrentInspector, TorrentFileSelector torrentFileSelector) {
        this(manager, onClose, autoDiscover, gatewayInspector, networkConfigurator, routeSpeedTester,
                networkSettingsProvider, hubInspector, hubFileSelector, torrentInspector, torrentFileSelector,
                DesktopUI::confirmJobDeletion, DesktopUI::chooseJobDirectory);
    }

    /** Owned hidden fixtures replace only modal dialogs; Manager still performs the real operations. */
    DesktopUI(Main.Manager manager, Runnable onClose, boolean autoDiscover, GatewayInspector gatewayInspector,
            NetworkConfigurator networkConfigurator, RouteSpeedTester routeSpeedTester,
            NetworkSettingsProvider networkSettingsProvider, HubInspector hubInspector, HubFileSelector hubFileSelector,
            TorrentInspector torrentInspector, TorrentFileSelector torrentFileSelector,
            JobDeletionConfirmation jobDeletionConfirmation, JobDirectorySelector jobDirectorySelector) {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Create DesktopUI on the EDT");
        this.manager = manager;
        this.onClose = onClose;
        this.tray = new DesktopTray(frame, this::requestExit);
        this.gatewayInspector = gatewayInspector;
        this.networkConfigurator = networkConfigurator;
        this.routeSpeedTester = routeSpeedTester;
        this.networkSettingsProvider = networkSettingsProvider;
        this.hubInspector = hubInspector;
        this.hubFileSelector = hubFileSelector;
        this.torrentInspector = torrentInspector;
        this.torrentFileSelector = torrentFileSelector;
        this.jobDeletionConfirmation = jobDeletionConfirmation;
        this.jobDirectorySelector = jobDirectorySelector;
        this.settingsPath = manager.dataDir.resolve("desktop-settings.config");
        I18n.setLanguage(savedLanguage(settingsPath));
        I18n.installSwingDefaults();
        buildWindow();
        restoreSettings();
        restoring = true;
        try { languageCombo.setSelectedItem(I18n.language()); }
        finally { restoring = false; }
        refreshLanguage();
        ClipboardSupport.install(frame.getContentPane());
        ClipboardSupport.installUrl(urlField);
        ClipboardSupport.errors(urlField, this::showError);
        installControlTips();
        ToolTipManager.sharedInstance().setEnabled(true);
        ToolTipManager.sharedInstance().setInitialDelay(250);
        ToolTipManager.sharedInstance().setReshowDelay(200);
        ToolTipManager.sharedInstance().setDismissDelay(20000);
        installImports();
        if (networkSettingsProvider != null)
            configuredNetwork = networkSettingsInput(networkSettingsProvider.settings());
        refreshTimer = new Timer(900, event -> refreshJobs());
        refreshTimer.start();
        if (autoDiscover) findGateways();
    }

    private void requestWindowClose() {
        if (closing || tray.hideWindow()) return;
        // Linux desktops without tray integration still retain a reachable taskbar window.
        frame.setExtendedState(frame.getExtendedState() | JFrame.ICONIFIED);
        status("Системный трей недоступен. Окно свёрнуто, загрузки продолжаются. Для выхода нажмите «Выйти из приложения».");
    }

    private void requestExit() {
        if (closing) return;
        tray.close();
        frame.dispose();
    }

    private void buildWindow() {
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1080, 700));
        frame.setSize(1280, 820);
        frame.setLocationRelativeTo(null);
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { requestWindowClose(); }
            @Override public void windowClosed(WindowEvent event) {
                if (closing) return;
                closing = true;
                tray.close();
                discoveryGeneration++;
                speedUrlGeneration++;
                for (RouteRow row : routes) if (row != null) row.generation++;
                refreshTimer.stop();
                if (logDialog != null) logDialog.dispose();
                stopLogRefresh();
                saveSettings();
                background.shutdown();
                settingsWriter.shutdown();
                Thread closer = new Thread(() -> {
                    try { settingsWriter.awaitTermination(3, TimeUnit.SECONDS); }
                    catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
                    SwingUtilities.invokeLater(onClose);
                }, "desktop-settings-close");
                closer.setDaemon(true);
                closer.start();
            }
        });

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(14, 16, 12, 16));
        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("MultiRoute Resume");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 23f));
        JLabel subtitle = translatedLabel("Докачка через выбранные шлюзы · MD5, SHA-256, SHA-512");
        subtitle.setForeground(muted());
        JPanel brand = new JPanel();
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.add(title); brand.add(Box.createVerticalStrut(3)); brand.add(subtitle);
        header.add(brand, BorderLayout.WEST);
        JLabel mode = translatedLabel("Локальное приложение  •  Java 17");
        mode.setForeground(muted());
        JPanel preferences = new JPanel();
        preferences.setLayout(new BoxLayout(preferences, BoxLayout.Y_AXIS));
        mode.setAlignmentX(Component.RIGHT_ALIGNMENT);
        preferences.add(mode);
        preferences.add(Box.createVerticalStrut(5));
        JPanel languageLine = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        languageLine.setAlignmentX(Component.RIGHT_ALIGNMENT);
        languageLine.add(translatedLabel("Язык:"));
        languageCombo.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(javax.swing.JList<?> list,
                    Object value, int index, boolean selected, boolean focus) {
                return super.getListCellRendererComponent(list,
                        I18n.displayName(Main.str(value)), index, selected, focus);
            }
        });
        I18n.tooltip(languageCombo, "Язык интерфейса");
        languageLine.add(languageCombo);
        logButton.addActionListener(event -> openLog());
        languageLine.add(logButton);
        exitButton.addActionListener(event -> requestExit());
        tip(exitButton, "Завершить приложение и сохранить загрузки для продолжения. Крестик окна сворачивает приложение в трей.");
        languageLine.add(exitButton);
        preferences.add(languageLine);
        header.add(preferences, BorderLayout.EAST);
        languageCombo.addActionListener(event -> {
            if (restoring || closing) return;
            I18n.setLanguage(Main.str(languageCombo.getSelectedItem()));
            I18n.installSwingDefaults();
            refreshLanguage();
            saveSettings();
        });
        JPanel sourceBar = new JPanel(new BorderLayout(0, 5));
        sourceBar.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        sourceBar.add(translatedLabel("Ссылка HTTP/HTTPS, FTP/FTPS, SFTP, WebDAV, magnet или torrent"), BorderLayout.NORTH);
        JPanel urlLine = new JPanel(new BorderLayout(7, 0));
        urlField.setMinimumSize(new Dimension(140, urlField.getPreferredSize().height));
        urlField.setPreferredSize(new Dimension(900, urlField.getPreferredSize().height));
        I18n.tooltip(urlField, "Вставьте полную ссылку; длина не ограничена 128 символами");
        urlLine.add(urlField, BorderLayout.CENTER);
        JPanel urlActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        urlActions.add(pasteButton);
        createButton.setFont(createButton.getFont().deriveFont(Font.BOLD));
        createButton.addActionListener(event -> createJob());
        urlActions.add(createButton);
        urlLine.add(urlActions, BorderLayout.EAST);
        sourceBar.add(urlLine, BorderLayout.CENTER);
        header.add(sourceBar, BorderLayout.SOUTH);
        pasteButton.addActionListener(event -> pasteUrl());
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        root.add(header, BorderLayout.NORTH);

        JPanel newDownload = buildEditor();
        JSplitPane columns = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(newDownload), buildJobsPanel());
        columns.setResizeWeight(0.28);
        columns.setDividerLocation(355);
        columns.setBorder(null);
        mainTabs.addTab(I18n.t("Загрузки"), columns);
        JScrollPane networkScroll = new JScrollPane(networkEditor);
        mainTabs.addTab(I18n.t("Настройки сети"), networkScroll);
        AtomicBoolean firstNetworkView=new AtomicBoolean(true);
        mainTabs.addChangeListener(event->{if(mainTabs.getSelectedIndex()==1&&firstNetworkView.getAndSet(false))
            SwingUtilities.invokeLater(()->networkScroll.getViewport().setViewPosition(new java.awt.Point(0,0)));});
        root.add(mainTabs, BorderLayout.CENTER);

        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 4, 0, 0));
        statusLabel.setForeground(muted());
        root.add(statusLabel, BorderLayout.SOUTH);
        frame.setContentPane(root);
    }

    private JPanel buildEditor() {
        JPanel panel = new EditorPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(8, 4, 12, 17));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridwidth = 2; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(0, 0, 8, 0);
        int row = 0;
        section(panel, c, row++, "Новая загрузка", "Адрес файла и место сохранения");

        JPanel torrentLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        torrentLine.add(torrentFileButton);
        torrentLine.add(translatedLabel("   или вставьте magnet-ссылку"));
        add(panel, c, row++, torrentLine);
        torrentFileButton.addActionListener(event -> browseTorrentFile());

        label(panel, c, row++, "Имя файла HTTP (torrent: имена из раздачи)");
        add(panel, c, row++, filenameField);

        label(panel, c, row++, "Папка для файла");
        JPanel directoryLine = new JPanel(new BorderLayout(7, 0));
        directoryLine.add(directoryField, BorderLayout.CENTER);
        directoryLine.add(browseButton, BorderLayout.EAST);
        add(panel, c, row++, directoryLine);
        browseButton.addActionListener(event -> browseDirectory());

        label(panel, c, row++, "Ожидаемая контрольная сумма (необязательно)");
        add(panel, c, row++, translatedLabel("MD5 · SHA-256 · SHA-512"));
        add(panel, c, row++, shaField);
        note(panel, c, row++, "Тип определяется автоматически: MD5 — 32, SHA-256 — 64, SHA-512 — 128 шестнадцатеричных символов.");
        add(panel, c, row++, additionalChecksumsCheck);
        JPanel additionalChecksums = new JPanel(new GridBagLayout());
        GridBagConstraints checksumGrid = new GridBagConstraints();
        checksumGrid.gridx = 0; checksumGrid.weightx = 1; checksumGrid.fill = GridBagConstraints.HORIZONTAL;
        checksumGrid.anchor = GridBagConstraints.NORTHWEST; checksumGrid.insets = new Insets(0, 0, 8, 0);
        label(additionalChecksums, checksumGrid, 0, "Ожидаемая MD5 (необязательно)");
        add(additionalChecksums, checksumGrid, 1, md5Field);
        label(additionalChecksums, checksumGrid, 2, "Ожидаемая SHA-512 (необязательно)");
        add(additionalChecksums, checksumGrid, 3, sha512Field);
        additionalChecksums.setVisible(false);
        add(panel, c, row++, additionalChecksums);
        additionalChecksumsCheck.addActionListener(event -> {
            additionalChecksums.setVisible(additionalChecksumsCheck.isSelected());
            frame.getContentPane().revalidate(); frame.repaint();
        });
        note(panel, c, row++, "Файл: MD5, SHA-256, SHA-512 по доступным эталонам. Части BitTorrent проверяются отдельно и автоматически: v1 — SHA-1, v2 — SHA-256 Merkle из метаданных раздачи.");

        JPanel chunkLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JLabel chunkLabel = translatedLabel("Сегмент:  ");
        chunkLine.add(chunkLabel);
        chunkSpinner.setPreferredSize(new Dimension(76, chunkSpinner.getPreferredSize().height));
        chunkLine.add(chunkSpinner);
        chunkLine.add(translatedLabel("  МиБ  ·  от 1 до 64"));
        add(panel, c, row++, chunkLine);

        JPanel extras = new JPanel(new java.awt.GridLayout(0,1,0,5));
        extras.add(bulkButton); extras.add(sourceAccessButton); extras.add(queueButton);
        bulkButton.addActionListener(event -> importLinks(null));
        sourceAccessButton.addActionListener(event -> SourceAccessDialog.open(frame, urlField.getText().trim(), this::showError));
        queueButton.addActionListener(event -> openQueueSettings());
        add(panel, c, row++, extras);
        JPanel downloadPanel = panel;
        GridBagConstraints downloadFill=(GridBagConstraints)c.clone();
        downloadFill.gridy=row;downloadFill.weighty=1;downloadFill.fill=GridBagConstraints.BOTH;
        downloadPanel.add(Box.createVerticalGlue(),downloadFill);
        panel = new EditorPanel(); networkEditor = panel;
        panel.setBorder(BorderFactory.createEmptyBorder(10,12,15,15)); row=0;
        section(panel, c, row++, "Настройки сети", "Виртуальный адрес и ваши шлюзы");
        JPanel ipHeading = new JPanel(new BorderLayout());
        JLabel ipLabel = translatedLabel("Виртуальный IP приложения");
        ipHeading.add(ipLabel, BorderLayout.CENTER); ipHeading.add(virtualIpHelpButton, BorderLayout.EAST);
        tip(ipLabel, VIRTUAL_IP_HELP); tip(virtualIpHelpButton, VIRTUAL_IP_HELP);
        virtualIpHelpButton.addActionListener(event -> showVirtualIpHelp());
        add(panel, c, row++, ipHeading);
        add(panel, c, row++, virtualIpField);
        note(panel, c, row++, "Виртуальный адрес приложения; должен быть свободен в локальной сети.");
        add(panel, c, row++, applyNetworkButton);
        networkStatusLabel.setEditable(false);
        networkStatusLabel.setLineWrap(true);
        networkStatusLabel.setWrapStyleWord(true);
        networkStatusLabel.setOpaque(false);
        networkStatusLabel.setFont(UIManager.getFont("Label.font"));
        networkStatusLabel.setForeground(muted());
        add(panel, c, row++, networkStatusLabel);
        applyNetworkButton.addActionListener(event -> applyNetwork());
        virtualIpField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { virtualIpEdited(); }
            @Override public void removeUpdate(DocumentEvent event) { virtualIpEdited(); }
            @Override public void changedUpdate(DocumentEvent event) { virtualIpEdited(); }
        });
        virtualIpField.addActionListener(event -> applyNetwork());
        virtualIpField.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent event) { saveSettings(); }
        });
        add(panel, c, row++, findGatewaysButton);
        findGatewaysButton.addActionListener(event -> findGateways());
        note(panel, c, row++, "Адреса можно менять. Приложение проверит соединение через каждый шлюз.");
        gatewayRows.setLayout(new BoxLayout(gatewayRows, BoxLayout.Y_AXIS));
        for (String gateway : GATEWAYS) appendGatewayRow(gateway);
        addGatewayButton.addActionListener(event -> promptAddGateway());
        JPanel selections = new JPanel(new java.awt.GridLayout(1, 3, 5, 0));
        selections.add(addGatewayButton);
        selections.add(allRoutesButton);
        selections.add(onlyDefaultButton);
        add(panel, c, row++, selections);
        allRoutesButton.addActionListener(event -> {
            for (RouteRow route : routes) if (gatewayProblem(route) == null) route.checkBox.setSelected(true);
            updateSelectionStatus();
            saveSettings();
        });
        onlyDefaultButton.addActionListener(event -> {
            for (RouteRow route : routes) route.checkBox.setSelected(false);
            updateSelectionStatus();
            saveSettings();
        });

        add(panel, c, row++, allSpeedButton);
        allSpeedButton.addActionListener(event -> runSpeedTests(true, null));
        add(panel, c, row++, gatewayRows);
        note(panel, c, row++, "Только тест скорости: до 16 МиБ или 5 секунд.\nЗагрузка файлов этими пределами не ограничена.\nБез прямой HTTP-ссылки — тестовый файл Selectel.");
        urlField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { invalidateSpeedsForUrl(); }
            @Override public void removeUpdate(DocumentEvent event) { invalidateSpeedsForUrl(); }
            @Override public void changedUpdate(DocumentEvent event) { invalidateSpeedsForUrl(); }
        });

        note(panel, c, row++, "Перед загрузкой приложение проверит соединение через каждый выбранный шлюз.");

        c.gridy = row; c.weighty = 1; c.fill = GridBagConstraints.BOTH;
        panel.add(Box.createVerticalGlue(), c);
        return downloadPanel;
    }

    /** Keep the URL actions visible when the window or split pane becomes narrower. */
    private static final class EditorPanel extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;
        EditorPanel() { super(new GridBagLayout()); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(24, orientation == SwingConstants.VERTICAL ? visible.height - 24 : visible.width - 24);
        }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() {
            return getParent() != null && getPreferredSize().height < getParent().getHeight();
        }
    }

    private JPanel buildJobsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 16, 4, 3));
        JLabel title = translatedLabel("Загрузки");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        JPanel heading = new JPanel(new BorderLayout(8,5));
        heading.add(title, BorderLayout.NORTH);
        JPanel filters = new JPanel(new BorderLayout(8,0));
        filters.add(searchField, BorderLayout.CENTER);
        JPanel viewControls=new JPanel(new FlowLayout(FlowLayout.RIGHT,5,0));viewControls.add(graphCheck);viewControls.add(jobFilter);filters.add(viewControls, BorderLayout.EAST);
        speedGraph.setVisible(false);graphCheck.addActionListener(event->{speedGraph.setVisible(graphCheck.isSelected());frame.getContentPane().revalidate();});
        tip(graphCheck,"Показать историю общей скорости и скорости каждого используемого шлюза. График строится по полученным измерениям.");
        heading.add(filters, BorderLayout.SOUTH); panel.add(heading, BorderLayout.NORTH);
        tip(searchField, "Поиск загрузки по имени файла или ссылке.");
        jobFilter.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(javax.swing.JList<?> list,Object value,int index,boolean selected,boolean focus) {
                String source=switch(Main.str(value)){case "active"->"Активные";case "failed"->"Ошибки";case "complete"->"Завершённые";default->"Все загрузки";};
                return super.getListCellRendererComponent(list,I18n.t(source),index,selected,focus);
            }
        });
        searchField.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e){applyJobFilter();}public void removeUpdate(DocumentEvent e){applyJobFilter();}public void changedUpdate(DocumentEvent e){applyJobFilter();}});
        jobFilter.addActionListener(event->applyJobFilter());

        jobsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jobsTable.setRowHeight(Math.max(32, jobsTable.getRowHeight() + 9));
        jobsTable.setAutoCreateRowSorter(false);
        jobsTable.setRowSorter(jobsSorter);
        installJobMenu();
        jobsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        jobsTable.setFillsViewportHeight(true);
        DefaultTableCellRenderer plaintext = new DefaultTableCellRenderer();
        plaintext.putClientProperty("html.disable", Boolean.TRUE);
        jobsTable.setDefaultRenderer(Object.class, plaintext);
        jobsTable.setDefaultRenderer(DownloadProgress.class, new DownloadProgress.Renderer());
        int[] widths = {210, 110, 160, 110, 80, 95, 70};
        int[] minimumWidths = {150, 85, 110, 90, 70, 85, 60};
        for (int column = 0; column < widths.length; column++) {
            jobsTable.getColumnModel().getColumn(column).setMinWidth(minimumWidths[column]);
            jobsTable.getColumnModel().getColumn(column).setPreferredWidth(widths[column]);
        }
        tip(jobsScroll, "Прогресс каждой загрузки. Если размер неизвестен, показан объём полученных данных. Подробности доступны при выборе строки.");
        jobsTable.getSelectionModel().addListSelectionListener(event -> {
            if (!updatingJobs && !event.getValueIsAdjusting()) selectCurrentJob();
        });

        JPanel details = new JPanel(new BorderLayout(0, 8));
        details.setBorder(BorderFactory.createEmptyBorder(8, 3, 0, 3));
        JPanel detailTop = new JPanel(new BorderLayout(8, 8));
        selectionLabel.setFont(selectionLabel.getFont().deriveFont(Font.BOLD, 15f));
        detailTop.add(selectionLabel, BorderLayout.NORTH);
        progressBar.setStringPainted(true);
        I18n.text(progressBar, "Нет выбранной загрузки");
        JPanel transferStatus = new JPanel();
        transferStatus.setLayout(new BoxLayout(transferStatus, BoxLayout.Y_AXIS));
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        transferStatus.add(progressBar);
        transferMessageArea.setEditable(false);
        transferMessageArea.setOpaque(false);
        transferMessageArea.setLineWrap(true);
        transferMessageArea.setWrapStyleWord(true);
        transferMessageArea.setFont(UIManager.getFont("Label.font"));
        transferMessageArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        transferMessageArea.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                transferMessageArea.getPreferredSize().height));
        transferMessageArea.setVisible(false);
        transferStatus.add(transferMessageArea);
        JPanel metrics = new JPanel(new java.awt.GridLayout(3, 1, 0, 3));
        metrics.setAlignmentX(Component.LEFT_ALIGNMENT);
        totalSpeedLabel.setFont(totalSpeedLabel.getFont().deriveFont(Font.BOLD));
        metrics.add(totalSpeedLabel); metrics.add(elapsedLabel); metrics.add(etaLabel);
        transferStatus.add(Box.createVerticalStrut(7));
        JPanel metricsColumns=new JPanel(new BorderLayout(15,0));
        metricsColumns.setAlignmentX(Component.LEFT_ALIGNMENT);metricsColumns.add(metrics,BorderLayout.WEST);
        gatewaySpeedsArea.setEditable(false);
        gatewaySpeedsArea.setOpaque(false);
        gatewaySpeedsArea.setLineWrap(true);
        gatewaySpeedsArea.setWrapStyleWord(true);
        gatewaySpeedsArea.setFont(UIManager.getFont("Label.font"));
        gatewaySpeedsArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        transferStatus.add(Box.createVerticalStrut(7));
        JScrollPane gatewayMetrics = new JScrollPane(gatewaySpeedsArea);
        gatewayMetrics.setBorder(null); gatewayMetrics.setPreferredSize(new Dimension(400,75));
        metricsColumns.add(gatewayMetrics,BorderLayout.CENTER);
        metricsColumns.setMaximumSize(new Dimension(Integer.MAX_VALUE,85));
        transferStatus.add(metricsColumns);
        speedGraph.setAlignmentX(Component.LEFT_ALIGNMENT); transferStatus.add(speedGraph);
        detailTop.add(transferStatus, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.add(changeGatewaysButton); actions.add(pauseButton); actions.add(resumeButton);
        JPanel fileActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        fileActions.add(optionsButton); fileActions.add(openFolderButton); fileActions.add(changeDirectoryButton); fileActions.add(removeButton);
        optionsButton.addActionListener(event -> openJobOptions());
        openFolderButton.addActionListener(event -> openSelectedFolder());
        JPanel actionRows = new JPanel(new java.awt.GridLayout(2, 1, 0, 5));
        actionRows.add(actions); actionRows.add(fileActions);
        detailTop.add(actionRows, BorderLayout.SOUTH);
        details.add(detailTop, BorderLayout.NORTH);
        detailsArea.setEditable(false);
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        detailsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        detailsArea.setMargin(new Insets(9, 9, 9, 9));
        I18n.text(detailsArea, "После запуска здесь появятся сведения о шлюзах и проверке файла.");
        details.add(new JScrollPane(detailsArea), BorderLayout.CENTER);
        pauseButton.setEnabled(false); resumeButton.setEnabled(false); changeGatewaysButton.setEnabled(false);
        changeDirectoryButton.setEnabled(false); removeButton.setEnabled(false);
        pauseButton.addActionListener(event -> changeJob(false));
        resumeButton.addActionListener(event -> changeJob(true));
        changeGatewaysButton.addActionListener(event -> changeJobGateways());
        changeDirectoryButton.addActionListener(event -> changeJobDirectory());
        removeButton.addActionListener(event -> removeJob());

        int retryMessageHeight = transferMessageArea.getPreferredSize().height;
        details.setMinimumSize(new Dimension(200, detailTop.getPreferredSize().height
                + retryMessageHeight + details.getInsets().top + 12));
        jobsScroll.setMinimumSize(new Dimension(200, 120));
        jobsScroll.getVerticalScrollBar().setUnitIncrement(jobsTable.getRowHeight());
        JSplitPane rows = new JSplitPane(JSplitPane.VERTICAL_SPLIT, jobsScroll, details) {
            @Override public void doLayout() {
                // Preserve the action rows when a retry reason appears or the window gets shorter.
                int topLimit = getHeight() - getDividerSize() - details.getMinimumSize().height;
                if (getDividerLocation() > topLimit) setDividerLocation(Math.max(120, topLimit));
                super.doLayout();
            }
        };
        rows.setResizeWeight(0.4);
        rows.setDividerLocation(260);
        rows.setBorder(null);
        panel.add(rows, BorderLayout.CENTER);
        return panel;
    }

    private static Color muted() {
        Color color = UIManager.getColor("Label.disabledForeground");
        return color == null ? new Color(95, 103, 112) : color;
    }

    private static JLabel translatedLabel(String source) {
        JLabel label = new JLabel() {
            @Override public Dimension getMinimumSize() {
                return new Dimension(0, getPreferredSize().height);
            }
        };
        // User filenames and gateway diagnostics must never become Swing HTML markup.
        label.putClientProperty("html.disable", Boolean.TRUE);
        I18n.text(label, source);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JButton translatedButton(String source) {
        JButton button = new JButton();
        I18n.text(button, source);
        return button;
    }

    private static JCheckBox translatedCheckBox(String source, boolean selected) {
        JCheckBox checkBox = new JCheckBox("", selected);
        I18n.text(checkBox, source);
        return checkBox;
    }

    private static JTextArea translatedArea(String source, int rows, int columns) {
        JTextArea area = new JTextArea(rows, columns);
        area.setEditable(false);
        I18n.text(area, source);
        return area;
    }

    private static String savedLanguage(Path settingsPath) {
        try {
            if (Files.isRegularFile(settingsPath))
                return Main.str(Main.obj(Json.parse(Files.readString(settingsPath))).get("language"));
        } catch (Exception ignored) { }
        return I18n.DEFAULT_LANGUAGE;
    }

    /** Refresh only labels and snapshots; editing and running downloads retain their state. */
    private void refreshLanguage() {
        I18n.refresh(frame.getContentPane());
        if(jobsTable.getComponentPopupMenu()!=null) I18n.refresh(jobsTable.getComponentPopupMenu());
        if(mainTabs.getTabCount()>1){mainTabs.setTitleAt(0,I18n.t("Загрузки"));mainTabs.setTitleAt(1,I18n.t("Настройки сети"));}
        jobFilter.repaint(); speedGraph.repaint();
        fitEditorNotes(frame.getContentPane());
        for (RouteRow row : routes) if (row != null)
            row.reasonLabel.setRows(Math.max(2, (row.reasonLabel.getText().length() + 44) / 45));
        for (int column = 0; column < HEADERS.length; column++)
            jobsTable.getColumnModel().getColumn(column).setHeaderValue(I18n.t(HEADERS[column]));
        jobsTable.getTableHeader().repaint();
        updateJobs(new ArrayList<>(jobs.values()));
        frame.getContentPane().revalidate();
        frame.repaint();
        refreshLog();
    }

    /** The viewer renders structured application events in the active interface language. */
    private void openLog() {
        openLog(true);
    }

    /** Package-only hidden fixture uses the same viewer without displaying a window. */
    void openLog(boolean visible) {
        if (closing) return;
        if (logDialog != null && logDialog.isDisplayable()) {
            refreshLog(); logDialog.toFront(); return;
        }
        logDialog = new JDialog(frame, false);
        I18n.title(logDialog, "Журнал приложения");
        logDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        logDialog.setMinimumSize(new Dimension(620, 360));
        logDialog.setSize(860, 500);
        logDialog.setLocationRelativeTo(frame);
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        content.add(translatedLabel("Сообщения отображаются на выбранном языке интерфейса."), BorderLayout.NORTH);
        LogView view = new LogView();
        logView = view;
        logArea = view.text;
        content.add(view, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refresh = translatedButton("Обновить");
        tip(refresh, "Обновить сообщения в журнале.");
        refresh.addActionListener(event -> refreshLog());
        JButton close = translatedButton("Закрыть");
        tip(close, "Закрыть окно журнала; загрузки продолжатся.");
        close.addActionListener(event -> logDialog.dispose());
        actions.add(refresh); actions.add(close); content.add(actions, BorderLayout.SOUTH);
        logDialog.setContentPane(content);
        logDialog.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent event) {
                view.stop();
                if (event.getWindow() == logDialog) stopLogRefresh();
            }
        });
        logLanguageListener = this::refreshLog;
        I18n.addLanguageListener(logLanguageListener);
        logTimer = new Timer(900, event -> refreshLog());
        logTimer.start();
        lastLogSnapshot = "";
        lastLogRevision = -1;
        lastLogLanguage = "";
        refreshLog();
        if (visible) logDialog.setVisible(true);
        else logDialog.addNotify();
    }

    private void refreshLog() {
        if (logDialog == null || logArea == null || logTimer == null) return;
        long revision = AppLog.snapshotRevision();
        String language = I18n.language();
        if (revision == lastLogRevision && language.equals(lastLogLanguage)) return;
        String snapshot = AppLog.snapshotText();
        lastLogRevision = revision;
        lastLogLanguage = language;
        if (snapshot.isBlank()) snapshot = I18n.t("Записей пока нет");
        if (!snapshot.equals(lastLogSnapshot)) {
            logView.updateSnapshot(snapshot);
            lastLogSnapshot = snapshot;
        }
    }

    private void stopLogRefresh() {
        if (logView != null) logView.stop();
        if (logTimer != null) { logTimer.stop(); logTimer = null; }
        if (logLanguageListener != null) {
            I18n.removeLanguageListener(logLanguageListener); logLanguageListener = null;
        }
    }

    /** Only trusted static help text enters this fixed-width HTML tooltip wrapper. */
    private static void tip(javax.swing.JComponent component, String source) {
        I18n.tooltip(component, "<html><div style='width: 320px'>%s</div></html>", new UiText(source));
    }

    private static void diagnosticTip(javax.swing.JComponent component, UiText message) {
        I18n.tooltip(component, "<html><div style='width: 320px'>%s</div></html>", new Object() {
            @Override public String toString() {
                return message.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            }
        });
    }

    private void installControlTips() {
        tip(changeGatewaysButton, "Подключить или отключить шлюзы для выбранной загрузки. Скачанные части сохраняются; остальные шлюзы продолжают работу. Через 60 секунд без новых данных зависшая часть возвращается в общую очередь.");
        tip(urlField, "Вставьте ссылку HTTP/HTTPS, magnet или ссылку на torrent. Ctrl+V вставляет текст; на macOS также Cmd+V.");
        tip(pasteButton, "Вставить ссылку из буфера обмена и заменить текущее поле.");
        tip(createButton, "Загрузить файл через отмеченные шлюзы в указанную папку.");
        tip(directoryField, "Папка для готовых файлов и скачанных частей. Можно выбрать другую папку системным диалогом.");
        tip(browseButton, "Папка для готовых файлов и скачанных частей. Можно выбрать другую папку системным диалогом.");
        tip(filenameField, "Указать своё имя HTTP-файла. Имена файлов torrent сохраняются из раздачи.");
        tip(shaField, "Вставьте контрольную сумму из доверенного источника: MD5 — 32, SHA-256 — 64 или SHA-512 — 128 шестнадцатеричных символов. Тип определяется автоматически. Ctrl+V вставляет сумму; на macOS также Cmd+V. Эталоны источника также ищутся автоматически.");
        tip(additionalChecksumsCheck, "Показать поля для дополнительных эталонов MD5 и SHA-512. Введённые значения используются и после сворачивания раздела; очистите поле, чтобы убрать эталон. Все указанные эталоны должны совпасть с файлом.");
        tip(md5Field, "Необязательная MD5 из доверенного источника: 32 шестнадцатеричных символа. Ctrl+V вставляет сумму; на macOS также Cmd+V. Все указанные эталоны должны совпасть с файлом.");
        tip(sha512Field, "Необязательная SHA-512 из доверенного источника: 128 шестнадцатеричных символов. Ctrl+V вставляет сумму; на macOS также Cmd+V. Все указанные эталоны должны совпасть с файлом.");
        String segment = "Размер HTTP-части: от 1 до 64 МиБ, по умолчанию 20 МиБ. Шлюзы берут части из общей очереди; для torrent размер частей задан раздачей.";
        tip(chunkSpinner, segment);
        if (chunkSpinner.getEditor() instanceof JSpinner.DefaultEditor editor) tip(editor.getTextField(), segment);
        tip(virtualIpField, VIRTUAL_IP_HELP);
        tip(applyNetworkButton, "Проверить, свободен ли виртуальный адрес, и применить сетевые настройки приложения.");
        tip(findGatewaysButton, "Проверить подключение к источнику через каждый указанный шлюз.");
        for (RouteRow route : routes) {
            tip(route.checkBox, "Выбрать этот шлюз для загрузки. Быстрые шлюзы берут больше частей из общей очереди.");
            tip(route.gatewayField, "IPv4 адрес вашего маршрутизатора с выходом в интернет. По умолчанию доступны 192.168.1.1–192.168.1.4; адреса можно изменить.");
            tip(route.speedButton, "Короткий тест скорости этого шлюза: до 16 МиБ или 5 секунд. Размер и длительность обычных загрузок не ограничиваются этим тестом.");
        }
        tip(allSpeedButton, "Проверить скорости всех выбранных шлюзов по очереди.");
        tip(allRoutesButton, "Отметить все настроенные шлюзы для следующей загрузки.");
        tip(onlyDefaultButton, "Снять отметки со всех шлюзов.");
        tip(pauseButton, "Приостановить загрузку и сохранить уже проверенные части для докачки.");
        tip(resumeButton, "Продолжить выбранную загрузку с сохранённых частей или повторить после ошибки.");
        tip(changeDirectoryButton, "Изменить папку приостановленной загрузки и перенести сохранённые части.");
        tip(removeButton, "Удалить запись о загрузке. В окне подтверждения можно дополнительно выбрать удаление её файлов; эта галочка выключена по умолчанию.");
        tip(logButton, "Просмотреть сообщения приложения на выбранном языке интерфейса.");
        tip(languageCombo, "Изменить язык интерфейса и журнала без остановки загрузок.");
        tip(torrentFileButton, "Выбрать torrent-файл с вашего компьютера.");
    }

    private static void fitEditorNotes(Component component) {
        if (component instanceof JTextArea area && Boolean.TRUE.equals(area.getClientProperty("multiroute.note"))) {
            int lines = area.getText().lines().mapToInt(line -> Math.max(1, (line.length() + 47) / 48)).sum();
            area.setRows(Math.max(2, lines));
        }
        if (component instanceof java.awt.Container container)
            for (Component child : container.getComponents()) fitEditorNotes(child);
    }

    static void installReadableFonts() {
        UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        Enumeration<Object> keys = defaults.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = defaults.get(key);
            if (value instanceof FontUIResource font && font.getSize() < 14)
                defaults.put(key, new FontUIResource(font.deriveFont(14f)));
        }
    }

    private static void add(JPanel panel, GridBagConstraints c, int row, Component child) {
        c.gridy = row; c.insets = new Insets(0, 0, 9, 0);
        // The viewport owns the editor width. A long translated label must not force
        // GridBagLayout to center an oversized minimum grid at a negative x position.
        JPanel cell = new JPanel(new BorderLayout()) {
            @Override public Dimension getMinimumSize() {
                return new Dimension(0, getPreferredSize().height);
            }
        };
        cell.add(child, BorderLayout.CENTER);
        panel.add(cell, c);
    }

    private static void label(JPanel panel, GridBagConstraints c, int row, String text) {
        JLabel label = translatedLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        add(panel, c, row, label);
    }

    private static void note(JPanel panel, GridBagConstraints c, int row, String text) {
        JTextArea label = translatedArea(text, Math.max(2, (int) text.lines().count()), 34);
        label.putClientProperty("multiroute.note", Boolean.TRUE);
        label.setEditable(false);
        label.setLineWrap(true);
        label.setWrapStyleWord(true);
        label.setOpaque(false);
        label.setFont(UIManager.getFont("Label.font"));
        label.setForeground(muted());
        add(panel, c, row, label);
    }

    private static void section(JPanel panel, GridBagConstraints c, int row, String heading, String subheading) {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        JLabel title = translatedLabel(heading);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        box.add(title); box.add(Box.createVerticalStrut(2));
        JLabel subtitle = translatedLabel(subheading);
        subtitle.setForeground(muted());
        box.add(subtitle);
        box.setBorder(BorderFactory.createEmptyBorder(row == 0 ? 0 : 11, 0, 0, 0));
        add(panel, c, row, box);
    }

    private void pasteUrl() {
        ClipboardSupport.paste(urlField, true);
    }

    private void browseDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setLocale(I18n.locale());
        ClipboardSupport.install(chooser);
        chooser.setDialogTitle(I18n.t("Выберите папку для загрузки"));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        String current = directoryField.getText().trim();
        try {
            if (!current.isEmpty() && Files.isDirectory(Path.of(current)))
                chooser.setCurrentDirectory(Path.of(current).toFile());
        } catch (RuntimeException ignored) { }
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
            directoryField.setText(chooser.getSelectedFile().getAbsolutePath());
    }

    record DeletionPrompt(JPanel content, JCheckBox deleteFiles) { }

    static DeletionPrompt deletionPrompt(Map<String,Object> job) {
        String message = I18n.t("Убрать загрузку «%s» из приложения?\nПапка: %s\n\nБез галочки готовые файлы и скачанные части останутся на диске.\nС галочкой будут удалены только файлы и части этой загрузки.", Main.str(job.get("filename")), Main.str(job.get("directory")));
        JTextArea explanation = new JTextArea(message, 6, 58);
        explanation.setEditable(false); explanation.setOpaque(false);
        explanation.setLineWrap(true); explanation.setWrapStyleWord(true);
        explanation.setFont(UIManager.getFont("Label.font"));
        JCheckBox deleteFiles = translatedCheckBox("Удалить также файлы и скачанные части", false);
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.add(explanation, BorderLayout.CENTER); content.add(deleteFiles, BorderLayout.SOUTH);
        return new DeletionPrompt(content, deleteFiles);
    }

    private static DeleteChoice confirmJobDeletion(JFrame owner, Map<String,Object> job) {
        DeletionPrompt prompt = deletionPrompt(job);
        int answer = JOptionPane.showConfirmDialog(owner, prompt.content(), I18n.t("Удаление загрузки"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        return answer == JOptionPane.OK_OPTION
                ? (prompt.deleteFiles().isSelected() ? DeleteChoice.DELETE_FILES : DeleteChoice.KEEP_FILES) : DeleteChoice.CANCEL;
    }

    private static String chooseJobDirectory(JFrame owner, String currentDirectory) {
        JFileChooser chooser = new JFileChooser();
        chooser.setLocale(I18n.locale());
        ClipboardSupport.install(chooser);
        chooser.setDialogTitle(I18n.t("Выберите новую папку для загрузки"));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        try {
            if (!currentDirectory.isBlank() && Files.isDirectory(Path.of(currentDirectory)))
                chooser.setCurrentDirectory(Path.of(currentDirectory).toFile());
        } catch (RuntimeException ignored) { }
        return chooser.showOpenDialog(owner) == JFileChooser.APPROVE_OPTION
                ? chooser.getSelectedFile().getAbsolutePath() : null;
    }

    private void browseTorrentFile() {
        if (closing || createRunning) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setLocale(I18n.locale());
        ClipboardSupport.install(chooser);
        chooser.setDialogTitle(I18n.t("Откройте файл torrent"));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new FileNameExtensionFilter(I18n.t("Раздача BitTorrent (*.torrent)"), "torrent"));
        chooser.setAcceptAllFileFilterUsed(false);
        String current = directoryField.getText().trim();
        try {
            if (!current.isEmpty() && Files.isDirectory(Path.of(current)))
                chooser.setCurrentDirectory(Path.of(current).toFile());
        } catch (RuntimeException ignored) { }
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            urlField.setText(chooser.getSelectedFile().toPath().toAbsolutePath().normalize().toString());
            status("Файл torrent выбран; отметьте шлюзы и нажмите «Начать загрузку»");
        }
    }

    final class RouteRow {
        String gateway;
        final JPanel panel = new JPanel(new BorderLayout(0, 6));
        final JCheckBox checkBox;
        final JTextField gatewayField = new JTextField(11);
        final JLabel resultLabel = translatedLabel("Недоступен");
        final JTextArea reasonLabel = translatedArea("Маршрут ещё не проверен", 2, 34);
        final JButton speedButton = translatedButton("Тест скорости");
        final JButton deleteGatewayButton = translatedButton("Удалить шлюз");
        final JLabel speedLabel = translatedLabel("Скорость: —");
        String endpoint = "";
        String routeType = "";
        String verifiedUrl = "";
        boolean verified;
        int generation;
        String resultKey = "Недоступен";

        RouteRow(String gateway) {
            this.gateway = gateway;
            gatewayField.setText(gateway);
            I18n.tooltip(gatewayField, "IPv4 адрес вашего шлюза");
            checkBox = translatedCheckBox("Шлюз", true);
            checkBox.setEnabled(true);
            checkBox.addActionListener(event -> { updateSelectionStatus(); saveSettings(); });
            JPanel top = new JPanel(new BorderLayout());
            JPanel identity = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            identity.add(checkBox);
            identity.add(gatewayField);
            top.add(identity, BorderLayout.WEST);
            resultLabel.setForeground(muted());
            I18n.tooltip(resultLabel, "Шлюз не найден или ещё не проверен");
            JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,0));
            controls.add(resultLabel); controls.add(deleteGatewayButton);
            top.add(controls, BorderLayout.EAST);
            deleteGatewayButton.addActionListener(event -> removeGatewayProfile(this));
            installRouteTips(this);
            panel.add(top, BorderLayout.NORTH);
            reasonLabel.setEditable(false);
            reasonLabel.setLineWrap(true);
            reasonLabel.setWrapStyleWord(true);
            reasonLabel.setOpaque(false);
            reasonLabel.setFont(UIManager.getFont("Label.font"));
            reasonLabel.setForeground(muted());
            panel.add(reasonLabel, BorderLayout.CENTER);
            JPanel speedLine = new JPanel(new BorderLayout(7, 0));
            speedButton.setEnabled(true);
            speedLine.add(speedButton, BorderLayout.WEST);
            speedLine.add(speedLabel, BorderLayout.CENTER);
            panel.add(speedLine, BorderLayout.SOUTH);
            speedButton.addActionListener(event -> runSpeedTests(false, this));
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(8, 9, 8, 9)));
            gatewayField.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent event) { gatewayEdited(RouteRow.this); }
                @Override public void removeUpdate(DocumentEvent event) { gatewayEdited(RouteRow.this); }
                @Override public void changedUpdate(DocumentEvent event) { gatewayEdited(RouteRow.this); }
            });
            gatewayField.addFocusListener(new FocusAdapter() {
                @Override public void focusLost(FocusEvent event) { saveSettings(); }
            });
            gatewayField.addActionListener(event -> {
                saveSettings();
                findGateways();
            });
        }

        void unavailable(String statusText, String reason, Object... args) {
            generation++;
            endpoint = "";
            verified = false;
            routeType = "";
            verifiedUrl = "";
            boolean valid = gatewayProblem(this) == null;
            if (!valid) checkBox.setSelected(false);
            checkBox.setEnabled(valid);
            I18n.text(speedLabel, "Скорость: —");
            I18n.tooltip(speedLabel, null);
            speedButton.setEnabled(valid && !speedRunning);
            resultKey = statusText;
            I18n.text(resultLabel, statusText);
            diagnosticTip(resultLabel, new UiText(reason, args));
            setReason(reason, args);
            resultLabel.setForeground(muted());
            updateSelectionStatus();
        }

        void available(Map<String,Object> resolved, String sourceUrl) {
            verified = true;
            routeType = Main.str(resolved.get("type"));
            endpoint = Main.str(resolved.get("endpoint"));
            verifiedUrl = sourceUrl;
            checkBox.setEnabled(gatewayProblem(this) == null);
            resultKey = "ОК";
            I18n.text(resultLabel, "ОК");
            resultLabel.setForeground(new Color(28, 118, 67));
            String reason = Main.str(resolved.get("message"));
            Object diagnostic = resolved.get("ui_diagnostic");
            if (diagnostic instanceof UiText text) {
                diagnosticTip(resultLabel, text);
                setReason("%s", text);
            } else
            if (reason.isBlank()) {
                I18n.tooltip(resultLabel, "Маршрут через шлюз %s подтверждён", gateway);
                setReason("Маршрут через шлюз %s подтверждён", gateway);
            } else {
                diagnosticTip(resultLabel, new UiText("%s", DiagnosticText.arg(reason)));
                setReason("%s", DiagnosticText.arg(reason));
            }
            updateSpeedControls();
        }

        void setReason(String reason, Object... args) {
            I18n.text(reasonLabel, "%s", new UiText(reason, args));
            reasonLabel.setRows(Math.max(2, (reasonLabel.getText().length() + 44) / 45));
            panel.revalidate();
        }
    }

    private static boolean ipv4(String value) {
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) return false;
        for (String part : parts) {
            if (!part.matches("0|[1-9][0-9]{0,2}") || Integer.parseInt(part) > 255) return false;
        }
        return true;
    }

    private String gatewayProblem(RouteRow row) {
        if (!ipv4(row.gateway)) return "Некорректный IP";
        for (RouteRow other : routes)
            if (other != null && other != row && row.gateway.equals(other.gateway)) return "Повтор IP";
        return null;
    }

    private record NetworkInput(String virtualIp, List<String> gateways) { }

    private static NetworkInput networkSettingsInput(Map<String,Object> settings) {
        if (settings == null) return null;
        String virtualIp = Main.str(settings.get("virtual_ip"));
        if (!ipv4(virtualIp) || !(settings.get("gateways") instanceof List<?> values) ||
                values.isEmpty() || values.size() > 32) return null;
        List<String> gateways = new ArrayList<>();
        for (Object value : values) {
            if (!(value instanceof String gateway) || !ipv4(gateway) ||
                    virtualIp.equals(gateway) || gateways.contains(gateway)) return null;
            gateways.add(gateway);
        }
        return new NetworkInput(virtualIp, List.copyOf(gateways));
    }

    /** Never overwrite edits in progress, or write a stale local snapshot over an API update. */
    private void synchronizeNetworkSettings(Map<String,Object> settings, NetworkInput observedApplied) {
        if (!java.util.Objects.equals(observedApplied, configuredNetwork)) return;
        NetworkInput external = networkSettingsInput(settings);
        if (external == null || external.equals(configuredNetwork) || configuredNetwork == null) return;
        try { if (!networkInput().equals(configuredNetwork)) return; }
        catch (IllegalArgumentException pendingEdits) { return; }
        restoring = true;
        try {
            virtualIpField.setText(external.virtualIp());
            replaceGatewayRows(external.gateways());
        } finally { restoring = false; }
        configuredNetwork = external;
        I18n.text(networkStatusLabel, "Настройки сети приложения обновлены через API");
        speedUrlGeneration++;
        for (RouteRow row : routes)
            row.unavailable("Проверка нужна", "Настройки сети обновлены через API; соединение нужно проверить повторно");
        validateGatewayRows();
        status("Настройки сети приложения обновлены через API");
    }

    /** Read controls only on the EDT; backend setup uses this immutable snapshot. */
    private NetworkInput networkInput() {
        String virtualIp = virtualIpField.getText().trim();
        if (!ipv4(virtualIp)) throw new IllegalArgumentException("Укажите корректный виртуальный IPv4 адрес приложения");
        List<String> gateways = new ArrayList<>();
        for (RouteRow row : routes) if (row != null) {
            if(gatewayProblem(row)!=null)throw new IllegalArgumentException("Укажите уникальный IPv4 адрес каждого шлюза");
            if (virtualIp.equals(row.gateway)) throw new IllegalArgumentException("Виртуальный IP должен отличаться от адресов шлюзов");
            gateways.add(row.gateway);
        }
        if(gateways.isEmpty() || gateways.size()>32)throw new IllegalArgumentException("Можно настроить от 1 до 32 шлюзов");
        return new NetworkInput(virtualIp, List.copyOf(gateways));
    }

    private void ensureNetwork(NetworkInput input) throws IOException {
        ensureNetwork(input, false);
    }

    private void ensureNetwork(NetworkInput input, boolean checkAddress) throws IOException {
        synchronized (networkConfigurationLock) {
            AtomicBoolean current = new AtomicBoolean();
            try {
                SwingUtilities.invokeAndWait(() -> {
                    try { current.set(input.equals(networkInput())); }
                    catch (IllegalArgumentException invalid) { current.set(false); }
                });
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw failure("Настройка сети прервана", interrupted);
            } catch (java.lang.reflect.InvocationTargetException error) {
                throw failure("Не удалось проверить настройки сети", error.getCause());
            }
            if (!current.get()) throw failure("Адреса изменились; повторите действие после настройки сети");
            if (!checkAddress && input.equals(configuredNetwork)) return;
            networkConfigurator.configure(input.virtualIp(), input.gateways());
            configuredNetwork = input;
        }
    }

    private void virtualIpEdited() {
        if (restoring) return;
        I18n.text(networkStatusLabel, "Адрес изменён; нажмите «Проверить и применить»");
        speedUrlGeneration++;
        for (RouteRow row : routes) if (row != null)
            row.unavailable("Проверка нужна", "Виртуальный адрес изменён; примените настройки сети приложения");
    }

    private void applyNetwork() {
        if (closing) return;
        final NetworkInput input;
        try { input = networkInput(); }
        catch (IllegalArgumentException error) {
            I18n.text(networkStatusLabel, "Настройки сети не применены: %s", errorArgument(error));
            showError("%s", errorArgument(error)); return;
        }
        applyNetworkButton.setEnabled(false);
        I18n.text(networkStatusLabel, "Проверяем, свободен ли адрес %s…", input.virtualIp());
        status("Проверяем, свободен ли адрес %s…", input.virtualIp());
        background.execute(() -> {
            try {
                ensureNetwork(input, true);
                SwingUtilities.invokeLater(() -> {
                    applyNetworkButton.setEnabled(true);
                    try {
                        if (!input.equals(networkInput())) { status("Адреса изменились; примените настройки повторно"); return; }
                    } catch (IllegalArgumentException changed) { status("Адреса изменились; исправьте настройки сети"); return; }
                    saveSettings();
                    I18n.text(networkStatusLabel, "Адрес %s свободен, настройки применены", input.virtualIp());
                    status("Адрес %s свободен, настройки применены", input.virtualIp());
                    findGateways();
                });
            } catch (Exception error) {
                SwingUtilities.invokeLater(() -> {
                    applyNetworkButton.setEnabled(true);
                    I18n.text(networkStatusLabel, "Настройки сети не применены: %s", errorArgument(error));
                    status("Настройки сети не применены: %s", errorArgument(error));
                    showError("Настройки сети не применены:\n%s", errorArgument(error));
                });
            }
        });
    }

    private void gatewayEdited(RouteRow changed) {
        if (restoring) return;
        String updated = changed.gatewayField.getText().trim();
        if (updated.equals(changed.gateway)) return;
        speedUrlGeneration++;
        I18n.text(networkStatusLabel, "Настройки изменены; нажмите «Проверить и применить»");
        changed.gateway = updated;
        changed.unavailable("Проверка нужна", "Адрес шлюза изменён; нажмите «Проверить шлюзы»");
        validateGatewayRows();
    }

    private void validateGatewayRows() {
        for (RouteRow row : routes) {
            if (row == null) continue;
            String problem = gatewayProblem(row);
            if (problem != null) {
                if (!problem.equals(row.resultKey) || row.verified || !row.endpoint.isEmpty())
                    row.unavailable(problem, "Укажите уникальный IPv4 адрес шлюза");
            } else if ("Некорректный IP".equals(row.resultKey) ||
                    "Повтор IP".equals(row.resultKey)) {
                row.unavailable("Проверка нужна", "Нажмите «Проверить шлюзы» для проверки адреса");
            }
        }
    }

    private void findGateways() {
        if (closing || discoveryRunning) return;
        validateGatewayRows();
        discoveryRunning = true;
        int search = ++discoveryGeneration;
        findGatewaysButton.setEnabled(false);
        Map<String,String> known = new LinkedHashMap<>();
        for (RouteRow row : routes)
            if (gatewayProblem(row) == null && !"gateway".equals(row.routeType) && !row.endpoint.isBlank())
                known.put(row.gateway, row.endpoint);
        status("Проверяем маршруты шлюзов для этого источника…");
        background.execute(() -> {
            Map<String,String> candidates = new LinkedHashMap<>(known);
            try { candidates.putAll(WorkerDiscovery.find(2000)); }
            catch (IOException ignored) { /* Previously found endpoints remain candidates. */ }
            SwingUtilities.invokeLater(() -> {
                if (search != discoveryGeneration) return;
                discoveryRunning = false;
                findGatewaysButton.setEnabled(true);
                applyDiscovery(candidates);
                status("Проверяем доступность выбранных шлюзов для этого источника…");
            });
        });
    }

    /** Own gateway connections and discovered workers must report a real transport. */
    void applyDiscovery(Map<String,String> candidates) {
        if (!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Apply discovery on the EDT");
        if (closing) return;
        validateGatewayRows();
        final NetworkInput network;
        try { network = networkInput(); }
        catch (IllegalArgumentException error) {
            for (RouteRow row : routes) if (gatewayProblem(row) == null)
                row.unavailable("Настройки нужны", "%s", errorArgument(error));
            status("%s", errorArgument(error));
            return;
        }
        String url = testSourceUrl();
        int urlGeneration = speedUrlGeneration;
        for (RouteRow row : routes) {
            if (gatewayProblem(row) != null) continue;
            int generation = ++row.generation;
            String gateway = row.gateway;
            String endpoint = candidates.getOrDefault(gateway, "");
            row.verified = false;
            row.routeType = "";
            row.verifiedUrl = "";
            row.resultKey = "Проверка…";
            I18n.text(row.resultLabel, "Проверка…");
            I18n.text(row.speedLabel, "Скорость: —");
            row.setReason("Проверяем маршрут через шлюз %s", gateway);
            background.execute(() -> {
                Map<String,Object> resolved;
                try { ensureNetwork(network); resolved = resolveGateway(gateway, endpoint, url); }
                catch (Exception error) {
                    resolved = diagnostic(Map.of("available", false, "gateway", gateway), "%s", errorArgument(error));
                }
                final Map<String,Object> result = resolved;
                SwingUtilities.invokeLater(() -> {
                    if (generation != row.generation) return;
                    if ("direct".equals(result.get("type")) && urlGeneration != speedUrlGeneration) {
                        row.unavailable("Проверка нужна", "Ссылка изменилась; повторите проверку шлюза для этого файла");
                        return;
                    }
                    applyGatewayResult(row, result, url);
                    saveSettings();
                });
            });
        }
    }

    private String testSourceUrl() {
        String url = urlField.getText().trim();
        return url.isEmpty() ? SpeedTest.DEFAULT_URL : url;
    }

    /** Source templates survive async dispatch; known diagnostics and legacy MAC text are rendered at display time. */
    private record UiText(String source, Object... args) {
        UiText { args = args.clone(); }
        @Override public String toString() {
            return args.length == 0 ? DiagnosticText.render(source) : I18n.t(source, args);
        }
        String raw() {
            Object[] canonical = args.clone();
            for (int i = 0; i < canonical.length; i++) {
                if (canonical[i] instanceof UiText nested) canonical[i] = nested.raw();
                else if (canonical[i] instanceof DiagnosticText.Arg nested) canonical[i] = nested.raw();
            }
            return I18n.tFor("ru", source, canonical);
        }
    }

    private static final class UiIOException extends IOException {
        final UiText text;
        UiIOException(UiText text, Throwable cause) { super(text.source(), cause); this.text = text; }
        @Override public String getMessage() { return text.toString(); }
    }

    private static IOException failure(String source, Object... args) {
        return new UiIOException(new UiText(source, args), null);
    }

    private static IOException failure(String source, Throwable cause) {
        return new UiIOException(new UiText(source), cause);
    }

    private static Object errorArgument(Throwable error) {
        return error instanceof UiIOException localized ? localized.text : DiagnosticText.arg(message(error));
    }

    private static Map<String,Object> diagnostic(Map<String,Object> resolved, String source, Object... args) {
        Map<String,Object> value = new LinkedHashMap<>(resolved);
        UiText text = new UiText(source, args);
        value.put("message", text.raw());
        value.put("ui_diagnostic", text);
        return value;
    }

    private static Object resolvedReason(Map<String,Object> resolved) {
        Object diagnostic = resolved.get("ui_diagnostic");
        return diagnostic instanceof UiText ? diagnostic : DiagnosticText.arg(Main.str(resolved.get("message")));
    }

    private static Map<String,Object> transportRoute(Map<String,Object> resolved) {
        Map<String,Object> route = new LinkedHashMap<>(resolved);
        route.remove("ui_diagnostic");
        return route;
    }

    /** Called outside the EDT. Never substitutes the default route for another gateway. */
    private Map<String,Object> resolveGateway(String gateway, String endpoint, String url) {
        Map<String,Object> local;
        try {
            local = new LinkedHashMap<>(gatewayInspector.inspect(gateway, url));
            String type = Main.str(local.get("type"));
            if (Boolean.TRUE.equals(local.get("available")) &&
                    ("gateway".equals(type) || "direct".equals(type))) {
                local.put("gateway", gateway);
                if ("direct".equals(type)) local.put("endpoint", "");
                // A localhost gateway proxy speaks HTTP CONNECT, not the worker /probe or /range protocol.
                return local;
            }
        } catch (Exception error) {
            local = diagnostic(Map.of("available", false, "gateway", gateway),
                    "Не удалось проверить соединение шлюза: %s", errorArgument(error));
        }
        UiText workerError = null;
        if (endpoint != null && !endpoint.isBlank()) {
            try {
                Map<String,Object> probe = Transport.probe(Map.of("type", "worker", "endpoint", endpoint,
                        "gateway", gateway));
                if (!Boolean.TRUE.equals(probe.get("ok")) ||
                        !gateway.equals(Main.str(probe.get("expected_gateway"))) ||
                        !Boolean.TRUE.equals(probe.get("gateway_match")))
                    throw failure("Не подтверждён выбранный выход в интернет");
                String ip = Main.str(probe.get("egress_ip"));
                UiText suffix = new UiText(ip.isBlank() ? "" : "; внешний IP %s", ip);
                return diagnostic(Map.of("available", true, "type", "worker", "gateway", gateway,
                        "endpoint", endpoint), "%s%s", new UiText("Маршрут через шлюз подтверждён"), suffix);
            } catch (Exception error) { workerError = new UiText("Узел не подтвердил маршрут: %s. ", errorArgument(error)); }
        }
        Object reason = resolvedReason(local);
        if (reason.toString().isBlank()) reason = new UiText("Нет отдельного соединения через этот шлюз");
        local.put("available", false);
        local.put("gateway", gateway);
        return diagnostic(local, "%s%s", workerError == null ? "" : workerError, reason);
    }

    private void applyGatewayResult(RouteRow row, Map<String,Object> resolved, String url) {
        if (Boolean.TRUE.equals(resolved.get("available"))) {
            row.available(resolved, url);
            status("Шлюз %s готов", row.gateway);
        } else {
            row.unavailable("Нет соединения", "%s", resolvedReason(resolved));
            status("Шлюз %s: %s", row.gateway, resolvedReason(resolved));
        }
    }

    private void updateSelectionStatus() {
        int selected = 0;
        for (RouteRow route : routes) if (route != null && route.checkBox.isSelected()) selected++;
        if (selected == 0) status("Выберите хотя бы один шлюз"); else status("Выбрано шлюзов: %d", selected);
    }

    private final class SpeedTarget {
        final RouteRow row;
        final int generation;
        final String gateway;
        final String endpoint;
        SpeedTarget(RouteRow row) {
            this.row = row;
            generation = row.generation;
            gateway = row.gateway;
            endpoint = "gateway".equals(row.routeType) ? "" : row.endpoint;
        }
        String name() { return I18n.t("Шлюз %s", gateway); }
        JLabel label() { return row.speedLabel; }
        boolean current(int urlGeneration) {
            return speedUrlGeneration == urlGeneration && row.generation == generation;
        }
    }

    private void invalidateSpeedsForUrl() {
        speedUrlGeneration++;
        for (RouteRow row : routes) if (row != null) {
            I18n.text(row.speedLabel, "Скорость: —");
            I18n.tooltip(row.speedLabel, null);
            if ("direct".equals(row.routeType))
                row.unavailable("Проверка нужна", "Ссылка изменилась; маршрут будет проверен перед загрузкой или тестом");
        }
    }

    private void updateSpeedControls() {
        allSpeedButton.setEnabled(!speedRunning);
        for (RouteRow row : routes) if (row != null)
            row.speedButton.setEnabled(!speedRunning && gatewayProblem(row) == null);
    }

    private void runSpeedTests(boolean all, RouteRow single) {
        if (closing || speedRunning) return;
        List<SpeedTarget> targets = new ArrayList<>();
        if (all) {
            for (RouteRow row : routes) if (gatewayProblem(row) == null) targets.add(new SpeedTarget(row));
        } else if (single != null && gatewayProblem(single) == null) targets.add(new SpeedTarget(single));
        if (targets.isEmpty()) return;
        final NetworkInput network;
        try { network = targets.stream().anyMatch(target -> target.row != null) ? networkInput() : null; }
        catch (IllegalArgumentException error) { showError("%s", errorArgument(error)); return; }
        final String sourceUrl = testSourceUrl();
        final int urlGeneration = speedUrlGeneration;
        speedRunning = true;
        updateSpeedControls();
        for (SpeedTarget target : targets) I18n.text(target.label(), "Скорость: проверка…");
        if (all) status("Последовательно проверяем скорости шлюзов…");
        else status("Проверяем скорость: шлюз %s", targets.get(0).gateway);
        background.execute(() -> {
            Object networkError = null;
            try { if (network != null) ensureNetwork(network); }
            catch (Exception error) { networkError = errorArgument(error); }
            Map<String,String> candidates = new LinkedHashMap<>();
            boolean needsDiscovery = targets.stream().anyMatch(target -> target.row != null && target.endpoint.isBlank());
            if (needsDiscovery) try { candidates.putAll(WorkerDiscovery.find(2000)); }
            catch (IOException ignored) { }
            int failures = 0;
            for (SpeedTarget target : targets) {
                Map<String,Object> resolved = networkError != null ? diagnostic(Map.of("available", false), "%s", networkError)
                        : resolveGateway(target.gateway, candidates.getOrDefault(target.gateway, target.endpoint), sourceUrl);
                try {
                    if (!Boolean.TRUE.equals(resolved.get("available")))
                        throw new IOException(Main.str(resolved.get("message")));
                    Map<String,Object> route = transportRoute(resolved);
                    route.put("name", target.name());
                    Map<String,Object> result = routeSpeedTester.run(route, sourceUrl);
                    if (!Boolean.TRUE.equals(result.get("ok")) || !(result.get("mbps") instanceof Number number))
                        throw failure("Не получен результат замера");
                    double mbps = number.doubleValue();
                    if (!Double.isFinite(mbps) || mbps < 0) throw failure("Некорректный результат замера");
                    long measuredBytes = Main.num(result.get("bytes"), 0);
                    long measuredElapsed = Main.num(result.get("elapsed_ms"), 0);
                    SwingUtilities.invokeLater(() -> {
                        if (!target.current(urlGeneration)) return;
                        if (target.row != null) target.row.available(resolved, sourceUrl);
                        I18n.text(target.label(), "Скорость: %.1f Мбит/с", mbps);
                        I18n.tooltip(target.label(), "Получено %d байт за %d мс", measuredBytes, measuredElapsed);
                        status("Шлюз %s · скорость: %.1f Мбит/с", target.gateway, mbps);
                    });
                } catch (Exception error) {
                    failures++;
                    Object reason = !Boolean.TRUE.equals(resolved.get("available"))
                            ? resolvedReason(resolved) : errorArgument(error);
                    SwingUtilities.invokeLater(() -> {
                        if (!target.current(urlGeneration)) return;
                        if (target.row != null) {
                            target.row.unavailable("Нет замера", "%s", reason);
                        }
                        I18n.text(target.label(), "Скорость: не измерена");
                        diagnosticTip(target.label(), new UiText("%s", reason));
                        status("Шлюз %s: %s", target.gateway, reason);
                    });
                }
            }
            final int failed = failures;
            SwingUtilities.invokeLater(() -> {
                speedRunning = false;
                updateSpeedControls();
                if (speedUrlGeneration != urlGeneration)
                    status("Ссылка или сеть изменились; повторите тест скорости");
                else if (all)
                    status("Проверка скоростей завершена: %d/%d", targets.size() - failed, targets.size());
            });
        });
    }

    private record ManualChecksumText(String primary, String md5, String sha512) { }

    private ManualChecksumText checksumText() {
        return new ManualChecksumText(shaField.getText(), md5Field.getText(), sha512Field.getText());
    }

    /** The primary field detects MD5/SHA-256/SHA-512; extra references are merged without hiding conflicts. */
    private Map<String,Object> manualChecksums() {
        Map<String,Object> input = new LinkedHashMap<>();
        for (String algorithm : List.of("md5", "sha256", "sha512")) input.put("expected_" + algorithm, "");
        String primary = shaField.getText().trim();
        if (!primary.isEmpty()) {
            String algorithm = switch (primary.length()) { case 32 -> "md5"; case 64 -> "sha256"; case 128 -> "sha512"; default -> ""; };
            if (algorithm.isEmpty() || !validChecksum(algorithm, primary))
                throw new IllegalArgumentException(I18n.tFor("ru", "Контрольная сумма должна содержать 32, 64 или 128 шестнадцатеричных символов: MD5, SHA-256 или SHA-512."));
            input.put("expected_" + algorithm, primary);
        }
        for (String algorithm : List.of("md5", "sha512")) {
            String value = (algorithm.equals("md5") ? md5Field : sha512Field).getText().trim();
            if (!value.isEmpty() && !validChecksum(algorithm, value))
                throw new IllegalArgumentException(I18n.tFor("ru", switch (algorithm) {
                    case "md5" -> "Ожидаемая MD5 должна содержать 32 шестнадцатеричных символа";
                    default -> "Ожидаемая SHA-512 должна содержать 128 шестнадцатеричных символов";
                }));
            if (value.isEmpty()) continue;
            String existing = Main.str(input.get("expected_" + algorithm));
            if (!existing.isEmpty() && !value.equalsIgnoreCase(existing))
                throw new IllegalArgumentException("Conflicting manual " + (algorithm.equals("md5") ? "MD5" : "SHA-512") + " checksums");
            if (existing.isEmpty()) input.put("expected_" + algorithm, value);
        }
        return input;
    }

    private Map<String,Object> jobInput() {
        Map<String,Object> input = new LinkedHashMap<>(manualChecksums());
        input.put("url", urlField.getText().trim());
        input.put("filename", filenameField.getText().trim());
        input.put("directory", directoryField.getText().trim());
        input.put("chunk_mb", ((Number) chunkSpinner.getValue()).longValue());
        return input;
    }

    private static boolean validChecksum(String algorithm, String value) {
        int length = switch (algorithm) { case "md5" -> 32; case "sha512" -> 128; default -> 64; };
        return value.matches("[a-fA-F0-9]{" + length + "}");
    }

    private static boolean hasManualChecksums(Map<String,Object> input) {
        return List.of("md5", "sha256", "sha512").stream()
                .anyMatch(algorithm -> !Main.str(input.get("expected_" + algorithm)).isBlank());
    }

    private static void requireSingleChecksumFile(Map<String,Object> input, List<?> selected) {
        if (hasManualChecksums(input) && selected.size() != 1)
            throw new IllegalArgumentException(I18n.tFor("ru", "Ожидаемые контрольные суммы относятся к одному файлу. Выберите один файл или очистите поля MD5, SHA-256 и SHA-512 для групповой загрузки."));
    }

    private boolean checksumsCurrent(Map<String,Object> input) {
        try {
            Map<String,Object> current = manualChecksums();
            return List.of("md5", "sha256", "sha512").stream()
                    .allMatch(algorithm -> Main.str(input.get("expected_" + algorithm)).equals(Main.str(current.get("expected_" + algorithm))));
        } catch (IllegalArgumentException changed) { return false; }
    }

    private void requireCurrentChecksums(Map<String,Object> input) throws Exception {
        requireCurrentChecksums(input, null);
    }

    private void requireCurrentChecksums(Map<String,Object> input, ManualChecksumText captured) throws Exception {
        AtomicBoolean current = new AtomicBoolean();
        SwingUtilities.invokeAndWait(() -> current.set(!closing && checksumsCurrent(input)
                && (captured == null || captured.equals(checksumText()))));
        if (!current.get()) throw failure("Ссылка или настройки изменились; начните загрузку повторно");
    }

    private void createJob() {
        if (closing || createRunning) return;
        String url = urlField.getText().trim();
        if (url.isEmpty()) { showError("Укажите ссылку на файл"); return; }
        final boolean torrent = TorrentSource.isTorrentSource(url);
        final ManualChecksumText capturedChecksums = checksumText();
        final Map<String,Object> input;
        try { input = jobInput(); }
        catch (IllegalArgumentException invalid) { showError("%s", errorArgument(invalid)); return; }
        List<SpeedTarget> selectedGateways = new ArrayList<>();
        for (RouteRow route : routes) if (route.checkBox.isSelected()) {
            String problem = gatewayProblem(route);
            if (problem != null) { showError("Шлюз %s: %s", route.gateway, DiagnosticText.arg(problem)); return; }
            selectedGateways.add(new SpeedTarget(route));
        }
        final int urlGeneration = speedUrlGeneration;
        if (selectedGateways.isEmpty()) {
            showError("Выберите хотя бы один шлюз"); return;
        }
        final NetworkInput network;
        try { network = selectedGateways.isEmpty() ? null : networkInput(); }
        catch (IllegalArgumentException error) { showError("%s", errorArgument(error)); return; }
        if (torrent) input.put("source_type", "torrent");
        createRunning = true;
        createButton.setEnabled(false);
        I18n.text(createButton, "Подготовка…");
        status("Создаём загрузку и проверяем выбранные шлюзы…");
        background.execute(() -> {
            try {
                if (network != null) ensureNetwork(network);
                Map<String,String> discovered = new LinkedHashMap<>();
                if (selectedGateways.stream().anyMatch(target -> target.endpoint.isBlank())) {
                    try { discovered.putAll(WorkerDiscovery.find(2000)); }
                    catch (IOException ignored) { }
                }
                List<Object> selectedRoutes = new ArrayList<>();
                for (SpeedTarget target : selectedGateways) {
                    Map<String,Object> resolved = resolveGateway(target.gateway,
                            discovered.getOrDefault(target.gateway, target.endpoint), torrent || !(url.startsWith("http://")||url.startsWith("https://")) ? SpeedTest.DEFAULT_URL : url);
                    if (!Boolean.TRUE.equals(resolved.get("available"))) {
                        String reason = Main.str(resolved.get("message"));
                        SwingUtilities.invokeLater(() -> {
                            if (target.current(urlGeneration)) applyGatewayResult(target.row, resolved, url);
                        });
                        throw failure("Шлюз %s: %s", target.gateway, resolvedReason(resolved));
                    }
                    if (torrent && !"gateway".equals(resolved.get("type")))
                        throw failure("Torrent требует соединение приложения через шлюз %s; нажмите «Проверить и применить» в настройках сети", target.gateway);
                    Map<String,Object> selected = transportRoute(resolved);
                    selected.put("name", target.name());
                    selected.put("enabled", true);
                    selectedRoutes.add(selected);
                }
                requireCurrentCreate(input, selectedGateways, urlGeneration, network, capturedChecksums);
                input.put("routes", selectedRoutes);
                if (torrent) {
                    SwingUtilities.invokeLater(() -> {
                        if (!closing) status("Получаем сведения о раздаче через выбранный шлюз…");
                    });
                    Map<String,Object> inspection = torrentInspector.inspect(url, Main.obj(selectedRoutes.get(0)));
                    requireCurrentCreate(input, selectedGateways, urlGeneration, network, capturedChecksums);
                    AtomicReference<List<String>> selectedFiles = new AtomicReference<>();
                    SwingUtilities.invokeAndWait(() -> {
                        if (!closing) {
                            I18n.text(createButton, "Выбор файлов…");
                            selectedFiles.set(torrentFileSelector.choose(frame, inspection));
                        }
                    });
                    if (selectedFiles.get() == null) {
                        SwingUtilities.invokeLater(() -> finishCreate("Выбор файлов отменён"));
                        return;
                    }
                    if (selectedFiles.get().isEmpty()) throw failure("Выберите хотя бы один файл раздачи");
                    requireSingleChecksumFile(input, selectedFiles.get());
                    input.put("selected_files", List.copyOf(selectedFiles.get()));
                    input.put("info_hash_v1", Main.str(inspection.get("info_hash_v1")));
                    input.put("info_hash_v2", Main.str(inspection.get("info_hash_v2")));
                } else if (Metalink.isMetalink(url) || WebDav.isWebDav(url)) {
                    Map<String,Object> inspection=manager.inspectSource(input);
                    requireCurrentCreate(input,selectedGateways,urlGeneration,network,capturedChecksums);
                    AtomicReference<List<String>> chosen=new AtomicReference<>();
                    SwingUtilities.invokeAndWait(()->{if(!closing)chosen.set(SourceFileDialog.choose(frame,inspection));});
                    if(chosen.get()==null){SwingUtilities.invokeLater(()->finishCreate("Выбор файлов отменён"));return;}
                    requireSingleChecksumFile(input, chosen.get());
                    input.put("selected_files",chosen.get());
                } else if (HubSource.isListingUrl(url)) {
                    SwingUtilities.invokeLater(() -> {
                        if (!closing) status("Получаем список файлов Hugging Face…");
                    });
                    Map<String,Object> inspection = hubInspector.inspect(url, Main.obj(selectedRoutes.get(0)));
                    requireCurrentCreate(input, selectedGateways, urlGeneration, network, capturedChecksums);
                    AtomicReference<List<String>> selectedFiles = new AtomicReference<>();
                    SwingUtilities.invokeAndWait(() -> {
                        if (!closing) {
                            I18n.text(createButton, "Выбор файлов…");
                            selectedFiles.set(hubFileSelector.choose(frame, inspection));
                        }
                    });
                    if (selectedFiles.get() == null) {
                        SwingUtilities.invokeLater(() -> finishCreate("Выбор файлов отменён"));
                        return;
                    }
                    if (selectedFiles.get().isEmpty()) throw failure("Выберите хотя бы один файл");
                    requireSingleChecksumFile(input, selectedFiles.get());
                    input.put("selected_files", List.copyOf(selectedFiles.get()));
                    input.put("revision", Main.str(inspection.get("revision")));
                }
                requireCurrentCreate(input, selectedGateways, urlGeneration, network, capturedChecksums);
                Map<String,Object> createInput = new LinkedHashMap<>(input);
                if (torrent) createInput.put("filename", "");
                Map<String,Object> created = manager.createFromSource(createInput);
                SwingUtilities.invokeLater(() -> {
                    if (closing) return;
                    List<Object> createdJobs = Main.arr(created.get("jobs"));
                    Map<String,Object> first = createdJobs.isEmpty() ? created : Main.obj(createdJobs.get(0));
                    long reused = Main.num(created.get("reused_count"), Boolean.TRUE.equals(first.get("reused")) ? 1 : 0);
                    Map<String,Object> selection = first;
                    if (reused > 0) for (Object item : createdJobs) {
                        Map<String,Object> candidate = Main.obj(item);
                        if (Boolean.TRUE.equals(candidate.get("reused"))) { selection = candidate; break; }
                    }
                    selectedId = Main.str(selection.get("id"));
                    jobsGeneration++;
                    if (createdJobs.size() <= 1 && reused > 0)
                        finishCreate(List.of("paused", "failed").contains(Main.str(selection.get("status")))
                                ? "Загрузка уже есть в списке: %s. Нажмите «Продолжить», чтобы докачать её."
                                : "Загрузка уже есть в списке: %s. Выделено существующее задание.", Main.str(selection.get("filename")));
                    else if (reused > 0)
                        finishCreate("Новых загрузок: %d; уже есть в списке: %d. Выделено существующее задание.", Math.max(0, createdJobs.size() - reused), reused);
                    else if (createdJobs.size() > 1) finishCreate("Создано загрузок: %d", createdJobs.size());
                    else finishCreate("Загрузка создана: %s", Main.str(first.get("filename")));
                    saveSettings();
                    refreshJobs();
                });
            } catch (Exception error) {
                SwingUtilities.invokeLater(() -> {
                    if (closing) return;
                    finishCreate("Не удалось создать загрузку: %s", errorArgument(error));
                    showError("Не удалось создать загрузку:\n%s%s", errorArgument(error),
                            (isNoSpaceError(message(error)) ? new UiText("\nВыберите папку со свободным местом.") : ""));
                });
            }
        });
    }

    /** Inspecting a large repository must not later start a stale URL or edited configuration. */
    private void requireCurrentCreate(Map<String,Object> input, List<SpeedTarget> targets,
            int generation, NetworkInput network) throws Exception {
        requireCurrentCreate(input, targets, generation, network, null);
    }

    private void requireCurrentCreate(Map<String,Object> input, List<SpeedTarget> targets,
            int generation, NetworkInput network, ManualChecksumText capturedChecksums) throws Exception {
        AtomicBoolean unchanged = new AtomicBoolean(true);
        SwingUtilities.invokeAndWait(() -> {
            if (closing || generation != speedUrlGeneration
                    || !Main.str(input.get("url")).equals(urlField.getText().trim())
                    || !Main.str(input.get("filename")).equals(filenameField.getText().trim())
                    || !Main.str(input.get("directory")).equals(directoryField.getText().trim())
                    || !checksumsCurrent(input)
                    || (capturedChecksums != null && !capturedChecksums.equals(checksumText()))
                    || Main.num(input.get("chunk_mb"), 0) != ((Number) chunkSpinner.getValue()).longValue())
                unchanged.set(false);
            if (network != null) {
                try { if (!network.equals(networkInput())) unchanged.set(false); }
                catch (IllegalArgumentException invalid) { unchanged.set(false); }
            }
            for (SpeedTarget target : targets)
                if (!target.current(generation) || !target.row.checkBox.isSelected()) unchanged.set(false);
            for (RouteRow row : routes)
                if (row.checkBox.isSelected() && targets.stream().noneMatch(target -> target.row == row))
                    unchanged.set(false);
        });
        if (!unchanged.get()) throw failure("Ссылка или настройки изменились; начните загрузку повторно");
    }

    private void finishCreate(String text, Object... args) {
        createRunning = false;
        createButton.setEnabled(true);
        I18n.text(createButton, "Начать загрузку");
        if (!closing) status(text, args);
    }

    private void refreshJobs() {
        if (closing) return;
        if (!refreshing.compareAndSet(false, true)) return;
        long generation = jobsGeneration;
        background.execute(() -> {
            try {
                NetworkInput observedApplied = configuredNetwork;
                Map<String,Object> networkSettings;
                synchronized (manager) {
                    // A configuration may be rolled back after a failed address check.
                    // Read only after the whole Manager operation has finished.
                    networkSettings = networkSettingsProvider == null ? null : networkSettingsProvider.settings();
                }
                Map<String,Object> state = manager.state();
                List<Object> snapshots = Main.arr(state.get("jobs"));
                SwingUtilities.invokeLater(() -> {
                    if (closing) return;
                    if (networkSettings != null) synchronizeNetworkSettings(networkSettings, observedApplied);
                    if (generation == jobsGeneration) updateJobs(snapshots);
                });
            } catch (Exception error) {
                SwingUtilities.invokeLater(() -> status("Не удалось обновить список: %s", errorArgument(error)));
            } finally { refreshing.set(false); }
        });
    }

    private void updateJobs(List<Object> snapshots) {
        tray.update(snapshots);
        Point viewportPosition = jobsScroll.getViewport().getViewPosition();
        int firstVisibleRow = jobsTable.rowAtPoint(new Point(0, viewportPosition.y));
        String anchorId = firstVisibleRow < 0 ? "" :
                new ArrayList<>(jobs.keySet()).get(jobsTable.convertRowIndexToModel(firstVisibleRow));
        int anchorOffset = firstVisibleRow < 0 ? 0 : viewportPosition.y - firstVisibleRow * jobsTable.getRowHeight();
        String keepId = selectedId;
        Map<String,String> priorStatuses = new LinkedHashMap<>();
        for (Map.Entry<String,Map<String,Object>> entry : jobs.entrySet())
            priorStatuses.put(entry.getKey(), Main.str(entry.getValue().get("status")));
        updatingJobs = true;
        jobs.clear();
        jobsModel.setRowCount(0);
        int selectedRow = -1;
        for (Object item : snapshots) {
            Map<String,Object> job = Main.obj(item);
            String id = Main.str(job.get("id"));
            jobs.put(id, job);
            String currentStatus = Main.str(job.get("status"));
            if (priorStatuses.containsKey(id) && !currentStatus.equals(priorStatuses.get(id))) {
                if ("complete".equals(currentStatus)) status("Загрузка завершена: %s", Main.str(job.get("filename")));
                else if ("failed".equals(currentStatus)) status("Ошибка загрузки: %s", DiagnosticText.arg(Main.str(job.get("error"))));
            }
            int channels = 0;
            for (Object routeItem : Main.arr(job.get("routes")))
                if (Boolean.TRUE.equals(Main.obj(routeItem).get("enabled"))) channels++;
            jobsModel.addRow(new Object[]{Main.str(job.get("filename")), statusName(Main.str(job.get("status"))),
                    DownloadProgress.from(job),
                    speed(Main.num(job.get("speed_bps"), 0)), duration(Main.num(job.get("elapsed_ms"), 0)),
                    remaining(job), channels});
            if (id.equals(keepId)) selectedRow = jobsModel.getRowCount() - 1;
        }
        if (selectedRow < 0 && jobsModel.getRowCount() > 0) selectedRow = jobsModel.getRowCount() - 1;
        applyJobFilter();
        int visibleRow = selectedRow < 0 ? -1 : jobsTable.convertRowIndexToView(selectedRow);
        if(visibleRow < 0 && jobsTable.getRowCount()>0) visibleRow=0;
        if (visibleRow >= 0) jobsTable.setRowSelectionInterval(visibleRow, visibleRow);
        updatingJobs = false;
        if (visibleRow >= 0) selectCurrentJob();
        else { selectedId = null; renderDetails(null); }
        restoreJobsViewport(viewportPosition, anchorId, anchorOffset);
    }

    /** Keep the first visible job in place, even when sorting or appending jobs on refresh. */
    private void restoreJobsViewport(Point position, String anchorId, int anchorOffset) {
        int y = position.y;
        int modelRow = new ArrayList<>(jobs.keySet()).indexOf(anchorId);
        if (modelRow >= 0) {
            int viewRow = jobsTable.convertRowIndexToView(modelRow);
            if (viewRow >= 0) y = viewRow * jobsTable.getRowHeight() + anchorOffset;
        }
        Dimension extent = jobsScroll.getViewport().getExtentSize();
        Dimension content = jobsTable.getPreferredSize();
        int x = Math.max(0, Math.min(position.x, content.width - extent.width));
        y = Math.max(0, Math.min(y, content.height - extent.height));
        jobsScroll.getViewport().setViewPosition(new Point(x, y));
    }

    private void selectCurrentJob() {
        int row = jobsTable.getSelectedRow();
        if (row < 0 || row >= jobs.size()) { selectedId = null; renderDetails(null); return; }
        selectedId = new ArrayList<>(jobs.keySet()).get(jobsTable.convertRowIndexToModel(row));
        renderDetails(jobs.get(selectedId));
    }

    private void renderDetails(Map<String,Object> job) {
        optionsButton.setEnabled(job != null && !jobActionRunning);
        openFolderButton.setEnabled(job != null);
        if (job == null) {
            speedGraph.clear(); graphJobId="";
            I18n.text(selectionLabel, "Выберите загрузку");
            progressBar.setValue(0); I18n.text(progressBar, "Нет выбранной загрузки");
            I18n.tooltip(progressBar, null);
            I18n.text(transferMessageArea, ""); transferMessageArea.setVisible(false);
            I18n.text(totalSpeedLabel, "Общая скорость: —");
            I18n.text(elapsedLabel, "Прошло: —"); I18n.text(etaLabel, "Осталось: —");
            renderGatewaySpeeds(Map.of());
            I18n.text(detailsArea, "После запуска здесь появятся сведения о шлюзах и проверке файла.");
            pauseButton.setEnabled(false); resumeButton.setEnabled(false); changeGatewaysButton.setEnabled(false);
            changeDirectoryButton.setEnabled(false); removeButton.setEnabled(false); return;
        }
        String status = Main.str(job.get("status"));
        String graphId=Main.str(job.get("id"));
        if(!graphId.equals(graphJobId)){speedGraph.clear();graphJobId=graphId;}
        speedGraph.observe(job);
        long size = Main.num(job.get("size"), 0);
        long downloaded = Main.num(job.get("progress_bytes"), Main.num(job.get("downloaded"), 0));
        I18n.text(selectionLabel, Main.str(job.get("filename")) + "  ·  " + statusName(status));
        I18n.text(totalSpeedLabel, "Общая скорость: %s", speed(Main.num(job.get("speed_bps"), 0)));
        I18n.text(elapsedLabel, "Прошло: %s", duration(Main.num(job.get("elapsed_ms"), 0)));
        I18n.text(etaLabel, "Осталось: %s", remaining(job));
        renderGatewaySpeeds(job);
        DownloadProgress transferProgress = DownloadProgress.from(job);
        int perMille = transferProgress.perMille();
        progressBar.setValue(perMille);
        if (size <= 0 && !transferProgress.determinate()) I18n.text(progressBar,
                downloaded > 0 ? "Получено: %s · размер неизвестен" : unknownSizeProgress(status),
                downloaded > 0 ? new Object[]{bytes(downloaded)} : new Object[]{});
        else I18n.text(progressBar, bytes(downloaded) + " / " + bytes(size) + "  ·  " + transferProgress.text());
        renderTransferMessage(job, status);
        pauseButton.setEnabled(!jobActionRunning && List.of("running", "retrying", "queued", "verifying", "seeding").contains(status));
        resumeButton.setEnabled(!jobActionRunning && List.of("paused", "failed").contains(status));
        changeDirectoryButton.setEnabled(!jobActionRunning && List.of("paused", "failed").contains(status));
        removeButton.setEnabled(!jobActionRunning);
        changeGatewaysButton.setEnabled(!jobActionRunning && List.of("running", "retrying", "queued", "paused", "failed", "seeding").contains(status));

        StringBuilder info = new StringBuilder();
        info.append(I18n.t("Размер: ")).append(bytes(size)).append("\n");
        info.append(I18n.t("Проверено и сохранено: ")).append(bytes(Main.num(job.get("downloaded"), 0))).append("\n");
        info.append(I18n.t("Источник: ")).append(Main.str(job.get("url"))).append("\n");
        info.append(I18n.t("Файл: ")).append(Main.str(job.get("directory"))).append("/")
                .append(Main.str(job.get("filename"))).append("\n");
        if(Boolean.FALSE.equals(job.get("resumable"))||Boolean.FALSE.equals(job.get("distribution_supported")))
            info.append(I18n.t("Источник не поддерживает запросы частей. Передача выполняется одним шлюзом; после обрыва этот файл начнёт скачиваться заново.\n"));
        if ("torrent".equals(job.get("kind"))) renderTorrentDetails(info, job, status);
        else renderChecksums(info, job);
        info.append(I18n.t("\nШЛЮЗЫ\n"));
        List<Object> jobRoutes = job.get("routes") instanceof List<?> ? Main.arr(job.get("routes")) : List.of();
        for (Object item : jobRoutes) {
            Map<String,Object> route = Main.obj(item);
            if (!Boolean.TRUE.equals(route.get("enabled"))) continue;
            info.append("• ").append(Main.str(route.get("gateway")).isBlank() ? Main.str(route.get("name"))
                    : I18n.t("Шлюз %s", Main.str(route.get("gateway")))).append(" — ")
                    .append(speed(Main.num(route.get("speed_bps"), 0))).append(", ")
                    .append(bytes(Main.num(route.get("bytes"), 0))).append(", ")
                    .append(Main.num(route.get("chunks"), 0)).append(I18n.t(" сегм.\n"));
            String routeError = Main.str(route.get("last_error"));
            if (!routeError.isEmpty()) info.append(I18n.t("  Последняя ошибка: ")).append(DiagnosticText.render(routeError)).append("\n");
        }
        String error = Main.str(job.get("error"));
        if (!error.isEmpty()) {
            info.append(I18n.t("\nОШИБКА\n")).append(DiagnosticText.render(error)).append("\n");
            if (isNoSpaceError(error)) info.append(I18n.t("\nВыберите «Изменить папку…» и папку со свободным местом, затем нажмите «Продолжить». Скачанные части сохранятся.\n"));
        }
        I18n.text(detailsArea, info.toString());
        detailsArea.setCaretPosition(0);
    }

    /** The latest cause must stay visible even when the detailed log is below the fold. */
    private void renderTransferMessage(Map<String,Object> job, String status) {
        String error = Main.str(job.get("error"));
        String text = "";
        if ("retrying".equals(status)) text = error.isBlank()
                ? I18n.t("Повторяем запрос к источнику. Причина уточняется.")
                : I18n.t("Повтор запроса: %s", conciseError(error));
        else if ("failed".equals(status)) text = error.isBlank()
                ? I18n.t("Не удалось загрузить файл. Подробности ниже.")
                : I18n.t("Ошибка: %s", conciseError(error));
        else if ("paused".equals(status) && !error.isBlank()) text = I18n.t("Последняя ошибка: %s", conciseError(error));
        I18n.text(transferMessageArea, text);
        transferMessageArea.setCaretPosition(0);
        transferMessageArea.setVisible(!text.isBlank());
        transferMessageArea.setForeground("failed".equals(status) ? new Color(165, 38, 38) : UIManager.getColor("Label.foreground"));
        String tooltip = error.isBlank() ? null : I18n.t("Причина: %s", conciseError(error, 360));
        I18n.tooltip(transferMessageArea, tooltip);
        I18n.tooltip(progressBar, tooltip);
    }

    private static String unknownSizeProgress(String status) {
        return switch (status) {
            case "retrying" -> I18n.t("Повторяем запрос к источнику");
            case "failed" -> I18n.t("Не удалось получить файл");
            case "paused" -> I18n.t("Загрузка приостановлена");
            case "queued" -> I18n.t("Ожидаем начала запроса");
            default -> I18n.t("Определяем размер");
        };
    }

    private static String conciseError(String error) { return conciseError(error, 105); }

    static String conciseError(String error, int limit) {
        // URLs can include long signed query strings; the full original remains in details.
        String text = DiagnosticText.render(error).replaceAll("(?i)https?://\\S+", I18n.t("[адрес источника]"))
                .replaceAll("\\s+", " ").trim();
        return text.length() <= limit ? text : text.substring(0, limit - 1).stripTrailing() + "…";
    }

    /** Show the actual task gateways, then unused configured gateways, at most four rows. */
    private void renderGatewaySpeeds(Map<String,Object> job) {
        Map<String,Map<String,Object>> values = new LinkedHashMap<>();
        List<Object> jobRoutes = job.get("routes") instanceof List<?> ? Main.arr(job.get("routes")) : List.of();
        for (Object item : jobRoutes) {
            Map<String,Object> route = Main.obj(item);
            String gateway = Main.str(route.get("gateway"));
            if (!gateway.isBlank() && Boolean.TRUE.equals(route.get("enabled"))) values.putIfAbsent(gateway, route);
        }
        for (RouteRow row : routes) if (job.isEmpty() && row != null && values.size() < 32)
            values.putIfAbsent(row.gatewayField.getText().trim(), Map.of());
        StringBuilder text = new StringBuilder();
        for (var entry : values.entrySet()) {
            if (text.length() > 0) text.append('\n');
            Map<String,Object> route = entry.getValue();
            text.append(I18n.t("Шлюз ")).append(entry.getKey()).append(": ")
                    .append(speed(Main.num(route.get("speed_bps"), 0))).append(" · ")
                    .append(Boolean.TRUE.equals(route.get("enabled"))
                            ? bytes(Main.num(route.get("received_bytes"), Main.num(route.get("bytes"), 0)))
                            : I18n.t("не выбран"));
        }
        gatewaySpeedsArea.setText(text.toString());
    }

    private static String speed(long bps) {
        return DesktopTray.formatSpeed(bps);
    }

    private static String duration(long milliseconds) {
        long seconds = Math.max(0, milliseconds) / 1000;
        long hours = seconds / 3600;
        return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", hours, seconds / 60 % 60, seconds % 60);
    }

    private static String remaining(Map<String,Object> job) {
        String status = Main.str(job.get("status"));
        if ("complete".equals(status)) return "00:00:00";
        if ("paused".equals(status)) return I18n.t("пауза");
        if ("failed".equals(status)) return I18n.t("ошибка");
        if ("verifying".equals(status)) return I18n.t("проверка файла");
        if (!(job.get("eta_seconds") instanceof Number seconds)) return I18n.t("оценивается…");
        return "≈ " + duration(seconds.longValue() * 1000);
    }

    /** A calculated local digest is distinct from a comparison with a known reference. */
    private static void renderChecksums(StringBuilder info, Map<String,Object> job) {
        Map<String,Object> sources = checksumSources(job);
        Map<String,Object> warnings = job.get("checksum_warnings") instanceof Map<?,?> ? Main.obj(job.get("checksum_warnings")) : Map.of();
        for (String algorithm : List.of("md5", "sha256", "sha512")) {
            String name = switch (algorithm) { case "md5" -> "MD5"; case "sha512" -> "SHA-512"; default -> "SHA-256"; };
            String actual = Main.str(job.get(algorithm)), expected = Main.str(job.get("expected_" + algorithm));
            String source = Main.str(sources.get(algorithm));
            if (source.isBlank() && algorithm.equals("sha256")) source = Main.str(job.get("checksum_source"));
            info.append(I18n.t("\nПРОВЕРКА %s\n", name));
            info.append(I18n.t("Полученная: ")).append(actual.isEmpty() ? I18n.t("ожидается расчёт") : actual).append('\n');
            info.append(I18n.t("Эталон:    ")).append(expected.isEmpty() ? I18n.t("отсутствует") : expected).append('\n');
            info.append(I18n.t("Источник эталона: ")).append(expected.isEmpty() ? I18n.t("отсутствует")
                    : source.equals("manual") ? I18n.t("указан вручную") : source.isBlank() ? I18n.t("не указан") : source).append('\n');
            String result;
            if (expected.isEmpty()) result = actual.isEmpty() ? I18n.t("ожидается расчёт; эталона нет")
                    : I18n.t("локальная сумма; эталона нет");
            else if (actual.isEmpty()) result = I18n.t("ожидается проверка");
            else if (validChecksum(algorithm, actual) && validChecksum(algorithm, expected) && actual.equalsIgnoreCase(expected))
                result = I18n.t("совпадает ✓");
            else result = I18n.t("НЕ СОВПАДАЕТ ✗");
            info.append(I18n.t("Результат: ")).append(result).append('\n');
            String warning = Main.str(warnings.get(algorithm));
            if (!warning.isBlank()) info.append(I18n.t("Поиск эталона: ")).append(DiagnosticText.render(warning)).append('\n');
        }
    }

    private static Map<String,Object> checksumSources(Map<String,Object> value) {
        return value.get("checksum_sources") instanceof Map<?,?> ? Main.obj(value.get("checksum_sources")) : Map.of();
    }

    private static void renderTorrentDetails(StringBuilder info, Map<String,Object> job, String status) {
        info.append(I18n.t("Тип: BitTorrent\n"));
        info.append(I18n.t("Пиры: %d · сиды: %d\n",Main.num(job.get("peers"),0),Main.num(job.get("seeds"),0)));
        List<Object> trackers = job.get("tracker_states") instanceof List<?> ? Main.arr(job.get("tracker_states")) : List.of();
        for(Object raw:trackers){Map<String,Object> tracker=Main.obj(raw);
            info.append(I18n.t("Шлюз %s",Main.str(tracker.get("gateway")))).append(" · ")
                .append(I18n.t("active".equals(tracker.get("status"))?"Трекер активен":"Ожидание трекера")).append('\n');}
        if(Main.num(job.get("uploaded_bytes"),0)>0)info.append(I18n.t("Отдано: %s\n",bytes(Main.num(job.get("uploaded_bytes"),0))));
        String v1 = Main.str(job.get("info_hash_v1")), v2 = Main.str(job.get("info_hash_v2"));
        if (!v1.isEmpty()) info.append(I18n.t("ID раздачи v1: ")).append(v1).append("\n");
        if (!v2.isEmpty()) info.append(I18n.t("ID раздачи v2: ")).append(v2).append("\n");
        info.append(I18n.t("\nПРОВЕРКА ЧАСТЕЙ BITTORRENT\n"));
        info.append(I18n.t("Контрольные суммы частей берутся из метаданных раздачи.\n"));
        info.append(I18n.t("Результат: ")).append(Boolean.TRUE.equals(job.get("pieces_verified"))
                ? I18n.t("части проверены ✓") : I18n.t("ожидается проверка частей")).append("\n");
        info.append(I18n.t("\nКОНТРОЛЬНЫЕ СУММЫ ФАЙЛОВ\n"));
        List<Object> selected = job.get("selected_files") instanceof List<?> ? Main.arr(job.get("selected_files")) : List.of();
        List<Map<String,Object>> files = new ArrayList<>();
        List<Object> torrentFiles = job.get("files") instanceof List<?> ? Main.arr(job.get("files")) : List.of();
        for (Object value : torrentFiles) {
            Map<String,Object> file = Main.obj(value);
            String path = Main.str(file.get("path"));
            if (Boolean.TRUE.equals(file.get("pad")) || (!selected.isEmpty() && !selected.contains(path))) continue;
            files.add(file);
        }
        for (Map<String,Object> file : files) {
            info.append('\n').append(Main.str(file.get("path"))).append('\n');
            Map<String,Object> digest = new LinkedHashMap<>(file);
            if (files.size() == 1) {
                Map<String,Object> sources = new LinkedHashMap<>(checksumSources(file));
                for (String algorithm : List.of("md5", "sha256", "sha512")) {
                    if (Main.str(digest.get(algorithm)).isBlank()) digest.put(algorithm, Main.str(job.get(algorithm)));
                    String expected = Main.str(job.get("expected_" + algorithm));
                    if (!expected.isBlank()) {
                        digest.put("expected_" + algorithm, expected);
                        String source = Main.str(checksumSources(job).get(algorithm));
                        if (source.isBlank() && algorithm.equals("sha256")) source = Main.str(job.get("checksum_source"));
                        sources.put(algorithm, source);
                    }
                }
                digest.put("checksum_sources", sources);
            }
            renderChecksums(info, digest);
        }
        if (files.isEmpty() || (files.size() > 1 && hasManualChecksums(job))) renderChecksums(info, job);
    }

    private static final String VIRTUAL_IP_HELP = "Свободный IPv4 адрес в вашей локальной сети для трафика приложения через выбранные шлюзы. Например, 192.168.1.70. Адрес должен быть в одной подсети со шлюзами. Он используется внутри приложения и не добавляется сетевому адаптеру ОС; системные маршруты и подключение других программ не меняются.";

    private void showVirtualIpHelp() {
        JTextArea explanation=translatedArea(VIRTUAL_IP_HELP,8,46);
        explanation.setEditable(false); explanation.setLineWrap(true); explanation.setWrapStyleWord(true);
        explanation.setOpaque(false); explanation.setFont(UIManager.getFont("Label.font"));
        JOptionPane.showMessageDialog(frame,explanation,I18n.t("Виртуальный IP приложения"),JOptionPane.INFORMATION_MESSAGE);
    }

    private void installRouteTips(RouteRow row) {
        tip(row.checkBox,"Выбрать этот шлюз для загрузки. Быстрые шлюзы берут больше частей из общей очереди.");
        tip(row.gatewayField,"IPv4 адрес маршрутизатора с выходом в интернет. Можно добавить до 32 шлюзов в одной локальной сети.");
        tip(row.speedButton,"Короткий тест скорости этого шлюза: до 16 МиБ или 5 секунд. Размер и длительность обычных загрузок не ограничиваются этим тестом.");
        tip(row.deleteGatewayButton,"Удалить профиль шлюза. Его незавершённые части передаются оставшимся шлюзам; проверенные части сохраняются.");
    }

    private RouteRow appendGatewayRow(String ip) {
        RouteRow row=new RouteRow(ip); routes.add(row);
        row.panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        gatewayRows.add(row.panel); gatewayRows.add(Box.createVerticalStrut(8));
        gatewayRows.revalidate(); gatewayRows.repaint(); return row;
    }

    private void replaceGatewayRows(List<String> values) {
        Map<String,Boolean> selected=new LinkedHashMap<>();
        for(RouteRow row:routes){selected.put(row.gateway,row.checkBox.isSelected());row.generation++;}
        routes.clear();gatewayRows.removeAll();
        for(String ip:values){RouteRow row=appendGatewayRow(ip);row.checkBox.setSelected(selected.getOrDefault(ip,true));}
    }

    void promptAddGateway() {
        if(closing||profileChangeRunning) return;
        if(routes.size()>=32){showError("Можно настроить не более 32 шлюзов");return;}
        JTextField address=new JTextField(20); ClipboardSupport.install(address);
        JPanel content=new JPanel(new BorderLayout(0,8));content.add(translatedLabel("IPv4 адрес нового шлюза"),BorderLayout.NORTH);content.add(address,BorderLayout.CENTER);
        JCheckBox use=translatedCheckBox("Подключить к выбранной загрузке",true);
        use.setEnabled(selectedId!=null&&jobs.containsKey(selectedId)&&List.of("running","retrying","queued","paused","failed").contains(Main.str(jobs.get(selectedId).get("status"))));
        content.add(use,BorderLayout.SOUTH);
        if(JOptionPane.showConfirmDialog(frame,content,I18n.t("Добавить шлюз…"),JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION) return;
        addGatewayProfile(address.getText().trim(),use.isEnabled()&&use.isSelected()?selectedId:null);
    }

    /** Network mutation is committed before the visible profile or its selection is published. */
    void addGatewayProfile(String ip,String jobId) {
        if(closing||profileChangeRunning) return;
        NetworkInput previous;
        try{previous=networkInput();}
        catch(Exception error){showError("%s",errorArgument(error));return;}
        if(!ipv4(ip)||ip.equals(previous.virtualIp())||previous.gateways().contains(ip)){showError("Укажите уникальный IPv4 адрес шлюза");return;}
        if(previous.gateways().size()>=32){showError("Можно настроить не более 32 шлюзов");return;}
        List<String> added=new ArrayList<>(previous.gateways());added.add(ip);
        NetworkInput desired=new NetworkInput(previous.virtualIp(),List.copyOf(added));
        profileChangeRunning=true; addGatewayButton.setEnabled(false); status("Добавляем шлюз %s…",ip);
        background.execute(()->{
            Exception jobFailure=null;
            try{
                synchronized(networkConfigurationLock){networkConfigurator.configure(desired.virtualIp(),desired.gateways());configuredNetwork=desired;}
                if(jobId!=null){
                    try{
                        Map<String,Object> latest=manager.job(jobId).snapshot();
                        List<Object> selected=new ArrayList<>();
                        for(Object raw:Main.arr(latest.get("routes"))){Map<String,Object> route=Main.obj(raw);if(Main.yes(route.get("enabled")))selected.add(new LinkedHashMap<>(route));}
                        selected.add(Map.of("type","gateway","gateway",ip,"name","Шлюз "+ip,"virtual_ip",desired.virtualIp(),"enabled",true));
                        manager.updateJobRoutes(jobId,selected);
                    }catch(Exception error){jobFailure=error;}
                }
                final Exception routeFailure=jobFailure;
                SwingUtilities.invokeLater(()->{
                    if(closing)return;
                    boolean pending=restoring;restoring=true;
                    try{if(routes.stream().noneMatch(row->ip.equals(row.gateway)))appendGatewayRow(ip);}
                    finally{restoring=pending;}
                    profileChangeRunning=false;addGatewayButton.setEnabled(true);installControlTips();saveSettings();refreshJobs();
                    if(routeFailure==null) status("Шлюз %s добавлен",ip);
                    else showError("Шлюз добавлен, но не подключён к загрузке: %s",errorArgument(routeFailure));
                });
            }catch(Exception error){SwingUtilities.invokeLater(()->{if(closing)return;profileChangeRunning=false;addGatewayButton.setEnabled(true);showError("Не удалось добавить шлюз: %s",errorArgument(error));});}
        });
    }

    private void removeGatewayProfile(RouteRow row) {
        if(closing||profileChangeRunning) return;
        if(routes.size()<=1){showError("Нужен хотя бы один профиль шлюза");return;}
        JTextArea message=translatedArea("Удалить шлюз %s? Незавершённые части будут переданы другим шлюзам. Проверенные части сохранятся.",4,46);
        I18n.text(message,"Удалить шлюз %s? Незавершённые части будут переданы другим шлюзам. Проверенные части сохранятся.",row.gateway);
        message.setEditable(false);message.setOpaque(false);message.setLineWrap(true);message.setWrapStyleWord(true);
        if(JOptionPane.showConfirmDialog(frame,message,I18n.t("Удалить шлюз"),JOptionPane.OK_CANCEL_OPTION,JOptionPane.WARNING_MESSAGE)!=JOptionPane.OK_OPTION)return;
        deleteGatewayProfile(row.gateway);
    }

    void deleteGatewayProfile(String ip) {
        if(closing||profileChangeRunning)return;
        NetworkInput previous;
        try{previous=networkInput();}catch(Exception error){showError("%s",errorArgument(error));return;}
        List<String> remaining=previous.gateways().stream().filter(value->!value.equals(ip)).toList();
        if(remaining.size()==previous.gateways().size())return;
        if(remaining.isEmpty()){showError("Нужен хотя бы один профиль шлюза");return;}
        NetworkInput desired=new NetworkInput(previous.virtualIp(),remaining);
        profileChangeRunning=true;addGatewayButton.setEnabled(false);status("Удаляем шлюз %s…",ip);
        background.execute(()->{try{
            synchronized(networkConfigurationLock){networkConfigurator.configure(desired.virtualIp(),desired.gateways());configuredNetwork=desired;}
            SwingUtilities.invokeLater(()->{if(closing)return;restoring=true;try{replaceGatewayRows(remaining);}finally{restoring=false;}
                profileChangeRunning=false;addGatewayButton.setEnabled(true);saveSettings();refreshJobs();status("Шлюз %s удалён",ip);});
        }catch(Exception error){SwingUtilities.invokeLater(()->{if(closing)return;profileChangeRunning=false;addGatewayButton.setEnabled(true);showError("Не удалось удалить шлюз: %s",errorArgument(error));});}});
    }

    private void applyJobFilter() {
        String query=searchField.getText().strip().toLowerCase(java.util.Locale.ROOT),filter=Main.str(jobFilter.getSelectedItem());
        jobsSorter.setRowFilter(new RowFilter<DefaultTableModel,Integer>(){
            @Override public boolean include(Entry<? extends DefaultTableModel,? extends Integer> entry){
                int index=entry.getIdentifier();List<Map<String,Object>> rows=new ArrayList<>(jobs.values());
                if(index<0||index>=rows.size())return false;Map<String,Object> job=rows.get(index);
                String status=Main.str(job.get("status"));
                if("active".equals(filter)&&!List.of("running","retrying","queued","verifying","seeding").contains(status))return false;
                if("failed".equals(filter)&&!"failed".equals(status))return false;
                if("complete".equals(filter)&&!"complete".equals(status))return false;
                return query.isEmpty()||(Main.str(job.get("filename"))+" "+Main.str(job.get("url"))).toLowerCase(java.util.Locale.ROOT).contains(query);
            }
        });
        if(!updatingJobs){
            if(jobsTable.getSelectedRow()<0&&jobsTable.getRowCount()>0)jobsTable.setRowSelectionInterval(0,0);
            selectCurrentJob();
        }
    }

    private void installJobMenu() {
        JPopupMenu menu=new JPopupMenu();
        JButton[] controls={pauseButton,resumeButton,changeGatewaysButton,optionsButton,openFolderButton,removeButton};
        String[] sources={"Пауза","Продолжить","Изменить шлюзы…","Параметры загрузки…","Открыть папку","Удалить задание"};
        for(int i=0;i<controls.length;i++){JButton control=controls[i];JMenuItem item=new JMenuItem();I18n.text(item,sources[i]);item.addActionListener(event->control.doClick());menu.add(item);}
        jobsTable.setComponentPopupMenu(menu);
        jobsTable.addMouseListener(new MouseAdapter(){
            private void select(MouseEvent e){if(e.isPopupTrigger()){int row=jobsTable.rowAtPoint(e.getPoint());if(row>=0)jobsTable.setRowSelectionInterval(row,row);
                for(int i=0;i<controls.length;i++)menu.getComponent(i).setEnabled(controls[i].isEnabled());}}
            @Override public void mousePressed(MouseEvent e){select(e);}@Override public void mouseReleased(MouseEvent e){select(e);}
            @Override public void mouseClicked(MouseEvent e){if(e.getClickCount()==2&&SwingUtilities.isLeftMouseButton(e))openSelectedFolder();}
        });
    }

    private void openSelectedFolder() {
        Map<String,Object> job=jobs.get(selectedId);if(job==null)return;
        Path directory;
        try{directory=Path.of(Main.str(job.get("directory")));}catch(Exception error){showError("Не удалось открыть папку: %s",errorArgument(error));return;}
        background.execute(()->{try{if(!Desktop.isDesktopSupported()||!Desktop.getDesktop().isSupported(Desktop.Action.OPEN))throw new IOException("Открытие папки недоступно");Desktop.getDesktop().open(directory.toFile());}
            catch(Exception error){SwingUtilities.invokeLater(()->showError("Не удалось открыть папку: %s",errorArgument(error)));}});
    }

    private void openQueueSettings() {
        background.execute(()->{try{Map<String,Object> current=manager.queueSettings();SwingUtilities.invokeLater(()->{
            Map<String,Object> edited=DesktopOptionsDialog.queue(frame,current,routes.stream().map(row->row.gateway).toList());if(edited==null)return;
            background.execute(()->{try{manager.updateQueueSettings(edited);SwingUtilities.invokeLater(()->{status("Настройки очереди сохранены");refreshJobs();});}
                catch(Exception error){SwingUtilities.invokeLater(()->showError("Не удалось сохранить параметры: %s",errorArgument(error)));}});
        });}catch(Exception error){SwingUtilities.invokeLater(()->showError("Не удалось прочитать параметры: %s",errorArgument(error)));}});
    }

    private void openJobOptions() {
        String id=selectedId;Map<String,Object> job=jobs.get(id);if(job==null||!beginJobAction())return;
        Map<String,Object> edited=DesktopOptionsDialog.job(frame,job);if(edited==null){finishJobAction();return;}
        background.execute(()->{try{manager.updateJobOptions(id,edited);SwingUtilities.invokeLater(()->{finishJobAction();status("Параметры загрузки сохранены");refreshJobs();});}
            catch(Exception error){SwingUtilities.invokeLater(()->{finishJobAction();showError("Не удалось сохранить параметры: %s",errorArgument(error));});}});
    }

    private void installImports() {
        tip(addGatewayButton,"Добавить новый маршрутизатор в список приложения. Можно настроить до 32 шлюзов; маршруты ОС не меняются.");
        tip(bulkButton,"Вставить несколько ссылок, импортировать TXT или перетащить ссылки и torrent-файлы. Перед запуском показывается список.");
        tip(queueButton,"Задать число одновременно работающих загрузок, ограничения скорости и часы работы очереди.");
        tip(sourceAccessButton,"Настроить доступ к конкретному источнику: Basic, Bearer, Cookie, заголовки, SFTP или FTP. Эти данные не относятся к шлюзам.");
        javax.swing.TransferHandler importer=new javax.swing.TransferHandler(){
            @Override public boolean canImport(TransferSupport support){return support.isDrop()&&(support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)||support.isDataFlavorSupported(DataFlavor.stringFlavor));}
            @Override public boolean importData(TransferSupport support){if(!canImport(support))return false;try{
                List<String> values=new ArrayList<>();
                if(support.isDataFlavorSupported(DataFlavor.javaFileListFlavor))for(Object raw:(List<?>)support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor)){
                    Path path=((java.io.File)raw).toPath();
                    if(path.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".torrent"))values.add(path.toUri().toString());
                    else if(path.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".txt")){if(Files.size(path)>1048576)throw new IOException("Список ссылок превышает 1 МиБ");values.add(Files.readString(path));}}
                else values.add(Main.str(support.getTransferable().getTransferData(DataFlavor.stringFlavor)));
                importLinks(String.join("\n",values));return true;
            }catch(Exception error){showError("Не удалось импортировать ссылки: %s",errorArgument(error));return false;}}
        };
        jobsTable.setTransferHandler(importer);mainTabs.setTransferHandler(importer);
    }

    private void importLinks(String initial) {
        List<String> urls=BulkImportDialog.choose(frame,initial);if(urls==null||urls.isEmpty())return;
        if(createRunning){showError("Дождитесь подготовки текущей загрузки");return;}
        final Map<String,Object> expectedChecksums;
        final ManualChecksumText capturedChecksums = checksumText();
        try { expectedChecksums=manualChecksums();requireSingleChecksumFile(expectedChecksums,urls); }
        catch (IllegalArgumentException invalid) { showError("%s",errorArgument(invalid));return; }
        NetworkInput network;try{network=networkInput();}catch(Exception error){showError("%s",errorArgument(error));return;}
        List<SpeedTarget> selected=routes.stream().filter(row->row.checkBox.isSelected()&&gatewayProblem(row)==null).map(SpeedTarget::new).toList();
        if(selected.isEmpty()){showError("Выберите хотя бы один шлюз");return;}
        String directory=directoryField.getText().trim();long chunk=((Number)chunkSpinner.getValue()).longValue();
        createRunning=true;createButton.setEnabled(false);bulkButton.setEnabled(false);status("Подготавливаем список загрузок…");
        background.execute(()->{try{
            ensureNetwork(network);List<Object> selectedRoutes=new ArrayList<>();
            for(SpeedTarget target:selected){Map<String,Object> resolved=resolveGateway(target.gateway,target.endpoint,SpeedTest.DEFAULT_URL);
                if(!Main.yes(resolved.get("available")))throw failure("Шлюз %s: %s",target.gateway,resolvedReason(resolved));
                Map<String,Object> route=transportRoute(resolved);route.put("name",target.name());route.put("enabled",true);selectedRoutes.add(route);}
            List<Map<String,Object>> inputs=new ArrayList<>();List<Integer> sourceIndices=new ArrayList<>();List<String> preparationErrors=new ArrayList<>();
            for(int sourceIndex=0;sourceIndex<urls.size();sourceIndex++){
                String url=urls.get(sourceIndex);Map<String,Object> input=new LinkedHashMap<>();input.put("url",url);input.put("directory",directory);input.put("chunk_mb",chunk);input.put("routes",selectedRoutes);
                input.putAll(expectedChecksums);
                try{
                    boolean torrent=TorrentSource.isTorrentSource(url),hub=HubSource.isListingUrl(url),generic=Metalink.isMetalink(url)||WebDav.isWebDav(url);
                    if(torrent||hub||generic){
                        Map<String,Object> inspection=manager.inspectSource(input);AtomicReference<List<String>> chosen=new AtomicReference<>();
                        SwingUtilities.invokeAndWait(()->{if(!closing)chosen.set(torrent?torrentFileSelector.choose(frame,inspection):hub?hubFileSelector.choose(frame,inspection):SourceFileDialog.choose(frame,inspection));});
                        if(chosen.get()==null){SwingUtilities.invokeLater(()->{bulkButton.setEnabled(true);finishCreate("Выбор файлов отменён");});return;}
                        requireCurrentChecksums(expectedChecksums,capturedChecksums);
                        requireSingleChecksumFile(input,chosen.get());
                        input.put("selected_files",chosen.get());if(torrent)input.put("source_type","torrent");
                        if(hub)input.put("revision",Main.str(inspection.get("revision")));
                    }
                    inputs.add(input);sourceIndices.add(sourceIndex);
                }catch(Exception error){preparationErrors.add((sourceIndex+1)+": "+DiagnosticText.render(message(error)));}
            }
            if(closing)return;
            requireCurrentChecksums(expectedChecksums,capturedChecksums);
            List<Object> combined=new ArrayList<>();long successCount=0;
            for(int offset=0;offset<inputs.size();offset+=128){
                Map<String,Object> batch=manager.batchCreate(inputs.subList(offset,Math.min(inputs.size(),offset+128)));
                successCount+=Main.num(batch.get("success_count"),0);
                for(Object raw:Main.arr(batch.get("results"))){Map<String,Object> row=new LinkedHashMap<>(Main.obj(raw));row.put("index",Main.num(row.get("index"),0)+offset);combined.add(row);}
            }
            Map<String,Object> result=Map.of("results",combined,"success_count",successCount);
            SwingUtilities.invokeLater(()->{if(closing)return;bulkButton.setEnabled(true);finishCreate("Добавлено загрузок: %d из %d",Main.num(result.get("success_count"),0),urls.size());refreshJobs();
                List<String> errors=new ArrayList<>(preparationErrors);for(Object raw:Main.arr(result.get("results"))){Map<String,Object> row=Main.obj(raw);int index=(int)Main.num(row.get("index"),0);if(!Main.yes(row.get("ok")))errors.add((sourceIndices.get(index)+1)+": "+DiagnosticText.render(Main.str(row.get("error"))));}
                if(!errors.isEmpty()){JTextArea text=new JTextArea(String.join("\n",errors),8,60);text.setEditable(false);text.setLineWrap(true);text.setWrapStyleWord(true);JOptionPane.showMessageDialog(frame,new JScrollPane(text),I18n.t("Результаты импорта"),JOptionPane.WARNING_MESSAGE);}});
        }catch(Exception error){SwingUtilities.invokeLater(()->{bulkButton.setEnabled(true);finishCreate("Не удалось импортировать ссылки: %s",errorArgument(error));showError("Не удалось импортировать ссылки: %s",errorArgument(error));});}});
    }

    private void changeJobGateways() {
        String id = selectedId;
        Map<String,Object> job = jobs.get(id);
        if (job == null || !beginJobAction()) return;
        List<Object> selected;
        try {
            Map<String,Object> settings = networkSettingsProvider == null ? GatewayNetwork.settings() : networkSettingsProvider.settings();
            selected = JobGatewayDialog.choose(frame, job, settings);
        } catch (Exception error) {
            finishJobAction(); showError("Не удалось изменить шлюзы: %s", errorArgument(error)); return;
        }
        if (selected == null || closing) { finishJobAction(); return; }
        status("Изменяем шлюзы загрузки…");
        background.execute(() -> {
            try {
                Map<String,Object> changed = manager.updateJobRoutes(id, selected);
                SwingUtilities.invokeLater(() -> {
                    if (closing) return;
                    if (jobs.containsKey(id)) jobs.put(id, changed);
                    finishJobAction();
                    status("Шлюзы изменены. Скачанные части сохранены.");
                    refreshJobs();
                });
            } catch (Exception error) {
                SwingUtilities.invokeLater(() -> {
                    if (closing) return;
                    finishJobAction(); showError("Не удалось изменить шлюзы: %s", errorArgument(error)); refreshJobs();
                });
            }
        });
    }

    private void changeJob(boolean resume) {
        String id = selectedId;
        if (id == null || !beginJobAction()) return;
        background.execute(() -> {
            try {
                manager.action(id, resume ? "resume" : "pause");
                SwingUtilities.invokeLater(() -> {
                    if (closing) return;
                    finishJobAction();
                    status(resume ? "Загрузка продолжена" : "Загрузка приостановлена");
                    refreshJobs();
                });
            } catch (Exception error) {
                SwingUtilities.invokeLater(() -> {
                    if (closing) return;
                    finishJobAction();
                    showError("Не удалось изменить состояние загрузки: %s%s", errorArgument(error),
                            (isNoSpaceError(message(error)) ? new UiText("\nНажмите «Изменить папку…», выберите папку со свободным местом, затем «Продолжить».") : ""));
                    refreshJobs();
                });
            }
        });
    }

    private boolean beginJobAction() {
        if (closing || jobActionRunning || selectedId == null || !jobs.containsKey(selectedId)) return false;
        jobActionRunning = true;
        jobsGeneration++;
        renderDetails(jobs.get(selectedId));
        return true;
    }

    private void finishJobAction() {
        jobActionRunning = false;
        jobsGeneration++;
        renderDetails(jobs.get(selectedId));
    }

    private void removeJob() {
        String id = selectedId;
        Map<String,Object> job = jobs.get(id);
        if (job == null || !beginJobAction()) return;
        DeleteChoice choice;
        try { choice = jobDeletionConfirmation.confirm(frame, new LinkedHashMap<>(job)); }
        catch (Exception error) {
            finishJobAction(); showError("Не удалось открыть подтверждение удаления: %s", errorArgument(error)); return;
        }
        if (choice == null || choice == DeleteChoice.CANCEL || closing) {
            finishJobAction(); if (!closing) status("Удаление отменено"); return;
        }
        boolean deleteFiles = choice == DeleteChoice.DELETE_FILES;
        status(deleteFiles ? "Удаляем задание, файлы и скачанные части…" : "Удаляем запись о загрузке…");
        background.execute(() -> {
            try {
                manager.remove(id, deleteFiles);
                SwingUtilities.invokeLater(() -> {
                    if (closing) return;
                    jobs.remove(id);
                    if (id.equals(selectedId)) selectedId = null;
                    finishJobAction();
                    updateJobs(new ArrayList<>(jobs.values()));
                    status(deleteFiles ? "Задание, его файлы и скачанные части удалены."
                            : "Запись удалена. Файлы и скачанные части сохранены.");
                    refreshJobs();
                });
            } catch (Exception error) {
                SwingUtilities.invokeLater(() -> {
                    if (closing) return;
                    finishJobAction();
                    showError("Не удалось удалить задание: %s", errorArgument(error));
                    refreshJobs();
                });
            }
        });
    }

    private void changeJobDirectory() {
        String id = selectedId;
        Map<String,Object> job = jobs.get(id);
        if (job == null || !List.of("paused", "failed").contains(Main.str(job.get("status")))
                || !beginJobAction()) return;
        String directory;
        try { directory = jobDirectorySelector.choose(frame, Main.str(job.get("directory"))); }
        catch (Exception error) {
            finishJobAction(); showError("Не удалось выбрать папку: %s", errorArgument(error)); return;
        }
        if (directory == null || directory.isBlank() || closing) {
            finishJobAction(); if (!closing) status("Выбор папки отменён"); return;
        }
        status("Переносим скачанные части в выбранную папку…");
        background.execute(() -> {
            try {
                Map<String,Object> changed = manager.changeDirectory(id, directory);
                SwingUtilities.invokeLater(() -> {
                    if (closing) return;
                    if (jobs.containsKey(id)) jobs.put(id, changed);
                    finishJobAction();
                    status("Папка изменена. Скачанные части сохранены; нажмите «Продолжить».");
                    refreshJobs();
                });
            } catch (Exception error) {
                SwingUtilities.invokeLater(() -> {
                    if (closing) return;
                    finishJobAction();
                    showError("Не удалось изменить папку: %s", errorArgument(error));
                    refreshJobs();
                });
            }
        });
    }

    private static boolean isNoSpaceError(String error) {
        String text = error.toLowerCase(java.util.Locale.ROOT);
        return (text.contains("недостаточно") && text.contains("мест"))
                || (text.contains("not enough") && text.contains("space"))
                || text.contains("no space left") || text.contains("disk full");
    }

    private void restoreSettings() {
        if (!Files.exists(settingsPath)) return;
        restoring = true;
        try {
            Map<String,Object> settings = Main.obj(Json.parse(Files.readString(settingsPath)));
            String savedDirectory = Main.str(settings.get("directory"));
            if (DIRECTORY_POLICY.equals(settings.get("directory_policy")))
                directoryField.setText(defaultDownloadDirectory().toString());
            else if (!savedDirectory.isBlank()) directoryField.setText(savedDirectory);
            long chunk = Main.num(settings.get("chunk_mb"), 20);
            if (chunk >= 1 && chunk <= 64) chunkSpinner.setValue((int) chunk);
            String virtualIp = Main.str(settings.get("virtual_ip"));
            if (!virtualIp.isBlank()) virtualIpField.setText(virtualIp);
            Object savedGateways = settings.get("gateways");
            if (savedGateways instanceof List<?> values && !values.isEmpty() && values.size() <= 32
                    && values.stream().allMatch(value -> value instanceof String text && ipv4(text))
                    && values.stream().distinct().count() == values.size())
                replaceGatewayRows(values.stream().map(Main::str).toList());
            Object configured = settings.get("endpoints");
            if (configured instanceof Map<?,?> endpoints) {
                for (RouteRow route : routes) {
                    Object value = endpoints.get(route.gateway);
                    if (value instanceof String text) route.endpoint = text;
                }
            }
            Object savedSelected = settings.get("selected_gateways");
            if (SELECTION_POLICY.equals(settings.get("gateway_selection_policy"))
                    && savedSelected instanceof List<?> selected && selected.size() == routes.size())
                for (int i = 0; i < routes.size(); i++) routes.get(i).checkBox.setSelected(Boolean.TRUE.equals(selected.get(i)));
            status("Настройки восстановлены; шлюзы нужно проверить повторно");
        } catch (Exception error) {
            status("Настройки окна не прочитаны: %s", errorArgument(error));
        } finally { restoring = false; validateGatewayRows(); }
    }

    void saveSettings() {
        if (settingsWriter.isShutdown()) return;
        Map<String,Object> endpoints = new LinkedHashMap<>();
        List<String> gateways = new ArrayList<>();
        List<Boolean> selectedGateways = new ArrayList<>();
        for (RouteRow route : routes) {
            gateways.add(route.gatewayField.getText().trim());
            selectedGateways.add(route.checkBox.isSelected() && gatewayProblem(route) == null);
            if (gatewayProblem(route) == null && !"gateway".equals(route.routeType) && !route.endpoint.isBlank())
                endpoints.put(route.gateway, route.endpoint);
        }
        Map<String,Object> settings = new LinkedHashMap<>();
        settings.put("directory", directoryField.getText().trim());
        try {
            settings.put("directory_policy", Path.of(directoryField.getText().trim()).toAbsolutePath().normalize()
                    .equals(defaultDownloadDirectory()) ? DIRECTORY_POLICY : "custom");
        } catch (java.nio.file.InvalidPathException ignored) {
            settings.put("directory_policy", "custom");
        }
        settings.put("language", I18n.language());
        settings.put("chunk_mb", ((Number) chunkSpinner.getValue()).longValue());
        String virtualIp = virtualIpField.getText().trim();
        settings.put("virtual_ip", ipv4(virtualIp) ? virtualIp :
                configuredNetwork == null ? "192.168.1.70" : configuredNetwork.virtualIp());
        settings.put("gateways", gateways);
        settings.put("selected_gateways", selectedGateways);
        settings.put("gateway_selection_policy", SELECTION_POLICY);
        settings.put("endpoints", endpoints);
        settingsWriter.execute(() -> {
            try {
                synchronized (manager) {
                    // API and Manager own the applied network. A delayed UI settings write
                    // must not restore an older configuration after an API update.
                    if (networkSettingsProvider != null) {
                        NetworkInput applied = networkSettingsInput(networkSettingsProvider.settings());
                        if (applied != null) {
                            settings.put("virtual_ip", applied.virtualIp());
                            settings.put("gateways", applied.gateways());
                        }
                    }
                    Main.save(settingsPath, settings);
                }
            }
            catch (Exception error) { SwingUtilities.invokeLater(() -> status("Не удалось сохранить настройки окна: %s", errorArgument(error))); }
        });
    }

    static Path defaultDownloadDirectory() {
        return defaultDownloadDirectory(System.getProperty("os.name", ""),
                System.getProperty("user.home"), System.getenv());
    }

    /** Resolve the current profile at launch instead of storing a particular user's default. */
    static Path defaultDownloadDirectory(String operatingSystem, String userHome, Map<String,String> environment) {
        Path home = Path.of(userHome).toAbsolutePath().normalize();
        if (operatingSystem.toLowerCase(java.util.Locale.ROOT).startsWith("windows")) {
            String profile = environment.get("USERPROFILE");
            if (profile != null && !profile.isBlank()) try {
                Path current = Path.of(profile.trim());
                if (current.isAbsolute()) home = current.normalize();
            } catch (java.nio.file.InvalidPathException ignored) { }
        }
        return home.resolve("Downloads");
    }

    static String statusName(String status) {
        return switch (status) {
            case "queued" -> I18n.t("В очереди");
            case "running" -> I18n.t("Загрузка");
            case "retrying" -> I18n.t("Повторная попытка");
            case "verifying" -> I18n.t("Проверка");
            case "paused" -> I18n.t("Пауза");
            case "failed" -> I18n.t("Ошибка");
            case "complete" -> I18n.t("Готово");
            case "seeding" -> I18n.t("Раздача");
            default -> status;
        };
    }

    static String bytes(long size) {
        if(size<0)return I18n.t("неизвестен");
        if (size < 1024) return I18n.t("%d Б", size);
        if (size < 1024L * 1024) return I18n.t("%.1f КБ", size / 1024.0);
        if (size < 1024L * 1024 * 1024) return I18n.t("%.1f МБ", size / (1024.0 * 1024));
        return I18n.t("%.2f ГБ", size / (1024.0 * 1024 * 1024));
    }

    private static String message(Throwable error) {
        String text = error.getMessage();
        return text == null || text.isBlank() ? error.getClass().getSimpleName() : text;
    }

    private static Object[] logArguments(Object[] args) {
        Object[] canonical = args.clone();
        for (int i = 0; i < canonical.length; i++) {
            if (canonical[i] instanceof UiText text) canonical[i] = AppLog.arg(text.source(), logArguments(text.args()));
        }
        return canonical;
    }

    private void status(String value, Object... args) {
        I18n.text(statusLabel, "%s", new UiText(value, args));
        Object[] canonical = logArguments(args);
        StringBuilder key = new StringBuilder(value);
        for (Object argument : args) key.append('\u0000').append(argument instanceof UiText nested ? nested.raw()
                : argument instanceof DiagnosticText.Arg diagnostic ? diagnostic.raw() : argument);
        if (!key.toString().equals(lastLoggedStatus)) {
            lastLoggedStatus = key.toString();
            AppLog.info(value, canonical);
        }
    }

    private void showError(String source, Object... args) {
        String value = new UiText(source, args).toString();
        AppLog.error(source, logArguments(args));
        if (frame.isShowing()) JOptionPane.showMessageDialog(frame, value, "MultiRoute Resume", JOptionPane.ERROR_MESSAGE);
        else I18n.text(statusLabel, "%s", new UiText(source, args));
    }
}
