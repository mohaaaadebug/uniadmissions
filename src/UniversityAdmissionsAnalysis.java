import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.statistics.HistogramDataset;

public class UniversityAdmissionsAnalysis extends JFrame {
    private JTable table;
    private DefaultTableModel tableModel;
    private DataQueries dataQueries;
    private Connection conn;

    public UniversityAdmissionsAnalysis() {
        setTitle("University Admissions Analysis Dashboard");
        setSize(1000, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel topPanel = new JPanel(new FlowLayout());
        JButton btnAcceptanceRates = new JButton("Acceptance Rates");
        JButton btnAvgScores = new JButton("Average Scores");
        JButton btnCityGender = new JButton("City/Gender Distribution");
        JButton btnTopApplicants = new JButton("Top Applicants");
        JButton btnVisuals = new JButton("Display Charts");

        topPanel.add(btnAcceptanceRates);
        topPanel.add(btnAvgScores);
        topPanel.add(btnCityGender);
        topPanel.add(btnTopApplicants);
        topPanel.add(btnVisuals);

        tableModel = new DefaultTableModel();
        table = new JTable(tableModel);
        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        conn = DatabaseConnection.getConnection();
        dataQueries = new DataQueries(tableModel);

        btnAcceptanceRates.addActionListener(e -> dataQueries.loadTable("""
            SELECT program,
                   SUM(CASE WHEN status='accepted' THEN 1 ELSE 0 END) AS accepted,
                   COUNT(*) AS total,
                   ROUND((SUM(CASE WHEN status='accepted' THEN 1 ELSE 0 END) / COUNT(*)) * 100, 2) AS acceptance_rate
            FROM applications
            GROUP BY program;
        """));

        btnAvgScores.addActionListener(e -> dataQueries.loadTable("""
            SELECT a.program, ROUND(AVG(e.score),2) AS average_score
            FROM exam_scores e
            JOIN applicants ap ON e.applicant_id = ap.applicant_id
            JOIN applications a ON ap.applicant_id = a.applicant_id
            GROUP BY a.program;
        """));

        btnCityGender.addActionListener(e -> dataQueries.loadTable("""
            SELECT city, gender, COUNT(*) AS total
            FROM applicants
            GROUP BY city, gender
            ORDER BY city;
        """));

        btnTopApplicants.addActionListener(e -> dataQueries.loadTable("""
            SELECT ap.first_name, ap.last_name, ROUND(AVG(e.score),2) AS average_score
            FROM exam_scores e
            JOIN applicants ap ON e.applicant_id = ap.applicant_id
            GROUP BY ap.applicant_id
            ORDER BY average_score DESC
            LIMIT 10;
        """));

        btnVisuals.addActionListener(e -> showCharts());
    }

    private void showCharts() {
        try {
            Statement st = conn.createStatement();



            DefaultCategoryDataset barDataset = new DefaultCategoryDataset();
            ResultSet rs = st.executeQuery("""
                SELECT program,
                       ROUND((SUM(CASE WHEN status='accepted' THEN 1 ELSE 0 END) / COUNT(*)) * 100, 2) AS rate
                FROM applications GROUP BY program;
            """);
            while (rs.next()) barDataset.addValue(rs.getDouble("rate"), "Acceptance Rate", rs.getString("program"));
            ChartFrame barFrame = new ChartFrame("Bar Chart", ChartFactory.createBarChart("Acceptance Rates", "Program", "Rate (%)", barDataset));
            barFrame.pack(); barFrame.setVisible(true);

            DefaultPieDataset pieDataset = new DefaultPieDataset();
            rs = st.executeQuery("SELECT gender, COUNT(*) AS total FROM applicants GROUP BY gender;");
            while (rs.next()) pieDataset.setValue(rs.getString("gender"), rs.getInt("total"));
            ChartFrame pieFrame = new ChartFrame("Pie Chart", ChartFactory.createPieChart("Gender Split", pieDataset));
            pieFrame.pack(); pieFrame.setVisible(true);

            HistogramDataset histDataset = new HistogramDataset();
            rs = st.executeQuery("SELECT score FROM exam_scores;");
            java.util.List<Double> scores = new java.util.ArrayList<>();
            while (rs.next()) scores.add(rs.getDouble("score"));
            double[] data = scores.stream().mapToDouble(Double::doubleValue).toArray();
            histDataset.addSeries("Exam Scores", data, 10);
            ChartFrame histFrame = new ChartFrame("Histogram", ChartFactory.createHistogram("Exam Score Distribution", "Score", "Frequency", histDataset));
            histFrame.pack(); histFrame.setVisible(true);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Chart Error: " + e.getMessage());
        }
    }
}

//