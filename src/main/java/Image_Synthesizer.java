import ij.*;
import ij.plugin.PlugIn;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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
    private JButton addFunctionPresetButton;
    private JComboBox functionPresetsComboBox;
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
    private JButton yEqualZ;
    private JButton xEqualY;
    private JButton zEqualZ;

    // constants
    private static final String TITLE = "Function Image Synthesizer";
    private static final String VERSION = " v0.1.0";

    // FIS
    private FunctionImageSynthesizer FIS = new FunctionImageSynthesizer();

    // globals
    private boolean doNewImage = true;

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
            boolean isRGB = newValue.equals("RGB");
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

        invertingLUTCheckBox.addActionListener(e -> updatePreview());

        initFocusListener();

        drawAxesCheckBox.addActionListener(e -> updatePreview());

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

        // apply
        ImagePlus imagePlus;
        if(doNewImage) {
            imagePlus = IJ.createImage(function, type, width, height, slices);
        } else {
            imagePlus = WindowManager.getImage((String)imageComboBox.getSelectedItem()).duplicate();
            imagePlus.setTitle(function);
        }
        if(invertingLUTCheckBox.isSelected()) imagePlus.getProcessor().invertLut();

        Image previewImage = FIS.getPreview(imagePlus, min, max, function, drawAxesCheckBox.isSelected());
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

        // apply
        ImagePlus imagePlus;
        if(doNewImage) {
            imagePlus = IJ.createImage(function, type, width, height, slices);
        } else {
            imagePlus = WindowManager.getImage((String)imageComboBox.getSelectedItem()).duplicate();
            imagePlus.setTitle(function);
        }
        if(invertingLUTCheckBox.isSelected()) imagePlus.getProcessor().invertLut();
        FIS.functionToImage(imagePlus, min, max, function);
        IJ.resetMinAndMax(imagePlus);
        imagePlus.show();
    }

    private void openMacroHelp() {
        URI uri;
        try {
            URL url = getClass().getResource("/functions.html");
            System.out.println(url);
            uri = url.toURI();
            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(uri);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
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