import javax.swing.*;

import javax.swing.table.*;

import java.awt.*;

import java.sql.*;

import java.util.*;

import org.jfree.chart.*;

import org.jfree.chart.plot.*;

import org.jfree.chart.renderer.category.BarPainter;

import org.jfree.chart.renderer.category.BarRenderer;

import org.jfree.chart.renderer.xy.StandardXYBarPainter;

import org.jfree.data.category.DefaultCategoryDataset;

import org.jfree.data.general.DefaultPieDataset;

import org.jfree.data.statistics.HistogramDataset;

import org.jfree.chart.ui.ApplicationFrame;

import org.jfree.chart.ui.UIUtils;



public class UniversityAdmissionsAnalysis extends JFrame {

    private Connection conn;

    private JTable table;

    private DefaultTableModel tableModel;



    public UniversityAdmissionsAnalysis() {

        setTitle("University Admissions Analysis Dashboard");

        setSize(1000, 600);

        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setLocationRelativeTo(null);



        // --- GUI Layout ---

        JPanel topPanel = new JPanel();

        topPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

        topPanel.setBackground(new Color(245, 245, 245));



        // layout of the user interface(GUI)

        JButton btnAcceptanceRates = new JButton("Acceptance Rates per Program");

        JButton btnAvgScores = new JButton("Average Exam Score per Program");

        JButton btnCityGender = new JButton("Distribution by City & Gender");

        JButton btnTopApplicants = new JButton("Top 10 Applicants");

        JButton btnVisuals = new JButton("Show Charts");



        Font buttonFont = new Font("Segoe UI", Font.BOLD, 13);

        for (JButton btn : new JButton[]{btnAcceptanceRates, btnAvgScores, btnCityGender, btnTopApplicants, btnVisuals}) {

            btn.setFont(buttonFont);

            btn.setBackground(new Color(0, 120, 215));

            btn.setForeground(Color.WHITE);

            btn.setFocusPainted(false);

            topPanel.add(btn);

        }



        // --- Table Setup ---

        tableModel = new DefaultTableModel();

        table = new JTable(tableModel);

        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        table.setRowHeight(28);

        table.setGridColor(new Color(220, 220, 220));

        table.setShowGrid(true);

        table.setFillsViewportHeight(true);

        table.getTableHeader().setBackground(new Color(50, 90, 160));

        table.getTableHeader().setForeground(Color.WHITE);

        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 15));



        JScrollPane scrollPane = new JScrollPane(table);

        scrollPane.setBorder(BorderFactory.createTitledBorder("📊 Analysis Results"));



        add(topPanel, BorderLayout.NORTH);

        add(scrollPane, BorderLayout.CENTER);



        // --- Database Connection ---

        connectDB();



        // --- Button Actions ---

        btnAcceptanceRates.addActionListener(e -> showAcceptanceRates());

        btnAvgScores.addActionListener(e -> showAverageScores());

        btnCityGender.addActionListener(e -> showCityGenderDistribution());

        btnTopApplicants.addActionListener(e -> showTopApplicants());

        btnVisuals.addActionListener(e -> showCharts());

    }



    private void connectDB() { //connecting the database using JDBC

        try {

            conn = DriverManager.getConnection(

                    "jdbc:mysql://localhost:3306/datafiles",

                    "root", ""

            );

            System.out.println("✅ Connected to Database!");

        } catch (SQLException e) {

            JOptionPane.showMessageDialog(this, "DB Connection Failed: " + e.getMessage());

        }

    }



    private void showAcceptanceRates() {    //query to calculate acceptance rates per program

        String query = """

           SELECT program,

                  SUM(CASE WHEN status='accepted' THEN 1 ELSE 0 END) AS accepted,

                  COUNT(*) AS total,

                  ROUND((SUM(CASE WHEN status='accepted' THEN 1 ELSE 0 END) / COUNT(*)) * 100, 2) AS acceptance_rate

           FROM applications

           GROUP BY program;

       """;

        loadTable(query);

    }



    private void showAverageScores() {  //query to calculate average scores

        String query = """

           SELECT a.program, ROUND(AVG(e.score),2) AS average_score

           FROM exam_scores e

           JOIN applicants ap ON e.applicant_id = ap.applicant_id

           JOIN applications a ON ap.applicant_id = a.applicant_id

           GROUP BY a.program;

       """;

        loadTable(query);

    }



    private void showCityGenderDistribution() { //city gender distribution query

        String query = """

           SELECT city, gender, COUNT(*) AS total

           FROM applicants

           GROUP BY city, gender

           ORDER BY city;

       """;

        loadTable(query);

    }



    private void showTopApplicants() {  //query to display top 10 applicants

        String query = """

           SELECT ap.first_name, ap.last_name, ROUND(AVG(e.score),2) AS average_score

           FROM exam_scores e

           JOIN applicants ap ON e.applicant_id = ap.applicant_id

           GROUP BY ap.applicant_id

           ORDER BY average_score DESC

           LIMIT 10;

       """;

        loadTable(query);

    }



    private void loadTable(String query) {

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {

            ResultSetMetaData meta = rs.getMetaData();

            int columnCount = meta.getColumnCount();



            // Set table headers

            tableModel.setRowCount(0);

            tableModel.setColumnCount(0);

            for (int i = 1; i <= columnCount; i++) {

                tableModel.addColumn(meta.getColumnLabel(i));

            }



            // Add rows

            while (rs.next()) {

                Object[] row = new Object[columnCount];

                for (int i = 1; i <= columnCount; i++) {

                    row[i - 1] = rs.getObject(i);

                }

                tableModel.addRow(row);

            }

        } catch (SQLException e) {

            JOptionPane.showMessageDialog(this, "Query Error: " + e.getMessage());

        }

    }



    private void showCharts() {

        try {

            Statement st = conn.createStatement();



            // --- Bar Chart: Acceptance Rates per Program ---

            DefaultCategoryDataset barDataset = new DefaultCategoryDataset();

            ResultSet rs = st.executeQuery("""

               SELECT program,

                      ROUND((SUM(CASE WHEN status='accepted' THEN 1 ELSE 0 END) / COUNT(*)) * 100, 2) AS rate

               FROM applications GROUP BY program;

           """);

            while (rs.next()) {

                barDataset.addValue(rs.getDouble("rate"), "Acceptance Rate", rs.getString("program"));

            }

            JFreeChart barChart = ChartFactory.createBarChart(

                    "Acceptance Rates per Program", "Program", "Rate (%)", barDataset,

                    PlotOrientation.VERTICAL, true, true, false);

            CategoryPlot plot = barChart.getCategoryPlot();

            BarRenderer renderer = (BarRenderer) plot.getRenderer();

            Color[] colors = {new Color(66, 133, 244), new Color(219, 68, 55),

                    new Color(244, 180, 0), new Color(15, 157, 88)};

            for (int i = 0; i < barDataset.getRowCount(); i++) {

                renderer.setSeriesPaint(i, colors[i % colors.length]);

            }

            plot.setBackgroundPaint(Color.white);

            ChartFrame barFrame = new ChartFrame("Bar Chart", barChart);

            barFrame.pack();

            barFrame.setVisible(true);



            // --- Pie Chart: Gender Split ---

            DefaultPieDataset pieDataset = new DefaultPieDataset();

            rs = st.executeQuery("SELECT gender, COUNT(*) AS total FROM applicants GROUP BY gender;");

            while (rs.next()) {

                pieDataset.setValue(rs.getString("gender"), rs.getInt("total"));

            }

            JFreeChart pieChart = ChartFactory.createPieChart(

                    "Gender Split", pieDataset, true, true, false);

            pieChart.setBackgroundPaint(Color.white);

            pieChart.getPlot().setBackgroundPaint(Color.white);

            ChartFrame pieFrame = new ChartFrame("Pie Chart", pieChart);

            pieFrame.pack();

            pieFrame.setVisible(true);





            // Histogram - exam scores

            HistogramDataset histDataset = new HistogramDataset();

            rs = st.executeQuery("SELECT score FROM exam_scores;");

            java.util.List<Double> scores = new java.util.ArrayList<>();

            while (rs.next()) scores.add(rs.getDouble("score"));

            double[] data = scores.stream().mapToDouble(Double::doubleValue).toArray();

            histDataset.addSeries("Exam Scores", data, 10);

            JFreeChart histChart = ChartFactory.createHistogram(
                    "Distribution of Exam Scores",
                    "Score",
                    "Frequency",
                    histDataset,
                    PlotOrientation.VERTICAL,
                    true,
                    true,
                    false
            );



        } catch (SQLException e) {

            JOptionPane.showMessageDialog(this, "Chart Error: " + e.getMessage());

        }

    }



    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> new UniversityAdmissionsAnalysis().setVisible(true));

    }

}