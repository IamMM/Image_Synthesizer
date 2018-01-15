import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import ij.*;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.plugin.PlugIn;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created on october 2016
 *
 * v1.0: first version 20/01/2017
 *
 * @author Maximlian Maske
 */
public class Image_Synthesizer implements PlugIn, ImageListener {
    // swing components
    private JFrame frame;
    private JPanel mainPanel;
    private JComboBox<String> imageComboBox;
    private JComboBox<String> typesComboBox;
    private JButton openImageButton;
    private JCheckBox invertingLUTCheckBox;
    private JLabel preview;
    private JCheckBox drawAxesCheckBox;
    private JTextField widthTextField;
    private JTextField heightTextField;
    private JTextField slicesTextField;
    private JFormattedTextField minX;
    private JTextField maxX;
    private JTextField minY;
    private JTextField maxY;
    private JTextField minZ;
    private JTextField maxZ;
    private JComboBox<String> functionPresetsComboBox;
    private JButton addFunctionPresetButton;
    private JButton removeFunctionPresetButton;
    private JButton openHelpButton;
    private JLabel f1Label;
    private JLabel f2Label;
    private JLabel f3Label;
    private JTextField f1TextField;
    private JTextField f2TextField;
    private JTextField f3TextField;
    private JButton previewButton;
    private JButton generateButton;
    private JComboBox comboBox1;
    private JButton xEqualY;
    private JButton yEqualZ;
    private JButton zEqualX;
    private JButton centerX;
    private JButton centerY;
    private JButton centerZ;
    private JButton inverseX;
    private JButton inverseY;
    private JButton inverseZ;
    private JCheckBox normalizeCheckBox;

    // constants
    private static final String TITLE = "Function Image Synthesizer";
    private static final String VERSION = " v0.1.0";

    // FIS
    private FunctionImageSynthesizer FIS = new FunctionImageSynthesizer();

    // globals
    private boolean doNewImage = true;
    private boolean isRGB;
    private Map<String, FunctionPreset> functionPresetMap;
    private Map<String, FunctionPreset> userFunctionPresetMap;

    /**
     * Main method for debugging.
     * For debugging, it is convenient to have a method that starts ImageJ, loads an
     * image and calls the plugin, e.g. after setting breakpoints.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        // set the plugins.dir property to make the plugin appear in the Plugins menu
        Class<?> clazz = Image_Synthesizer.class;
        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
        System.setProperty("plugins.dir", pluginsDir);

        // start ImageJ
        new ImageJ();

        // run the plugin
        IJ.runPlugIn(clazz.getName(), "");
    }

    /**
     * @see ij.plugin.PlugIn#run(String)
     */
    @Override
    public void run(String arg) {

        initComponents();
        frame = new JFrame(TITLE + VERSION);
        frame.setContentPane(this.mainPanel);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
//        frame.setLocationRelativeTo(null); //center the frame on screen
        setLocationRelativeToImageJFrame();
        setLookAndFeel(frame);
        frame.setVisible(true);
        WindowManager.addWindow(frame);
    }

    private void setLocationRelativeToImageJFrame() {
        Frame imageJFrame = ImageJ.getFrames()[0];
        Point p = imageJFrame.getLocation();

        p.move(p.x, p.y + imageJFrame.getHeight());
        frame.setLocation(p);
    }

    private void initComponents() {

        ImagePlus.addImageListener(this);

        // fill choice boxes
        initImageList();

        typesComboBox.addItem("8-bit");
        typesComboBox.addItem("16-bit");
        typesComboBox.addItem("32-bit");
        typesComboBox.addItem("RGB");
        typesComboBox.setSelectedIndex(2);

        // init change- and actions listeners
        imageComboBox.addActionListener(evt -> {
            doNewImage = imageComboBox.getSelectedIndex()==0;
            if(!doNewImage) {
                ImagePlus tmp = WindowManager.getImage((String)imageComboBox.getSelectedItem());
                typesComboBox.setSelectedItem(getTypeString(tmp.getType()));
                widthTextField.setText(""+tmp.getWidth());
                heightTextField.setText(""+tmp.getHeight());
                slicesTextField.setText(""+tmp.getNSlices());
            }
            widthTextField.setEnabled(doNewImage);
            heightTextField.setEnabled(doNewImage);
            slicesTextField.setEnabled(doNewImage);
            updatePreview();
        });

        typesComboBox.addActionListener(evt -> {
            String newValue = (String) typesComboBox.getSelectedItem();
            assert newValue != null;
            isRGB = newValue.equals("RGB");
            if(isRGB) {
                invertingLUTCheckBox.setSelected(false);
                f1Label.setText("r=");
            } else {
                f1Label.setText("v=");
            }
                f2Label.setVisible(isRGB);
                f3Label.setVisible(isRGB);
            invertingLUTCheckBox.setEnabled(!isRGB);
            f2TextField.setVisible(isRGB);
            f3TextField.setVisible(isRGB);

            if(!doNewImage) {
                ImagePlus tmp = WindowManager.getImage((String) imageComboBox.getSelectedItem());
                if(!getTypeString(tmp.getType()).equals(newValue)){
                    newValue = newValue.equals("RGB")?"RGB Color":newValue.toLowerCase();
                    IJ.doCommand(tmp,newValue);
                }
            }
            updatePreview();
        });

        openImageButton.addActionListener(evt -> openButtonAction());

        invertingLUTCheckBox.addActionListener(e -> updatePreview());

        initFocusListener();

        centerX.addActionListener(e -> center(minX, maxX));
        centerY.addActionListener(e -> center(minY, maxY));
        centerZ.addActionListener(e -> center(minZ, maxZ));

        inverseX.addActionListener(e -> inverse(minX, maxX));
        inverseY.addActionListener(e -> inverse(minY, maxY));
        inverseZ.addActionListener(e -> inverse(minZ, maxZ));

        xEqualY.addActionListener(e -> {
                equal(minX, maxX, minY, maxY);
                updatePreview();
        });

        yEqualZ.addActionListener(e -> {
                equal(minY, maxY, minZ, maxZ);
                updatePreview();
        });

        zEqualX.addActionListener(e -> {
                equal(minZ, maxZ, minX, maxX);
                updatePreview();
        });

        drawAxesCheckBox.addActionListener(e -> updatePreview());

        initFunctionPresets();
        addFunctionPresetButton.addActionListener(e -> addFunctionPreset());
        removeFunctionPresetButton.addActionListener(e -> removeFunctionPreset());
        openHelpButton.addActionListener(e -> openMacroHelp());
        previewButton.addActionListener(e -> updatePreview());
        generateButton.addActionListener(e -> generateFunction());

        updatePreview();
    }

    private String getTypeString(int type) {
        switch (type){
            case ImagePlus.GRAY8: return "8-Bit";
            case ImagePlus.GRAY16: return "16-Bit";
            case ImagePlus.GRAY32: return "32-Bit";
            default: return "RGB";

        }
    }

    /********************************************************
     * 														*
     *						GUI-METHODS						*
     *														*
     ********************************************************/

    private void setLookAndFeel(JFrame frame) {
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.updateComponentTreeUI(frame);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initImageList() {
        String[] titles = WindowManager.getImageTitles();
        imageComboBox.addItem("new image...");
        for (String title :titles) {
            imageComboBox.addItem(title);
        }
    }

    private void initFocusListener() {
        widthTextField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {

            }

            @Override
            public void focusLost(FocusEvent e) {
                updatePreview();
            }
        });

        heightTextField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {

            }

            @Override
            public void focusLost(FocusEvent e) {
                updatePreview();
            }
        });

        minX.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {

            }

            @Override
            public void focusLost(FocusEvent e) {
                updatePreview();
            }
        });

        maxX.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {

            }

            @Override
            public void focusLost(FocusEvent e) {
                updatePreview();
            }
        });

        minY.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {

            }

            @Override
            public void focusLost(FocusEvent e) {
                updatePreview();
            }
        });

        maxY.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {

            }

            @Override
            public void focusLost(FocusEvent e) {
                updatePreview();
            }
        });

        minZ.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {

            }

            @Override
            public void focusLost(FocusEvent e) {
                updatePreview();
            }
        });

        maxZ.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {

            }

            @Override
            public void focusLost(FocusEvent e) {
                updatePreview();
            }
        });
    }

    private void initFunctionPresets() {
//        String[] functions1 = new String[]{"255*sin(d + a)", "255*sin(d + a + 4.2)", "255*sin(d + a + 2.1)"};
//        FunctionPreset functionPreset1 = new FunctionPreset("RGB",functions1);
//
//        String[] functions2 = new String[]{"255*(sin(log(d)*8 + a) * sin(a*8))",
//                "255*(sin(log(d)*8 + a - PI/2) * sin(a*8))", "255*(sin(log(d)*8 + a + PI/2) * sin(a*8))"};
//        FunctionPreset functionPreset2 = new FunctionPreset("RGB",functions2);
//
//        String[] functions3 = new String[]{"255*(floor((a * 40.75 + 1) % 2))",
//                "255*(sin(log(d)*8 + a - PI/2) * sin(a*8))", "255*(sin(log(d)*8 +a + PI/2) * sin(a*8))"};
//        FunctionPreset functionPreset3 = new FunctionPreset("8-bit",functions3);
//
//        functionPresetMap = new HashMap<>();
//        functionPresetMap.put("Spiral", functionPreset1);
//        functionPresetMap.put("Fibonacci", functionPreset2);
//        functionPresetMap.put("Polar Moire", functionPreset3);
//        InputStream url = getClass().getResource("/FunctionPresets.json");
//        IJ.log(url.toString());
//        try (BufferedWriter writer = new BufferedWriter(new FileWriter(url.getPath()))) {
//            Gson gson = new GsonBuilder().setPrettyPrinting().create();
//            gson.toJson(functionPresetMap, writer);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        functionPresetMap = new HashMap<>();
        InputStream inputStream = getClass().getResourceAsStream("/FunctionPresets.json");
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, FunctionPreset>>(){}.getType();
        try (Reader reader = new InputStreamReader(inputStream)) {
            functionPresetMap = gson.fromJson(reader, type);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (String preset : functionPresetMap.keySet()) {
            functionPresetsComboBox.addItem(preset);
        }

        // get user function presets from preferences
        String userPrefs = Prefs.get("fis.FunctionPresets", "");
        userFunctionPresetMap = gson.fromJson(userPrefs, type);

        if(userFunctionPresetMap!=null) {
            for (String preset : userFunctionPresetMap.keySet()) {
                functionPresetsComboBox.addItem(preset);
            }
            functionPresetMap.putAll(userFunctionPresetMap);
        }

        functionPresetsComboBox.addActionListener(e -> {
            FunctionPreset functionPreset = functionPresetMap.get(functionPresetsComboBox.getSelectedItem());
            typesComboBox.setSelectedItem(functionPreset.getType());
            if(functionPreset.getType().equals("RGB")) {
                f1TextField.setText(functionPreset.getFunctions()[0]);
                f2TextField.setText(functionPreset.getFunctions()[1]);
                f3TextField.setText(functionPreset.getFunctions()[2]);
            } else {
                f1TextField.setText(functionPreset.getFunction());
                f2TextField.setText(functionPreset.getFunction());
                f3TextField.setText(functionPreset.getFunction());
            }
            updatePreview();
        });
    }

    private void addFunctionPreset() {
        GenericDialog genericDialog = new GenericDialog("Add Function Preset");
        genericDialog.addStringField("Name: ", "", 15);
        genericDialog.showDialog();
        if (genericDialog.wasCanceled()) return;
        String name = genericDialog.getNextString();

        if (functionPresetMap.containsKey(name)) {
            name = checkName(name);
            if(name.isEmpty()) return;
        }

        FunctionPreset functionPreset;
        if(isRGB) {
            String[] functions = new String[3];
            functions[0] = f1TextField.getText();
            functions[1] = f2TextField.getText();
            functions[2] = f3TextField.getText();
            functionPreset = new FunctionPreset((String) typesComboBox.getSelectedItem(),functions);
        } else {
            String function = f1TextField.getText();
            functionPreset = new FunctionPreset((String) typesComboBox.getSelectedItem(),function);
        }

        if(userFunctionPresetMap==null) userFunctionPresetMap = new HashMap<>();
        userFunctionPresetMap.put(name, functionPreset);
        functionPresetMap.put(name, functionPreset);
        functionPresetsComboBox.addItem(name);
        functionPresetsComboBox.setSelectedItem(name);

        updateUserFunctionPresets();
    }

    private String checkName(String name) {
        GenericDialog id_alert = new GenericDialog("Identical Name Alert");
        id_alert.addMessage("Please give you preset an identical name.");
        id_alert.addMessage(name + " is the name of one of the presets already.");
        id_alert.addStringField("Better name: ", name + " alternative", 30);
        id_alert.showDialog();
        String newName = id_alert.getNextString();
        if(id_alert.wasOKed()) {
            if(newName.equals(name)) name = checkName(name);
            else name = newName;
            return name;
        }
        if(id_alert.wasCanceled()) return "";
        return "";
    }

    private void removeFunctionPreset() {
        String selectedPreset = (String) functionPresetsComboBox.getSelectedItem();
        GenericDialog genericDialog = new GenericDialog("Remove Function Preset");
        genericDialog.addMessage("You are about to remove the following preset: " + selectedPreset);
        genericDialog.showDialog();

        if (genericDialog.wasCanceled()) return;
        userFunctionPresetMap.remove(selectedPreset);
        functionPresetsComboBox.removeItem(selectedPreset);
        updateUserFunctionPresets();
    }

    private void updateUserFunctionPresets(){
        Gson gsonBuilder = new GsonBuilder().create();
        String json = gsonBuilder.toJson(userFunctionPresetMap);
        Prefs.set("fis.FunctionPresets", json);
        Prefs.savePreferences();
    }

    private void updatePreview() {
        // meta
        String type = (String) typesComboBox.getSelectedItem();
        assert type != null;

        // size
        int width = Integer.parseInt(widthTextField.getText().replaceAll("[^\\d.]", ""));
        int height = Integer.parseInt(heightTextField.getText().replaceAll("[^\\d.]", ""));
        int slices = Integer.parseInt(slicesTextField.getText().replaceAll("[^\\d.]", ""));

        // coordinate range
        double[] min = new double[3];
        double[] max = new double[3];

        String x_minFromGUI = minX.getText().replaceAll("[^-\\d.]", "");
        double x_min = x_minFromGUI.equals("")?0:Double.parseDouble(x_minFromGUI);
        String x_maxFromGUI = maxX.getText().replaceAll("[^-\\d.]", "");
        double x_max = x_maxFromGUI.equals("")?width-1:Double.parseDouble(x_maxFromGUI);
        min[0] = x_min;
        max[0] = x_max;

        String y_minFromGUI = minY.getText().replaceAll("[^-\\d.]", "");
        double y_min = y_minFromGUI.equals("")?0:Double.parseDouble(y_minFromGUI);
        String y_maxFromGUI = maxY.getText().replaceAll("[^-\\d.]", "");
        double y_max = y_maxFromGUI.equals("")?height-1:Double.parseDouble(y_maxFromGUI);
        min[1] = y_min;
        max[1] = y_max;

        String z_minFromGUI = minZ.getText().replaceAll("[^-\\d.]", "");
        double z_min = z_minFromGUI.equals("")?0:Double.parseDouble(z_minFromGUI);
        String z_maxFromGUI = maxZ.getText().replaceAll("[^-\\d.]", "");
        double z_max = z_maxFromGUI.equals("")?slices-1:Double.parseDouble(z_maxFromGUI);
        min[2] = z_min;
        max[2] = z_max;

        // function
        String function = f1TextField.getText();
        String[] functions = new String[]{function, f2TextField.getText(), f3TextField.getText()};

        // apply
        ImagePlus imagePlus;
        if(doNewImage) {
            imagePlus = IJ.createImage(function, type, width, height, slices);
        } else {
            imagePlus = WindowManager.getImage((String)imageComboBox.getSelectedItem()).duplicate();
        }
        if(invertingLUTCheckBox.isSelected()) imagePlus.getProcessor().invertLut();

        Image previewImage = isRGB?FIS.getPreview(imagePlus, min, max, functions, drawAxesCheckBox.isSelected(), normalizeCheckBox.isSelected()):
                FIS.getPreview(imagePlus, min, max, function, drawAxesCheckBox.isSelected());
        preview.setIcon(new ImageIcon(previewImage));
    }

    private void generateFunction() {
        // meta
        String type = (String) typesComboBox.getSelectedItem();
        assert type != null;

        // size
        int width = Integer.parseInt(widthTextField.getText().replaceAll("[^\\d.]", ""));
        int height = Integer.parseInt(heightTextField.getText().replaceAll("[^\\d.]", ""));
        int slices = Integer.parseInt(slicesTextField.getText().replaceAll("[^\\d.]", ""));

        // coordinate range
        double[] min = new double[3];
        double[] max = new double[3];

        String x_minFromGUI = minX.getText().replaceAll("[^-\\d.]", "");
        double x_min = x_minFromGUI.equals("")?0:Double.parseDouble(x_minFromGUI);
        String x_maxFromGUI = maxX.getText().replaceAll("[^-\\d.]", "");
        double x_max = x_maxFromGUI.equals("")?width-1:Double.parseDouble(x_maxFromGUI);
        min[0] = x_min;
        max[0] = x_max;

        String y_minFromGUI = minY.getText().replaceAll("[^-\\d.]", "");
        double y_min = y_minFromGUI.equals("")?0:Double.parseDouble(y_minFromGUI);
        String y_maxFromGUI = maxY.getText().replaceAll("[^-\\d.]", "");
        double y_max = y_maxFromGUI.equals("")?height-1:Double.parseDouble(y_maxFromGUI);
        min[1] = y_min;
        max[1] = y_max;

        String z_minFromGUI = minZ.getText().replaceAll("[^-\\d.]", "");
        double z_min = z_minFromGUI.equals("")?0:Double.parseDouble(z_minFromGUI);
        String z_maxFromGUI = maxZ.getText().replaceAll("[^-\\d.]", "");
        double z_max = z_maxFromGUI.equals("")?slices-1:Double.parseDouble(z_maxFromGUI);
        min[2] = z_min;
        max[2] = z_max;

        // function
        String function = f1TextField.getText();
        String[] functions = new String[]{function, f2TextField.getText(), f3TextField.getText()};

        // apply
        ImagePlus imagePlus;
        if(doNewImage) {
            imagePlus = IJ.createImage(function, type, width, height, slices);
        } else {
            imagePlus = WindowManager.getImage((String)imageComboBox.getSelectedItem()).duplicate();
            imagePlus.setTitle(function);
        }
        if(invertingLUTCheckBox.isSelected()) imagePlus.getProcessor().invertLut();

        if(isRGB) {
            if(normalizeCheckBox.isSelected()) {
                FIS.functionToNormalizedImage(imagePlus, min, max, functions);
            } else {
                FIS.functionToImage(imagePlus, min, max, functions);
            }
        } else {
            FIS.functionToImage(imagePlus, min, max, function);
        }
        IJ.resetMinAndMax(imagePlus);
        imagePlus.show();
        IJ.run("Coordinates...", "left=" + min[0] + " right=" + max[0] + " top=" + min[1] + " bottom=" + max[1]);
    }

    private void openMacroHelp() {
        String pathToFile = Prefs.getPrefsDir() + "/ij-fis-functions.html";
        try {
            File file = new File(pathToFile);
            Path path;
            if(file.exists() && !file.isDirectory()) {
                path = file.toPath();
            } else {
                InputStream inputStream = getClass().getResourceAsStream("/functions.html");
                path = new File(pathToFile).toPath();
                Files.copy(inputStream, path);
            }

            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE))
                desktop.browse(path.toUri());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openButtonAction() {
        OpenDialog od = new OpenDialog("Open..", "");
        String directory = od.getDirectory();
        String name = od.getFileName();
        if (name == null) return;

        Opener opener = new Opener();
        ImagePlus image = opener.openImage(directory, name);
        image.show();
        imageComboBox.setSelectedIndex(imageComboBox.getItemCount() - 1);
    }

    private void center(JTextField textField1, JTextField textField2) {
        String minFromGUI = textField1.getText().replaceAll("[^-\\d.]", "");
        double min = minFromGUI.equals("")?0:Double.parseDouble(minFromGUI);
        String maxFromGUI = textField2.getText().replaceAll("[^-\\d.]", "");
        double max = maxFromGUI.equals("")?0:Double.parseDouble(maxFromGUI);

        double range = min-max;

        min = range/2;
        max = -range/2;

        textField1.setText(""+min);
        textField2.setText(""+max);

        updatePreview();
    }

    private void inverse(JTextField textField1, JTextField textField2) {
        String text = textField1.getText();
        textField1.setText(textField2.getText());
        textField2.setText(text);
        updatePreview();
    }

    private void equal(JTextField in1, JTextField in2, JTextField out1, JTextField out2) {
        out1.setText(in1.getText());
        out2.setText(in2.getText());
    }

    private void equal(boolean todo, JTextField in, JTextField out) {
        if(todo) out.setText(in.getText());
    }

    private void close() {
        WindowManager.removeWindow(this.frame);
        frame.dispose();
    }

    @Override
    public void imageOpened(ImagePlus imp) {
        imageComboBox.addItem(imp.getTitle());
    }

    @Override
    public void imageClosed(ImagePlus imp) {
        imageComboBox.removeItem(imp.getTitle());
    }

    @Override
    public void imageUpdated(ImagePlus imp) {
        if(Objects.equals(imageComboBox.getSelectedItem(), imp.getTitle())) {
            typesComboBox.setSelectedItem(getTypeString(imp.getType()));
            widthTextField.setText(""+imp.getWidth());
            heightTextField.setText(""+imp.getHeight());
            slicesTextField.setText(""+imp.getNSlices());
            updatePreview();
        }
    }
}