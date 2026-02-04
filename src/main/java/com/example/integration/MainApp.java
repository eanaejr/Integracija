package com.example.integration;

import com.example.integration.model.IntegrationJob;
import com.example.integration.ui.FunctionPlotPanel;
import java.util.function.DoubleUnaryOperator;
import java.util.List;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;


public class MainApp {

    private final IntegrationService service;
    
    private FunctionPlotPanel plotPanel;

    private JFrame frame;
    private JComboBox<String> cmbFunc;
    private JRadioButton rbPredefined;
    private JRadioButton rbCustom;
    private JTextField txtExpression;
    private JTextField txtA;
    private JTextField txtB;
    private JSpinner spnN;
    private JComboBox<String> cmbAlgo;
    private JButton btnStart;
    private JLabel lblStatus;
    private JLabel lblResult;
    private JProgressBar progressBar;

    private final String exprPlaceholder = "npr. sin(x) + x*x";

    private DefaultListModel<String> recentModel;
    private JList<String> recentList;

    //TODO provjeri jos
    private boolean validateExpressionSyntax(String expr, double a, double b) {
        try {
            Expression e = new ExpressionBuilder(expr)
                    .variable("x")
                    .build();

            double min = Math.min(a, b);
            double max = Math.max(a, b);
            double range = Math.max(max - min, 1e-6);
            double mid = (min + max) / 2.0;
            double eps = Math.max(range * 1e-8, 1e-8);

            double[] basePoints = new double[]{
                    min,
                    max,
                    mid,
                    min + range * 0.25,
                    min + range * 0.75
            };

            for (double x : basePoints) {
                if (x < min - 1e-12 || x > max + 1e-12) continue;

                if (isProblematicPoint(e, x)) {
                    boolean ok = false;
                    double[] deltas = new double[]{eps, -eps, 2 * eps, -2 * eps, range * 1e-3};
                    for (double d : deltas) {
                        double xp = x + d;
                        if (xp < min || xp > max) continue;
                        if (!isProblematicPoint(e, xp)) {
                            ok = true;
                            break;
                        }
                    }
                    if (!ok) {
                        return false;
                    }
                }
            }
            return true;
        } catch (IllegalArgumentException | ArithmeticException ex) {
            return false;
        }
    }

    //provjerava je li funkcija definirana u tockama intervala (1000 tocaka u ovom slucaju)
    private boolean isFunctionDefinedOnInterval(String expr, double a, double b) {
        try {
            Expression e = new ExpressionBuilder(expr)
                    .variable("x")
                    .build();

            int samples = 1000;   // može 500, 1000, 2000...
            for (int i = 0; i <= samples; i++) {
                double x = a + i * (b - a) / samples;
                double val = e.setVariable("x", x).evaluate();

                if (Double.isNaN(val) || Double.isInfinite(val)) {
                    return false;
                }
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }


    private boolean isProblematicPoint(Expression e, double x) {
        try {
            double val = e.setVariable("x", x).evaluate();
            return Double.isNaN(val) || Double.isInfinite(val);
        } catch (ArithmeticException | IllegalArgumentException ex) {
            return true;
        }
    }

    public MainApp() {
        this.service = new IntegrationService(3);
    }

    public void start() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
        }

        Font base = UIManager.getFont("Label.font");
        if (base != null) {
            Font f = base.deriveFont(base.getStyle(), Math.max(12f, base.getSize() + 1f));
            UIManager.put("Label.font", f);
            UIManager.put("Button.font", f.deriveFont(Font.BOLD));
            UIManager.put("ComboBox.font", f);
            UIManager.put("TextField.font", f);
        }

        SwingUtilities.invokeLater(this::buildAndShow);
    }

    private void buildAndShow() {
        frame = new JFrame("Numerička integracija");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        frame.setContentPane(root);

        JPanel params = new JPanel(new GridBagLayout());
        params.setBorder(new TitledBorder("Parametri integracije"));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 8, 6, 8);
        gc.anchor = GridBagConstraints.EAST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        gc.gridx = 0;
        gc.gridy = row;
        gc.weightx = 0;
        gc.gridwidth = 1;
        JLabel lblFunc = new JLabel("Funkcija:");
        params.add(lblFunc, gc);

        gc.gridx = 1;
        gc.gridy = row;
        gc.weightx = 1.0;
        gc.gridwidth = 2;
        cmbFunc = new JComboBox<>(new String[]{"sin(x)", "cos(x)", "x^2"});
        cmbFunc.setToolTipText("Odaberite predefiniranu funkciju");
        params.add(cmbFunc, gc);

        gc.gridx = 3;
        gc.gridy = row;
        gc.weightx = 0;
        gc.gridwidth = 1;
        rbPredefined = new JRadioButton("Predefined", true);
        rbCustom = new JRadioButton("Custom");
        ButtonGroup bg = new ButtonGroup();
        bg.add(rbPredefined);
        bg.add(rbCustom);
        JPanel rbPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        rbPanel.add(rbPredefined);
        rbPanel.add(rbCustom);
        params.add(rbPanel, gc);

        row++;
        gc.gridx = 0;
        gc.gridy = row;
        gc.gridwidth = 1;
        JLabel lblExpr = new JLabel("Custom expr:");
        params.add(lblExpr, gc);

        gc.gridx = 1;
        gc.gridy = row;
        gc.gridwidth = 3;
        txtExpression = new JTextField();
        txtExpression.setEnabled(false);
        txtExpression.setForeground(Color.GRAY);
        txtExpression.setText(exprPlaceholder);
        txtExpression.setToolTipText("Unesite matematički izraz koristeći varijablu x");
        addPlaceholderBehavior(txtExpression, exprPlaceholder);
        params.add(txtExpression, gc);

        row++;
        gc.gridx = 0;
        gc.gridy = row;
        gc.gridwidth = 1;
        JLabel lblAlgo = new JLabel("Metoda:");
        params.add(lblAlgo, gc);

        gc.gridx = 1;
        gc.gridy = row;
        gc.gridwidth = 3;
        cmbAlgo = new JComboBox<>(new String[]{"Trapezna metoda", "Simpsonova metoda"});
        params.add(cmbAlgo, gc);

        row++;
        gc.gridx = 0;
        gc.gridy = row;
        gc.gridwidth = 1;
        JLabel lblA = new JLabel("a:");
        params.add(lblA, gc);

        gc.gridx = 1;
        gc.gridy = row;
        gc.gridwidth = 1;
        txtA = new JTextField("0.0");
        txtA.setToolTipText("Početak intervala (a)");
        params.add(txtA, gc);

        gc.gridx = 2;
        gc.gridy = row;
        gc.gridwidth = 1;
        JLabel lblB = new JLabel("b:");
        params.add(lblB, gc);

        gc.gridx = 3;
        gc.gridy = row;
        gc.gridwidth = 1;
        txtB = new JTextField("1.0");
        txtB.setToolTipText("Kraj intervala (b)");
        params.add(txtB, gc);

        row++;
        gc.gridx = 0;
        gc.gridy = row;
        gc.gridwidth = 1;
        JLabel lblN = new JLabel("n (subintervals):");
        params.add(lblN, gc);

        gc.gridx = 1;
        gc.gridy = row;
        gc.gridwidth = 1;
        SpinnerNumberModel nm = new SpinnerNumberModel(1_000_000, 1, Integer.MAX_VALUE, 1);
        spnN = new JSpinner(nm);
        JComponent editor = spnN.getEditor();
        Dimension d = editor.getPreferredSize();
        d.width = 140;
        editor.setPreferredSize(d);
        params.add(spnN, gc);

        row++;
        gc.gridx = 0;
        gc.gridy = row;
        gc.gridwidth = 4;

        //Maknut JNI checkbox
        //chkNative = new JCheckBox("Koristi JNI (ako je dostupno)");
        //params.add(chkNative, gc);
        
        plotPanel = new FunctionPlotPanel();
        plotPanel.setPreferredSize(new Dimension(420, 320));
        plotPanel.setBorder(new TitledBorder("Graf funkcije i integral"));
        
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, params, plotPanel);
        centerSplit.setResizeWeight(0.60); // lijevo parametri, desno graf
        
        root.add(centerSplit, BorderLayout.CENTER);

        JPanel recentPanel = new JPanel(new BorderLayout(4, 4));
        recentPanel.setBorder(new TitledBorder("Zadnjih 5 rezultata"));
        recentModel = new DefaultListModel<>();
        recentList = new JList<>(recentModel);
        
        recentList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            if (uiBusy) {
                return;
            }
            String s = recentList.getSelectedValue();
            if (s == null || s.isBlank()) {
                return;
            }

            Pattern p = Pattern.compile(
                    "^(.*?)\\s*\\[\\s*([-+0-9.]+)\\s*,\\s*([-+0-9.]+)\\s*\\]\\s*=\\s*([-+0-9.Ee]+)\\s*$"
            );
            Matcher m = p.matcher(s);
            if (!m.matches()) {
                return;
            }

            String funcText = m.group(1).trim();
            double a = Double.parseDouble(m.group(2));
            double b = Double.parseDouble(m.group(3));
            double res = Double.parseDouble(m.group(4));

            lblResult.setText(String.format("Rezultat: %.12f", res));
            lblStatus.setText("Status: odabrano iz povijesti");

            txtA.setText(String.valueOf(a));
            txtB.setText(String.valueOf(b));

            String fLower = funcText.toLowerCase();
            if (fLower.contains("sin")) {
                plotPanel.setFunction(Math::sin, a, b);
            } else if (fLower.contains("cos")) {
                plotPanel.setFunction(Math::cos, a, b);
            } else if (fLower.contains("x^2") || fLower.contains("x*x")) {
                plotPanel.setFunction(x -> x * x, a, b);
            } else {
                try {
                    Expression ex = new ExpressionBuilder(funcText).variable("x").build();
                    DoubleUnaryOperator fn = (x) -> {
                        ex.setVariable("x", x);
                        return ex.evaluate();
                    };
                    plotPanel.setFunction(fn, a, b);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame,
                            "Ne mogu nacrtati graf iz povijesti",
                            "Greška", JOptionPane.ERROR_MESSAGE);
                }
            }
        });


        
        recentList.setVisibleRowCount(5);
        recentList.setFixedCellWidth(260);
        recentPanel.add(new JScrollPane(recentList), BorderLayout.CENTER);
        root.add(recentPanel, BorderLayout.WEST);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setBorder(new EmptyBorder(4, 4, 4, 4));

        btnStart = new JButton("Pokreni");
        btnStart.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnStart.setPreferredSize(new Dimension(120, 44));
        btnStart.setMaximumSize(new Dimension(140, 48));
        btnStart.setMnemonic(KeyEvent.VK_P);
        btnStart.setToolTipText("Pokreni izračun (Alt+P)");

        controls.add(Box.createVerticalGlue());
        controls.add(btnStart);
        controls.add(Box.createRigidArea(new Dimension(0, 10)));
        controls.add(Box.createVerticalGlue());

        root.add(controls, BorderLayout.EAST);

        JPanel status = new JPanel(new GridBagLayout());
        status.setBorder(new EmptyBorder(6, 6, 6, 6));
        GridBagConstraints sc = new GridBagConstraints();
        sc.insets = new Insets(4, 6, 4, 6);
        sc.fill = GridBagConstraints.HORIZONTAL;

        sc.gridx = 0;
        sc.gridy = 0;
        sc.weightx = 1.0;
        lblStatus = new JLabel("Status: idle");
        status.add(lblStatus, sc);

        sc.gridx = 1;
        sc.gridy = 0;
        sc.weightx = 2.0;
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("");
        status.add(progressBar, sc);

        sc.gridx = 0;
        sc.gridy = 1;
        sc.gridwidth = 2;
        sc.weightx = 1.0;
        lblResult = new JLabel("Rezultat: -");
        lblResult.setFont(lblResult.getFont().deriveFont(Font.BOLD));
        status.add(lblResult, sc);

        root.add(status, BorderLayout.SOUTH);

        rbPredefined.addActionListener(e -> toggleExpressionField());
        rbCustom.addActionListener(e -> toggleExpressionField());
        btnStart.addActionListener(e -> onStart());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                service.shutdown();
            }
        });

        frame.pack();
        frame.setMinimumSize(new Dimension(720, 360));
        frame.setLocationRelativeTo(null);
        loadRecentResults();
        frame.setVisible(true);
    }
    
    private DoubleUnaryOperator resolveFunction(int functionId) {
        return switch (functionId) {
            case 0 ->
                Math::sin;
            case 1 ->
                Math::cos;
            default ->
                (x) -> x * x;
        };
    }


    private void loadRecentResults() {
        SwingWorker<List<IntegrationJob>, IntegrationJob> loader = new SwingWorker<>() {
            @Override
            protected List<IntegrationJob> doInBackground() throws Exception {
                return service.getLastJobs(5);
            }

            @Override
            protected void done() {
                try {
                    List<IntegrationJob> list = get();
                    recentModel.clear();
                    for (IntegrationJob j : list) {
                        recentModel.addElement(formatJob(j));
                    }
                } catch (Exception ex) {
                    // ignore or log
                }
            }
        };
        loader.execute();
    }

    private String formatJob(IntegrationJob j) {
        String fn = j.getFunctionName() == null ? "-" : j.getFunctionName();
        return String.format("%s [%.6f, %.6f] = %.12f", fn, j.getA(), j.getB(), j.getResult());
    }

    private void addPlaceholderBehavior(JTextField field, String placeholder) {
        field.setForeground(Color.GRAY);
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (field.getText().trim().isEmpty()) {
                    field.setForeground(Color.GRAY);
                    field.setText(placeholder);
                }
            }
        });
    }

    private void toggleExpressionField() {
        boolean custom = rbCustom.isSelected();
        txtExpression.setEnabled(custom);
        if (!custom && txtExpression.getText().trim().isEmpty()) {
            txtExpression.setForeground(Color.GRAY);
            txtExpression.setText(exprPlaceholder);
        }
        cmbFunc.setEnabled(!custom);
    }

    private void onStart() {
        double a, b;
        int n;
        try {
            a = Double.parseDouble(txtA.getText().replace(",", ".")); // Osiguraj da prima i točku i zarez
            b = Double.parseDouble(txtB.getText().replace(",", "."));

            Object vn = spnN.getValue();
            if (vn instanceof Number) {
                n = ((Number) vn).intValue();
            } else {
                n = Integer.parseInt(vn.toString());
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Neispravan unos za a, b ili n.", "Greška", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (n <= 0) {
            JOptionPane.showMessageDialog(frame, "n mora biti veće od 0.", "Greška", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (a == b) {
            JOptionPane.showMessageDialog(frame, "a i b moraju biti različiti.", "Greška", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (rbCustom.isSelected()) {
            String expr = txtExpression.getText().trim();

            if (expr.isEmpty() || expr.equals(exprPlaceholder)) {
                JOptionPane.showMessageDialog(frame,
                        "Unesite izraz za funkciju.",
                        "Greška", JOptionPane.ERROR_MESSAGE);
                return;
            }

            boolean ok = validateExpressionSyntax(expr, a, b);
            if (!ok) {
                JOptionPane.showMessageDialog(frame,
                        "Izraz nije valjan.",
                        "Greška", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int algoId = cmbAlgo.getSelectedIndex();

            //Ako funkcija nije definirana u tocki intervala javlja se greska
            if (!isFunctionDefinedOnInterval(expr, a, b)) {
                JOptionPane.showMessageDialog(frame,
                        "Funkcija nije definirana u cijelom intervalu.",
                        "Greška", JOptionPane.ERROR_MESSAGE);
                return;
            }


            setUiBusy(true);
            
            Expression e = new ExpressionBuilder(expr).variable("x").build();
            DoubleUnaryOperator fn = (x) -> {
                e.setVariable("x", x);
                return e.evaluate();
            };
            plotPanel.setFunction(fn, a, b);

            Future<IntegrationJob> fut = service.submit(
                    expr,          // functionName
                    a,
                    b,
                    n,
                    -1,            // functionId (nije bitan)
                    algoId,
                    false,         // nema JNI za custom
                    expr           // customExpr
            );

            SwingWorker<IntegrationJob, Void> worker = new SwingWorker<>() {
                @Override
                protected IntegrationJob doInBackground() throws Exception {
                    return fut.get();
                }

                @Override
                protected void done() {
                    try {
                        IntegrationJob job = get();
                        lblResult.setText(String.format("Rezultat: %.12f", job.getResult()));
                        lblStatus.setText("Status: gotov");
                        recentModel.add(0, formatJob(job));
                        while (recentModel.size() > 5)
                            recentModel.remove(recentModel.size() - 1);
                    } catch (Exception e) {
                        lblStatus.setText("Status: greška");
                    } finally {
                        setUiBusy(false);
                    }
                    lblStatus.setText("Status: gotov");
                }
            };
            worker.execute();
            return;
        }



        int functionId = cmbFunc.getSelectedIndex();
        int algoId = cmbAlgo.getSelectedIndex();
        
        plotPanel.setFunction(resolveFunction(functionId), a, b);

        setUiBusy(true);
        lblStatus.setText("Status: računam...");

        //preferNative uvijek true ali necemo sve mijenjati negosamo staviti true umjesto svakog preferNative
        //to radimo jer smo maknuli JNI checkbox iz UI-a
        //JNI je interna optimizacija a ne UI opcija
        Future<IntegrationJob> fut = service.submit(cmbFunc.getSelectedItem().toString(), a, b, n, functionId, algoId, true,null);

        SwingWorker<IntegrationJob, Void> worker = new SwingWorker<>() {
            @Override
            protected IntegrationJob doInBackground() throws Exception {
                return fut.get();
            }

            @Override
            protected void done() {
                try {
                    IntegrationJob job = get();
                    lblResult.setText(String.format("Rezultat: %.12f", job.getResult()));
                    lblStatus.setText("Status: gotov");
                    recentModel.add(0, formatJob(job));
                    while (recentModel.size() > 5) recentModel.remove(recentModel.size() - 1);
                } catch (InterruptedException ie) {
                    lblStatus.setText("Status: prekinuto");
                } catch (ExecutionException ee) {
                    lblStatus.setText("Status: greška - " + ee.getCause().getMessage());
                    JOptionPane.showMessageDialog(frame, "Došlo je do greške: " + ee.getCause().getMessage(), "Greška", JOptionPane.ERROR_MESSAGE);
                } catch (Exception e) {
                    lblStatus.setText("Status: greška");
                } finally {
                    setUiBusy(false);
                }
            }
        };
        worker.execute();
    }
    
    private boolean uiBusy = false;

    private void setUiBusy(boolean busy) {
        uiBusy = busy;
        btnStart.setEnabled(!busy);
        progressBar.setIndeterminate(busy);
        progressBar.setString(busy ? "Računam..." : "");
    }

    public static void main(String[] args) {
        new MainApp().start();
    }
}