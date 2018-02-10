import Presets.DimensionPreset;
import Presets.FunctionPreset;
import Presets.SizePreset;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import ij.*;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.plugin.PlugIn;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
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
    // global swing components
    private JFrame frame;
    private JPanel mainPanel;
    private JComboBox<String> imageComboBox;
    private JComboBox<String> typesComboBox;
    private JButton openImageButton;
    private JCheckBox invertingLUTCheckBox;
    private JCheckBox normalizeCheckBox;
	private JToggleButton localToggleButton;
    private JLabel preview;
    private JCheckBox drawAxesCheckBox;
    private JComboBox<String> sizeComboBox;
    private JComboBox<String> dimensionComboBox;
    private JTextField widthTextField;
    private JTextField heightTextField;
    private JTextField slicesTextField;
    private JTextField minX;
    private JTextField maxX;
    private JTextField minY;
    private JTextField maxY;
    private JTextField minZ;
    private JTextField maxZ;
	private JButton addSizePresetButton;
	private JButton removeSizePresetButton;
	private JButton addDimensionPresetButton;
	private JButton removeDimensionPresetButton;
    private JSlider previewZSlider;
	private JLabel currentSliceLabel;

    // function swing components
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
	private JButton f1ResetButton;
	private JButton f2ResetButton;
	private JButton f3ResetButton;
    private JButton previewButton;
    private JButton generateFunctionButton;

    // primitive swing components
    private JTextPane primitiveTextArea;
	private JButton generatePrimitiveButton;
	private JComboBox primitivePresetsComboBox;
	private JButton addPrimitivePresetButton;
	private JButton removePrimitivePresetButton;

	// constants
    private static final String TITLE = "Function Image Synthesizer";
    private static final String VERSION = " v0.1.0";

    // Synthesizer Objects
    private static FunctionImageSynthesizer FIS = new FunctionImageSynthesizer();
    private static PrimitiveImageSynthesizer PIS = new PrimitiveImageSynthesizer();

    // globals
    private boolean doNewImage = true;
    private boolean isRGB;
    private boolean is32Bit = true;
    private Map<String, SizePreset> sizePresetMap;
    private Map<String, SizePreset> userSizePresetMap;
    private Map<String, DimensionPreset> dimensionPresetMap;
    private Map<String, DimensionPreset> userDimensionPresetMap;
    private Map<String, FunctionPreset> functionPresetMap;
    private Map<String, FunctionPreset> userFunctionPresetMap;
	private boolean previewIsActive = true;

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
            is32Bit = newValue.equals("32-bit");
            normalizeCheckBox.setEnabled(!is32Bit);
            if(isRGB) {
                invertingLUTCheckBox.setSelected(false);
                f1Label.setText("r=");
            } else {
                f1Label.setText("v=");
            }
            invertingLUTCheckBox.setEnabled(!isRGB);
            f2Label.setVisible(isRGB);
            f2TextField.setVisible(isRGB);
            f2ResetButton.setVisible(isRGB);
            f3Label.setVisible(isRGB);
            f3TextField.setVisible(isRGB);
            f3ResetButton.setVisible(isRGB);
            localToggleButton.setEnabled(isRGB);

            if(!doNewImage) {
                ImagePlus tmp = WindowManager.getImage((String) imageComboBox.getSelectedItem());
                if(!getTypeString(tmp.getType()).equals(newValue)){
                    newValue = newValue.equals("RGB")?"RGB Color":newValue.toLowerCase();
                    IJ.doCommand(tmp,newValue);
                }
            }
            updatePreview();
        });

        openImageButton.addActionListener(evt -> showOpenImageDialog());

        invertingLUTCheckBox.addActionListener(e -> updatePreview());

        normalizeCheckBox.addActionListener(e -> updatePreview());

        localToggleButton.addActionListener(e -> {
        	if(localToggleButton.isSelected()) localToggleButton.setText("global");
        	else localToggleButton.setText("local");
        	updatePreview();
		});

        initDocumentListener();

        initKeyListener();

        previewZSlider.addChangeListener(e -> currentSliceLabel.setText(previewZSlider.getValue() + ""));

        previewZSlider.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
				currentSliceLabel.setVisible(true);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				currentSliceLabel.setVisible(false);
				updatePreview();
			}

			@Override
			public void mouseEntered(MouseEvent e) {

			}

			@Override
			public void mouseExited(MouseEvent e) {

			}
		});

        drawAxesCheckBox.addActionListener(e -> updatePreview());

        initPresetMaps();

        addSizePresetButton.addActionListener(e -> addSizePreset());
        removeSizePresetButton.addActionListener(e -> removeSizePreset());
        addDimensionPresetButton.addActionListener(e -> addDimensionPreset());
        removeDimensionPresetButton.addActionListener(e -> removeDimensionPreset());
        addFunctionPresetButton.addActionListener(e -> addFunctionPreset());
        removeFunctionPresetButton.addActionListener(e -> removeFunctionPreset());
        openHelpButton.addActionListener(e -> openMacroHelp());
        f1ResetButton.addActionListener(e -> resetTextField(f1TextField));
        f2ResetButton.addActionListener(e -> resetTextField(f2TextField));
        f3ResetButton.addActionListener(e -> resetTextField(f3TextField));
        previewButton.addActionListener(e -> updatePreview());
        generateFunctionButton.addActionListener(e -> generateFunction());
        generatePrimitiveButton.addActionListener(e -> generatePrimitive());

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

	private void showOpenImageDialog() {
		OpenDialog od = new OpenDialog("Open..", "");
		String directory = od.getDirectory();
		String name = od.getFileName();
		if (name == null) return;

		Opener opener = new Opener();
		ImagePlus image = opener.openImage(directory, name);
		image.show();
		imageComboBox.setSelectedIndex(imageComboBox.getItemCount() - 1);
	}

    private void initImageList() {
        String[] titles = WindowManager.getImageTitles();
        imageComboBox.addItem("new image...");
        for (String title :titles) {
            imageComboBox.addItem(title);
        }
    }

    private void initDocumentListener() {
    	DocumentListener documentListener = new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				showInactivePreviewOverlay();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				showInactivePreviewOverlay();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {

			}
		};

        widthTextField.getDocument().addDocumentListener(documentListener);
        heightTextField.getDocument().addDocumentListener(documentListener);
		slicesTextField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				previewZSlider.setMinimum(1);
				previewZSlider.setMaximum((int) getRealNumValue(slicesTextField));
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				previewZSlider.setMinimum(1);
				previewZSlider.setMaximum((int) getRealNumValue(slicesTextField));
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
			}
		});
        minX.getDocument().addDocumentListener(documentListener);
        maxX.getDocument().addDocumentListener(documentListener);
        minY.getDocument().addDocumentListener(documentListener);
        maxY.getDocument().addDocumentListener(documentListener);
        minZ.getDocument().addDocumentListener(documentListener);
        maxZ.getDocument().addDocumentListener(documentListener);
        f1TextField.getDocument().addDocumentListener(documentListener);
        f2TextField.getDocument().addDocumentListener(documentListener);
        f3TextField.getDocument().addDocumentListener(documentListener);
    }

    private void initKeyListener() {
    	KeyListener keyListener = new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {

			}

			@Override
			public void keyPressed(KeyEvent e) {

			}

			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					updatePreview();
				}
			}
		};

    	widthTextField.addKeyListener(keyListener);
    	heightTextField.addKeyListener(keyListener);
    	slicesTextField.addKeyListener(keyListener);
		minX.addKeyListener(keyListener);
		maxX.addKeyListener(keyListener);
		minY.addKeyListener(keyListener);
		maxY.addKeyListener(keyListener);
		minZ.addKeyListener(keyListener);
		maxZ.addKeyListener(keyListener);
		f1TextField.addKeyListener(keyListener);
		f2TextField.addKeyListener(keyListener);
		f3TextField.addKeyListener(keyListener);
    }

	private int getNaturalNumValue(JTextField textField) {
		String textFromGUI = textField.getText().replaceAll("[^\\d.]", "");
		return textFromGUI.equals("")?1:Integer.parseInt(textFromGUI);
	}

	private double getRealNumValue(JTextField textField) {
		String textFromGUI = textField.getText().replaceAll("[^-\\d.]", "");
		return textFromGUI.equals("")?0:Double.parseDouble(textFromGUI);
	}

	private void showInactivePreviewOverlay() {
		if(previewIsActive) {
			Image currPreview = ((ImageIcon)preview.getIcon()).getImage();
			ImagePlus previewPlus = new ImagePlus("preview", currPreview);
			previewPlus.getProcessor().blurGaussian(20);
			preview.setIcon(new ImageIcon(previewPlus.getImage()));
			previewIsActive = false;
		}
	}

	/********************************************************
	 * 														*
	 *					Preset-METHODS						*
	 *														*
	 ********************************************************/

    private void initPresetMaps() {

//    	Map<String, DimensionPreset> tempMap = new HashMap<>();
//    	DimensionPreset dimensionPreset = new DimensionPreset(-10,10,-10,10,0,1);
//    	tempMap.put("-10..10", dimensionPreset);
//
//        try (BufferedWriter writer = new BufferedWriter(new FileWriter("DimensionPresets.json"))) {
//            Gson gson = new GsonBuilder().setPrettyPrinting().create();
//            gson.toJson(tempMap, writer);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    	// size
		sizePresetMap = new HashMap<>();
		InputStream inputStream = getClass().getResourceAsStream("/SizePresets.json");
		Gson gson = new Gson();
		Type type = new TypeToken<Map<String, SizePreset>>(){}.getType();
		try (Reader reader = new InputStreamReader(inputStream)) {
			sizePresetMap = gson.fromJson(reader, type);
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (String preset : sizePresetMap.keySet()) {
			sizeComboBox.addItem(preset);
		}

		sizeComboBox.addActionListener(e -> {
			SizePreset sizePreset = sizePresetMap.get(sizeComboBox.getSelectedItem());
			widthTextField.setText(""+sizePreset.getX());
			heightTextField.setText(""+sizePreset.getY());
			slicesTextField.setText(""+sizePreset.getZ());
			updatePreview();
		});

		// get user size presets from preferences
		String userSizePrefs = Prefs.get("fis.SizePresets", "");
		userSizePresetMap = gson.fromJson(userSizePrefs, type);
		if(userSizePresetMap!=null) {
			for (String preset : userSizePresetMap.keySet()) {
				sizeComboBox.addItem(preset);
			}
			sizePresetMap.putAll(userSizePresetMap);
		}

		// dimension
		dimensionPresetMap = new HashMap<>();
		inputStream = getClass().getResourceAsStream("/DimensionPresets.json");
		gson = new Gson();
		type = new TypeToken<Map<String, DimensionPreset>>(){}.getType();
		try (Reader reader = new InputStreamReader(inputStream)) {
			dimensionPresetMap = gson.fromJson(reader, type);
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (String preset : dimensionPresetMap.keySet()) {
			dimensionComboBox.addItem(preset);
		}

		dimensionComboBox.addActionListener(e -> {
			DimensionPreset dimensionPreset = dimensionPresetMap.get(dimensionComboBox.getSelectedItem());
			minX.setText(""+dimensionPreset.getMinX());
			maxX.setText(""+dimensionPreset.getMaxX());
			minY.setText(""+dimensionPreset.getMinY());
			maxY.setText(""+dimensionPreset.getMaxY());
			minZ.setText(""+dimensionPreset.getMinZ());
			maxZ.setText(""+dimensionPreset.getMaxZ());
			updatePreview();
		});

		// get user dimension presets from preferences
		String userDimPrefs = Prefs.get("fis.DimensionPresets", "");
		userDimensionPresetMap = gson.fromJson(userDimPrefs, type);
		if(userDimensionPresetMap!=null) {
			for (String preset : userDimensionPresetMap.keySet()) {
				dimensionComboBox.addItem(preset);
			}
			dimensionPresetMap.putAll(userDimensionPresetMap);
		}

		// functions
        functionPresetMap = new HashMap<>();
        inputStream = getClass().getResourceAsStream("/FunctionPresets.json");
        gson = new Gson();
        type = new TypeToken<Map<String, FunctionPreset>>(){}.getType();
        try (Reader reader = new InputStreamReader(inputStream)) {
            functionPresetMap = gson.fromJson(reader, type);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (String preset : functionPresetMap.keySet()) {
            functionPresetsComboBox.addItem(preset);
        }

        // get user function presets from preferences
        String userFuncPrefs = Prefs.get("fis.FunctionPresets", "");
        userFunctionPresetMap = gson.fromJson(userFuncPrefs, type);
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

	private void removeFunctionPreset() {
		String selectedPreset = (String) functionPresetsComboBox.getSelectedItem();
		GenericDialog genericDialog = new GenericDialog("Remove Function Preset");
		genericDialog.addMessage("You are about to remove the following preset: " + selectedPreset);
		genericDialog.showDialog();

		if (genericDialog.wasCanceled()) return;
		userFunctionPresetMap.remove(selectedPreset);
		functionPresetMap.remove(selectedPreset);
		functionPresetsComboBox.removeItem(selectedPreset);

		updateUserFunctionPresets();
	}

	private void addSizePreset() {
		GenericDialog genericDialog = new GenericDialog("Add Size Preset");
		genericDialog.addStringField("Name: ", "", 15);
		genericDialog.showDialog();
		if (genericDialog.wasCanceled()) return;
		String name = genericDialog.getNextString();

		if (sizePresetMap.containsKey(name)) {
			name = checkName(name);
			if(name.isEmpty()) return;
		}

		int x = (int) getRealNumValue(widthTextField);
		int y = (int) getRealNumValue(heightTextField);
		int z = (int) getRealNumValue(slicesTextField);

		SizePreset sizePreset = new SizePreset(x, y, z);

		if(userSizePresetMap==null) userSizePresetMap = new HashMap<>();
		userSizePresetMap.put(name, sizePreset);
		sizePresetMap.put(name, sizePreset);
		sizeComboBox.addItem(name);
		sizeComboBox.setSelectedItem(name);

		updateUserSizePresets();
	}

	private void removeSizePreset() {
		String selectedPreset = (String) sizeComboBox.getSelectedItem();
		GenericDialog genericDialog = new GenericDialog("Remove Size Preset");
		genericDialog.addMessage("You are about to remove the following preset: " + selectedPreset);
		genericDialog.showDialog();

		if (genericDialog.wasCanceled()) return;
		userSizePresetMap.remove(selectedPreset);
		sizePresetMap.remove(selectedPreset);
		sizeComboBox.removeItem(selectedPreset);

		updateUserSizePresets();
	}

	private void addDimensionPreset() {
		GenericDialog genericDialog = new GenericDialog("Add Dimension Preset");
		genericDialog.addStringField("Name: ", "", 15);
		genericDialog.showDialog();
		if (genericDialog.wasCanceled()) return;
		String name = genericDialog.getNextString();

		if (dimensionPresetMap.containsKey(name)) {
			name = checkName(name);
			if(name.isEmpty()) return;
		}

		double min_x = getRealNumValue(minX);
		double max_x = getRealNumValue(maxX);
		double min_y = getRealNumValue(minY);
		double max_y = getRealNumValue(maxY);
		double min_z = getRealNumValue(minZ);
		double max_z = getRealNumValue(maxZ);

		DimensionPreset dimensionPreset = new DimensionPreset(min_x, max_x, min_y, max_y, min_z, max_z);

		if(userDimensionPresetMap==null) userDimensionPresetMap = new HashMap<>();
		userDimensionPresetMap.put(name, dimensionPreset);
		dimensionPresetMap.put(name, dimensionPreset);
		dimensionComboBox.addItem(name);
		dimensionComboBox.setSelectedItem(name);

		updateUserDimensionPresets();
	}

	private void removeDimensionPreset() {
		String selectedPreset = (String) dimensionComboBox.getSelectedItem();
		GenericDialog genericDialog = new GenericDialog("Remove Dimension Preset");
		genericDialog.addMessage("You are about to remove the following preset: " + selectedPreset);
		genericDialog.showDialog();

		if (genericDialog.wasCanceled()) return;
		userDimensionPresetMap.remove(selectedPreset);
		dimensionPresetMap.remove(selectedPreset);
		dimensionComboBox.removeItem(selectedPreset);

		updateUserDimensionPresets();
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

    private void updateUserFunctionPresets(){
        Gson gsonBuilder = new GsonBuilder().create();
        String json = gsonBuilder.toJson(userFunctionPresetMap);
        Prefs.set("fis.FunctionPresets", json);
        Prefs.savePreferences();
    }

    private void updateUserSizePresets(){
        Gson gsonBuilder = new GsonBuilder().create();
        String json = gsonBuilder.toJson(userSizePresetMap);
        Prefs.set("fis.SizePresets", json);
        Prefs.savePreferences();
    }

    private void updateUserDimensionPresets(){
        Gson gsonBuilder = new GsonBuilder().create();
        String json = gsonBuilder.toJson(userDimensionPresetMap);
        Prefs.set("fis.DimensionPresets", json);
        Prefs.savePreferences();
    }

	/********************************************************
	 * 														*
	 *						FIS-METHODS						*
	 *														*
	 ********************************************************/

	private void updatePreview() {
		// meta
		String type = (String) typesComboBox.getSelectedItem();
		assert type != null;

		// size
		int width = getNaturalNumValue(widthTextField);
		int height = getNaturalNumValue(heightTextField);
		int slices = getNaturalNumValue(slicesTextField);

		// coordinate range
		double[] min = new double[3];
		double[] max = new double[3];

		min[0] = getRealNumValue(minX);
		max[0] = getRealNumValue(maxX);

		min[1] = getRealNumValue(minY);
		max[1] = getRealNumValue(maxY);

		min[2] = getRealNumValue(minZ);
		max[2] = getRealNumValue(maxZ);

		// function
		String function = getFunctionText(f1TextField);
		String[] functions = new String[]{function, getFunctionText(f2TextField), getFunctionText(f3TextField)};

		// apply
		ImagePlus imagePlus;
		if(doNewImage) {
			imagePlus = IJ.createImage(function, type, width, height, slices);
		} else {
			imagePlus = WindowManager.getImage((String)imageComboBox.getSelectedItem()).duplicate();
		}
		if(invertingLUTCheckBox.isSelected()) imagePlus.getProcessor().invertLut();

		Image previewImage;
		int frame = slices>1?previewZSlider.getValue():1;

		try {
			if (isRGB) {
				previewImage = FIS.getPreview(imagePlus, min, max, frame, functions, drawAxesCheckBox.isSelected(), normalizeCheckBox.isSelected());
			} else if(!is32Bit){
				previewImage = FIS.getPreview(imagePlus, min, max, frame, function, drawAxesCheckBox.isSelected(), normalizeCheckBox.isSelected());
			} else {
				previewImage = FIS.getPreview(imagePlus, min, max, frame, function, drawAxesCheckBox.isSelected(), false);
			}
			preview.setIcon(new ImageIcon(previewImage));
		} catch (RuntimeException e) {
			// do nothing
		}
		previewIsActive = true;
	}

    private void generateFunction() {
        // meta
        String type = (String) typesComboBox.getSelectedItem();
        assert type != null;

        // size
		int width = getNaturalNumValue(widthTextField);
		int height = getNaturalNumValue(heightTextField);
		int slices = getNaturalNumValue(slicesTextField);

		// coordinate range
        double[] min = new double[3];
        double[] max = new double[3];

        min[0] = getRealNumValue(minX);
        max[0] = getRealNumValue(maxX);

        min[1] = getRealNumValue(minY);
        max[1] = getRealNumValue(maxY);

        min[2] = getRealNumValue(minZ);
        max[2] = getRealNumValue(maxZ);

        // function
        String function = getFunctionText(f1TextField);
        String[] functions = new String[]{function, getFunctionText(f2TextField), getFunctionText(f3TextField)};

        // apply
        ImagePlus imagePlus;
        if(doNewImage) {
            imagePlus = IJ.createImage(function, type, width, height, slices);
        } else {
            imagePlus = WindowManager.getImage((String)imageComboBox.getSelectedItem()).duplicate();
            imagePlus.setTitle(function);
        }
        if(invertingLUTCheckBox.isSelected()) imagePlus.getProcessor().invertLut();

        try {
            if (isRGB) {
                if (normalizeCheckBox.isSelected()) {
                    FIS.functionToNormalizedImage(imagePlus, min, max, functions, localToggleButton.isSelected());
                } else {
                    FIS.functionToImage(imagePlus, min, max, functions);
                }
            } else {
				if (normalizeCheckBox.isSelected() && !is32Bit) {
					FIS.functionToNormalizedImage(imagePlus, min, max, function);
				} else {
					FIS.functionToImage(imagePlus, min, max, function);
				}
            }
            IJ.resetMinAndMax(imagePlus);
            imagePlus.show();
            IJ.run("Coordinates...", "left=" + min[0] + " right=" + max[0] + " top=" + min[1] + " bottom=" + max[1]);
        } catch (RuntimeException e) {
            // do nothing
        }
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

    private String getFunctionText(JTextField textField) {
    	String function = textField.getText();
    	return function.isEmpty()?"0":function;
	}

    private void resetTextField(JTextField textField) {
    	textField.setText("0");
    	updatePreview();
	}

	/********************************************************
	 * 														*
	 *						PIS-METHODS						*
	 *														*
	 ********************************************************/

	private void generatePrimitive() {
		// meta
		String type = (String) typesComboBox.getSelectedItem();
		assert type != null;

		// size
		int width = getNaturalNumValue(widthTextField);
		int height = getNaturalNumValue(heightTextField);
		int slices = getNaturalNumValue(slicesTextField);

		// coordinate range
		double[] min = new double[3];
		double[] max = new double[3];

		min[0] = getRealNumValue(minX);
		max[0] = getRealNumValue(maxX);

		min[1] = getRealNumValue(minY);
		max[1] = getRealNumValue(maxY);

		min[2] = getRealNumValue(minZ);
		max[2] = getRealNumValue(maxZ);

		// function
		String macro = primitiveTextArea.getText();

		// apply
		ImagePlus imagePlus;
		if(doNewImage) {
			imagePlus = IJ.createImage("", type, width, height, slices);
		} else {
			imagePlus = WindowManager.getImage((String)imageComboBox.getSelectedItem()).duplicate();
			imagePlus.setTitle("");
		}
		if(invertingLUTCheckBox.isSelected()) imagePlus.getProcessor().invertLut();

		try {
			PIS.primitiveToImage(imagePlus, min, max, macro);
			IJ.resetMinAndMax(imagePlus);
			imagePlus.show();
			IJ.run("Coordinates...", "left=" + min[0] + " right=" + max[0] + " top=" + min[1] + " bottom=" + max[1]);
		} catch (RuntimeException e) {
			// do nothing
			e.printStackTrace();
		}
	}

	/********************************************************
	 * 														*
	 *				Image Listener-METHODS					*
	 *														*
	 ********************************************************/

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
            showInactivePreviewOverlay();
        }
    }
}