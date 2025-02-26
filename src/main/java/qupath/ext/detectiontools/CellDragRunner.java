package qupath.ext.detectiontools;

import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import org.controlsfx.control.action.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.scripting.QPEx;
import qupath.lib.images.ImageData;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.regions.ImagePlane;
import qupath.lib.roi.interfaces.ROI;
import qupath.lib.roi.ROIs;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.stream.Collectors;

public class CellDragRunner implements Runnable{
    private static CellDragRunner instance;
    private static final Logger logger = LoggerFactory.getLogger(CellDragRunner.class);
    private EventHandler<MouseEvent> dragHandler;
    private EventHandler<MouseEvent> clickHandler;
    private final QuPathGUI qupath;
    private Action actionInstance;
    private boolean isActive = false;
    private static List<PathObject> selectedObjectsList = new ArrayList<>(); // Static allows for more convenient reference from elsewhere; should work since CellDragRunner should have only one instance
    private static double radius = 50; // Used to probe for nearby objects. Should be large enough to cover any reasonable cell detection.

    public CellDragRunner(final QuPathGUI qupath) {
        if (instance != null) {
            throw new IllegalStateException("An instance of this class already exists!");
        }
        instance = this;
        this.qupath = qupath;
    }
    @Override
    public void run() {
        var viewer = qupath.getViewer();
        var view = viewer.getView();

        if (isActive) {
            logger.info("Disabling drag selection for cells...");
            view.removeEventHandler(MouseEvent.MOUSE_DRAGGED, dragHandler);
            view.removeEventHandler(MouseEvent.MOUSE_CLICKED, clickHandler);
            actionInstance.setText("Allow enhanced drag selection for cells");
            isActive = false;
            return;
        }

        logger.info("Enabling drag selection for cells...");
        actionInstance.setText("✓ Allow enhanced drag selection for cells");
        isActive = true;

        try{
            selectedObjectsList = QPEx.getSelectedObjects()
                    .stream()
                    .filter(PathObject::isCell)
                    .collect(Collectors.toList());
        } catch (NullPointerException e){}

        dragHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                Point2D point;

                if (!event.isAltDown() && (event.isControlDown() || event.isMetaDown())) {
                    List<PathObject> nearbyObjects = null;
                    try {
                        point = viewer.getMousePosition();
                        nearbyObjects = findObjectsAt(point.getX(), point.getY());
                    } catch (NullPointerException e) {
                        event.consume();
                        return;
                    }
                    for (PathObject obj : nearbyObjects){
                        if (!selectedObjectsList.contains(obj)){
                            selectedObjectsList.add(obj);
                            QPEx.selectObjects(selectedObjectsList);
                        }
                    }
                    event.consume();
                }

                else if (event.isAltDown() && !(event.isControlDown() || event.isMetaDown())) {
                    List<PathObject> nearbyObjects = null;
                    try {
                        point = viewer.getMousePosition();
                        nearbyObjects = findObjectsAt(point.getX(), point.getY());
                    } catch (NullPointerException e) {
                        event.consume();
                        return;
                    }
                    selectedObjectsList.removeAll(nearbyObjects); // Remove all the objects traversed by the mouse
                    QPEx.selectObjects(selectedObjectsList); // Selected the updated list
                    event.consume(); // Prevents interference from other QuPath event handlers
                }

                else if (event.isAltDown() && (event.isControlDown() || event.isMetaDown())){
                    event.consume(); // Prevents interference from other QuPath event handlers
                }
            }
        };

        clickHandler = new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                try {
                    selectedObjectsList = QPEx.getSelectedObjects()
                            .stream()
                            .filter(PathObject::isCell)
                            .collect(Collectors.toList());
                } catch (NullPointerException e) {}
            }
        };

        view.addEventHandler(MouseEvent.MOUSE_DRAGGED, dragHandler);
        view.addEventHandler(MouseEvent.MOUSE_CLICKED, clickHandler);
    }

    private List<PathObject> findObjectsAt(double x, double y) {
        ImageData<BufferedImage> imageData = qupath.getViewer().getImageData();
        if (imageData == null) return Collections.emptyList();

        PathObjectHierarchy hierarchy = imageData.getHierarchy();
        ArrayList<PathObject> foundObjects = new ArrayList<>();

        var viewer = qupath.getViewer();
        Point2D imagePoint = viewer.componentPointToImagePoint(x, y, null, false);

        var plane = ImagePlane.getPlane(0, 0);
        ROI ellipseRoi = ROIs.createEllipseROI(imagePoint.getX() - radius, imagePoint.getY() - radius, 2 * radius, 2 * radius, plane);
        Collection<PathObject> objects = hierarchy.getObjectsForROI(null, ellipseRoi); // Limit the "search field" to only objects within that ROI.
        for (PathObject obj : objects) {
            if (obj.isCell() && obj.getROI().getShape().contains(imagePoint.getX(), imagePoint.getY())) {
                foundObjects.add(obj);
            }
        }
        return foundObjects;
    }

    public void setActionInstance(Action actionInstance){
        this.actionInstance = actionInstance;
    }

    public static List<PathObject> getSelectedObjectsList(){
        return selectedObjectsList;
    }

    public static double getRadius(){
        return radius;
    }

    public static void setRadius(double radius){
        CellDragRunner.radius = radius;
    }

}
