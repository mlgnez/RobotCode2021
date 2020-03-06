package frc.robot;

import edu.wpi.first.wpilibj.PowerDistributionPanel;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.*;
import edu.wpi.first.wpilibj2.command.button.*;
import frc.robot.auton.AutonStates;
import frc.robot.auton.ShootCloseAndDrive;
import frc.robot.commands.*;
import frc.robot.commands.states.*;
import frc.robot.commands.subsystems.*;
import frc.robot.subsystems.*;
import frc.team5431.titan.core.joysticks.*;
import frc.team5431.titan.core.vision.*;

/**
 * @author Ryan Hirasaki
 * @author Colin Wong
 * @author Rishmita Rao
 * @author Daniel Brubaker
 */
public class RobotMap {
	private final PowerDistributionPanel pdp = new PowerDistributionPanel();

	private final Balancer balancer = new Balancer();
	private final Drivebase drivebase = new Drivebase();
	private final Elevator elevator = new Elevator();
	private final Feeder feeder = new Feeder(pdp);
	private final Flywheel flywheel = new Flywheel();
	private final Hopper hopper = new Hopper();
	private final Intake intake = new Intake();
	private final Pivot pivot = new Pivot(pdp);

	private final Xbox driver = new Xbox(0);
	private final Joystick buttonBoard = new Joystick(1);
	private final LogitechExtreme3D operator = new LogitechExtreme3D(2);

	private final Limelight limelight = new Limelight(Constants.VISION_FRONT_LIMELIGHT);

	SendableChooser<AutonStates> chooser = new SendableChooser<>();

	public RobotMap() {
		limelight.setLEDState(LEDState.DEFAULT);
		limelight.setPipeline(9);
		bindKeys();
		outData();

		// chooser.setDefaultOption("Shoot, Drive foward one, stop", StartPosition.SHOOT_AND_DRIVE_FOWARD_ONE);
		// chooser.addOption("Drive back 0.5, Drive Foward 0.5, Shoot, Drive foward one, stop", StartPosition.DRIVE_BACK_AND_FOWARD_THEN_SHOOT_THEN_DRIVE_ONE);
		// SmartDashboard.putData("Auton Select", chooser);
	}

	public void outData() {
		chooser.setDefaultOption("Shoot, Drive foward one, stop", AutonStates.SHOOT_AND_DRIVE_FORWARD_ONE);
		chooser.addOption("Drive back 0.5, Drive Foward 0.5, Shoot, Drive foward one, stop", AutonStates.DRIVE_BACK_AND_FORWARD_THEN_SHOOT_THEN_DRIVE_ONE);
		SmartDashboard.putData("Auton Select", chooser);
		// SmartDashboard.putData("Auton Select", chooser);
	}

	private void bindKeys() {
		// Driver Controls
		{
			// Targetor REMINDER: SET TO FAR FOR TEST
			new JoystickButton(driver, Xbox.Button.B.ordinal() + 1)
					.whenHeld(new Targetor(drivebase, limelight));

			// Calibrate the Limelight
			new JoystickButton(driver, Xbox.Button.X.ordinal() + 1);

			// Intake
			new JoystickButton(driver, Xbox.Button.A.ordinal() + 1).whileHeld(new IntakeCommand(intake, 1));

			// Balancer Left
			new JoystickButton(driver, Xbox.Button.BUMPER_L.ordinal() + 1)
					.whenHeld(new BalancerCommand(balancer, true));

			// Balancer Right
			new JoystickButton(driver, Xbox.Button.BUMPER_R.ordinal() + 1)
					.whenHeld(new BalancerCommand(balancer, false));

		}

		// Operator Controls
		{
			// Human Player Intake
			new JoystickButton(buttonBoard, 11).toggleWhenPressed(new HumanPlayerIntake(feeder, hopper, pivot, flywheel));

			// Floor Intake
			new JoystickButton(buttonBoard, 4).toggleWhenPressed(new FloorIntakeCommand(intake, hopper, pivot, feeder, flywheel)
				.andThen(new PivotCommand(pivot, Pivot.POSITION.UP))); // TODO: test andThen

			// Pivot Down
			new JoystickButton(buttonBoard, 6).whenPressed(new PivotCommand(pivot, Pivot.POSITION.DOWN));

			// Pivot Up
			new JoystickButton(buttonBoard, 5).whenPressed(new PivotCommand(pivot, Pivot.POSITION.UP));

			// Vision Far
			// new JoystickButton(buttonBoard, 3).whenHeld(new Targetor(drivebase, limelight));

			// Vision Close
			new JoystickButton(buttonBoard, 7).whenPressed(new Targetor(drivebase, limelight));

			// // Human player Intake Super Command (labeled "in") TODO
			// new JoystickButton(buttonBoard, 11)
			// .whenPressed(new FloorIntakeSuperCommand(intake, hopper, flywheel, pivot,
			// feeder));

			// Shoot Close
			new JoystickButton(buttonBoard, 1)
					.whenHeld(new ShootSuperCommand(intake, hopper, feeder, flywheel, drivebase, true, false));

			// // Shoot Far
			new JoystickButton(buttonBoard, 14)
					.whenHeld(new ShootSuperCommand(intake, hopper, feeder, flywheel, drivebase, false, false));

			// Intake (By itself)
			// new JoystickButton(buttonBoard, 2)
			// 		.toggleWhenPressed(new IntakeCommand(intake, Constants.INTAKE_DEFAULT_SPEED));

			/*
			 * Not used as it is bound to triggers for the driver but here for historical
			 * purposes
			 * 
			 * // Climb new JoystickButton(buttonBoard, 8);
			 */

			// Stuck On Side.
			// Run Hopper and pivot up with intake on
			// new JoystickButton(buttonBoard, 13);

			new JoystickButton(buttonBoard, 12)
					.whenPressed(new StowSuperCommand(intake, hopper, feeder, flywheel, elevator, balancer, pivot));

			// Auto Switch
			// new JoystickButton(buttonBoard, 9).whenPressed(new KillBallCounter(feeder));
		}

		// Operator Logitech
		{
			// Indexer Up
			new POVButton(operator, 0).whenPressed(new FeederCommand(feeder, flywheel, -Constants.SHOOTER_FEEDER_DEFAULT_SPEED, false))
					.whenReleased(new FeederCommand(feeder, flywheel, 0, false));

			// Indexer Down
			new POVButton(operator, 180).whenPressed(new FeederCommand(feeder, flywheel, Constants.SHOOTER_FEEDER_DEFAULT_SPEED, false))
					.whenReleased(new FeederCommand(feeder, flywheel, 0, false));

			// Trigger Flywheel (Shoot Far)
			new JoystickButton(operator, LogitechExtreme3D.Button.TRIGGER.ordinal() + 1)
					.whenHeld(new FlywheelCommand(flywheel, Flywheel.Velocity.FULL))
					.whenReleased(new FlywheelCommand(flywheel, Flywheel.Velocity.OFF));

			// Arbitrary Flywheel control (Shoot Close)
			new JoystickButton(operator, LogitechExtreme3D.Button.TWELVE.ordinal() + 1)
					.whenHeld(new FlywheelCommand(flywheel, Flywheel.Velocity.HALF))
					.whenReleased(new FlywheelCommand(flywheel, Flywheel.Velocity.OFF));

			// Three Hopper Out
			new JoystickButton(operator, LogitechExtreme3D.Button.THREE.ordinal() + 1)
					.whenHeld(new HopperCommand(hopper, feeder, flywheel, Constants.HOPPER_LEFT_SPEED, Constants.HOPPER_RIGHT_SPEED));

			// Five Hopper In
			new JoystickButton(operator, LogitechExtreme3D.Button.FIVE.ordinal() + 1)
					.whileHeld(new HopperCommand(hopper, feeder, flywheel, -Constants.HOPPER_LEFT_SPEED, -Constants.HOPPER_RIGHT_SPEED));

			// Six Intake Pivot Down
			new JoystickButton(operator, LogitechExtreme3D.Button.SEVEN.ordinal() + 1)
					.whenPressed(new PivotCommand(pivot, Pivot.POSITION.DOWN));
			// .whenReleased(new PivotCommand(pivot, SPEED.ZERO));

			// Four Intake Pivot Up
			new JoystickButton(operator, LogitechExtreme3D.Button.EIGHT.ordinal() + 1)
					.whenPressed(new PivotCommand(pivot, Pivot.POSITION.UP));
			// .whenReleased(new PivotCommand(pivot, SPEED.ZERO));

			// Two Intake
			new JoystickButton(operator, LogitechExtreme3D.Button.TWO.ordinal() + 1)
					.whenHeld(new IntakeCommand(intake, 1));

			// Vision Far 11
			new JoystickButton(operator, 11).whenHeld(new Targetor(drivebase, limelight));

			// Vision Close 12
			new JoystickButton(operator, 12).whenHeld(new Targetor(drivebase, limelight));

			// Ten Intake
			new JoystickButton(operator, 10).whenHeld(new IntakeCommand(intake, Constants.INTAKE_DEFAULT_SPEED, false));

			// Nine Intake Reverse
			new JoystickButton(operator, 9).whenHeld(new IntakeCommand(intake, Constants.INTAKE_DEFAULT_SPEED, true));

			// Flywheel close 6
			new JoystickButton(operator, 6).whenHeld(new FlywheelCommand(flywheel, Flywheel.Velocity.HALF));
		}

		// Default Commands
		{
			driver.setDeadzone(Constants.DRIVER_XBOX_DEADZONE);

			drivebase.setDefaultCommand(new DefaultDrive(drivebase, () -> -driver.getRawAxis(Xbox.Axis.LEFT_Y),
					() -> -driver.getRawAxis(Xbox.Axis.LEFT_X)));

			elevator.setDefaultCommand(new DefaultElevator(elevator,
					() -> driver.getRawAxis(Xbox.Axis.TRIGGER_RIGHT) - driver.getRawAxis(Xbox.Axis.TRIGGER_LEFT)));
		}
	}

	public void resetBallCount() {
		feeder.resetVars();
	}

	public CommandBase getAutonomousCommand() {
		switch(chooser.getSelected()) {
		case SHOOT_AND_DRIVE_FORWARD_ONE:
			return new ShootCloseAndDrive(intake, hopper, feeder, flywheel, drivebase, limelight);
		default:
		case DRIVE_BACK_AND_FORWARD_THEN_SHOOT_THEN_DRIVE_ONE:
			return new SequentialCommandGroup(
				
			);
		}
	}

	public void resetEncoders() {
		pivot.reset();
	}

	public void disabled() {
		CommandScheduler.getInstance().cancelAll();
		resetEncoders();
		limelight.setPipeline(Constants.LIMELIGHT_PIPELINE_OFF);
	}
}