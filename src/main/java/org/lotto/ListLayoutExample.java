package org.lotto;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.nio.protocol.BasicAsyncResponseConsumer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.json.JSONArray;
import org.json.JSONObject;

public class ListLayoutExample extends JFrame {

    public static final String LOTTO_PL_URL_DATA = "https://www.lotto.pl/api/lotteries/draw-results/by-date-per-game?gameType=Lotto&drawDate=%s&index=1&size=100&sort=DrawSystemId&order=DESC";

    public static final String[] COLUMNS = {"Id of lottery", "Lottery type", "Date of lottery", "Results"};

    private static final Logger log = LogManager.getLogger(ListLayoutExample.class);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> createAndShowGUI());
    }

    private static void createAndShowGUI() {
        Configurator.initialize(new DefaultConfiguration());
        Configurator.setRootLevel(Level.TRACE);
        new ListLayoutExample();
    }

    private ListLayoutExample() {
        log.debug("start layout");
        DefaultTableModel model = new DefaultTableModel(COLUMNS, 0);

        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        JScrollPane scrollPane = new JScrollPane(table);
        table.setFillsViewportHeight(true);

        JLabel lblHeading = new JLabel("Lotto game results");
        lblHeading.setFont(new Font("Arial", Font.TRUETYPE_FONT, 24));

        getContentPane().setLayout(new BorderLayout());

        getContentPane().add(lblHeading, BorderLayout.PAGE_START);
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        JFormattedTextField txtDate = new JFormattedTextField(df);

        // BUTTONS
        JPanel control = new JPanel();
        JButton b1 = new JButton("Get results");

        b1.addActionListener(getResults(model, table, txtDate));

        txtDate.setPreferredSize(new Dimension(100, 20));
        txtDate.setValue(new Date());
        txtDate.addKeyListener(inputDateListener());
        control.add(b1);
        control.add(new JLabel("Date"));
        control.add(txtDate);
        add(control, BorderLayout.SOUTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(750, 400);
        setVisible(true);

        //  add(panel);
        pack();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private ActionListener getResults(DefaultTableModel model, JTable table, JFormattedTextField txtDate) {
        return e -> {
            log.debug("get results pressed");
            table.repaint();
            final var value = txtDate.getText();
            Object[][] objects = parse(value);
            Arrays.stream(objects).forEach(item -> model.addRow(item));
        };
    }

    private KeyAdapter inputDateListener() {
        return new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!((c >= '0') && (c <= '9') ||
                    (c == KeyEvent.VK_BACK_SPACE) ||
                    (c == KeyEvent.VK_DELETE) || (c == KeyEvent.VK_SLASH))) {
                    JOptionPane.showMessageDialog(null, "Please Enter Valid Date");
                    e.consume();
                }
            }
        };
    }

    private Object[][] parse(String date) {
        CloseableHttpAsyncClient httpClient = HttpAsyncClients.createDefault();
        httpClient.start();
        String url = String.format(LOTTO_PL_URL_DATA, date);
        BasicAsyncResponseConsumer consumer = new BasicAsyncResponseConsumer();
        Future<HttpResponse> future = httpClient.execute(HttpAsyncMethods.createGet(url), consumer, null);
        try (InputStream content = future.get().getEntity().getContent()) {
            String json = IOUtils.toString(content, Charset.defaultCharset());
            return extract(json);
        } catch (Exception e) {
            log.error("something went wrong", e);
        }
        return new Object[0][0];
    }

    private Object[][] extract(String json) {
        log.info("Extracting data from url");
        JSONObject jsonObject = new JSONObject(json);
        final JSONArray items = jsonObject.getJSONArray("items");
        List<Object[]> result = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject object = items.getJSONObject(i);

            final JSONArray results = object.getJSONArray("results");
            for (int n = 0; n < results.length(); n++) {
                Object[] singleResult = new Object[4];
                JSONObject item = results.getJSONObject(n);
                final ZonedDateTime drawDate = ZonedDateTime.parse(item.getString("drawDate"));
                log.info("the id is {}", item.getInt("drawSystemId"));
                log.info("the game type is {}", item.getString("gameType"));
                log.info("date is {}", drawDate);
                log.info("result  is {}", item.getJSONArray("resultsJson").toString());
                singleResult[0] = (item.getInt("drawSystemId"));
                singleResult[1] = (item.getString("gameType"));
                singleResult[2] = (drawDate);
                singleResult[3] = (item.getJSONArray("resultsJson").toString());
                result.add(singleResult);
            }
        }
        return result.toArray(Object[][]::new);
    }

}