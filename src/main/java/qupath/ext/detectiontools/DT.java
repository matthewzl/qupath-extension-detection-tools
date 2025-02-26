package qupath.ext.detectiontools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.gui.scripting.QPEx;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.roi.interfaces.ROI;
import java.util.HashSet;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for scripting/debugging...
 */
public class DT {
    private static HashMap<ROI, PathClass> copiedObjects = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(DT.class);


    /**
     * Get the currently set radius used to make an ellipse ROI object around the cursor for the
     * CellDragRunner functionality. (Cell objects in the ROI are probed to identify the cell that the mouse is
     * hovering on while dragging.)
     */
    public static double getCellDragRadius(){
        return CellDragRunner.getRadius();
    }

    /**
     * Set the radius used to make an ellipse ROI object around the cursor for the CellDragRunner
     * functionality. (Cell objects in the ROI are probed to identify the cell that the mouse is hovering on
     * while dragging.)
     *
     * @param radius the radius in microns
     */
    public static void setCellDragRadius(double radius){
        CellDragRunner.setRadius(radius);
    }

    /**
     * Merge annotations by their PathClass. Can be helpful if there is lag from having many individual
     * annotations on cells.
     */
    public static void merge(){
        Collection<PathObject> annotations = QPEx.getAnnotationObjects();
        HashSet<PathClass> pathClassSet = new HashSet<>();
        annotations.forEach(annotation -> pathClassSet.add(annotation.getPathClass()));

        for (PathClass pathClass : pathClassSet){
            List<PathObject> collectedAnnotations = annotations
                .stream()
                .filter(annotation -> annotation.getPathClass() == pathClass)
                .collect(Collectors.toList());
            QPEx.mergeAnnotations(collectedAnnotations);
        }
    }

    /**
     * Merge all annotations that intersect with each other.
     */
    public static void mergeIntersecting(){

        HashMap<PathObject, Integer> annotReg = new HashMap<>();
        HashMap<Integer, List<PathObject>> annotRegRev = new HashMap<>();
        int assigned = 0, retrieved = 0;

        List<PathObject> annots = QPEx.getAnnotationObjects().stream().toList();
        for (int i = 0; i < annots.size() - 1; i++) {

            for (int j = i + 1; j < annots.size(); j++) {

                var annotGeomA = annots.get(i).getROI().getGeometry();
                var annotGeomB = annots.get(j).getROI().getGeometry();

                if (annotGeomA.intersects(annotGeomB)) {

                    if (annotReg.get(annots.get(i)) != null && annotReg.get(annots.get(j)) != null) {

                        // print "DIFFERENCE: " + annotReg.get(annots[i]) + " : " + annotReg.get(annots[j])

                        if (annotReg.get(annots.get(i)) > annotReg.get(annots.get(j))) {
                            int larger = annotReg.get(annots.get(i));
                            retrieved = annotReg.get(annots.get(j)); // should get the "count" associated w/ the annot
                            annotReg.put(annots.get(i), retrieved);

                            int finalRetrieved = retrieved;
                            annotRegRev.get(larger).forEach(annot -> {
                                annotRegRev.get(finalRetrieved).add(annot);
                                annotReg.put(annot, finalRetrieved);
                            });
                            annotRegRev.remove(larger);

                        } else if (annotReg.get(annots.get(i)) < annotReg.get(annots.get(j))) {
                            int larger = annotReg.get(annots.get(j));
                            retrieved = annotReg.get(annots.get(i)); // should get the "count" associated w/ the annot
                            annotReg.put(annots.get(j), retrieved);

                            int finalRetrieved = retrieved;
                            annotRegRev.get(larger).forEach (annot -> {
                                annotRegRev.get(finalRetrieved).add(annot);
                                annotReg.put(annot, finalRetrieved);
                            });
                            annotRegRev.remove(larger);

                        }
                        continue;
                    }

                    if (annotReg.get(annots.get(i)) != null || annotReg.get(annots.get(j)) != null) {

                        if (annotReg.get(annots.get(i)) != null) {
                            retrieved = annotReg.get(annots.get(i)); // should get the "count" associated w/ the annot
                            annotReg.put(annots.get(j), retrieved);

                            annotRegRev.get(retrieved).add(annots.get(j));

                        } else {
                            retrieved = annotReg.get(annots.get(j)); // should get the "count" associated w/ the annot
                            annotReg.put(annots.get(i), retrieved);

                            annotRegRev.get(retrieved).add(annots.get(j));
                        }
                        continue;
                    }

                    if (annotReg.get(annots.get(i)) == null && annotReg.get(annots.get(j)) == null) {

                        annotReg.put(annots.get(i), assigned);
                        annotReg.put(annots.get(j), assigned);

                        annotRegRev.put(assigned, new ArrayList<PathObject>());
                        annotRegRev.get(assigned).add(annots.get(i));
                        annotRegRev.get(assigned).add(annots.get(j));

                        assigned++;
                    }
                }
            }
        }

//        print annotReg
//        print annotRegRev

        annotRegRev.entrySet().parallelStream().forEach( e -> {
            QPEx.mergeAnnotations(e.getValue());
        });
    }

}
