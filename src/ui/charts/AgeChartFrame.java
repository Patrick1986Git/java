package ui.charts;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import dto.statistics.AgeDistributionDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;

/**
 * Swing frame that shows an XY line chart with three series and a control panel
 * containing three checkboxes + colored square indicators.
 */
public class AgeChartFrame extends JFrame {

    private final XYSeries personsSeries;
    private final XYSeries employeesSeries;
    private final XYSeries studentsSeries;
    private final XYSeriesCollection dataset;

    public AgeChartFrame(AgeDistributionDTO data) {
        setTitle("Age Distribution Chart");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        // Defensive: allow null DTO or missing maps
        Map<Integer, Long> personsMap = data == null ? null : data.getPersons();
        Map<Integer, Long> employeesMap = data == null ? null : data.getEmployees();
        Map<Integer, Long> studentsMap = data == null ? null : data.getStudents();

        // Build series
        personsSeries = toSeries("Persons", personsMap);
        employeesSeries = toSeries("Employees", employeesMap);
        studentsSeries = toSeries("Students", studentsMap);

        dataset = new XYSeriesCollection();
        dataset.addSeries(personsSeries);
        dataset.addSeries(employeesSeries);
        dataset.addSeries(studentsSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Age Distribution",
                "Age",
                "Count",
                dataset
        );

        XYPlot plot = chart.getXYPlot();
        XYItemRenderer renderer = plot.getRenderer();

        // set series paints
        final Color COLOR_PERSONS = new Color(0, 180, 0);
        final Color COLOR_EMPLOYEES = new Color(0, 90, 255);
        final Color COLOR_STUDENTS = new Color(160, 90, 0);

        renderer.setSeriesPaint(0, COLOR_PERSONS);
        renderer.setSeriesPaint(1, COLOR_EMPLOYEES);
        renderer.setSeriesPaint(2, COLOR_STUDENTS);

        // initial visibility
        renderer.setSeriesVisible(0, Boolean.TRUE);
        renderer.setSeriesVisible(1, Boolean.TRUE);
        renderer.setSeriesVisible(2, Boolean.TRUE);

        // Top control panel with three checkboxes + colored squares
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        controls.setBorder(new EmptyBorder(6, 6, 6, 6));

        JCheckBox cbPersons = new JCheckBox("PERSONS", true);
        JLabel colorPersons = new JLabel(new ColorIcon(COLOR_PERSONS, 14, 14));

        JCheckBox cbEmployees = new JCheckBox("EMPLOYEES", true);
        JLabel colorEmployees = new JLabel(new ColorIcon(COLOR_EMPLOYEES, 14, 14));

        JCheckBox cbStudents = new JCheckBox("STUDENTS", true);
        JLabel colorStudents = new JLabel(new ColorIcon(COLOR_STUDENTS, 14, 14));

        // Wire actions — toggle renderer visibility
        cbPersons.addActionListener(e -> {
            boolean sel = cbPersons.isSelected();
            renderer.setSeriesVisible(0, sel);
        });
        cbEmployees.addActionListener(e -> {
            boolean sel = cbEmployees.isSelected();
            renderer.setSeriesVisible(1, sel);
        });
        cbStudents.addActionListener(e -> {
            boolean sel = cbStudents.isSelected();
            renderer.setSeriesVisible(2, sel);
        });

        controls.add(cbPersons);
        controls.add(colorPersons);
        controls.add(cbEmployees);
        controls.add(colorEmployees);
        controls.add(cbStudents);
        controls.add(colorStudents);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(controls, BorderLayout.NORTH);
        getContentPane().add(new ChartPanel(chart), BorderLayout.CENTER);
    }

    private XYSeries toSeries(String name, Map<Integer, Long> map) {
        XYSeries s = new XYSeries(name);
        if (map != null && !map.isEmpty()) {
            map.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> {
                        // defensive: convert to double to avoid integer issues
                        Number x = e.getKey();
                        Number y = e.getValue();
                        if (x != null && y != null) {
                            s.add(x.doubleValue(), y.doubleValue());
                        }
                    });
        }
        return s;
    }

    // Simple Icon implementation that paints a filled square with a dark border
    private static class ColorIcon implements Icon {
        private final Color color;
        private final int w;
        private final int h;

        ColorIcon(Color color, int w, int h) {
            this.color = color;
            this.w = w;
            this.h = h;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setColor(color);
                g2.fillRect(x, y, w, h);
                g2.setColor(Color.DARK_GRAY);
                g2.drawRect(x, y, w - 1, h - 1);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return w;
        }

        @Override
        public int getIconHeight() {
            return h;
        }
    }
}
