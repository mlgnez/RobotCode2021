package frc.robot.commands;

import org.opencv.videoio.VideoCapture;
import org.opencv.core.*;
import org.opencv.core.Mat;
import org.opencv.highgui.*;

import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.Constants;
import frc.robot.Systems;
import frc.robot.subsystems.*;
import frc.team5431.titan.core.misc.Logger;

import java.util.Dictionary;

/**
 * Not a wrapper for camera capture class, just the command class because making another class would be dumb in this context
 *
 * @author Nicholas Vettor
 */

public class CameraCaptureCommand{

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private static Dictionary<Boolean, Mat> dict = null;

    private static Boolean hasWorked = false;
    private static Mat workingImage = new Mat();

    private static VideoCapture videoCapture;
    private static Mat frame = new Mat();

    public static void initialize(int ID){

        videoCapture = new VideoCapture(ID);

    }

    public static void execute() {

        if (videoCapture.isOpened()) {
            videoCapture.read(frame);
            //HighGui.imshow("Video Window", frame); add gui later lol
            dict = null;
            if(videoCapture.grab()){
                dict = RunAICommand.execute(frame);
            }

            if(dict != null){

                frame = dict.get(true);

                hasWorked = true;

                workingImage = frame;

                //HighGui.imshow("Video Window", frame); make gui work later because you have to show webcam stream to driver station computer and that sounds like a colin question

            }

            if (HighGui.waitKey(41) > 0) {
                System.out.println("Camera is off!");
                videoCapture.release();
            }
        }else{
            System.out.println("Camera Didn't work");
        }

    }

    public static void end() {
        if(videoCapture.isOpened()) {
            videoCapture.release();
        }
    }

    public static Dictionary<Boolean, Mat> getDict() {
        return dict;
    }

    public static Boolean hasWorked(){
        return hasWorked;
    }

    public static Mat returnWorkingImage(){
        return workingImage;
    }

    public static Mat getFrame(){
        return frame;
    }

    public static boolean isOpen() {
        return videoCapture.isOpened();
    }

}
