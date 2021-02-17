package frc.robot.subsystems;

import java.util.List;

import com.ctre.phoenix.ErrorCode;
import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.DemandType;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.FollowerType;
import com.ctre.phoenix.motorcontrol.RemoteSensorSource;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonFX;
import com.ctre.phoenix.sensors.PigeonIMU;

import edu.wpi.first.wpilibj.SpeedController;
import edu.wpi.first.wpilibj.geometry.Pose2d;
import edu.wpi.first.wpilibj.geometry.Rotation2d;
import edu.wpi.first.wpilibj.kinematics.DifferentialDriveOdometry;
import edu.wpi.first.wpilibj.kinematics.DifferentialDriveWheelSpeeds;
import edu.wpi.first.wpilibj.simulation.DifferentialDrivetrainSim;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.system.plant.LinearSystemId;
import edu.wpi.first.wpiutil.math.Matrix;
import edu.wpi.first.wpiutil.math.VecBuilder;
import edu.wpi.first.wpiutil.math.numbers.N1;
import edu.wpi.first.wpiutil.math.numbers.N7;
import frc.robot.Constants;
import frc.robot.util.MotionMagic;
import frc.team5431.titan.core.misc.Logger;
import frc.team5431.titan.core.subsystem.DrivebaseSubsystem;
import frc.team5431.titan.pathfinder.PathFinderControls;
/*
 * a lot of asserts were added as there are many things that can go wrong in this code
*/

/**
 * @author Ryan Hirasaki
 * @author Colin Wong
 */
public class Drivebase extends DrivebaseSubsystem implements PathFinderControls {

    private static class ProcessError {
        public interface Function {
            ErrorCode run();
        }

        public static void test(Function lambda) {
            ErrorCode code = lambda.run();
            // Weird syntax but valid
            // https://docs.oracle.com/javase/8/docs/technotes/guides/language/assert.html
            assert (code == ErrorCode.OK) : code.toString();
        }
    }

    private PigeonIMU pidgey;

    private WPI_TalonFX left;
    private WPI_TalonFX right;

    private WPI_TalonFX _leftFollow;
    private WPI_TalonFX _rightFollow;

    private double ramping;

    private DifferentialDriveOdometry odometry;

    private Field2d m_field = new Field2d();
    private DifferentialDrivetrainSim drivetrainSim;

    public Drivebase(WPI_TalonFX frontLeft, WPI_TalonFX frontRight, WPI_TalonFX rearLeft, WPI_TalonFX rearRight) {

        pidgey = new PigeonIMU(Constants.DRIVEBASE_PIGEON_IMU_ID);

        left = frontLeft;
        right = frontRight;

        _leftFollow = rearLeft;
        _rightFollow = rearRight;

        left.setInverted(Constants.DRIVEBASE_LEFT_REVERSE);
        right.setInverted(Constants.DRIVEBASE_RIGHT_REVERSE);
        _leftFollow.setInverted(Constants.DRIVEBASE_LEFT_REVERSE);
        _rightFollow.setInverted(Constants.DRIVEBASE_RIGHT_REVERSE);

        _leftFollow.follow(left);
        _rightFollow.follow(right);

        /* Factory Default all hardware to prevent unexpected behavior */
        ProcessError.test(() -> left.configFactoryDefault());
        ProcessError.test(() -> right.configFactoryDefault());
        ProcessError.test(() -> pidgey.configFactoryDefault());

        /* Set what state the motors will be at when the speed is at zero */
        left.setNeutralMode(Constants.DRIVEBASE_NEUTRAL_MODE);
        right.setNeutralMode(Constants.DRIVEBASE_NEUTRAL_MODE);

        /* Set the motor output ranges */
        ProcessError.test(() -> left.configPeakOutputForward(1, Constants.DRIVEBASE_TIMEOUT_MS));
        ProcessError.test(() -> left.configPeakOutputReverse(1, Constants.DRIVEBASE_TIMEOUT_MS));
        ProcessError.test(() -> right.configPeakOutputForward(1, Constants.DRIVEBASE_TIMEOUT_MS));
        ProcessError.test(() -> right.configPeakOutputReverse(1, Constants.DRIVEBASE_TIMEOUT_MS));

        /* Tell motors which sensors it is reading from */
        ProcessError.test(() -> left.configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder,
                Constants.DRIVEBASE_MOTIONMAGIC_DRIVE_REMOTE, Constants.DRIVEBASE_TIMEOUT_MS));

        /* Tell the motors which sesnor specifically is being used */
        ProcessError.test(
                () -> right.configRemoteFeedbackFilter(left.getDeviceID(), RemoteSensorSource.TalonFX_SelectedSensor,
                        Constants.DRIVEBASE_MOTIONMAGIC_DRIVE_REMOTE, Constants.DRIVEBASE_TIMEOUT_MS));
        ProcessError.test(() -> right.configRemoteFeedbackFilter(pidgey.getDeviceID(), RemoteSensorSource.Pigeon_Yaw,
                Constants.DRIVEBASE_MOTIONMAGIC_TURN_REMOTE, Constants.DRIVEBASE_TIMEOUT_MS));

        ProcessError.test(() -> right.configSelectedFeedbackSensor(FeedbackDevice.SensorSum,
                Constants.DRIVEBASE_MOTIONMAGIC_DRIVE_REMOTE, Constants.DRIVEBASE_TIMEOUT_MS));
        ProcessError.test(() -> right.configSelectedFeedbackSensor(FeedbackDevice.RemoteSensor1,
                Constants.DRIVEBASE_MOTIONMAGIC_TURN_REMOTE, Constants.DRIVEBASE_TIMEOUT_MS));

        ProcessError.test(() -> right.configSelectedFeedbackCoefficient(0.5,
                Constants.DRIVEBASE_MOTIONMAGIC_DRIVE_REMOTE, Constants.DRIVEBASE_TIMEOUT_MS));
        ProcessError.test(() -> right.configSelectedFeedbackCoefficient(1, Constants.DRIVEBASE_MOTIONMAGIC_TURN_REMOTE,
                Constants.DRIVEBASE_TIMEOUT_MS));

        odometry = new DifferentialDriveOdometry(Rotation2d.fromDegrees(-pidgey.getFusedHeading()));

        /* Set PID values for each slot */
        setPID(Constants.DRIVEBASE_MOTIONMAGIC_DRIVE_SLOT, Constants.DRIVEBASE_MOTIONMAGIC_DRIVE_GAINS);
        setPID(Constants.DRIVEBASE_MOTIONMAGIC_TURN_SLOT, Constants.DRIVEBASE_MOTIONMAGIC_TURN_GAINS);

        right.selectProfileSlot(Constants.DRIVEBASE_MOTIONMAGIC_DRIVE_SLOT,
                Constants.DRIVEBASE_MOTIONMAGIC_DRIVE_REMOTE);
        right.selectProfileSlot(Constants.DRIVEBASE_MOTIONMAGIC_TURN_SLOT, Constants.DRIVEBASE_MOTIONMAGIC_TURN_REMOTE);

        zeroDistance();

        setRamping(Constants.DRIVEBASE_DEFAULT_RAMPING);

        Matrix<N7, N1> deviation = null;
        if (Constants.ROBOT_DEVIATION_ENABLE) {
            deviation = VecBuilder.fill(//
                    Constants.ROBOT_DEVIATION_X, Constants.ROBOT_DEVIATION_Y, //
                    Constants.ROBOT_DEVIATION_HEADING, //
                    Constants.ROBOT_DEVIATION_VEL_L, Constants.ROBOT_DEVIATION_VEL_R, //
                    Constants.ROBOT_DEVIATION_POS_L, Constants.ROBOT_DEVIATION_POS_R);
        }

        drivetrainSim = new DifferentialDrivetrainSim(//
                LinearSystemId.identifyDrivetrainSystem( //
                        Constants.ROBOT_V_LINEAR, //
                        Constants.ROBOT_A_LINEAR, //
                        Constants.ROBOT_V_ANGULAR, //
                        Constants.ROBOT_A_ANGULAR //
                ), //
                Constants.ROBOT_GEARBOX_MOTORS, //
                Constants.GEAR_RATIO, //
                Constants.DRIVEBASE_PATHWEAVER_CONFIG.kTrackwidthMeters, //
                Constants.WHEEL_CIRCUMFERENCE / (Math.PI * 2.0), // Get radius from circumfrence
                deviation);
    }

    private void setPID(final int slot, final MotionMagic gain) {
        ProcessError.test(() -> right.config_kP(slot, gain.kP));
        ProcessError.test(() -> right.config_kI(slot, gain.kI));
        ProcessError.test(() -> right.config_kD(slot, gain.kD));
        ProcessError.test(() -> right.config_kF(slot, gain.kF));
        ProcessError.test(() -> right.config_IntegralZone(slot, gain.kIntegralZone, Constants.DRIVEBASE_TIMEOUT_MS));
        ProcessError
                .test(() -> right.configClosedLoopPeakOutput(slot, gain.kPeakOutput, Constants.DRIVEBASE_TIMEOUT_MS));
        ProcessError
                .test(() -> right.configClosedLoopPeriod(slot, gain.kClosedLoopTime, Constants.DRIVEBASE_TIMEOUT_MS));
    }

    private void zeroDistance() {
        ProcessError
                .test(() -> left.getSensorCollection().setIntegratedSensorPosition(0, Constants.DRIVEBASE_TIMEOUT_MS));
        ProcessError
                .test(() -> right.getSensorCollection().setIntegratedSensorPosition(0, Constants.DRIVEBASE_TIMEOUT_MS));
    }

    @Override
    public void periodic() {

        SmartDashboard.putNumber("Drivebase Left", left.get());
        SmartDashboard.putNumber("Drivebase Right", right.get());
        SmartDashboard.putNumber("Drivebase Right Encoder", right.getSelectedSensorPosition());
        SmartDashboard.putNumber("Drivebase Left Encoder", left.getSelectedSensorPosition());
        setRamping(ramping);
        updateOdometry();
        SmartDashboard.putData("Field", m_field);

        // Check if the the motors are working together
        assert (left.get() == _leftFollow.get());
        assert (right.get() == _rightFollow.get());
    }

    public void setSlot(int slot) {
        // Add asserts as the motorcontrollers only support 4 slots
        assert (slot >= 0);
        assert (slot <= 3);

        right.selectProfileSlot(slot, 0);
    }

    public void driveMotionMagic(double distance, double angle) {
        left.follow(right, FollowerType.AuxOutput1);
        right.set(ControlMode.MotionMagic, distance, DemandType.AuxPID, angle);

        Logger.l("Distance: %f, Angle: %f", distance, angle);
    }

    public float getHeading() {
        return (float) pidgey.getCompassHeading();
    }

    public void updateOdometry() {
        odometry.update(Rotation2d.fromDegrees(pidgey.getFusedHeading()), //
                getLeftDistance(), getRightDistance());
        m_field.setRobotPose(odometry.getPoseMeters());
    }

    public void setRamping(double ramping) {
        this.ramping = ramping;

        left.configOpenloopRamp(ramping);
        left.configClosedloopRamp(0);
        right.configOpenloopRamp(ramping);
        right.configClosedloopRamp(0);
    }

    public void resetSensors() {
        zeroDistance();
    }

    public double getLeftEncoderCount() {
        return left.getSelectedSensorPosition() / Constants.COUNTS_PER_REVOLUTION;
    };

    public double getRightEncoderCount() {
        return right.getSelectedSensorPosition() / Constants.COUNTS_PER_REVOLUTION;
    };

    public double getLeftDistance() {
        return getLeftEncoderCount() / Constants.COUNTS_PER_REVOLUTION * Constants.WHEEL_CIRCUMFERENCE
                * Constants.GEAR_RATIO;
    };

    public double getRightDistance() {
        return getRightEncoderCount() / Constants.COUNTS_PER_REVOLUTION * Constants.WHEEL_CIRCUMFERENCE
                * Constants.GEAR_RATIO;
    };

    public double getRPM() {
        return ((getLeftEncoderCount() * 600) + (getRightEncoderCount() * 600)) / 2;
    }

    public Pose2d getPose() {
        return odometry.getPoseMeters();
    }

    public void tankDriveVolts(double leftVolts, double rightVolts) {
        left.setVoltage(leftVolts);
        right.setVoltage(-rightVolts);
        // m_drive.feed();
    }

    public DifferentialDriveWheelSpeeds getWheelSpeeds() {
        throw new RuntimeException("Unimplemented");
    }

    public List<WPI_TalonFX> getMotors() {
        return List.of(new WPI_TalonFX[] { left, right, _leftFollow, _rightFollow });
    }

    @Override
    protected SpeedController getLeft() {
        return left;
    }

    @Override
    protected SpeedController getRight() {
        return right;
    }

    @Override
    protected double getMaxTurnValue() {
        return Constants.DRIVEBASE_TURN_MAX_SPEED;
    }
}