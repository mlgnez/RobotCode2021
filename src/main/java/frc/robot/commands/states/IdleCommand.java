package frc.robot.commands.states;

import com.ctre.phoenix.motorcontrol.NeutralMode;
import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.Constants;
import frc.robot.commands.CameraCaptureCommand;
import frc.robot.subsystems.Drivebase;
import frc.team5431.titan.core.vision.CameraMode;
import frc.team5431.titan.core.vision.LEDState;
import frc.team5431.titan.core.vision.Limelight;

/**
 * Watch out Phillip
 *
 * @author Rudy Soliz
 * @author Nicholas Vettor
 */

public class IdleCommand extends CommandBase {
    private final Drivebase drivebase;

    public int rotationCounts;
    public boolean finishedFlag = false;
    public int CameraID = 0;

    public IdleCommand(Drivebase drivebase) {
        this.drivebase = drivebase;
        // each subsystem used by the command must be passed into the
        // addRequirements() method (which takes a vararg of Subsystem)
        addRequirements(this.drivebase);
    }

    /**
     * The initial subroutine of a command.  Called once when the command is initially scheduled.
     */
    @Override
    public void initialize() {
        resetVarsToDefault();

        CameraCaptureCommand.initialize(CameraID);

    }

    /**
     * The main body of a command.  Called repeatedly while the command is scheduled.
     * (That is, it is called repeatedly until {@link #isFinished()}) returns true.)
     */
    @Override
    public void execute() {
        drivebase.drivePercentageArcade(0, 0.3f);
        // TODO: Check if apriltag is detected then set finish-flag to true

        CameraCaptureCommand.execute();

        //somehow send frame using wpilib CameraCaptureCommand.getFrame();

        if(CameraCaptureCommand.getDict() != null) {

            if (CameraCaptureCommand.hasWorked()) {

                finishedFlag = true;

                /*Limelight limelight = new Limelight(Constants.VISION_FRONT_LIMELIGHT);

                limelight.setLEDState(LEDState.DEFAULT);
                limelight.setPipeline(9);

                limelight.setCameraMode(CameraMode.VISION);

                //if(limelight.)

                //keep limelight on until it gets results, then aim and shoot!

                //implement debounce so this doesnt run 100's of times (also add timeout to that debounce, so it doesnt break the robot)

                //ill try to get the robot to stop spinning if it detects something */

            }

        }

        // Maybe with a bool ApriltagDetect()
    }

    /**
     * <p>
     * Returns whether this command has finished. Once a command finishes -- indicated by
     * this method returning true -- the scheduler will call its {@link #end(boolean)} method.
     * </p><p>
     * Returning false will result in the command never ending automatically. It may still be
     * cancelled manually or interrupted by another command. Hard coding this command to always
     * return true will result in the command executing once and finishing immediately. It is
     * recommended to use * {@link edu.wpi.first.wpilibj2.command.InstantCommand InstantCommand}
     * for such an operation.
     * </p>
     *
     * @return whether this command has finished.
     */
    @Override
    public boolean isFinished() {
        return finishedFlag;
    }

    /**
     * The action to take when the command ends. Called when either the command
     * finishes normally -- that is it is called when {@link #isFinished()} returns
     * true -- or when  it is interrupted/canceled. This is where you may want to
     * wrap up loose ends, like shutting off a motor that was being used in the command.
     *
     * @param interrupted whether the command was interrupted/canceled
     */
    @Override
    public void end(boolean interrupted) {
        drivebase.drivePercentageArcade(0, 0);
        resetVarsToDefault();
    }

    public void resetVarsToDefault() {
        rotationCounts = 0;
        finishedFlag = false;
    }
}
