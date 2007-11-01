package org.jscsi.scsi.target;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Random;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.jscsi.scsi.lu.LogicalUnit;
import org.jscsi.scsi.protocol.Command;
import org.jscsi.scsi.protocol.cdb.ReportLuns;
import org.jscsi.scsi.protocol.cdb.TestUnitReady;
import org.jscsi.scsi.protocol.inquiry.InquiryDataRegistry;
import org.jscsi.scsi.protocol.inquiry.StaticInquiryDataRegistry;
import org.jscsi.scsi.protocol.mode.ModePageRegistry;
import org.jscsi.scsi.protocol.mode.StaticModePageRegistry;
import org.jscsi.scsi.protocol.sense.SenseData;
import org.jscsi.scsi.protocol.sense.exceptions.IllegalRequestException;
import org.jscsi.scsi.protocol.sense.exceptions.LogicalUnitNotSupportedException;
import org.jscsi.scsi.tasks.AbstractTask;
import org.jscsi.scsi.tasks.Status;
import org.jscsi.scsi.tasks.Task;
import org.jscsi.scsi.tasks.TaskAttribute;
import org.jscsi.scsi.tasks.TaskFactory;
import org.jscsi.scsi.tasks.TaskRouter;
import org.jscsi.scsi.transport.Nexus;
import org.jscsi.scsi.transport.TargetTransportPort;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class GeneralTaskRouterTest
{
   static
   {
      BasicConfigurator.configure();
   }
   
   private static Logger _logger = Logger.getLogger(DummyTask.class);
   
   ////////////////////////////////////////////////////////////////////////////
   // dummy task implementation
   
   private static class DummyTask extends AbstractTask implements TaskFactory
   {
      private static Logger _logger = Logger.getLogger(DummyTask.class);


      ////////////////////////////////////////////////////////////////////////////
      // constructor(s)
      
      public DummyTask()
      {
         super();
      }

      public DummyTask(TargetTransportPort targetPort,
                       Command command,
                       ModePageRegistry modePageRegistry,
                       InquiryDataRegistry inquiryDataRegistry)
      {
         super(targetPort, command, modePageRegistry, inquiryDataRegistry);
      }


      ////////////////////////////////////////////////////////////////////////////
      // protected setters to be used by DummyTaskFactory

      protected void setTargetTransportPort(TargetTransportPort targetPort)
      {
         super.setTargetTransportPort(targetPort);
      }

      protected void setCommand(Command command)
      {
         super.setCommand(command);
      }
      
      protected void setModePageRegistry(ModePageRegistry modePageRegistry)
      {
         super.setModePageRegistry(modePageRegistry);
      }

      protected void setInquiryDataRegistry(InquiryDataRegistry inquiryDataRegistry)
      {
         super.setInquiryDataRegistry(inquiryDataRegistry);
      }

      
      /////////////////////////////////////////////////////////////////////////////
      // Runnable implementation

      public void run()
      {
         _logger.debug("running dummy task");
      }

      /////////////////////////////////////////////////////////////////////////
      // TaskFactory implementation
      
      public Task getInstance(TargetTransportPort port,
                              Command command,
                              ModePageRegistry modePageRegistry,
                              InquiryDataRegistry inquiryDataRegistry)
      throws IllegalRequestException
      {
         return this;
      }
   }
   
   
   
   
   
   public TaskRouter getTaskRouterInstance()
   {
      return new GeneralTaskRouter(new DummyTask(), new StaticModePageRegistry(), new StaticInquiryDataRegistry());
   }
  
   public interface SuccessCallback
   {
      public void success();      
      public void failure(String reason);
   }
   
   public static class TestTargetTransportPort implements TargetTransportPort
   {
      private Nexus expectedNexus;
      private Status expectedStatus;
      private ByteBuffer expectedSenseData;
      private SuccessCallback callback;
      
      
      public TestTargetTransportPort(
            Nexus expectedNexus,
            Status expectedStatus,
            ByteBuffer expectedSenseData,
            SuccessCallback callback)
      {
         this.expectedNexus = expectedNexus;
         this.expectedStatus = expectedStatus;
         this.expectedSenseData = expectedSenseData;
         this.callback = callback;
      }

      public boolean readData(Nexus nexus, long commandReferenceNumber, ByteBuffer output) { return false; }
      public void registerTarget(Target target) {}
      public void removeTarget(String targetName) throws Exception {}
      public void terminateDataTransfer(Nexus nexus) {}
      public boolean writeData(Nexus nexus, long commandReferenceNumber, ByteBuffer input) { return false; }
      

      public void writeResponse(Nexus nexus, long commandReferenceNumber, Status status, ByteBuffer senseData)
      {
         if ( ! nexus.equals(this.expectedNexus) )
         {
            this.callback.failure("Response nexus not equal to expected nexus");
            return;
         }
         if ( status != this.expectedStatus )
         {
            this.callback.failure("Response status not equal to expected status");
            return;
         }
         
         try
         {
            SenseData expected = SenseData.decode(this.expectedSenseData);
            SenseData actual = SenseData.decode(senseData);
            
            if ( expected.getKCQ() != actual.getKCQ() )
            {
               this.callback.failure("Response sense data contains unexpected KCQ");
               return;
            }
            
         }
         catch (BufferUnderflowException e)
         {
            this.callback.failure("Could not decode sense data: " + e.getMessage());
            return;
         }
         catch (IOException e)
         {
            this.callback.failure("Could not decode sense data: " + e.getMessage());
            return;
         }
         
         this.callback.success();
      }
      
   }
   
   public static Command getTestUnitReadyCommand( Nexus nexus )
   {
      return new Command( nexus, new TestUnitReady(), TaskAttribute.SIMPLE, 0, 0 );
   }
   
   public static Command getReportLunsCommand( Nexus nexus )
   {
      return new Command( nexus, new ReportLuns(), TaskAttribute.SIMPLE, 0, 0 );
   }
   
   
   public static class TestLogicalUnit implements LogicalUnit
   {
      private Nexus nexus;
      private SuccessCallback callback;
      

      public TestLogicalUnit(
            String target,
            String initiator,
            long logicalUnitNumber,
            SuccessCallback callback )
      {
         this.nexus = new Nexus( target, initiator, logicalUnitNumber );
         this.callback = callback;
      }
     
      public TestLogicalUnit(
            Nexus nexus,
            SuccessCallback callback)
      {
         this.nexus = nexus;
         this.callback = callback;
      }

      public void enqueue(TargetTransportPort port, Command command)
      {
         if ( !this.nexus.equals(command.getNexus()) )
         {
            this.callback.success();
         }
         else
         {
            this.callback.failure("Enqueued command has improper nexus");
         }
      }

      public void setModePageRegistry(ModePageRegistry modePageRegistry) {}
      
   }
   

   @BeforeClass
   public static void setUpBeforeClass() throws Exception
   {
   }

   @AfterClass
   public static void tearDownAfterClass() throws Exception
   {
   }

   @Before
   public void setUp() throws Exception
   {
   }

   @After
   public void tearDown() throws Exception
   {
   }
   
   @Test
   public void invalidLUNTest()
   {
      
      SuccessCallback transportResults = 
         new SuccessCallback()
         {
            public void failure(String reason)
            {
               failure("Expected router to return invalid LUN error: " + reason);
            }

            public void success() {}
         };
         
      SuccessCallback luResults =
         new SuccessCallback()
         {
            public void failure(String reason)
            {
               fail("Received results in invalid logical unit");
            }
            
            public void success()
            {
               fail("Received results in invalid logical unit");
            }
         };
      
      Nexus nexus = new Nexus( "initA", "targetB", 100 );
         
      TargetTransportPort ttp =
         new TestTargetTransportPort( 
               nexus,
               Status.CHECK_CONDITION,
               (new LogicalUnitNotSupportedException()).encode(),
               transportResults );
      
      LogicalUnit lu = new TestLogicalUnit( nexus, luResults );
      
      TaskRouter router = this.getTaskRouterInstance();
      
      try
      {
         router.registerLogicalUnit(nexus.getLogicalUnitNumber(), lu);
      }
      catch (Exception e)
      {
         e.printStackTrace();
         fail("Exception occurred during Logical Unit registration: " + e.getMessage());
      }
      
      router.enqueue(ttp, GeneralTaskRouterTest.getTestUnitReadyCommand(nexus));
   }
   
   @Test
   public void validRandomLUNTest()
   {
      SuccessCallback transportResults =
         new SuccessCallback()
         {
            public void failure(String reason)
            {
               fail("Received invalid response from Task Router");
            }
            
            public void success()
            {
               fail("Received invalid response from Task Router");
            }
         };
         
      SuccessCallback luResults =
         new SuccessCallback()
         {
            public void failure(String reason)
            {
               fail(reason);
            }
            
            public void success() {}
         };
      
      Nexus nexus = new Nexus( "initA", "targetB", (new Random()).nextLong() );
      
      TargetTransportPort ttp =
         new TestTargetTransportPort(
               nexus,
               Status.GOOD,
               null,
               transportResults );
      
      TaskRouter router = this.getTaskRouterInstance();
      
      try
      {
         router.registerLogicalUnit(0, new TestLogicalUnit("initA", "targetB", 0, luResults));
         router.registerLogicalUnit( 
               nexus.getLogicalUnitNumber(), new TestLogicalUnit(nexus, luResults) );
      }
      catch (Exception e)
      {
         e.printStackTrace();
         fail("Exception occurred during Logical Unit registration: " + e.getMessage());
      }
      
      router.enqueue(ttp, GeneralTaskRouterTest.getTestUnitReadyCommand(nexus));
   }
   
   @Test
   public void validDefaultLUNTest()
   {
      SuccessCallback transportResults =
         new SuccessCallback()
         {
            public void failure(String reason)
            {
               fail("Received invalid response from Task Router");
            }
            
            public void success()
            {
               fail("Received invalid response from Task Router");
            }
         };
         
      SuccessCallback luResults =
         new SuccessCallback()
         {
            public void failure(String reason)
            {
               fail(reason);
            }
            
            public void success() {}
         };
         
         Nexus nexus = new Nexus( "initA", "targetB", 0 );
         
         TargetTransportPort ttp =
            new TestTargetTransportPort(
                  nexus,
                  Status.GOOD,
                  null,
                  transportResults );
         
         TaskRouter router = this.getTaskRouterInstance();
         
         try
         {
            router.registerLogicalUnit( 
                  nexus.getLogicalUnitNumber(), new TestLogicalUnit(nexus, luResults) );
         }
         catch (Exception e)
         {
            e.printStackTrace();
            fail("Exception occurred during Logical Unit registration: " + e.getMessage());
         }
         
         router.enqueue(ttp, GeneralTaskRouterTest.getTestUnitReadyCommand(nexus));
   }
   
   // TODO: I_T nexus task test (need interface for target task executor first)

}


