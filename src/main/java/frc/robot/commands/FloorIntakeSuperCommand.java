package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.ParallelCommandGroup;
import frc.robot.Constants;
import frc.robot.subsystems.*;

/**
 * @author Colin Wong
 * @author Ryan Hirasaki
 */
public class FloorIntakeSuperCommand extends ParallelCommandGroup {
    public FloorIntakeSuperCommand(Intake intake, Hopper hopper, Flywheel flywheel, Pivot pivot) {
        addCommands(
            new PivotCommand(pivot, Pivot.POSITION.DOWN), // Puts the Intake Down
            new IntakeCommand(intake, 1.0), // Starts the Motor
            new HopperCommand(hopper, Constants.HOPPER_LEFT_SPEED, Constants.HOPPER_RIGHT_SPEED)  // Starts the Motor
        );
    }
}