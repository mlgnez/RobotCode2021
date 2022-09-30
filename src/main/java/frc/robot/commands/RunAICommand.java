package frc.robot.commands;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import edu.wpi.first.wpilibj2.command.CommandBase;
//import edu.wpi.first.wpilibj2.command.ParallelCommandGroup;
import frc.robot.Constants;
import frc.robot.Systems;
import frc.robot.subsystems.*;
import frc.team5431.titan.core.misc.Logger;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * Runs the AI and returns if the result was positive or not
 *
 * @author Nicholas Vettor
 */

public class RunAICommand{

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static Dictionary<Boolean, Mat> execute(Mat image){
        System.out.println("\nRunning DetectFaceDemo");
        CascadeClassifier faceDetector = new CascadeClassifier(Constants.DATASET_XML_NAME);
        MatOfRect faceDetections = new MatOfRect();
        faceDetector.detectMultiScale(image, faceDetections);
        System.out.println(String.format("Detected %s faces", faceDetections.toArray().length));
        for (Rect rect : faceDetections.toArray()) {
            Imgproc.rectangle(image, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0));
        }
        Dictionary<Boolean, Mat> booleanMatDictionary = new Hashtable<>();
        booleanMatDictionary.put(faceDetections.size().width != 0 || faceDetections.size().height != 0 , image);
        return booleanMatDictionary;
    }

}
