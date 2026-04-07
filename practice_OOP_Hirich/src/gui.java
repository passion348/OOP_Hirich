import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class gui implements task6.Observer {

    private JTextField inputField;
    private JTextField paramField;
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel statsLabel;
    private List<task6.ShapeResult> results;
    private task6.WorkerThread worker;
    private task6.ObservableResults observable;
    private GraphPanel graph;

    public gui(task6.WorkerThread worker,
               List<task6.ShapeResult> results,
               task6.ObservableResults observable) {

        this.worker = worker;
        this.results = results;
        this.observable = observable;

        observable.addObserver(this);

        JFrame frame = new JFrame("Gui");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        inputField = new JTextField(8);
        paramField = new JTextField(5);
        JButton addBtn = new JButton("Додати");

        topPanel.add(new JLabel("Binary:"));
        topPanel.add(inputField);
        topPanel.add(addBtn);
        topPanel.add(new JLabel("Параметр:"));
        topPanel.add(paramField);

        /* Кнопки */
        JPanel buttonPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        JButton scaleBtn = new JButton("Масштаб");
        JButton sortBtn = new JButton("Сортування");
        JButton normBtn = new JButton("Нормалізація");
        JButton searchBtn = new JButton("Пошук");
        JButton macroBtn = new JButton("Макрокоманда");
        JButton statsBtn = new JButton("Статистика");
        JButton filterBtn = new JButton("Фільтр");
        JButton undoBtn = new JButton("Відмінити");

        buttonPanel.add(scaleBtn);
        buttonPanel.add(sortBtn);
        buttonPanel.add(normBtn);
        buttonPanel.add(searchBtn);
        buttonPanel.add(macroBtn);
        buttonPanel.add(statsBtn);
        buttonPanel.add(filterBtn);
        buttonPanel.add(undoBtn);

        /* Таблиця */
        String[] columns = {"Side", "Result"};
        tableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);

        /* Графіка */
        graph = new GraphPanel(results);

        /* Статистика   */
        statsLabel = new JLabel("Статистика:");

        /* Розташування */
        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(buttonPanel, BorderLayout.WEST);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(graph, BorderLayout.SOUTH);
        frame.add(statsLabel, BorderLayout.PAGE_END);

        addBtn.addActionListener(e -> {
            try {
                String bin = inputField.getText();
                task6.ShapeData data = new task6.ShapeData(bin);
                task6.ShapeResult r = new task6.TriangleFactory().create(data);
                r.calculate();
                results.add(r);
                task6.observable.notifyObservers();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Помилка");
            }
        });

        scaleBtn.addActionListener(e -> {
            try {
                double factor = Double.parseDouble(paramField.getText());
                worker.addCommand(new task6.ScaleCommand(results, factor));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Помилка");
            }
        });

        sortBtn.addActionListener(e -> {
            worker.addCommand(new task6.SortCommand(results));
        });

        normBtn.addActionListener(e -> {
            worker.addCommand(new task6.NormalizeCommand(results));
        });

        searchBtn.addActionListener(e -> {
            try {
                double val = Double.parseDouble(paramField.getText());
                highlightSearch(val);
            } catch (Exception ex) {}
        });

        macroBtn.addActionListener(e -> {
            task6.MacroCommand macro = new task6.MacroCommand();
            macro.add(new task6.ScaleCommand(results, 2));
            macro.add(new task6.SortCommand(results));
            worker.addCommand(macro);
        });

        statsBtn.addActionListener(e -> {
            double min = results.stream().mapToDouble(r -> r.data.getResult()).min().orElse(0);
            double max = results.stream().mapToDouble(r -> r.data.getResult()).max().orElse(0);
            double avg = results.stream().mapToDouble(r -> r.data.getResult()).average().orElse(0);

            statsLabel.setText("Min=" + min + " Max=" + max + " Avg=" + avg);
        });

        filterBtn.addActionListener(e -> {
            try {
                double t = Double.parseDouble(paramField.getText());
                worker.addCommand(new task6.FilterCommand(results, t));
            } catch (Exception ex) {}
        });

        undoBtn.addActionListener(e -> {
            task6.UndoCommand cmd = new task6.UndoCommand();
            worker.addCommand(cmd);
        });

        frame.setVisible(true);
    }

    /* Оновлення даних через Observer */
    @Override
    public void update() {
        refreshTable();
        graph.repaint();

        double min = results.stream().mapToDouble(r -> r.data.getResult()).min().orElse(0);
        double max = results.stream().mapToDouble(r -> r.data.getResult()).max().orElse(0);
        double avg = results.stream().mapToDouble(r -> r.data.getResult()).average().orElse(0);
        statsLabel.setText(String.format("Статистика: Min=%.2f  Max=%.2f  Avg=%.2f", min, max, avg));
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (task6.ShapeResult r : results) {
            tableModel.addRow(new Object[]{
                    r.data.getSide(),
                    r.data.getResult()
            });
        }
    }

    private void highlightSearch(double val) {
        table.clearSelection();
        for (int i = 0; i < results.size(); i++) {
            if (Math.abs(results.get(i).data.getResult() - val) < 0.0001) {
                table.addRowSelectionInterval(i, i);
            }
        }
    }

    class GraphPanel extends JPanel {
        private List<task6.ShapeResult> results;

        public GraphPanel(List<task6.ShapeResult> results) {
            this.results = results;
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            int x = 20;
            for (task6.ShapeResult r : results) {
                int h = (int) r.data.getResult();
                g.fillRect(x, 200 - h, 20, h);
                x += 30;
            }
        }
    }
}