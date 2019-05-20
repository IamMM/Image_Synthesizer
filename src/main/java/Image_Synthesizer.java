import Presets.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import ij.*;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
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
	private JTextField titleTextField;
    private JComboBox<String> imageComboBox;
    private JComboBox<String> typesComboBox;
    private JButton openImageButton;
    private JCheckBox invertingLUTCheckBox;
    private JLabel preview;
    private JSlider previewZSlider;
	private JLabel currentSliceLabel;
	private JComboBox<String> interpolateComboBox;
    private JCheckBox drawAxesCheckBox;
    private JComboBox<String> sizePresetComboBox;
    private JComboBox<String> rangePresetComboBox;
    private JTextField widthTextField;
    private JTextField heightTextField;
    private JTextField slicesTextField;
    private JTextField minX;
    private JTextField maxX;
    private JTextField minY;
    private JTextField maxY;
    private JTextField minZ;
    private JTextField maxZ;
    private JCheckBox normalizeCheckBox;
	private JRadioButton localRadioButton;
	private JRadioButton globalRadioButton;
	private JTabbedPane synthieSelector;
	private JButton addSizePresetButton;
	private JButton removeSizePresetButton;
	private JButton addDimensionPresetButton;
	private JButton removeDimensionPresetButton;

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
	private JToolBar f2ResetToolBar;
	private JToolBar f3ResetToolBar;
    private JButton generateFunctionButton;


	// conditional swing components
	private JTextField conditionalIfField;
	private JTextArea conditionalThenField;
	private JTextArea conditionalVariableField;
	private JTextArea conditionalElseField;
	private JComboBox<String> conditionalPrestComboBox;
	private JButton addConditionalPresetButton;
	private JButton removeConditionalPresetButton;
	private JButton generateConditional;
	private JButton openHelpButton2;

	// constants
    private static final String TITLE = "Image Synthesizer";
    private static final String VERSION = " v1.0";

    // Synthesizer Objects
    private static FunctionImageSynthesizer FIS = new FunctionImageSynthesizer();
    private static MacroImageSynthesizer MIS = new MacroImageSynthesizer();

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
    private Map<String, ConditionalPreset> conditionalPresetMap;
    private Map<String, ConditionalPreset> userConditionalPresetMap;
	private boolean previewIsActive = true;
	private boolean customSize;
	private boolean customRange;
	private boolean customFunction;
	private boolean customConditional;
	private DecimalFormat decimalFormat = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.US));


	/**
     * Main method for debugging.
     * For debugging, it is convenient to have a method that starts ImageJ, loads an
     * image and calls the plugin, e.g. after setting breakpoints.
     *
     * @param args unused
     */
    public static void main(String[] args) throws Exception {
        // set the plugins.dir property to make the plugin appear in the Plugins menu
        // see: https://stackoverflow.com/a/7060464/1207769
        Class<?> clazz = Image_Synthesizer.class;
        java.net.URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
        java.io.File file = new java.io.File(url.toURI());
        System.setProperty("plugins.dir", file.getAbsolutePath());

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
        frame.setResizable(false);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
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

        // init change- and actions listeners
        imageComboBox.addActionListener(evt -> {
            doNewImage = imageComboBox.getSelectedIndex()==0;
            if(!doNewImage) {
                ImagePlus tmp = WindowManager.getImage((String)imageComboBox.getSelectedItem());
                titleTextField.setText("new_" + tmp.getTitle());
                typesComboBox.setSelectedItem(getTypeString(tmp.getType()));
                widthTextField.setText(""+tmp.getWidth());
                heightTextField.setText(""+tmp.getHeight());
                slicesTextField.setText(""+tmp.getNSlices());
                WindowManager.setTempCurrentImage(tmp);
            } else {
            	titleTextField.setText("new_image");
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
            f2ResetToolBar.setVisible(isRGB);
            f3Label.setVisible(isRGB);
            f3TextField.setVisible(isRGB);
            f3ResetToolBar.setVisible(isRGB);
            localRadioButton.setEnabled(isRGB && normalizeCheckBox.isSelected());
            globalRadioButton.setEnabled(isRGB && normalizeCheckBox.isSelected());

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

        initKeyListener();

        initMouseListener();

        previewZSlider.addChangeListener(e -> updateSliceLabelText());
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

        interpolateComboBox.addItem("none");
        interpolateComboBox.addItem("bilinear");
        interpolateComboBox.addItem("bicubic");
        interpolateComboBox.addActionListener(e -> updatePreview());
        drawAxesCheckBox.addActionListener(e -> updatePreview());

        initPresetMaps();

        invertingLUTCheckBox.addActionListener(e -> updatePreview());

        normalizeCheckBox.addActionListener(e -> {
        	if(Objects.equals(typesComboBox.getSelectedItem(), "RGB")){
				localRadioButton.setEnabled(normalizeCheckBox.isSelected());
				globalRadioButton.setEnabled(normalizeCheckBox.isSelected());
			}
        	updatePreview();
		});

        localRadioButton.addActionListener(e -> updatePreview());
        globalRadioButton.addActionListener(e -> updatePreview());

        synthieSelector.addChangeListener(e -> updatePreview());
        addSizePresetButton.addActionListener(e -> addSizePreset());
        removeSizePresetButton.addActionListener(e -> removeSizePreset());
        addDimensionPresetButton.addActionListener(e -> addDimensionPreset());
        removeDimensionPresetButton.addActionListener(e -> removeDimensionPreset());
        addFunctionPresetButton.addActionListener(e -> addFunctionPreset());
        removeFunctionPresetButton.addActionListener(e -> removeFunctionPreset());
        addConditionalPresetButton.addActionListener(e -> addConditionalPreset());
        removeConditionalPresetButton.addActionListener(e -> removeConditionalPreset());
        openHelpButton.addActionListener(e -> openMacroHelp());
        f1ResetButton.addActionListener(e -> resetTextField(f1TextField));
        f2ResetButton.addActionListener(e -> resetTextField(f2TextField));
        f3ResetButton.addActionListener(e -> resetTextField(f3TextField));
        generateFunctionButton.addActionListener(e -> generateFunction());
        openHelpButton2.addActionListener(e -> openMacroHelp());
        generateConditional.addActionListener(e -> generateConditional());

		// select first default preset and update preview
        functionPresetsComboBox.setSelectedIndex(1);
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

    private void initKeyListener() {
    	KeyListener sizeKeyListener = new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				customSize = true;
				sizePresetComboBox.setSelectedIndex(0);
				showInactivePreviewOverlay();
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

		KeyListener rangeKeyListener = new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				customRange = true;
				rangePresetComboBox.setSelectedIndex(0);
				showInactivePreviewOverlay();
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

		KeyListener functionKeyListener = new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				customFunction = true;
				functionPresetsComboBox.setSelectedIndex(0);
				showInactivePreviewOverlay();
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

		KeyListener conditionalKeyListener = new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				customConditional = true;
				conditionalPrestComboBox.setSelectedIndex(0);
				showInactivePreviewOverlay();
			}

			@Override
			public void keyPressed(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}
		};

    	widthTextField.addKeyListener(sizeKeyListener);
    	heightTextField.addKeyListener(sizeKeyListener);
    	slicesTextField.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				customSize = true;
				sizePresetComboBox.setSelectedIndex(0);
				showInactivePreviewOverlay();
			}

			@Override
			public void keyPressed(KeyEvent e) {

			}

			@Override
			public void keyReleased(KeyEvent e) {
				double newValue = getRealNumValue(slicesTextField);
				previewZSlider.setMinimum(1);
				previewZSlider.setMaximum((int)newValue);
				maxZ.setEnabled(newValue != 1);
				previewZSlider.setEnabled(newValue > 1);
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					updatePreview();
				}
			}
		});
		minX.addKeyListener(rangeKeyListener);
		maxX.addKeyListener(rangeKeyListener);
		minY.addKeyListener(rangeKeyListener);
		maxY.addKeyListener(rangeKeyListener);
		minZ.addKeyListener(rangeKeyListener);
		maxZ.addKeyListener(rangeKeyListener);
		f1TextField.addKeyListener(functionKeyListener);
		f2TextField.addKeyListener(functionKeyListener);
		f3TextField.addKeyListener(functionKeyListener);
		conditionalVariableField.addKeyListener(conditionalKeyListener);
		conditionalIfField.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				customConditional = true;
				conditionalPrestComboBox.setSelectedIndex(0);
				showInactivePreviewOverlay();
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
		});
		conditionalThenField.addKeyListener(conditionalKeyListener);
		conditionalElseField.addKeyListener(conditionalKeyListener);
    }

    private void initMouseListener() {
		preview.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {

			}

			@Override
			public void mousePressed(MouseEvent e) {

			}

			@Override
			public void mouseReleased(MouseEvent e) {
				updatePreview();
			}

			@Override
			public void mouseEntered(MouseEvent e) {

			}

			@Override
			public void mouseExited(MouseEvent e) {

			}
		});
	}

	private int getNaturalNumValue(JTextField textField) {
		String textFromGUI = textField.getText().replaceAll("[^\\d.]", "");
		int value = 0;
		try {
			value = textFromGUI.equals("")?1:Integer.parseInt(textFromGUI);
		} catch (NumberFormatException e) {
			IJ.showMessage("Invalid Value Error", "Please provide a natural number (e.g. 128)");
		}
		return value;
	}

	private double getRealNumValue(JTextField textField) {
		String textFromGUI = textField.getText().replaceAll("[^-\\d.]", "");
		double value = 0;
		try {
			value =  textFromGUI.equals("")?0:Double.parseDouble(textFromGUI);
		} catch (NumberFormatException e) {
			IJ.showMessage("Invalid Value Error", "Please provide a float number (e.g. -10.0)");
		}
		return value;
	}

	private void updateSliceLabelText() {
    	double min_z = getRealNumValue(minZ);
    	double max_z = getRealNumValue(maxZ);
    	int slices = getNaturalNumValue(slicesTextField);
    	slices = slices>1?slices:2;
    	int currentSlice = previewZSlider.getValue();
    	double dz = min_z + ((max_z - min_z) / (slices - 1)) * (currentSlice - 1);
    	currentSliceLabel.setText("slice = " + currentSlice + " | z = " + decimalFormat.format(dz));
	}

	private void showInactivePreviewOverlay() {
		if(previewIsActive) {
			Image currPreview = ((ImageIcon)preview.getIcon()).getImage();
			ImagePlus previewPlus = new ImagePlus("", currPreview);
			previewPlus.getProcessor().add(-128);
			int x = previewPlus.getWidth()/2;
			int y = previewPlus.getHeight()/2;
			previewPlus.getProcessor().setColor(Color.WHITE);
			previewPlus.getProcessor().setJustification(ImageProcessor.CENTER_JUSTIFY);
			previewPlus.getProcessor().drawString("click here or press enter to update", x, y);
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
			sizePresetComboBox.addItem(preset);
		}

		sizePresetComboBox.addActionListener(e -> {
			String selectedItem = (String) sizePresetComboBox.getSelectedItem();
			customSize = sizePresetComboBox.getSelectedIndex() == 0;
			if(!customSize) {
				SizePreset sizePreset = sizePresetMap.get(selectedItem);
				widthTextField.setText("" + sizePreset.getX());
				heightTextField.setText("" + sizePreset.getY());
				slicesTextField.setText("" + sizePreset.getZ());
				updatePreview();
			}
		});

		// get user size presets from preferences
		String userSizePrefs = Prefs.get("fis.SizePresets", "");
		userSizePresetMap = gson.fromJson(userSizePrefs, type);
		if(userSizePresetMap!=null) {
			for (String preset : userSizePresetMap.keySet()) {
				sizePresetComboBox.addItem(preset);
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
			rangePresetComboBox.addItem(preset);
		}

		rangePresetComboBox.addActionListener(e -> {
			String selectedItem = (String) rangePresetComboBox.getSelectedItem();
			customRange = rangePresetComboBox.getSelectedIndex() == 0;
			if(!customRange) {
				DimensionPreset dimensionPreset = dimensionPresetMap.get(selectedItem);
				minX.setText("" + dimensionPreset.getMinX());
				maxX.setText("" + dimensionPreset.getMaxX());
				minY.setText("" + dimensionPreset.getMinY());
				maxY.setText("" + dimensionPreset.getMaxY());
				minZ.setText("" + dimensionPreset.getMinZ());
				maxZ.setText("" + dimensionPreset.getMaxZ());
				updatePreview();
			}
		});

		// get user dimension presets from preferences
		String userDimPrefs = Prefs.get("fis.DimensionPresets", "");
		userDimensionPresetMap = gson.fromJson(userDimPrefs, type);
		if(userDimensionPresetMap!=null) {
			for (String preset : userDimensionPresetMap.keySet()) {
				rangePresetComboBox.addItem(preset);
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
			String selectedItem = (String) functionPresetsComboBox.getSelectedItem();
			customFunction = functionPresetsComboBox.getSelectedIndex() == 0;
			if(!customFunction) {
				FunctionPreset functionPreset = functionPresetMap.get(selectedItem);
				normalizeCheckBox.setSelected(functionPreset.isNormalized());
				if (functionPreset.getType().equals("RGB")) {
					f1TextField.setText(functionPreset.getFunctions()[0]);
					f2TextField.setText(functionPreset.getFunctions()[1]);
					f3TextField.setText(functionPreset.getFunctions()[2]);
					localRadioButton.setSelected(functionPreset.isLocal());
					globalRadioButton.setSelected(!functionPreset.isLocal());
				} else {
					f1TextField.setText(functionPreset.getFunction());
					f2TextField.setText(functionPreset.getFunction());
					f3TextField.setText(functionPreset.getFunction());
				}
				typesComboBox.setSelectedItem(functionPreset.getType());
			}
        });

        // conditional
		conditionalPresetMap = new HashMap<>();
		inputStream = getClass().getResourceAsStream("/ConditionalPresets.json");
		gson = new Gson();
		type = new TypeToken<Map<String, ConditionalPreset>>(){}.getType();
		try (Reader reader = new InputStreamReader(inputStream)) {
			conditionalPresetMap = gson.fromJson(reader, type);
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (String preset : conditionalPresetMap.keySet()) {
			conditionalPrestComboBox.addItem(preset);
		}

		// get user function presets from preferences
		String userConditionalPrefs = Prefs.get("fis.ConditionalPresets", "");
		userConditionalPresetMap = gson.fromJson(userConditionalPrefs, type);
		if(userConditionalPresetMap!=null) {
			for (String preset : userConditionalPresetMap.keySet()) {
				conditionalPrestComboBox.addItem(preset);
			}
			conditionalPresetMap.putAll(userConditionalPresetMap);
		}

		conditionalPrestComboBox.addActionListener(e -> {
			String selectedItem = (String) conditionalPrestComboBox.getSelectedItem();
			customConditional = conditionalPrestComboBox.getSelectedIndex() == 0;
			if(!customConditional) {
				ConditionalPreset conditionalPreset = conditionalPresetMap.get(selectedItem);
				normalizeCheckBox.setSelected(conditionalPreset.isNormalized());
				conditionalVariableField.setText(conditionalPreset.getVariables());
				conditionalIfField.setText(conditionalPreset.getCondition());
				conditionalThenField.setText(conditionalPreset.getThen_statement());
				conditionalElseField.setText(conditionalPreset.getElse_statement());
				if(conditionalPreset.getType().equals("RGB")) {
					localRadioButton.setSelected(conditionalPreset.isLocal());
					globalRadioButton.setSelected(!conditionalPreset.isLocal());
				}
				typesComboBox.setSelectedItem(conditionalPreset.getType());
				updatePreview();
			}
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
            functionPreset = new FunctionPreset((String) typesComboBox.getSelectedItem(), normalizeCheckBox.isSelected(), functions);
            functionPreset.setLocal(localRadioButton.isSelected());
		} else {
            String function = f1TextField.getText();
            functionPreset = new FunctionPreset((String) typesComboBox.getSelectedItem(), normalizeCheckBox.isSelected(), function);
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
		sizePresetComboBox.addItem(name);
		sizePresetComboBox.setSelectedItem(name);

		updateUserSizePresets();
	}

	private void removeSizePreset() {
		String selectedPreset = (String) sizePresetComboBox.getSelectedItem();
		GenericDialog genericDialog = new GenericDialog("Remove Size Preset");
		genericDialog.addMessage("You are about to remove the following preset: " + selectedPreset);
		genericDialog.showDialog();

		if (genericDialog.wasCanceled()) return;
		userSizePresetMap.remove(selectedPreset);
		sizePresetMap.remove(selectedPreset);
		sizePresetComboBox.removeItem(selectedPreset);

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
		rangePresetComboBox.addItem(name);
		rangePresetComboBox.setSelectedItem(name);

		updateUserDimensionPresets();
	}

	private void removeDimensionPreset() {
		String selectedPreset = (String) rangePresetComboBox.getSelectedItem();
		GenericDialog genericDialog = new GenericDialog("Remove Dimension Preset");
		genericDialog.addMessage("You are about to remove the following preset: " + selectedPreset);
		genericDialog.showDialog();

		if (genericDialog.wasCanceled()) return;
		userDimensionPresetMap.remove(selectedPreset);
		dimensionPresetMap.remove(selectedPreset);
		rangePresetComboBox.removeItem(selectedPreset);

		updateUserDimensionPresets();
	}

	private void addConditionalPreset() {
		GenericDialog genericDialog = new GenericDialog("Add Conditional Preset");
		genericDialog.addStringField("Name: ", "", 15);
		genericDialog.showDialog();
		if (genericDialog.wasCanceled()) return;
		String name = genericDialog.getNextString();

		if (conditionalPresetMap.containsKey(name)) {
			name = checkName(name);
			if(name.isEmpty()) return;
		}

		String type = (String) typesComboBox.getSelectedItem();
		boolean normalized = normalizeCheckBox.isSelected();
		String variables = conditionalVariableField.getText();
		String condition = conditionalIfField.getText();
		String then_statement = conditionalThenField.getText();
		String else_statement = conditionalElseField.getText();

		ConditionalPreset conditionalPreset =
				new ConditionalPreset(type, normalized, variables, condition, then_statement, else_statement);

		if("RGB".equals(type)) conditionalPreset.setLocal(localRadioButton.isSelected());
		if(userConditionalPresetMap==null) userConditionalPresetMap = new HashMap<>();
		userConditionalPresetMap.put(name, conditionalPreset);
		conditionalPresetMap.put(name, conditionalPreset);
		conditionalPrestComboBox.addItem(name);
		conditionalPrestComboBox.setSelectedItem(name);

		updateUserConditionalPresets();
	}

	private void removeConditionalPreset() {
		String selectedPreset = (String) conditionalPrestComboBox.getSelectedItem();
		GenericDialog genericDialog = new GenericDialog("Remove Conditional Preset");
		genericDialog.addMessage("You are about to remove the following preset: " + selectedPreset);
		genericDialog.showDialog();

		if (genericDialog.wasCanceled()) return;
		userConditionalPresetMap.remove(selectedPreset);
		conditionalPresetMap.remove(selectedPreset);
		conditionalPrestComboBox.removeItem(selectedPreset);

		updateUserConditionalPresets();
	}

    private String checkName(String name) {
        GenericDialog id_alert = new GenericDialog("Identical Name Alert");
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

	private void updateUserConditionalPresets(){
		Gson gsonBuilder = new GsonBuilder().disableHtmlEscaping().create();
		String json = gsonBuilder.toJson(userConditionalPresetMap);
		Prefs.set("fis.ConditionalPresets", json);
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

		String macro = conditionalToMacro();

		if(doNewImage && (containsSubstringGetPixel(functions) || macro.contains("getPixel"))) {
			IJ.showMessage("Error", "Please select or open an image to use getPixel()");
			return;
		}

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

		boolean drawAxes = drawAxesCheckBox.isSelected();
		boolean normalize = !is32Bit && normalizeCheckBox.isSelected();
		boolean globalNorm = globalRadioButton.isSelected();
		int interpolate = interpolateComboBox.getSelectedIndex();

		try {
			if(synthieSelector.getSelectedIndex()==0) { // preview function
				if (isRGB) {
					previewImage = FIS.getPreview(imagePlus, min, max, frame, functions, drawAxes, normalize, globalNorm, interpolate);
				} else {
					previewImage = FIS.getPreview(imagePlus, min, max, frame, function, drawAxes, normalize, interpolate);
				}
			} else { // preview conditional
				previewImage = MIS.getPreview(imagePlus, min, max, frame, macro, drawAxes, normalize, globalNorm, interpolate);
			}
			preview.setIcon(new ImageIcon(previewImage));
		} catch (RuntimeException e) {
			// do nothing
		}
		previewIsActive = true;
	}

	private String conditionalToMacro() {
		String variables = conditionalVariableField.getText();
		String condition = conditionalIfField.getText();
		String then_statement = conditionalThenField.getText();
		String else_statement = conditionalElseField.getText();

		return variables + "\n" + "if(" + condition + ")\n{" + then_statement + "} \nelse {" + else_statement + "}";
	}

	private boolean containsSubstringGetPixel(String[] strings) {
		for (String s : strings) {
			if(s.contains("getPixel")) return true;
		}
		return false;
	}

	private void generateFunction() {
        // meta
		String title = WindowManager.makeUniqueName(titleTextField.getText());
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

		if(doNewImage && containsSubstringGetPixel(functions)) {
			IJ.showMessage("Error", "Please select or open an image to use getPixel()");
			return;
		}
        // apply
        ImagePlus imagePlus;
        if(doNewImage) {
            imagePlus = IJ.createImage(title, type, width, height, slices);
        } else {
            imagePlus = WindowManager.getImage((String)imageComboBox.getSelectedItem()).duplicate();
            imagePlus.setTitle(title);
        }
        if(invertingLUTCheckBox.isSelected()) imagePlus.getProcessor().invertLut();

        try {
            if (isRGB) {
                if (normalizeCheckBox.isSelected()) {
                	if(globalRadioButton.isSelected()) {
                		FIS.functionToGlobalNormalizedImage(imagePlus, min, max, functions);
					} else {
                    	FIS.functionToNormalizedImage(imagePlus, min, max, functions);
					}
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
            imagePlus.getCalibration().setUnit("units");
            imagePlus.show();
            IJ.run("Coordinates...", "left=" + min[0] + " right=" + max[0] + " top=" + min[1] + " bottom=" + max[1]);
        } catch (RuntimeException e) {
            // do nothing
        }
    }

    private void openMacroHelp() {
        String pathToFile = Prefs.getPrefsDir() + "/image-synthesizer-help.html";
        try {
            File file = new File(pathToFile);
            Path path;
            if(file.exists() && !file.isDirectory()) {
                path = file.toPath();
            } else {
                InputStream inputStream = getClass().getResourceAsStream("/image-synthesizer-help.html");
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
	 *						CIS-METHODS						*
	 *														*
	 ********************************************************/

	private void generateConditional() {
		// meta
		String title = WindowManager.makeUniqueName(titleTextField.getText());
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

		// convert conditional to macro
		String macro = conditionalToMacro();

		if(doNewImage && macro.contains("getPixel")) {
			IJ.showMessage("Error", "Please select or open an image to use getPixel()");
			return;
		}

		// apply
		ImagePlus imagePlus;
		if(doNewImage) {
			imagePlus = IJ.createImage(title, type + " Black", width, height, slices);
		} else {
			imagePlus = WindowManager.getImage((String)imageComboBox.getSelectedItem()).duplicate();
			imagePlus.setTitle(title);
		}
		if(invertingLUTCheckBox.isSelected()) imagePlus.getProcessor().invertLut();

		try {
			if(normalizeCheckBox.isSelected() && !is32Bit){
				MIS.macroToNormalizedImage(imagePlus, min, max, macro, globalRadioButton.isSelected());
			} else {
				MIS.macroToImage(imagePlus, min, max, macro);
			}
			IJ.resetMinAndMax(imagePlus);
			imagePlus.getCalibration().setUnit("units");
			imagePlus.show();
			IJ.run("Coordinates...", "left=" + min[0] + " right=" + max[0] + " top=" + min[1] + " bottom=" + max[1]);
		} catch (RuntimeException e) {
			// do nothing
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
