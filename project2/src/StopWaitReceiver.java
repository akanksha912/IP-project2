import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;

public class StopWaitReceiver{
	private static int RECVBUFFER = 1000008;//(Data MSS 1000000 bytes+ header 8 bytes)
	private static int SENDBUFFER = 8;//(header 8 bytes)

	private static int PORT = 7735;
	public static int firstPkt = 0;
	public static void main(String[] args) throws IOException {

		// Create a server socket
		DatagramSocket serverSocket = new DatagramSocket( PORT );

		// Set up byte arrays for sending/receiving data
		byte[] rcvData = new byte[ RECVBUFFER ];
		byte[] ackSegment = new byte[ SENDBUFFER ];
		byte[] seqNo = new byte[4];

		// Infinite loop to check for connections 
		while(true){

			// Get the received packet
			DatagramPacket rcvdPkt = new DatagramPacket( rcvData, rcvData.length );
			serverSocket.receive( rcvdPkt );

			// Get the sequence number from the packet
			System.arraycopy(rcvdPkt.getData(), 0, seqNo, 0, 4);


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

}//StopWaitReceiver
