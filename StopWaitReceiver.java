import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.util.Arrays;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;


public class StopWaitReceiver{
	private static int RECVBUFFER = 1048576;//(Data MSS 1000000 bytes+ header 8 bytes)
	private static int SENDBUFFER = 8;//(header 8 bytes)

	private static int PORT = 7735;
	public static int firstPkt = 0;
	public static String FILE_TO_RECEIVED;
	
	public static void main(String[] args) throws IOException {

		// Create a server socket
		DatagramSocket serverSocket = new DatagramSocket( PORT );

		// Set up byte arrays for sending/receiving data
		byte[] rcvData = new byte[ RECVBUFFER ];
		byte[] rcvData1 = new byte[ RECVBUFFER ];
		byte[] ackSegment = new byte[ SENDBUFFER ];
		byte[] seqNo = new byte[4];
		
 		FILE_TO_RECEIVED = "./server-1/abc.txt";
		 
		byte [] mybytearray  = new byte [RECVBUFFER];
	    //  InputStream is = .getInputStream();
	      
		// Infinite loop to check for connections 
		FileOutputStream fos=null;
		BufferedOutputStream bos=null;
		while(true){

			// Get the received packet
			DatagramPacket rcvdPkt = new DatagramPacket( rcvData, rcvData.length );


			//Calculate current time
			long startTime = System.currentTimeMillis();


			serverSocket.receive( rcvdPkt );

			// Get the sequence number from the packet
			System.arraycopy(rcvdPkt.getData(), 0, seqNo, 0, 4);
			
			try {
				
		//	InputStream is = new ByteArrayInputStream(rcvdPkt.getData());
				System.arraycopy(rcvdPkt.getData(), 0, mybytearray, 0, rcvdPkt.getLength());
				InputStream is = new ByteArrayInputStream(mybytearray);
			
			  fos = new FileOutputStream(FILE_TO_RECEIVED);
		      bos = new BufferedOutputStream(fos);
		      int bytesRead = is.read(rcvData1,0,rcvData1.length);
		      System.out.println(bytesRead);
		      int current = bytesRead;

		      do {
		         bytesRead =
		            is.read(rcvData1, current, (rcvData1.length-current));
		         if(bytesRead >= 0) current += bytesRead;
		      } while(bytesRead > -1);
  
		      bos.write(rcvData1, 0 , current);
		      bos.flush();
		      System.out.println("File " + FILE_TO_RECEIVED
		          + " downloaded (" + current + " bytes read)");
			//Calculate current time
			long endTime = System.currentTimeMillis();
		      System.out.println("Time to download = "+(endTime-startTime)+" ms");
			
		    }
		    finally {
		      if (fos != null) fos.close();
		      if (bos != null) bos.close();
		    }
			
		 
			
			//Checksum
//			byte[] received_checksum = copyOfRange(rcvData , 32, 48);
//			CRC32 checksum = new CRC32();
//			checksum.update(copyOfRange(rcvData, 32, 48));
//			byte[] calculated_checksum = ByteBuffer.allocate(16).putLong(checksum.getValue()).array();
//
//			// if packet is not corrupted
//			if (Arrays.equals(received_checksum, calculated_checksum)){
//	
//				System.out.println("Checksum is equal " );
//			}
//			else {
//				System.out.println("Checksum is not equal " );
//				System.out.printf("Rec chksum " ,received_checksum);
//				System.out.printf("Calc chksum ",calculated_checksum );
//			
//				
//			}
			if (Checksum(rcvData)){
			
						System.out.println("Checksum is equal " );
					}
			
			


			//ByteBuffer.wrap(rcvdPkt.getData(seqNo, 0, 4));//
			//int msg = ByteBuffer.wrap(rcvdPkt.getData( )).getInt();
			double p = 0.05;
			Random random = new Random();
			float r = random.nextFloat();

			// 1 in 2 chance of responding to the message
			if( r > p ){
			//if( ((chance % 2) == 0) ){
				firstPkt = 1;
				// Get packet's IP and port
				InetAddress ipAddress = rcvdPkt.getAddress();
				int port = rcvdPkt.getPort();

				// Convert message to uppercase 
				//dataToSend = ByteBuffer.allocate(SENDBUFFER).putInt( msg ).array();
				ackSegment[0] = (byte) (seqNo[0]);
				ackSegment[1] = (byte) (seqNo[1]);
				ackSegment[2] = (byte) (seqNo[2]);
				ackSegment[3] = (byte) (seqNo[3]);
				ackSegment[4] = 0;
				ackSegment[5] = 0;
				ackSegment[6] = -86;//10101010
				ackSegment[7] = -86;//10101010
				System.out.println("Send ACK Sequence No: " + intToString(ackSegment[0]) + intToString(ackSegment[1]) + intToString(ackSegment[2]) + intToString(ackSegment[3]));
				System.out.println("Send ACK Checksum: " + intToString(ackSegment[4]) + intToString(ackSegment[5]));
				System.out.println("Send ACK Indication: " + intToString(ackSegment[6]) + intToString(ackSegment[7]));

				// Send the packet data back to the client
				DatagramPacket packet = new DatagramPacket( ackSegment, ackSegment.length, ipAddress, port );
				serverSocket.send( packet ); 
			} else {
				if(firstPkt == 0){
					System.out.println( "Packet loss, sequence number= "+intToString(seqNo[0]) + intToString(seqNo[1]) + intToString(seqNo[2]) + intToString(seqNo[3]));
				}else{
					//Next expected packet
					byte[] expSeqNo = new byte[4];
					expSeqNo[3] = (byte) ((1) | seqNo[3]);
					expSeqNo[2] = (byte) ((1>>8) | seqNo[2]);
					expSeqNo[1] = (byte) ((1>>16) | seqNo[1]);
					expSeqNo[0] = (byte) ((1>>24) | seqNo[0]);

				System.out.println( "Packet loss, sequence number= "+intToString(expSeqNo[0]) + intToString(expSeqNo[1]) + intToString(expSeqNo[2]) + intToString(expSeqNo[3]));
				}
			}
		}
	}//main


	public static String intToString(int number) {
		StringBuilder result = new StringBuilder();

		for(int i = 7; i >= 0 ; i--) {
			int mask = 1 << i;
			result.append((number & mask) != 0 ? "1" : "0");
		}

		return result.toString();
	}//intToString
	
	private static boolean Checksum(byte[] rcvData) {
		int sum;
		Checksum checksum=new Checksum();
		sum=(int) checksum.calculateChecksum(rcvData);
		if (sum==0)
		 return true;
		else 
		 return false;
	}
	
//	public static byte[] copyOfRange(byte[] srcArr, int start, int end){
//		int length = (end > srcArr.length)? srcArr.length-start: end-start;
//		byte[] destArr = new byte[length];
//		System.arraycopy(srcArr, start, destArr, 0, length);
//		return destArr;
//	}

}//StopWaitReceiver






	
	
	





