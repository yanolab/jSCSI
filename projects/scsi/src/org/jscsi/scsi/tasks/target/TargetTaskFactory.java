
package org.jscsi.scsi.tasks.target;

import java.util.Set;

import org.jscsi.scsi.protocol.Command;
import org.jscsi.scsi.protocol.cdb.ReportLuns;
import org.jscsi.scsi.protocol.sense.exceptions.IllegalRequestException;
import org.jscsi.scsi.protocol.sense.exceptions.InvalidCommandOperationCodeException;
import org.jscsi.scsi.tasks.Task;
import org.jscsi.scsi.tasks.TaskFactory;
import org.jscsi.scsi.transport.TargetTransportPort;

public class TargetTaskFactory implements TaskFactory
{
   private Set<Long> logicalUnits;

   public TargetTaskFactory(Set<Long> logicalUnits)
   {
      this.logicalUnits = logicalUnits;
   }

   public Task getInstance(TargetTransportPort port, Command command)
         throws IllegalRequestException
   {
      switch (command.getCommandDescriptorBlock().getOperationCode())
      {
         case ReportLuns.OPERATION_CODE :
            return new ReportLunsTask(logicalUnits, port, command, null, null);
         default :
            throw new InvalidCommandOperationCodeException();
      }
   }
}
