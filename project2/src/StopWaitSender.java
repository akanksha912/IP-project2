import java.io.*; //file access
import java.net.*; //network access
import java.nio.*; //non-blocking I/O (intensive I/O operations)
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.Thread.*;




public class StopWaitSender{

	//Declare variables
	private static int BUFFER = 1008;//(Data MSS 1000000 bytes+ header 8 bytes)
	private static int PORT1 = 7735;
	private static int PORT2 = 7736;
//	private static String HOSTNAME = "localhost";
	public static int counter = 0;
	public static int pack = 3;
	public static int mss = 100; //TODO: Command line arg
	public final static String FILE_TO_SEND = "/home/tripti/abc.txt";
	public static byte [] newBuffer  = new byte [1008];//Buffer for writing MSS size of data read from file

	//Header elements of sendSegment
	static AtomicInteger sequenceNo = new AtomicInteger(0);//Sequence Number
	//public volatile AtomicInteger sequenceNo = new AtomicInteger(0);//Sequence Number

	public static void main(String args[]) throws Exception{

		StopWaitSender sender = new StopWaitSender();
		sender.createPacket();

	}//main	

	private void createPacket() throws Exception{	
		//Constructs a datagram socket and binds it to any available port on the local host machine for UDP transport.
		//DatagramSocket socket = new DatagramSocket();		
		//socket.setSoTimeout(1000); //Enable/disable SO_TIMEOUT(Set a timeout on blocking Socket operations) with the specified timeout, in milliseconds.
		int cksum;
		FileInputStream fis = null;
		BufferedInputStream bis = null;
		//InetAddress ipAddress = InetAddress.getByName(HOSTNAME); //getByName- Determines the IP address of a host, given the host's name.
		byte[] sendSegment = new byte[BUFFER]; //byte array for sending data
		byte[] recvSegment = new byte[BUFFER]; //byte array for receiving data 
		boolean timeOut = true;
		while(timeOut){
			//while(timeOut && pack !=0){
			//Header
			//32 bits of sequence number
			sendSegment[0] = (byte) ((sequenceNo.get()>>24)&0xFF);	//Most significant 8 bits
			sendSegment[1] = (byte) ((sequenceNo.get()>>16)&0xFF);	
			sendSegment[2] = (byte) ((sequenceNo.get()>>8)&0xFF);	
			sendSegment[3] = (byte) (sequenceNo.get()&0xFF); //Least significant 8 bits

			//16 bits of checksum	
			sendSegment[4] = 0; //Init checksum	
			sendSegment[5] = 0;

			//16 bits of data indication (0101010101010101)	
			sendSegment[6] = 85;//01010101	
			sendSegment[7] = 85;//01010101

			//Update Checksum
			//This checksum is calculated only for the header bytes (with the checksum bytes set to 0), 
			//is 16 bits long and is a part of the IP packet header.

			Checksum checksum=new Checksum();
			cksum=(int) checksum.calculateChecksum(sendSegment);

			sendSegment[4]=(byte)((cksum>>8)&0xFF);
			sendSegment[5]=(byte)((cksum)&0xFF);

			System.out.println("Sequence Number: " + intToString(sendSegment[0]) + intToString(sendSegment[1]) + intToString(sendSegment[2]) + intToString(sendSegment[3]));
			System.out.println("Checksum: " + intToString(sendSegment[4]) + intToString(sendSegment[5]));
			System.out.println("Data Indication: " + intToString(sendSegment[6]) + intToString(sendSegment[7]));
			int nextSequence = sequenceNo.getAndIncrement();//For next sequence number	

			//Data
			System.out.println("Waiting...");
			// send file
			File myFile = new File (FILE_TO_SEND);
			fis = new FileInputStream(myFile);
			bis = new BufferedInputStream(fis);
			System.out.println( "Counter " + counter );
			bis.read(newBuffer,0+counter,mss);//MSS bytes copied 
			System.out.println("Sending " + mss + " bytes (MSS)");
			System.out.println("Done.");
			for(int i = 0; i <= 99; i++){
				sendSegment[8+i] = newBuffer[counter];
				int sum = 8 + i;
				counter++;//make counter value zero at the end of sending all the packets
			}
			//TO BE IMPLEMENTED IN THREAD START
			//	try{
			//		//Send the UDP Packet to the server
			//		DatagramPacket packet = new DatagramPacket(sendSegment, sendSegment.length, ipAddress, PORT);
			//		socket.send( packet );
			//			System.out.println( "Packet Sent!!!");

			//		// Receive the server's packet
			//		DatagramPacket rcvdPkt = new DatagramPacket(recvSegment, recvSegment.length);
			//		socket.receive( rcvdPkt );
			//			System.out.println( "Packet Received!!!");

			//		// Get the message from the server's packet
			//		int returnMessage = ByteBuffer.wrap( rcvdPkt.getData( ) ).getInt();

			//		System.out.println( "ACK= " + returnMessage );
			//		pack--;
			//		// If we receive an ack, stop the while loop
			//	//	timeOut = false;
			//	//	System.out.println( "timeOut is false");


			//	} catch( SocketTimeoutException exception ){

			//		// If we don't get an ack, resend sequence number
			//		nextSequence = sequenceNo.getAndDecrement();//For next sequence number	
			//		System.out.println( "Timeout, sequence number= " + sequenceNo.get());
			//		counter = counter-mss;//since the packet was lost, again read the same mss bytes
			//	}	
			//TO BE IMPLEMENTED IN THREAD END

			//Thread start for each of the servers
			PacketTransfer T1 = new PacketTransfer( "server-1", sendSegment, PORT1, BUFFER);

			//try{
				T1.start();
//			}catch(SocketTimeoutException exception){
		//		// If we don't get an ack, resend sequence number
		//		int nextSeq = sequenceNo.getAndDecrement();//For next sequence number	
		//		System.out.println( "Timeout, sequence number= " + sequenceNo.get());
		//		counter = counter-mss;//since the packet was lost, again read the same mss bytes

		//	}

			//PacketTransfer T2 = new PacketTransfer( "server-2", sendSegment, PORT2, BUFFER);
			//T2.start();
			timeOut = false;	
		}//while(timeOut == true)
		//System.out.println( "closing socket");
		//socket.close();


		}//createPacket

		public static String intToString(int number) {
			StringBuilder result = new StringBuilder();

			for(int i = 7; i >= 0 ; i--) {
				int mask = 1 << i;
				result.append((number & mask) != 0 ? "1" : "0");
			}

			return result.toString();
		}//intToString	


		//class PacketTransfer implements Runnable {
		class PacketTransfer extends Thread {
			private Thread t;
			private String threadName;
			private int PORT;
			private int SIZE;
			private byte[] sendSegment = new byte[SIZE]; //byte array for sending data
			private byte[] recvSegment = new byte[SIZE]; //byte array for receiving data 
			private String HOSTNAME = "localhost";
			//	public volatile AtomicInteger sequenceNo = new AtomicInteger(0);//Sequence Number


			PacketTransfer( String name, byte[] sendSeg, int port, int bufsize) {
				threadName = name;
				PORT = port;
				sendSegment = sendSeg;
				SIZE = bufsize;
				System.arraycopy(sendSeg, 0, sendSegment, 0, SIZE);
				System.out.println("Creating " +  threadName );
			}

			public void run() {
				System.out.println("Running " +  threadName );
			//	try{
					//Constructs a datagram socket and binds it to any available port on the local host machine for UDP transport.
					DatagramSocket socket = new DatagramSocket();		
					socket.setSoTimeout(1000); //Enable/disable SO_TIMEOUT(Set a timeout on blocking Socket operations) with the specified timeout, in milliseconds.
					InetAddress ipAddress = InetAddress.getByName(HOSTNAME);//getByName- Determines the IP address of a host, given the host's name.
					//Send the UDP Packet to the server
					DatagramPacket packet = new DatagramPacket(sendSegment, sendSegment.length, ipAddress, PORT);
					socket.send( packet );
					System.out.println( "Packet Sent!!!");

					// Receive the server's packet
					DatagramPacket rcvdPkt = new DatagramPacket(recvSegment, recvSegment.length);
					socket.receive( rcvdPkt );
					System.out.println( "Packet Received!!!");

					// Get the message from the server's packet
					int returnMessage = ByteBuffer.wrap( rcvdPkt.getData( ) ).getInt();

					System.out.println( "ACK= " + returnMessage );
					//pack--;
					// If we receive an ack, stop the while loop
					//	timeOut = false;
					//	System.out.println( "timeOut is false");
					System.out.println( "closing socket");
					socket.close();


			//	} catch( SocketTimeoutException exception ){

			//		// If we don't get an ack, resend sequence number
			//		int nextSeq = sequenceNo.getAndDecrement();//For next sequence number	
			//		System.out.println( "Timeout, sequence number= " + sequenceNo.get());
			//		counter = counter-mss;//since the packet was lost, again read the same mss bytes
			//	}	

				System.out.println("Thread " +  threadName + " exiting.");
			}

			public void start () {
				System.out.println("Starting " +  threadName );
				if (t == null) {
					t = new Thread (this, threadName);
					try{
						t.start ();
					}catch(SocketTimeoutException exception){
						// If we don't get an ack, resend sequence number
						int nextSeq = sequenceNo.getAndDecrement();//For next sequence number	
						System.out.println( "Timeout, sequence number= " + sequenceNo.get());
						counter = counter-mss;//since the packet was lost, again read the same mss bytes

					}
				}
			}
	}//PacketTransfer

}//StopWaitSender
