
package org.jscsi.scsi.protocol.mode;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class ReadWriteErrorRecovery extends ModePage
{
   public static final byte PAGE_CODE = 0x01;
   public static final int PAGE_LENGTH = 0x0A;

   private boolean AWRE;
   private boolean ARRE;
   private boolean TB;
   private boolean RC;
   private boolean EER;
   private boolean PER;
   private boolean DTE;
   private boolean DCR;
   private int readRetryCount;
   private int writeRetryCount;
   private int recoveryTimeLimit;

   public ReadWriteErrorRecovery()
   {
      super(PAGE_CODE);
   }

   @Override
   protected void decodeModeParameters(int dataLength, DataInputStream inputStream)
         throws BufferUnderflowException, IllegalArgumentException
   {
      try
      {
         // byte 2
         int b = inputStream.readUnsignedByte();
         this.AWRE = ((b >>> 7) & 1) == 1;
         this.ARRE = ((b >>> 6) & 1) == 1;
         this.TB = ((b >>> 5) & 1) == 1;
         this.RC = ((b >>> 4) & 1) == 1;
         this.EER = ((b >>> 3) & 1) == 1;
         this.PER = ((b >>> 2) & 1) == 1;
         this.DTE = ((b >>> 1) & 1) == 1;
         this.DCR = (b & 1) == 1;

         // byte 3
         this.readRetryCount = inputStream.readUnsignedByte();

         // byte 4
         inputStream.readByte();

         // byte 5
         inputStream.readByte();

         // byte 6
         inputStream.readByte();

         // byte 7
         inputStream.readByte();

         // byte 8
         this.writeRetryCount = inputStream.readUnsignedByte();

         // byte 9
         inputStream.readByte();

         // bytes 10 - 11
         this.recoveryTimeLimit = inputStream.readUnsignedShort();
      }
      catch (IOException e)
      {
         throw new IllegalArgumentException("Error reading input data.");
      }
   }

   @Override
   protected void encodeModeParameters(ByteBuffer output)
   {
      ByteArrayOutputStream page = new ByteArrayOutputStream(this.getPageLength());
      DataOutputStream out = new DataOutputStream(page);

      try
      {

         // byte 2
         int b = 0;
         if (this.AWRE)
         {
            b |= 0x80;
         }
         if (this.ARRE)
         {
            b |= 0x40;
         }
         if (this.TB)
         {
            b |= 0x20;
         }
         if (this.RC)
         {
            b |= 0x10;
         }
         if (this.EER)
         {
            b |= 0x08;
         }
         if (this.PER)
         {
            b |= 0x04;
         }
         if (this.DTE)
         {
            b |= 0x02;
         }
         if (this.DCR)
         {
            b |= 0x01;
         }
         out.writeByte(b);

         // byte 3
         out.writeByte(this.readRetryCount);

         // byte 4
         out.writeByte(0);

         // byte 5
         out.writeByte(0);

         // byte 6
         out.writeByte(0);

         // byte 7
         out.writeByte(0);

         // byte 8
         out.write(this.writeRetryCount);

         // byte 9
         out.writeByte(0);

         // bytes 10 - 11
         out.writeShort(this.recoveryTimeLimit);

         output.put(page.toByteArray());
      }
      catch (IOException e)
      {
         throw new RuntimeException("Unable to encode CDB.");
      }
   }

   @Override
   protected int getPageLength()
   {
      return PAGE_LENGTH;
   }

   public boolean isAWRE()
   {
      return this.AWRE;
   }

   public void setAWRE(boolean awre)
   {
      this.AWRE = awre;
   }

   public boolean isARRE()
   {
      return this.ARRE;
   }

   public void setARRE(boolean arre)
   {
      this.ARRE = arre;
   }

   public boolean isTB()
   {
      return this.TB;
   }

   public void setTB(boolean tb)
   {
      this.TB = tb;
   }

   public boolean isRC()
   {
      return this.RC;
   }

   public void setRC(boolean rc)
   {
      this.RC = rc;
   }

   public boolean isEER()
   {
      return this.EER;
   }

   public void setEER(boolean eer)
   {
      this.EER = eer;
   }

   public boolean isPER()
   {
      return this.PER;
   }

   public void setPER(boolean per)
   {
      this.PER = per;
   }

   public boolean isDTE()
   {
      return this.DTE;
   }

   public void setDTE(boolean dte)
   {
      this.DTE = dte;
   }

   public boolean isDCR()
   {
      return this.DCR;
   }

   public void setDCR(boolean dcr)
   {
      this.DCR = dcr;
   }

   public int getReadRetryCount()
   {
      return this.readRetryCount;
   }

   public void setReadRetryCount(int readRetryCount)
   {
      this.readRetryCount = readRetryCount;
   }

   public int getWriteRetryCount()
   {
      return this.writeRetryCount;
   }

   public void setWriteRetryCount(int writeRetryCount)
   {
      this.writeRetryCount = writeRetryCount;
   }

   public int getRecoveryTimeLimit()
   {
      return this.recoveryTimeLimit;
   }

   public void setRecoveryTimeLimit(int recoveryTimeLimit)
   {
      this.recoveryTimeLimit = recoveryTimeLimit;
   }
}
