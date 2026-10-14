package multiroute;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;

/** Changes one job's selection within the application's configured gateway profiles. */
final class JobGatewayDialog {
    private JobGatewayDialog() { }

    static List<Map<String,Object>> options(Map<String,Object> job, Map<String,Object> settings) {
        List<Map<String,Object>> result = new ArrayList<>();
        List<?> profiles = settings.get("gateways") instanceof List<?> list ? list : List.of();
        List<?> previous = job.get("routes") instanceof List<?> list ? list : List.of();
        for (Object raw : profiles) {
            String gateway = Main.str(raw);
            if (gateway.isBlank() || result.stream().anyMatch(route -> gateway.equals(route.get("gateway")))) continue;
            boolean enabled = previous.stream().anyMatch(value -> value instanceof Map<?,?> route
                    && "gateway".equals(route.get("type")) && gateway.equals(route.get("gateway")) && Main.yes(route.get("enabled")));
            Map<String,Object> route = new LinkedHashMap<>();
            route.put("type", "gateway"); route.put("gateway", gateway); route.put("name", "Шлюз " + gateway);
            route.put("virtual_ip", Main.str(settings.get("virtual_ip"))); route.put("enabled", enabled);
            result.add(route);
        }
        return result;
    }

    static List<Object> choose(JFrame owner, Map<String,Object> job, Map<String,Object> settings) {
        java.util.concurrent.atomic.AtomicReference<List<Object>> answer = new java.util.concurrent.atomic.AtomicReference<>();
        JDialog dialog = create(owner, job, settings, answer::set);
        dialog.setVisible(true); return answer.get();
    }

    /** Shared dialog construction for hidden, synthetic UI checks. */
    static JDialog create(JFrame owner, Map<String,Object> job, Map<String,Object> settings,
            java.util.function.Consumer<List<Object>> chosen) {
        JDialog dialog = new JDialog(owner, true);
        I18n.title(dialog, "Шлюзы выбранной загрузки");
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JPanel body = new JPanel(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints cell = new java.awt.GridBagConstraints();
        cell.gridx = 0; cell.gridy = 0; cell.weightx = 1;
        cell.fill = java.awt.GridBagConstraints.HORIZONTAL; cell.anchor = java.awt.GridBagConstraints.NORTHWEST;
        cell.insets = new java.awt.Insets(0, 0, 12, 0);
        body.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        JTextArea explanation = note("Выберите шлюзы для этой загрузки. Добавленные шлюзы берут части из общей очереди. Незавершённые части отключённого шлюза будут переданы другим; проверенные части сохраняются.");
        explanation.setColumns(48);
        body.add(explanation, cell);
        List<Map<String,Object>> options = options(job, settings);
        JPanel checks = new JPanel(); checks.setLayout(new BoxLayout(checks, BoxLayout.Y_AXIS));
        List<JCheckBox> selections = new ArrayList<>();
        for (Map<String,Object> route : options) {
            JCheckBox check = new JCheckBox();
            I18n.text(check, "Шлюз %s", route.get("gateway"));
            check.setSelected(Main.yes(route.get("enabled"))); selections.add(check); checks.add(check);
        }
        JScrollPane choices = new JScrollPane(checks);
        choices.setBorder(null);
        choices.setPreferredSize(new Dimension(500, Math.min(380,Math.max(50, options.size()*30))));
        cell.gridy++; body.add(choices, cell);
        JPanel bottom = new JPanel(new BorderLayout(0, 10));
        JTextArea hint = note("Не менее одного шлюза. Через 60 секунд без поступления данных часть возвращается в общую очередь.");
        hint.setColumns(48); bottom.add(hint, BorderLayout.NORTH);
        JButton apply = new JButton(), cancel = new JButton();
        I18n.text(apply, "Применить"); I18n.text(cancel, "Отмена");
        Runnable validate = () -> apply.setEnabled(selections.stream().anyMatch(JCheckBox::isSelected));
        for (JCheckBox check : selections) check.addActionListener(event -> validate.run());
        validate.run();
        apply.addActionListener(event -> {
            List<Object> desired = new ArrayList<>();
            for (int i = 0; i < options.size(); i++) if (selections.get(i).isSelected()) {
                Map<String,Object> route = new LinkedHashMap<>(options.get(i)); route.put("enabled", true); desired.add(route);
            }
            if (desired.isEmpty()) return;
            chosen.accept(desired); dialog.dispose();
        });
        cancel.addActionListener(event -> dialog.dispose());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0)); actions.add(cancel); actions.add(apply);
        bottom.add(actions, BorderLayout.SOUTH); cell.gridy++; cell.insets = new java.awt.Insets(0,0,0,0); body.add(bottom, cell);
        dialog.setContentPane(body); dialog.getRootPane().setDefaultButton(apply);
        dialog.getRootPane().registerKeyboardAction(event -> dialog.dispose(),
                javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack(); dialog.validate();
        // Wrapped note heights depend on the allocated width. Recompute after the first layout;
        // every checkbox keeps its preferred height instead of sharing a compressed CENTER area.
        dialog.pack(); dialog.validate(); dialog.pack();
        dialog.setMinimumSize(dialog.getSize()); dialog.setLocationRelativeTo(owner);
        return dialog;
    }

    private static JTextArea note(String source) {
        JTextArea area = new JTextArea(); area.setEditable(false); area.setOpaque(false);
        area.setLineWrap(true); area.setWrapStyleWord(true); area.setFont(javax.swing.UIManager.getFont("Label.font"));
        I18n.text(area, source); return area;
    }
}
