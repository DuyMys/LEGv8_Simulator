package datapath;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import javax.swing.Timer;


/**
 * A JPanel that visually represents the LEGv8 datapath, drawing components with PNG images and paths with destination labels.
 */
public class DatapathPanel extends JPanel {
    private final Datapath datapath;
    private List<String> activeComponents;
    private List<String> activeBuses;
    private Map<String, String> busDataValues; 
    
    private final List<ComponentInfo> components;
    private final List<BusInfo> buses;
    private final Map<ComponentID, BufferedImage> activeImages;
    private final Map<ComponentID, BufferedImage> inactiveImages;
    private final Map<ComponentID, Dimension> imageDimensions;

    private Map<String, Float> busAnimationProgress; // Tracks animation progress (0 to 1) per bus
    private Timer animationTimer; // Timer for bus label animation
    private static final int ANIMATION_DURATION = 4000; // Matches ANIMATION_DELAY
    private static final int ANIMATION_STEP_MS = 10; // Update every 10ms

    private final Map<BusID, String> busDestinationLabels;
    private final Map<BusID, String> busSourceLabels;
    private final Map<BusID, Set<LabelPlacement>> busLabelPlacements;
    private final Map<BusID, Boolean> busDrawArrow;

    private static final Color DEFAULT_BUS_COLOR = Color.LIGHT_GRAY;
    private static final Color HIGHLIGHT_BUS_COLOR = new Color(165, 56, 96);
    private static final Stroke DEFAULT_BUS_STROKE = new BasicStroke(1);
    private static final Stroke HIGHLIGHT_BUS_STROKE = new BasicStroke(3);
    private static final int ARROW_HEAD_SIZE = 10;
    private static final String IMAGE_PATH = "src/images/";

    // Enum to specify label placement
    private enum LabelPlacement {
        SOURCE, DESTINATION
    }

    /**
     * Constructs the DatapathPanel.
     */
    public DatapathPanel() {
        this.datapath = new Datapath();
        this.activeComponents = new ArrayList<>();
        this.activeBuses = new ArrayList<>();
        this.busDataValues = new HashMap<>();
        this.activeImages = new HashMap<>();
        this.inactiveImages = new HashMap<>();
        this.imageDimensions = new HashMap<>();
        this.busDestinationLabels = new HashMap<>();
        this.busSourceLabels = new HashMap<>();
        this.busLabelPlacements = new HashMap<>();
        this.busDrawArrow = new HashMap<>();
        this.busAnimationProgress = new HashMap<>();
        this.animationTimer = new Timer(ANIMATION_STEP_MS, e -> updateAnimation());
        loadImages();
        this.components = initializeComponents();
        this.buses = initializeBuses();
        setPreferredSize(new Dimension(800, 600)); // Increased to accommodate varying image sizes
        setBackground(Color.WHITE);
    }

    @Override
    public Dimension getPreferredSize() {
        // Giả sử bạn đã tính toán kích thước tối đa cần thiết cho datapath
        // Ví dụ: width = maxX + margin, height = maxY + margin
        int width = 500;  // Tính toán dựa trên compBounds hoặc các thành phần vẽ
        int height = 500;
        // Nếu muốn vừa khung nhìn, có thể lấy kích thước từ parent scrollpane
        Container parent = getParent();
        if (parent instanceof JViewport) {
            Dimension viewportSize = parent.getSize();
            width = Math.max(width, viewportSize.width);
            height = Math.max(height, viewportSize.height);
        }
        return new Dimension(width, height);
    }

    /**
     * Loads active and inactive PNG images and stores their dimensions.
     */
    private void loadImages() {
        for (ComponentID id : ComponentID.values()) {
            String name = id.name().toLowerCase();
            try {
                File activeFile = new File(IMAGE_PATH + name + "_active.png");
                if (activeFile.exists()) {
                    BufferedImage img = ImageIO.read(activeFile);
                    activeImages.put(id, img);
                    imageDimensions.put(id, new Dimension(img.getWidth(), img.getHeight()));
                } else {
                    System.err.println("Warning: Active image not found for " + name);
                    imageDimensions.put(id, new Dimension(40, 40)); // Default size
                }
            } catch (IOException e) {
                System.err.println("Error loading active image for " + name + ": " + e.getMessage());
                imageDimensions.put(id, new Dimension(40, 40));
            }

            try {
                File inactiveFile = new File(IMAGE_PATH + name + "_inactive.png");
                if (inactiveFile.exists()) {
                    BufferedImage img = ImageIO.read(inactiveFile);
                    inactiveImages.put(id, img);
                    if (!imageDimensions.containsKey(id)) {
                        imageDimensions.put(id, new Dimension(img.getWidth(), img.getHeight()));
                    }
                } else {
                    System.err.println("Warning: Inactive image not found for " + name);
                    if (!imageDimensions.containsKey(id)) {
                        imageDimensions.put(id, new Dimension(40, 40));
                    }
                }
            } catch (IOException e) {
                System.err.println("Error loading inactive image for " + name + ": " + e.getMessage());
                if (!imageDimensions.containsKey(id)) {
                    imageDimensions.put(id, new Dimension(40, 40));
                }
            }
        }
    }

    /**
     * Initializes component positions and labels based on image sizes.
     * @return List of ComponentInfo objects.
     */
    private List<ComponentInfo> initializeComponents() {
        List<ComponentInfo> comps = new ArrayList<>();
        int baseX = 10, baseY = 300;
        int spacingX = 50, spacingY = 50;

        // Instruction Memory
        Dimension dim = imageDimensions.get(ComponentID.INSTRUCTION_MEMORY);
        comps.add(new ComponentInfo("INSTRUCTION_MEMORY", baseX + 80, baseY + 230, 120, 150, 
                                    "Inst\nMemory", null, null));

        // Registers
        dim = imageDimensions.get(ComponentID.REGISTERS);
        comps.add(new ComponentInfo("REGISTERS", baseX + 250 + spacingX, baseY + 100, 180, 180, 
                                    "Registers", null, null));
                                    

        // Sign Extend
        dim = imageDimensions.get(ComponentID.SIGN_EXTEND);
        comps.add(new ComponentInfo("SIGN_EXTEND", baseX + 300 + spacingX, baseY + 290, 80, 100, 
                                    "Sign\nExtend", null, null));

        // MUX_ALUsrc
        dim = imageDimensions.get(ComponentID.MUX_ALUsrc);
        comps.add(new ComponentInfo("MUX_ALUsrc", baseX + 520 + spacingX, baseY + 160, 30, 80, 
                                    "M\nU\nX", null, null));

        // ALU
        dim = imageDimensions.get(ComponentID.ALU);
        comps.add(new ComponentInfo("ALU", baseX + 600 + spacingX, baseY + 100, 100, 150, 
                                    "ALU", null, null));

        // Data Memory
        dim = imageDimensions.get(ComponentID.DATA_MEMORY);
        comps.add(new ComponentInfo("DATA_MEMORY", baseX + 750 + spacingX, baseY + 150, 150, 150, 
                                    "Data\nMemory", null, null));

        // MUX_memtoreg
        dim = imageDimensions.get(ComponentID.MUX_memtoreg);
        comps.add(new ComponentInfo("MUX_memtoreg", baseX + 940 + spacingX, baseY + 190, 30, 80, 
                                    "M\nU\nX", null, null));
        // MUX_reg2loc
        dim = imageDimensions.get(ComponentID.MUX_reg2loc);
        comps.add(new ComponentInfo("MUX_reg2loc", baseX + 200 + spacingX, baseY + 100, 30, 80, 
                                    "M\nU\nX", null, null));

        // PC
        dim = imageDimensions.get(ComponentID.PC);
        comps.add(new ComponentInfo("PC", baseX, baseY + 200, 50, 100, 
                                    "PC", null, null));

        // ADD_1
        dim = imageDimensions.get(ComponentID.ADD_1);
        comps.add(new ComponentInfo("ADD_1", baseX + 120, baseY - 180, 50, 90, 
                                    "Add", null, null));
        // ADD_2
        dim = imageDimensions.get(ComponentID.ADD_2);
        comps.add(new ComponentInfo("ADD_2", baseX + 550 + spacingX, baseY - 180, 50,90, 
                                    "Add", null, null));
        // SHIFT_LEFT_2
        dim = imageDimensions.get(ComponentID.SHIFT_LEFT_2);
        comps.add(new ComponentInfo("SHIFT_LEFT_2", baseX + 450 + spacingX, baseY - 155, 40, 50, 
                                    "Shift\nLeft 2", null, null));
        // MUX_PCSRC
        dim = imageDimensions.get(ComponentID.MUX_PCSRC);
        comps.add(new ComponentInfo("MUX_PCSRC", baseX + 900 + spacingX, baseY - 200, 30, 80, 
                                    "M\nU\nX", null, null));
        // ALU Control
        dim = imageDimensions.get(ComponentID.ALU_CONTROL);
        comps.add(new ComponentInfo("ALU_CONTROL", baseX + 600 + spacingX, baseY + 280, 80, 100, 
                                    "ALU\nControl", null, null));
        // Control Unit
        dim = imageDimensions.get(ComponentID.CONTROL_UNIT);
        comps.add(new ComponentInfo("CONTROL_UNIT", baseX + 240 + spacingX, baseY - 90, dim.width, 170, 
                                    "Control\nUnit", null, null));
        // AND Gate
        dim = imageDimensions.get(ComponentID.AND_GATE);
        comps.add(new ComponentInfo("AND_GATE", baseX + 720 + spacingX, baseY + 60, 65, 40, 
                                    "AND", null, null));
        // OR Gate
        dim = imageDimensions.get(ComponentID.OR_GATE);
        comps.add(new ComponentInfo("OR_GATE", baseX + 830 + spacingX, baseY - 80, 65, 40, 
                                    "OR", null, null));
        // Add_4
        dim = imageDimensions.get(ComponentID.ADD_4);
        comps.add(new ComponentInfo("ADD_4", baseX + 80, baseY - 120, 20, 20, 
                                    "4", null, null));
        return comps;
    }

    /**
     * Initializes bus (path) routes and destination labels.
     * @return List of BusInfo objects.
     */
    private List<BusInfo> initializeBuses() {
        List<BusInfo> busList = new ArrayList<>();
        Map<BusID, List<Point>> pathPoints = new HashMap<>();

        // Specify label placement and arrow preferences for each bus
        busDrawArrow.put(BusID.CONTROL_MEMWRITE_TO_DATA_MEMORY, false);     
        busDrawArrow.put(BusID.CONTROL_MEMREAD_TO_DATA_MEMORY, false);
        busDrawArrow.put(BusID.CONTROL_MEMTOREG_TO_MUX_memtoreg, false);
        busDrawArrow.put(BusID.CONTROL_REGWRITE_TO_REGISTERS, false);
        busDrawArrow.put(BusID.CONTROL_ALUSRC_TO_MUX_ALUsrc, false);
        busDrawArrow.put(BusID.CONTROL_ALUOP_TO_ALU_CONTROL, false);
        busDrawArrow.put(BusID.CONTROL_BRANCH_TO_AND_GATE, false);
        busDrawArrow.put(BusID.CONTROL_UNCOND_TO_OR_GATE, false);
        busDrawArrow.put(BusID.CONTROL_REG2LOC_TO_MUX_reg2loc, false);
        busDrawArrow.put(BusID.ALU_TO_AND_GATE, false);
        busDrawArrow.put(BusID.AND_GATE_TO_OR_GATE, false);
        busDrawArrow.put(BusID.OR_GATE_TO_MUX_PCSRC, false);
        busDrawArrow.put(BusID.ALU_CONTROL_TO_ALU, false);


        // Helper to get component bounds
        Map<String, Rectangle> compBounds = new HashMap<>();
        for (ComponentInfo comp : components) {
            compBounds.put(comp.id, new Rectangle(comp.x, comp.y, comp.width, comp.height));
        }

        // Define paths with points adjusted for image sizes
        // Registers paths
        pathPoints.put(BusID.REGISTERS_TO_ALU_READ1, 
            Arrays.asList(
                new Point(compBounds.get("REGISTERS").x + compBounds.get("REGISTERS").width, compBounds.get("ALU").y + 40),
                new Point(compBounds.get("ALU").x, compBounds.get("ALU").y + 40)
            ));

        pathPoints.put(BusID.REGISTERS_TO_MUX_ALUsrc_READ2, 
            Arrays.asList(
                new Point(compBounds.get("REGISTERS").x + compBounds.get("REGISTERS").width, compBounds.get("MUX_ALUsrc").y + 25),
                new Point(compBounds.get("MUX_ALUsrc").x, compBounds.get("MUX_ALUsrc").y + 25)
            ));

        pathPoints.put(BusID.REGISTERS_TO_DATA_MEMORY_WRITE_DATA, Arrays.asList(
            new Point(compBounds.get("REGISTERS").x + compBounds.get("REGISTERS").width + 20, compBounds.get("MUX_ALUsrc").y + 25),
            new Point(compBounds.get("REGISTERS").x + compBounds.get("REGISTERS").width + 20, compBounds.get("DATA_MEMORY").y + compBounds.get("DATA_MEMORY").height - 30),
            new Point(compBounds.get("DATA_MEMORY").x, compBounds.get("DATA_MEMORY").y + compBounds.get("DATA_MEMORY").height - 30)
        ));
        // ALU paths
        pathPoints.put(BusID.ALU_TO_AND_GATE, 
            Arrays.asList(
                new Point(compBounds.get("ALU").x + compBounds.get("ALU").width, compBounds.get("ALU").y + 60),
                new Point(compBounds.get("ALU").x + compBounds.get("ALU").width + 10, compBounds.get("ALU").y + 60),
                new Point(compBounds.get("ALU").x + compBounds.get("ALU").width + 10, compBounds.get("AND_GATE").y + 30),
                new Point(compBounds.get("AND_GATE").x + 10, compBounds.get("AND_GATE").y + 30)
            ));
            
        pathPoints.put(BusID.ALU_TO_DATA_MEMORY_ADDRESS, 
            Arrays.asList(
                new Point(compBounds.get("ALU").x + compBounds.get("ALU").width, compBounds.get("ALU").y + 90),
                new Point(compBounds.get("DATA_MEMORY").x, compBounds.get("ALU").y + 90)
            ));
        pathPoints.put(BusID.ALU_TO_MUX_memtoreg_RESULT, Arrays.asList(
            new Point(compBounds.get("ALU").x + compBounds.get("ALU").width + 10, compBounds.get("ALU").y + 90),
            new Point(compBounds.get("ALU").x + compBounds.get("ALU").width + 10, compBounds.get("ALU").y + 90 + 200),
            new Point(compBounds.get("MUX_memtoreg").x - 20, compBounds.get("ALU").y + 90 + 200),
            new Point(compBounds.get("MUX_memtoreg").x - 20, compBounds.get("MUX_memtoreg").y + compBounds.get("MUX_memtoreg").height - 20), // Inside MUX_memtoreg
            new Point(compBounds.get("MUX_memtoreg").x, compBounds.get("MUX_memtoreg").y + compBounds.get("MUX_memtoreg").height - 20) // Inside MUX_memtoreg
        ));

        pathPoints.put(BusID.DATA_MEMORY_TO_MUX_memtoreg_READ, 
            Arrays.asList(
                new Point(compBounds.get("DATA_MEMORY").x + compBounds.get("DATA_MEMORY").width, compBounds.get("MUX_memtoreg").y + 20),
                new Point(compBounds.get("MUX_memtoreg").x, compBounds.get("MUX_memtoreg").y + 20)
            ));
        // Instruction Memory paths
        pathPoints.put(BusID.INSTRUCTION_MEMORY_TO_REGISTERS_READ1, 
            Arrays.asList(
                new Point(compBounds.get("INSTRUCTION_MEMORY").x + compBounds.get("INSTRUCTION_MEMORY").width, compBounds.get("INSTRUCTION_MEMORY").y + compBounds.get("INSTRUCTION_MEMORY").height / 2),
                new Point(compBounds.get("INSTRUCTION_MEMORY").x + compBounds.get("INSTRUCTION_MEMORY").width + 20, compBounds.get("INSTRUCTION_MEMORY").y + compBounds.get("INSTRUCTION_MEMORY").height / 2),
                new Point(compBounds.get("INSTRUCTION_MEMORY").x + compBounds.get("INSTRUCTION_MEMORY").width + 20, compBounds.get("REGISTERS").y + 20),
                new Point(compBounds.get("REGISTERS").x, compBounds.get("REGISTERS").y + 20)
            ));
        pathPoints.put(BusID.INSTRUCTION_MEMORY_TO_MUX_reg2loc_0, 
            Arrays.asList(
                new Point(compBounds.get("INSTRUCTION_MEMORY").x + compBounds.get("INSTRUCTION_MEMORY").width, compBounds.get("INSTRUCTION_MEMORY").y + compBounds.get("INSTRUCTION_MEMORY").height / 2),
                new Point(compBounds.get("INSTRUCTION_MEMORY").x + compBounds.get("INSTRUCTION_MEMORY").width + 20, compBounds.get("INSTRUCTION_MEMORY").y + compBounds.get("INSTRUCTION_MEMORY").height / 2),
                new Point(compBounds.get("INSTRUCTION_MEMORY").x + compBounds.get("INSTRUCTION_MEMORY").width + 20, compBounds.get("MUX_reg2loc").y + 20),
                new Point(compBounds.get("MUX_reg2loc").x, compBounds.get("MUX_reg2loc").y + 20)
            ));
        pathPoints.put(BusID.INSTRUCTION_MEMORY_TO_MUX_reg2loc_1,
            Arrays.asList(
                new Point(compBounds.get("INSTRUCTION_MEMORY").x + compBounds.get("INSTRUCTION_MEMORY").width, compBounds.get("INSTRUCTION_MEMORY").y + compBounds.get("INSTRUCTION_MEMORY").height / 2),
                new Point(compBounds.get("INSTRUCTION_MEMORY").x + compBounds.get("INSTRUCTION_MEMORY").width + 20, compBounds.get("INSTRUCTION_MEMORY").y + compBounds.get("INSTRUCTION_MEMORY").height / 2),
                new Point(compBounds.get("INSTRUCTION_MEMORY").x + compBounds.get("INSTRUCTION_MEMORY").width + 20, compBounds.get("MUX_reg2loc").y + 60),
                new Point(compBounds.get("MUX_reg2loc").x, compBounds.get("MUX_reg2loc").y + 60)
            ));
        pathPoints.put(BusID.MUX_reg2loc_TO_REGISTERS_READ2, 
            Arrays.asList(
                new Point(compBounds.get("MUX_reg2loc").x + compBounds.get("MUX_reg2loc").width, compBounds.get("MUX_reg2loc").y + compBounds.get("MUX_reg2loc").height / 2),
                new Point(compBounds.get("REGISTERS").x, compBounds.get("MUX_reg2loc").y + compBounds.get("MUX_reg2loc").height / 2) 
            )); 
        pathPoints.put(BusID.INSTRUCTION_MEMORY_TO_REGISTERS_WRITE, 
            Arrays.asList(
                new Point(compBounds.get("INSTRUCTION_MEMORY").x + compBounds.get("INSTRUCTION_MEMORY").width, compBounds.get("INSTRUCTION_MEMORY").y + compBounds.get("INSTRUCTION_MEMORY").height / 2),
                new Point(compBounds.get("INSTRUCTION_MEMORY").x + compBounds.get("INSTRUCTION_MEMORY").width + 20, compBounds.get("INSTRUCTION_MEMORY").y + compBounds.get("INSTRUCTION_MEMORY").height / 2),
                new Point(compBounds.get("INSTRUCTION_MEMORY").x + compBounds.get("INSTRUCTION_MEMORY").width + 20, compBounds.get("REGISTERS").y + 100),
                new Point(compBounds.get("REGISTERS").x, compBounds.get("REGISTERS").y + 100)
            ));
        pathPoints.put(BusID.INSTRUCTION_MEMORY_TO_SIGN_EXTEND, Arrays.asList(
            new Point(compBounds.get("INSTRUCTION_MEMORY").x + compBounds.get("INSTRUCTION_MEMORY").width, compBounds.get("INSTRUCTION_MEMORY").y + compBounds.get("INSTRUCTION_MEMORY").height / 2),
            new Point(compBounds.get("INSTRUCTION_MEMORY").x + compBounds.get("INSTRUCTION_MEMORY").width + 20, compBounds.get("INSTRUCTION_MEMORY").y + compBounds.get("INSTRUCTION_MEMORY").height / 2),
            new Point(compBounds.get("INSTRUCTION_MEMORY").x + compBounds.get("INSTRUCTION_MEMORY").width + 20, compBounds.get("SIGN_EXTEND").y + compBounds.get("SIGN_EXTEND").height / 2),
            new Point(compBounds.get("SIGN_EXTEND").x, compBounds.get("SIGN_EXTEND").y + compBounds.get("SIGN_EXTEND").height / 2) 
        ));
        pathPoints.put(BusID.INSTRUCTION_MEMORY_TO_ALU_CONTROL, Arrays.asList(
            new Point(compBounds.get("INSTRUCTION_MEMORY").x + compBounds.get("INSTRUCTION_MEMORY").width, compBounds.get("INSTRUCTION_MEMORY").y + compBounds.get("INSTRUCTION_MEMORY").height / 2),
            new Point(compBounds.get("INSTRUCTION_MEMORY").x + compBounds.get("INSTRUCTION_MEMORY").width + 20, compBounds.get("INSTRUCTION_MEMORY").y + compBounds.get("INSTRUCTION_MEMORY").height / 2),
            new Point(compBounds.get("INSTRUCTION_MEMORY").x + compBounds.get("INSTRUCTION_MEMORY").width + 20, compBounds.get("SIGN_EXTEND").y + compBounds.get("SIGN_EXTEND").height / 2),
            new Point(compBounds.get("SIGN_EXTEND").x - 20, compBounds.get("SIGN_EXTEND").y + compBounds.get("SIGN_EXTEND").height / 2),
            new Point(compBounds.get("SIGN_EXTEND").x - 20, compBounds.get("SIGN_EXTEND").y + compBounds.get("SIGN_EXTEND").height / 2 + 80),
            new Point(compBounds.get("ALU_CONTROL").x - 20, compBounds.get("SIGN_EXTEND").y + compBounds.get("SIGN_EXTEND").height / 2 + 80),
            new Point(compBounds.get("ALU_CONTROL").x - 20, compBounds.get("ALU_CONTROL").y + compBounds.get("ALU_CONTROL").height / 2),
            new Point(compBounds.get("ALU_CONTROL").x, compBounds.get("ALU_CONTROL").y + compBounds.get("ALU_CONTROL").height / 2)
        ));
        pathPoints.put(BusID.INSTRUCTION_MEMORY_TO_CONTROL_UNIT, Arrays.asList(
            new Point(compBounds.get("INSTRUCTION_MEMORY").x + compBounds.get("INSTRUCTION_MEMORY").width, compBounds.get("INSTRUCTION_MEMORY").y + compBounds.get("INSTRUCTION_MEMORY").height / 2),
            new Point(compBounds.get("INSTRUCTION_MEMORY").x + compBounds.get("INSTRUCTION_MEMORY").width + 20, compBounds.get("INSTRUCTION_MEMORY").y + compBounds.get("INSTRUCTION_MEMORY").height / 2),
            new Point(compBounds.get("INSTRUCTION_MEMORY").x + compBounds.get("INSTRUCTION_MEMORY").width + 20, compBounds.get("CONTROL_UNIT").y + compBounds.get("CONTROL_UNIT").height / 2),
            new Point(compBounds.get("CONTROL_UNIT").x, compBounds.get("CONTROL_UNIT").y + compBounds.get("CONTROL_UNIT").height / 2) 
        ));

        pathPoints.put(BusID.MUX_ALUsrc_TO_ALU, Arrays.asList(
            new Point(compBounds.get("MUX_ALUsrc").x + compBounds.get("MUX_ALUsrc").width, compBounds.get("MUX_ALUsrc").y + compBounds.get("MUX_ALUsrc").height / 2),
            new Point(compBounds.get("ALU").x, compBounds.get("MUX_ALUsrc").y + compBounds.get("MUX_ALUsrc").height / 2)        
        ));

        pathPoints.put(BusID.AND_GATE_TO_OR_GATE, Arrays.asList(
            new Point(compBounds.get("AND_GATE").x + compBounds.get("AND_GATE").width - 10, compBounds.get("AND_GATE").y + compBounds.get("AND_GATE").height / 2),
            new Point(compBounds.get("AND_GATE").x + compBounds.get("AND_GATE").width + 10, compBounds.get("AND_GATE").y + compBounds.get("AND_GATE").height / 2),
            new Point(compBounds.get("AND_GATE").x + compBounds.get("AND_GATE").width + 10, compBounds.get("OR_GATE").y + compBounds.get("OR_GATE").height - 10),
            new Point(compBounds.get("OR_GATE").x + 10, compBounds.get("OR_GATE").y + compBounds.get("OR_GATE").height - 10)
        ));
        pathPoints.put(BusID.OR_GATE_TO_MUX_PCSRC, Arrays.asList(
            new Point(compBounds.get("OR_GATE").x + compBounds.get("OR_GATE").width - 10, compBounds.get("OR_GATE").y + compBounds.get("OR_GATE").height / 2),
            new Point(compBounds.get("MUX_PCSRC").x + compBounds.get("MUX_PCSRC").width / 2, compBounds.get("OR_GATE").y + compBounds.get("OR_GATE").height / 2),
            new Point(compBounds.get("MUX_PCSRC").x + compBounds.get("MUX_PCSRC").width / 2, compBounds.get("MUX_PCSRC").y + compBounds.get("MUX_PCSRC").height)
        ));
        // Sign Extend paths
        pathPoints.put(BusID.SIGN_EXTEND_TO_MUX_ALUsrc, Arrays.asList(
            new Point(compBounds.get("SIGN_EXTEND").x + compBounds.get("SIGN_EXTEND").width, compBounds.get("SIGN_EXTEND").y + compBounds.get("SIGN_EXTEND").height / 2),
            new Point(compBounds.get("SHIFT_LEFT_2").x + compBounds.get("SHIFT_LEFT_2").width / 2, compBounds.get("SIGN_EXTEND").y + compBounds.get("SIGN_EXTEND").height / 2),
            new Point(compBounds.get("SHIFT_LEFT_2").x + compBounds.get("SHIFT_LEFT_2").width / 2, compBounds.get("MUX_ALUsrc").y + compBounds.get("MUX_ALUsrc").height - 25),
            new Point(compBounds.get("MUX_ALUsrc").x, compBounds.get("MUX_ALUsrc").y + compBounds.get("MUX_ALUsrc").height - 25) 
        ));
        pathPoints.put(BusID.SIGN_EXTEND_TO_SHIFT_LEFT_2, Arrays.asList(
            new Point(compBounds.get("SIGN_EXTEND").x + compBounds.get("SIGN_EXTEND").width, compBounds.get("SIGN_EXTEND").y + compBounds.get("SIGN_EXTEND").height / 2),
            new Point(compBounds.get("SHIFT_LEFT_2").x + compBounds.get("SHIFT_LEFT_2").width / 2, compBounds.get("SIGN_EXTEND").y + compBounds.get("SIGN_EXTEND").height / 2),
            new Point(compBounds.get("SHIFT_LEFT_2").x + compBounds.get("SHIFT_LEFT_2").width / 2, compBounds.get("SHIFT_LEFT_2").y + compBounds.get("SHIFT_LEFT_2").height) 
        ));
        
        pathPoints.put(BusID.MUX_memtoreg_TO_REGISTERS_WRITE, Arrays.asList(
            new Point(compBounds.get("MUX_memtoreg").x + compBounds.get("MUX_memtoreg").width, compBounds.get("MUX_memtoreg").y + compBounds.get("MUX_memtoreg").height / 2),
            new Point(compBounds.get("MUX_memtoreg").x + compBounds.get("MUX_memtoreg").width + 10, compBounds.get("MUX_memtoreg").y + compBounds.get("MUX_memtoreg").height / 2),
            new Point(compBounds.get("MUX_memtoreg").x + compBounds.get("MUX_memtoreg").width + 10, compBounds.get("REGISTERS").y + compBounds.get("REGISTERS").height + 200),
            new Point(compBounds.get("REGISTERS").x - 20, compBounds.get("REGISTERS").y + compBounds.get("REGISTERS").height + 200),
            new Point(compBounds.get("REGISTERS").x - 20, compBounds.get("REGISTERS").y + compBounds.get("REGISTERS").height - 20),
            new Point(compBounds.get("REGISTERS").x, compBounds.get("REGISTERS").y + compBounds.get("REGISTERS").height - 20) // Inside Registers
        ));

        pathPoints.put(BusID.ADD_1_TO_MUX_PCSRC, Arrays.asList(
            new Point(compBounds.get("ADD_1").x + compBounds.get("ADD_1").width, compBounds.get("ADD_1").y + compBounds.get("ADD_1").height / 2),
            new Point(compBounds.get("ADD_1").x + compBounds.get("ADD_1").width + 20, compBounds.get("ADD_1").y + compBounds.get("ADD_1").height / 2),
            new Point(compBounds.get("ADD_1").x + compBounds.get("ADD_1").width + 20, compBounds.get("MUX_PCSRC").y + 20),
            new Point(compBounds.get("MUX_PCSRC").x, compBounds.get("MUX_PCSRC").y + 20)
        ));
        pathPoints.put(BusID.ADD_2_TO_MUX_PCSRC, Arrays.asList(
            new Point(compBounds.get("ADD_2").x + compBounds.get("ADD_2").width, compBounds.get("MUX_PCSRC").y + compBounds.get("MUX_PCSRC").height - 25),
            new Point(compBounds.get("MUX_PCSRC").x, compBounds.get("MUX_PCSRC").y + compBounds.get("MUX_PCSRC").height - 25) // Inside MUX_PCSRC
        ));
        pathPoints.put(BusID.MUX_PCSRC_TO_PC, Arrays.asList(
            new Point(compBounds.get("MUX_PCSRC").x + compBounds.get("MUX_PCSRC").width, compBounds.get("MUX_PCSRC").y + compBounds.get("MUX_PCSRC").height / 2),
            new Point(compBounds.get("MUX_PCSRC").x + compBounds.get("MUX_PCSRC").width + 40, compBounds.get("MUX_PCSRC").y + compBounds.get("MUX_PCSRC").height / 2),
            new Point(compBounds.get("MUX_PCSRC").x + compBounds.get("MUX_PCSRC").width + 40, compBounds.get("MUX_PCSRC").y - 30),
            new Point(compBounds.get("PC").x + compBounds.get("PC").width / 2, compBounds.get("MUX_PCSRC").y - 30),
            new Point(compBounds.get("PC").x + compBounds.get("PC").width / 2, compBounds.get("PC").y)
        ));
        pathPoints.put(BusID.ADD_4_TO_ADD_1, Arrays.asList(
            new Point(compBounds.get("ADD_4").x + compBounds.get("ADD_4").width, compBounds.get("ADD_4").y + compBounds.get("ADD_4").height / 2),
            new Point(compBounds.get("ADD_1").x, compBounds.get("ADD_4").y + compBounds.get("ADD_4").height / 2)
        ));
        // PC paths
        pathPoints.put(BusID.PC_TO_ADD_1, Arrays.asList(
            new Point(compBounds.get("PC").x + compBounds.get("PC").width, compBounds.get("PC").y + + compBounds.get("PC").height / 2),
            new Point(compBounds.get("PC").x + compBounds.get("PC").width + 15, compBounds.get("PC").y + + compBounds.get("PC").height / 2),
            new Point(compBounds.get("PC").x + compBounds.get("PC").width + 15, compBounds.get("ADD_1").y + 20),
            new Point(compBounds.get("ADD_1").x, compBounds.get("ADD_1").y + 20)
        ));
        pathPoints.put(BusID.PC_TO_ADD_2, Arrays.asList(
            new Point(compBounds.get("PC").x + compBounds.get("PC").width, compBounds.get("PC").y + + compBounds.get("PC").height / 2),
            new Point(compBounds.get("PC").x + compBounds.get("PC").width + 15, compBounds.get("PC").y + + compBounds.get("PC").height / 2),
            new Point(compBounds.get("PC").x + compBounds.get("PC").width + 15, compBounds.get("ADD_1").y + 120),
            new Point(compBounds.get("PC").x + compBounds.get("PC").width + 200, compBounds.get("ADD_1").y + 120),
            new Point(compBounds.get("PC").x + compBounds.get("PC").width + 200, compBounds.get("ADD_2").y + 20),
            new Point(compBounds.get("ADD_2").x, compBounds.get("ADD_2").y + 20)
        ));
        pathPoints.put(BusID.PC_TO_INSTRUCTION_MEMORY, Arrays.asList(
            new Point(compBounds.get("PC").x + compBounds.get("PC").width, compBounds.get("PC").y + + compBounds.get("PC").height / 2),
            new Point(compBounds.get("INSTRUCTION_MEMORY").x, compBounds.get("PC").y + + compBounds.get("PC").height / 2)
        ));
        pathPoints.put(BusID.SHIFT_LEFT_2_TO_ADD, Arrays.asList(
            new Point(compBounds.get("SHIFT_LEFT_2").x + compBounds.get("SHIFT_LEFT_2").width, compBounds.get("SHIFT_LEFT_2").y + compBounds.get("SHIFT_LEFT_2").height / 2),
            new Point(compBounds.get("ADD_2").x, compBounds.get("SHIFT_LEFT_2").y + compBounds.get("SHIFT_LEFT_2").height / 2)
        ));
        pathPoints.put(BusID.ALU_CONTROL_TO_ALU, Arrays.asList(
            new Point(compBounds.get("ALU_CONTROL").x + compBounds.get("ALU_CONTROL").width / 2, compBounds.get("ALU_CONTROL").y),
            new Point(compBounds.get("ALU_CONTROL").x + compBounds.get("ALU_CONTROL").width / 2, compBounds.get("ALU").y + compBounds.get("ALU").height)
        ));
        // Control Unit paths
        pathPoints.put(BusID.CONTROL_REG2LOC_TO_MUX_reg2loc, Arrays.asList(
            new Point(compBounds.get("CONTROL_UNIT").x + compBounds.get("CONTROL_UNIT").width, compBounds.get("CONTROL_UNIT").y + 10),
            new Point(compBounds.get("CONTROL_UNIT").x + compBounds.get("CONTROL_UNIT").width + 20, compBounds.get("CONTROL_UNIT").y + 10),
            new Point(compBounds.get("CONTROL_UNIT").x + compBounds.get("CONTROL_UNIT").width + 20, compBounds.get("CONTROL_UNIT").y - 20),
            new Point(compBounds.get("INSTRUCTION_MEMORY").x + compBounds.get("INSTRUCTION_MEMORY").width + 20, compBounds.get("CONTROL_UNIT").y - 20),
            new Point(compBounds.get("INSTRUCTION_MEMORY").x + compBounds.get("INSTRUCTION_MEMORY").width + 20, compBounds.get("MUX_reg2loc").y + compBounds.get("MUX_reg2loc").height + 20),
            new Point(compBounds.get("MUX_reg2loc").x + compBounds.get("MUX_reg2loc").width / 2, compBounds.get("MUX_reg2loc").y + compBounds.get("MUX_reg2loc").height + 20),
            new Point(compBounds.get("MUX_reg2loc").x + compBounds.get("MUX_reg2loc").width / 2, compBounds.get("MUX_reg2loc").y + compBounds.get("MUX_reg2loc").height) 
        ));
        pathPoints.put(BusID.CONTROL_UNCOND_TO_OR_GATE, Arrays.asList(
            new Point(compBounds.get("CONTROL_UNIT").x + compBounds.get("CONTROL_UNIT").width, compBounds.get("CONTROL_UNIT").y + 30),
            new Point(compBounds.get("OR_GATE").x - 50, compBounds.get("CONTROL_UNIT").y + 30),
            new Point(compBounds.get("OR_GATE").x - 50, compBounds.get("OR_GATE").y + 10),
            new Point(compBounds.get("OR_GATE").x + 10, compBounds.get("OR_GATE").y + 10)
        ));
        pathPoints.put(BusID.CONTROL_BRANCH_TO_AND_GATE, Arrays.asList(
            new Point(compBounds.get("CONTROL_UNIT").x + compBounds.get("CONTROL_UNIT").width, compBounds.get("CONTROL_UNIT").y + 50),
            new Point(compBounds.get("AND_GATE").x - 30, compBounds.get("CONTROL_UNIT").y + 50),
            new Point(compBounds.get("AND_GATE").x - 30, compBounds.get("AND_GATE").y + 10),
            new Point(compBounds.get("AND_GATE").x + 10, compBounds.get("AND_GATE").y + 10)
        ));
        pathPoints.put(BusID.CONTROL_MEMREAD_TO_DATA_MEMORY, Arrays.asList(
            new Point(compBounds.get("CONTROL_UNIT").x + compBounds.get("CONTROL_UNIT").width, compBounds.get("CONTROL_UNIT").y + 70),
            new Point(compBounds.get("MUX_memtoreg").x + compBounds.get("MUX_memtoreg").width + 20, compBounds.get("CONTROL_UNIT").y + 70),
            new Point(compBounds.get("MUX_memtoreg").x + compBounds.get("MUX_memtoreg").width + 20, compBounds.get("DATA_MEMORY").y + compBounds.get("DATA_MEMORY").height + 20),
            new Point(compBounds.get("DATA_MEMORY").x + compBounds.get("DATA_MEMORY").width / 2, compBounds.get("DATA_MEMORY").y + compBounds.get("DATA_MEMORY").height + 20),
            new Point(compBounds.get("DATA_MEMORY").x + compBounds.get("DATA_MEMORY").width / 2, compBounds.get("DATA_MEMORY").y + compBounds.get("DATA_MEMORY").height)
        ));
        pathPoints.put(BusID.CONTROL_MEMTOREG_TO_MUX_memtoreg, Arrays.asList(
            new Point(compBounds.get("CONTROL_UNIT").x + compBounds.get("CONTROL_UNIT").width, compBounds.get("CONTROL_UNIT").y + 90),
            new Point(compBounds.get("MUX_memtoreg").x + compBounds.get("MUX_memtoreg").width / 2, compBounds.get("CONTROL_UNIT").y + 90),
            new Point(compBounds.get("MUX_memtoreg").x + compBounds.get("MUX_memtoreg").width / 2, compBounds.get("MUX_memtoreg").y) // Inside MUX_memtoreg
        ));
        pathPoints.put(BusID.CONTROL_ALUOP_TO_ALU_CONTROL, Arrays.asList(
            new Point(compBounds.get("CONTROL_UNIT").x + compBounds.get("CONTROL_UNIT").width, compBounds.get("CONTROL_UNIT").y + 110),
            new Point(compBounds.get("REGISTERS").x + compBounds.get("REGISTERS").width + 20, compBounds.get("CONTROL_UNIT").y + 110),
            new Point(compBounds.get("REGISTERS").x + compBounds.get("REGISTERS").width + 20, compBounds.get("ALU_CONTROL").y + compBounds.get("ALU_CONTROL").height + 60),
            new Point(compBounds.get("ALU_CONTROL").x + compBounds.get("ALU_CONTROL").width / 2, compBounds.get("ALU_CONTROL").y + compBounds.get("ALU_CONTROL").height + 60),
            new Point(compBounds.get("ALU_CONTROL").x + compBounds.get("ALU_CONTROL").width / 2, compBounds.get("ALU_CONTROL").y + compBounds.get("ALU_CONTROL").height)
        ));
        pathPoints.put(BusID.CONTROL_MEMWRITE_TO_DATA_MEMORY, Arrays.asList(
            new Point(compBounds.get("CONTROL_UNIT").x + compBounds.get("CONTROL_UNIT").width, compBounds.get("CONTROL_UNIT").y + 130),
            new Point(compBounds.get("DATA_MEMORY").x + compBounds.get("DATA_MEMORY").width / 2, compBounds.get("CONTROL_UNIT").y + 130),
            new Point(compBounds.get("DATA_MEMORY").x + compBounds.get("DATA_MEMORY").width / 2, compBounds.get("DATA_MEMORY").y)
        ));
        pathPoints.put(BusID.CONTROL_ALUSRC_TO_MUX_ALUsrc, Arrays.asList(
            new Point(compBounds.get("CONTROL_UNIT").x + compBounds.get("CONTROL_UNIT").width, compBounds.get("CONTROL_UNIT").y + 150),
            new Point(compBounds.get("MUX_ALUsrc").x + compBounds.get("MUX_ALUsrc").width / 2, compBounds.get("CONTROL_UNIT").y + 150),
            new Point(compBounds.get("MUX_ALUsrc").x + compBounds.get("MUX_ALUsrc").width / 2, compBounds.get("MUX_ALUsrc").y) 
        ));
        pathPoints.put(BusID.CONTROL_REGWRITE_TO_REGISTERS, Arrays.asList(
            new Point(compBounds.get("CONTROL_UNIT").x + compBounds.get("CONTROL_UNIT").width, compBounds.get("CONTROL_UNIT").y + 170),
            new Point(compBounds.get("REGISTERS").x + compBounds.get("REGISTERS").width / 2, compBounds.get("CONTROL_UNIT").y + 170),
            new Point(compBounds.get("REGISTERS").x + compBounds.get("REGISTERS").width / 2, compBounds.get("REGISTERS").y) 
        ));

        for (Datapath.Path path : datapath.getPaths()) {
            BusID busId = mapPathToBusID(path);
            if (busId != null && pathPoints.containsKey(busId)) {
                busList.add(new BusInfo(busId.name(), pathPoints.get(busId), "#000000", 2));
                busSourceLabels.put(busId, path.getSourceOutput());
                busDestinationLabels.put(busId, path.getDestinationInput());
            }
        }
        return busList;
    }

    /**
     * Maps a Datapath.Path to a BusID.
     * @param path The Datapath.Path.
     * @return Corresponding BusID or null if unmapped.
     */
    private BusID mapPathToBusID(Datapath.Path path) {
        // Registers paths
        if (path.getSource() == Datapath.Component.REGISTERS && 
            path.getDestination() == Datapath.Component.ALU && 
            path.getSourceOutput().equals("Read\nData 1")) {
            return BusID.REGISTERS_TO_ALU_READ1;
        } 
        else if (path.getSource() == Datapath.Component.REGISTERS && 
                path.getDestination() == Datapath.Component.MUX_ALUsrc && 
                path.getSourceOutput().equals("Read\nData 2")) {
            return BusID.REGISTERS_TO_MUX_ALUsrc_READ2;
        } 
        else if (path.getSource() == Datapath.Component.REGISTERS && 
                path.getDestination() == Datapath.Component.DATA_MEMORY) {
            return BusID.REGISTERS_TO_DATA_MEMORY_WRITE_DATA;
        }

        // ALU paths
        else if (path.getSource() == Datapath.Component.ALU && 
                path.getDestination() == Datapath.Component.AND_GATE) {
            return BusID.ALU_TO_AND_GATE;
        }  
        else if (path.getSource() == Datapath.Component.ALU && 
                path.getDestination() == Datapath.Component.DATA_MEMORY && 
                path.getSourceOutput().equals("ALU\nResult")) {
            return BusID.ALU_TO_DATA_MEMORY_ADDRESS;
        } 
        else if (path.getSource() == Datapath.Component.ALU && 
                path.getDestination() == Datapath.Component.MUX_memtoreg) {
            return BusID.ALU_TO_MUX_memtoreg_RESULT;
        } 

        // Data Memory paths
        else if (path.getSource() == Datapath.Component.DATA_MEMORY && 
                path.getDestination() == Datapath.Component.MUX_memtoreg) {
            return BusID.DATA_MEMORY_TO_MUX_memtoreg_READ;
        } 
        
        else if (path.getSource() == Datapath.Component.MUX_memtoreg && 
                path.getDestination() == Datapath.Component.REGISTERS) {
            return BusID.MUX_memtoreg_TO_REGISTERS_WRITE;
        } 
        
        // Instruction Memory paths
        else if (path.getSource() == Datapath.Component.INSTRUCTION_MEMORY && 
                path.getDestination() == Datapath.Component.REGISTERS && 
                path.getDestinationInput().equals("Read\nRegister 1")) {
            return BusID.INSTRUCTION_MEMORY_TO_REGISTERS_READ1;
        } 
        
        // else if (path.getSource() == Datapath.Component.INSTRUCTION_MEMORY && 
        //            path.getDestination() == Datapath.Component.REGISTERS && 
        //            path.getDestinationInput().equals("Read\nRegister 2")) {
        //     return BusID.INSTRUCTION_MEMORY_TO_REGISTERS_READ2;
        // } 
        
        else if (path.getSource() == Datapath.Component.INSTRUCTION_MEMORY && 
                   path.getDestination() == Datapath.Component.MUX_reg2loc && 
                   path.getDestinationInput().equals("0")) {
            return BusID.INSTRUCTION_MEMORY_TO_MUX_reg2loc_0;
        }
        else if (path.getSource() == Datapath.Component.INSTRUCTION_MEMORY && 
                   path.getDestination() == Datapath.Component.MUX_reg2loc && 
                   path.getDestinationInput().equals("1")) {
            return BusID.INSTRUCTION_MEMORY_TO_MUX_reg2loc_1;
        }
        else if (path.getSource() == Datapath.Component.MUX_reg2loc && 
                   path.getDestination() == Datapath.Component.REGISTERS && 
                   path.getDestinationInput().equals("Read\nRegister 2")) {
            return BusID.MUX_reg2loc_TO_REGISTERS_READ2;
        } 
        
        else if (path.getSource() == Datapath.Component.INSTRUCTION_MEMORY && 
                   path.getDestination() == Datapath.Component.REGISTERS && 
                   path.getDestinationInput().equals("Write\nRegister")) {
            return BusID.INSTRUCTION_MEMORY_TO_REGISTERS_WRITE;
        } 
        
        else if (path.getSource() == Datapath.Component.INSTRUCTION_MEMORY && 
                   path.getDestination() == Datapath.Component.SIGN_EXTEND) {
            return BusID.INSTRUCTION_MEMORY_TO_SIGN_EXTEND;
        } 

        else if (path.getSource() == Datapath.Component.INSTRUCTION_MEMORY && 
                   path.getDestination() == Datapath.Component.CONTROL_UNIT) {
            return BusID.INSTRUCTION_MEMORY_TO_CONTROL_UNIT;
        }

        else if (path.getSource() == Datapath.Component.INSTRUCTION_MEMORY && 
                   path.getDestination() == Datapath.Component.ALU_CONTROL) {
            return BusID.INSTRUCTION_MEMORY_TO_ALU_CONTROL;
        }

        // Sign Extend paths
        else if (path.getSource() == Datapath.Component.SIGN_EXTEND && 
                   path.getDestination() == Datapath.Component.MUX_ALUsrc) {
            return BusID.SIGN_EXTEND_TO_MUX_ALUsrc;
        } 
        else if (path.getSource() == Datapath.Component.SIGN_EXTEND && 
                   path.getDestination() == Datapath.Component.SHIFT_LEFT_2) {
            return BusID.SIGN_EXTEND_TO_SHIFT_LEFT_2;
        }        
        else if (path.getSource() == Datapath.Component.MUX_ALUsrc && 
                   path.getDestination() == Datapath.Component.ALU) {
            return BusID.MUX_ALUsrc_TO_ALU;
        }

        // PC paths
        else if (path.getSource() == Datapath.Component.PC && 
                   path.getDestination() == Datapath.Component.ADD_1) {
            return BusID.PC_TO_ADD_1;
        } 
        
        else if (path.getSource() == Datapath.Component.PC && 
                   path.getDestination() == Datapath.Component.ADD_2) {
            return BusID.PC_TO_ADD_2;
        } 
        
        else if (path.getSource() == Datapath.Component.PC && 
                   path.getDestination() == Datapath.Component.INSTRUCTION_MEMORY) {
            return BusID.PC_TO_INSTRUCTION_MEMORY;
        } 
        
        else if (path.getSource() == Datapath.Component.MUX_PCSRC && 
                   path.getDestination() == Datapath.Component.PC) {
            return BusID.MUX_PCSRC_TO_PC;
        } 

        else if (path.getSource() == Datapath.Component.ADD_4 && 
                   path.getDestination() == Datapath.Component.ADD_1) {
            return BusID.ADD_4_TO_ADD_1;
        }
        
        else if (path.getSource() == Datapath.Component.ADD_1 && 
                   path.getDestination() == Datapath.Component.MUX_PCSRC) {
            return BusID.ADD_1_TO_MUX_PCSRC;
        } 
        
        else if (path.getSource() == Datapath.Component.ADD_2 && 
                   path.getDestination() == Datapath.Component.MUX_PCSRC) {
            return BusID.ADD_2_TO_MUX_PCSRC;
        } 
        
        else if (path.getSource() == Datapath.Component.SHIFT_LEFT_2 && 
                   path.getDestination() == Datapath.Component.ADD_2) {
            return BusID.SHIFT_LEFT_2_TO_ADD;
        }
        else if (path.getSource() == Datapath.Component.ALU_CONTROL && 
                   path.getDestination() == Datapath.Component.ALU) {
            return BusID.ALU_CONTROL_TO_ALU;
        }

        else if (path.getSource() == Datapath.Component.AND_GATE && 
                   path.getDestination() == Datapath.Component.OR_GATE) {
            return BusID.AND_GATE_TO_OR_GATE;
        }
        else if (path.getSource() == Datapath.Component.OR_GATE && 
                   path.getDestination() == Datapath.Component.MUX_PCSRC) {
            return BusID.OR_GATE_TO_MUX_PCSRC;
        }

        // Control Unit paths
        else if (path.getSource() == Datapath.Component.CONTROL_UNIT && 
                   path.getDestination() == Datapath.Component.MUX_reg2loc) {
            return BusID.CONTROL_REG2LOC_TO_MUX_reg2loc;
        }
        else if (path.getSource() == Datapath.Component.CONTROL_UNIT && 
                   path.getDestination() == Datapath.Component.OR_GATE) {
            return BusID.CONTROL_UNCOND_TO_OR_GATE;
        }
        else if (path.getSource() == Datapath.Component.CONTROL_UNIT && 
                   path.getDestination() == Datapath.Component.AND_GATE) {
            return BusID.CONTROL_BRANCH_TO_AND_GATE;
        }
        else if (path.getSource() == Datapath.Component.CONTROL_UNIT && 
                   path.getDestination() == Datapath.Component.DATA_MEMORY && 
                   path.getSourceOutput().equals("MemRead")) {
            return BusID.CONTROL_MEMREAD_TO_DATA_MEMORY;
        }
        else if (path.getSource() == Datapath.Component.CONTROL_UNIT && 
                   path.getDestination() == Datapath.Component.MUX_memtoreg) {
            return BusID.CONTROL_MEMTOREG_TO_MUX_memtoreg;
        }
        else if (path.getSource() == Datapath.Component.CONTROL_UNIT && 
                   path.getDestination() == Datapath.Component.ALU_CONTROL) {
            return BusID.CONTROL_ALUOP_TO_ALU_CONTROL;
        }
        else if (path.getSource() == Datapath.Component.CONTROL_UNIT && 
                   path.getDestination() == Datapath.Component.DATA_MEMORY &&
                   path.getSourceOutput().equals("MemWrite")) {
            return BusID.CONTROL_MEMWRITE_TO_DATA_MEMORY;
        }
        else if (path.getSource() == Datapath.Component.CONTROL_UNIT && 
                   path.getDestination() == Datapath.Component.MUX_ALUsrc) {
            return BusID.CONTROL_ALUSRC_TO_MUX_ALUsrc;
        }
        else if (path.getSource() == Datapath.Component.CONTROL_UNIT && 
                   path.getDestination() == Datapath.Component.REGISTERS) {
            return BusID.CONTROL_REGWRITE_TO_REGISTERS;
        }

        return null;
    }

    /**
     * Sets the active components and buses to highlight.
     * @param components List of component names to highlight.
     * @param buses List of bus names to highlight.
     */
    public void setActiveComponentsAndBuses(List<String> components, List<String> buses) {
        this.activeComponents = components != null ? new ArrayList<>(components) : new ArrayList<>();
        this.activeBuses = buses != null ? new ArrayList<>(buses) : new ArrayList<>();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));

        // Draw components
        for (ComponentInfo comp : components) {
            drawComponent(g2d, comp);
        }

        // Draw buses (paths)
        for (BusInfo bus : buses) {
            drawBus(g2d, bus);
        }
    }

    /**
     * Draws a component using a PNG image or a rectangle if the image is missing.
     * @param g2d Graphics2D context.
     * @param compInfo ComponentInfo object.
     */
    private void drawComponent(Graphics2D g2d, ComponentInfo compInfo) {
        try {
            ComponentID id = ComponentID.valueOf(compInfo.id);
            boolean isActive = activeComponents.contains(id.name());
            BufferedImage img = isActive ? activeImages.get(id) : inactiveImages.get(id);

            if (isActive && img == null) {
                img = inactiveImages.get(id); // Fallback to inactive image
            }

            if (img != null) {
                g2d.drawImage(img, compInfo.x, compInfo.y, compInfo.width, compInfo.height, this);
            } else {
                g2d.setColor(isActive ? Color.YELLOW : Color.RED);
                g2d.fillRect(compInfo.x, compInfo.y, compInfo.width, compInfo.height);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(compInfo.x, compInfo.y, compInfo.width, compInfo.height);
                System.err.println("Warning: Image not found for component " + id + 
                                   (isActive ? " (active)" : " (inactive)"));
            }

            if (compInfo.label != null && !compInfo.label.isEmpty()) {
                FontMetrics fm = g2d.getFontMetrics();
                int textHeight = fm.getHeight();
                int ascent = fm.getAscent();
                String[] lines = compInfo.label.split("\n");
                int totalLabelHeight = lines.length * textHeight;
                int maxWidth = 0;

                for (String line : lines) {
                    maxWidth = Math.max(maxWidth, fm.stringWidth(line));
                }

                int drawStartX = compInfo.labelRelativeX != null ? 
                    compInfo.x + compInfo.labelRelativeX : 
                    compInfo.x + (compInfo.width - maxWidth) / 2;
                int drawStartY = compInfo.labelRelativeY != null ? 
                    compInfo.y + compInfo.labelRelativeY + ascent : 
                    compInfo.y + (compInfo.height - totalLabelHeight) / 2 + ascent;

                int currentY = drawStartY;
                for (String line : lines) {
                    int lineX = drawStartX;
                    if (compInfo.labelRelativeX == null) {
                        int centerX = compInfo.x + compInfo.width / 2;
                        lineX = centerX - fm.stringWidth(line) / 2;
                    }
                    g2d.drawString(line, lineX, currentY);
                    currentY += textHeight;
                }
            }
        } catch (IllegalArgumentException e) {
            g2d.setColor(Color.RED);
            g2d.drawRect(compInfo.x, compInfo.y, compInfo.width, compInfo.height);
            g2d.drawString("Invalid ID: " + compInfo.id, compInfo.x + 5, compInfo.y + 15);
            System.err.println("Error rendering component: Invalid ID " + compInfo.id);
        }
    }

    /**
     * Draws a bus (path) with an arrow and destination label.
     * @param g2d Graphics2D context.
     * @param busInfo BusInfo object.
     */
    private void drawBus(Graphics2D g2d, BusInfo busInfo) {
        if (busInfo.path == null || busInfo.path.size() < 2) return;

        BusID id = null;
        boolean isBusHighlighted = false;
        Color busColor = DEFAULT_BUS_COLOR;

        try {
            id = BusID.valueOf(busInfo.id);
            isBusHighlighted = activeBuses.contains(id.name());
            if (busInfo.color != null && !busInfo.color.isEmpty()) {
                try {
                    busColor = Color.decode(busInfo.color);
                } catch (NumberFormatException e) {
                    System.err.println("Warning: Invalid color format '" + busInfo.color + "' for bus '" + busInfo.id + "'. Using default.");
                }
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Warning: Bus ID mismatch for '" + busInfo.id + "'.");
            isBusHighlighted = false;
        }

        Color lineColor = isBusHighlighted ? HIGHLIGHT_BUS_COLOR : busColor;
        Stroke lineStroke = isBusHighlighted ? HIGHLIGHT_BUS_STROKE : 
            (busInfo.thickness > 0) ? new BasicStroke(busInfo.thickness) : DEFAULT_BUS_STROKE;
        g2d.setColor(lineColor);
        g2d.setStroke(lineStroke);

        Point prevPoint = null;
        for (Point point : busInfo.path) {
            if (prevPoint != null) {
                g2d.drawLine(prevPoint.x, prevPoint.y, point.x, point.y);
            }
            prevPoint = point;
        }

        if (prevPoint != null && busInfo.path.size() >= 2) {
            Point secondLastPoint = busInfo.path.get(busInfo.path.size() - 2);
            // Draw arrow if configured
            if (busDrawArrow.getOrDefault(id, true)) {
                drawArrowHead(g2d, prevPoint, secondLastPoint, lineColor);
            }

            Set<LabelPlacement> placements = busLabelPlacements.getOrDefault(id, new HashSet<>());
            FontMetrics fm = g2d.getFontMetrics();
            int textHeight = fm.getHeight();

            // Source label
            
            String label = busSourceLabels.get(id);
            String[] lines = label.split("\\n");
            if (label != null && !label.isEmpty()) {
                int labelX, labelY;
                if ("Read\nData 1".equals(label) || "Read\nData 2".equals(label) ||
                    "Zero".equals(label) || "ALU\nResult".equals(label) || "Read\nData".equals(label)) {
                    Point startPoint = busInfo.path.get(0);
                    labelX = startPoint.x - 40; 
                    labelY = startPoint.y - textHeight / 2 + 2;
                    g2d.setColor(Color.BLACK);
                }
                else {
                    Point startPoint = busInfo.path.get(0);
                    labelX = startPoint.x + 5; 
                    labelY = startPoint.y - 2;
                    g2d.setColor(Color.BLACK);
                } 
                for (String line : lines) {
                    g2d.drawString(line, labelX, labelY);
                    labelY += fm.getHeight();
                }
            }
            

            // Destination label
            int labelX_2, labelY_2;
            String label_2 = busDestinationLabels.get(id);
            String[] lines_2 = label_2.split("\\n");
            if (label_2 != null && !label_2.isEmpty()) {
                labelX_2 = prevPoint.x + 5; 
                labelY_2 = prevPoint.y + textHeight / 2;
                g2d.setColor(Color.BLACK);
                for (String line_2 : lines_2) {
                    g2d.drawString(line_2, labelX_2, labelY_2);
                    labelY_2 += fm.getHeight();
                }
            }

            // Animated data value label along path
            if (isBusHighlighted && busDataValues.containsKey(id.name()) && busAnimationProgress.containsKey(id.name())) {
                String dataValue = busDataValues.get(id.name());
                if (dataValue != null && !dataValue.isEmpty()) {
                    float progress = busAnimationProgress.get(id.name());
                    int textWidth = fm.stringWidth(dataValue);
                    Point position = getPositionAlongPath(busInfo.path, progress);
                    int labelX = position.x - textWidth / 2;
                    int labelY = position.y;
                    g2d.setColor(new Color(165, 56, 96));
                    g2d.drawString(dataValue, labelX, labelY);
                }
            }
            
        }
    }
    public void setActiveComponentsAndBuses(List<String> activeComponents, List<String> activeBuses,
                                            Map<String, String> busDataValues) {
        this.activeComponents = new ArrayList<>(activeComponents);
        this.activeBuses = new ArrayList<>(activeBuses);
        this.busDataValues = new HashMap<>(busDataValues);

        // Reset animation progress for new active buses
        this.busAnimationProgress.clear();
        for (String busId : activeBuses) {
            busAnimationProgress.put(busId, 0.0f);
        }

        // Start animation timer if there are active buses
        if (!activeBuses.isEmpty() && !animationTimer.isRunning()) {
            animationTimer.start();
        } else if (activeBuses.isEmpty() && animationTimer.isRunning()) {
            animationTimer.stop();
        }

        repaint();
    }

    private void updateAnimation() {
        boolean animationActive = false;
        for (String busId : new ArrayList<>(busAnimationProgress.keySet())) {
            float progress = busAnimationProgress.get(busId);
            progress += (float) ANIMATION_STEP_MS / ANIMATION_DURATION;
            if (progress >= 1.0f) {
                busAnimationProgress.put(busId, 1.0f);
            } else {
                busAnimationProgress.put(busId, progress);
                animationActive = true;
            }
        }
        if (!animationActive && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        repaint();
    }
    private Point getPositionAlongPath(List<Point> path, float progress) {
        if (path == null || path.size() < 2) return new Point(0, 0);

        // Compute total path length
        double totalLength = 0.0;
        List<Double> segmentLengths = new ArrayList<>();
        for (int i = 1; i < path.size(); i++) {
            Point p1 = path.get(i - 1);
            Point p2 = path.get(i);
            double length = Math.hypot(p2.x - p1.x, p2.y - p1.y);
            segmentLengths.add(length);
            totalLength += length;
        }

        if (totalLength == 0.0) return path.get(0); // Avoid division by zero

        // Calculate target distance along path
        double targetDistance = totalLength * Math.min(progress, 1.0f);
        double accumulatedDistance = 0.0;

        // Find the segment containing the target distance
        for (int i = 0; i < segmentLengths.size(); i++) {
            double segmentLength = segmentLengths.get(i);
            if (accumulatedDistance + segmentLength >= targetDistance) {
                // Interpolate within this segment
                double segmentProgress = (targetDistance - accumulatedDistance) / segmentLength;
                Point p1 = path.get(i);
                Point p2 = path.get(i + 1);
                int x = (int) (p1.x + (p2.x - p1.x) * segmentProgress);
                int y = (int) (p1.y + (p2.y - p1.y) * segmentProgress);
                return new Point(x, y);
            }
            accumulatedDistance += segmentLength;
        }

        // If progress is 1.0, return the last point
        return path.get(path.size() - 1);
    }

    /**
     * Draws an arrowhead at the end of the bus.
     * @param g2d Graphics2D context.
     * @param endPoint End point of the line.
     * @param secondLastPoint Second-to-last point for direction.
     * @param color Arrowhead color.
     */
    private void drawArrowHead(Graphics2D g2d, Point endPoint, Point secondLastPoint, Color color) {
        g2d.setColor(color);
        double angle = Math.atan2(endPoint.y - secondLastPoint.y, endPoint.x - secondLastPoint.x);
        g2d.drawLine(endPoint.x, endPoint.y,
            (int) (endPoint.x - ARROW_HEAD_SIZE * Math.cos(angle - Math.PI / 6)),
            (int) (endPoint.y - ARROW_HEAD_SIZE * Math.sin(angle - Math.PI / 6)));
        g2d.drawLine(endPoint.x, endPoint.y,
            (int) (endPoint.x - ARROW_HEAD_SIZE * Math.cos(angle + Math.PI / 6)),
            (int) (endPoint.y - ARROW_HEAD_SIZE * Math.sin(angle + Math.PI / 6)));
    }
}