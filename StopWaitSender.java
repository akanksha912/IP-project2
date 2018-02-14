import java.io.*; //file access
import java.net.*; //network access
import java.nio.*; //non-blocking I/O (intensive I/O operations)
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.Thread.*;




public class StopWaitSender{

	//Declare variables
//	private static int BUFFER = 1048576;//(Data MSS 1000000 bytes+ header 8 bytes)
	private static int BUFFER = 100;//(Data MSS 1000000 bytes+ header 8 bytes)
	//private static int BUFFER = 1008;//(Data MSS 1000000 bytes+ header 8 bytes)
	private static int PORT1 = 7735;
	private static int PORT2 = 7736;
//	private static String HOSTNAME = "localhost";
	public static int counter = 0;
	public static int pack = 3;
	public static int mss = 100; //TODO: Command line arg
	public static int totalSize = 2048; //TODO: Command line arg
	public final static String FILE_TO_SEND = "/home/tripti/abc.txt";
	public static byte [] newBuffer  = new byte [totalSize];//Buffer for writing MSS size of data read from file

	//Header elements of sendSegment
	static AtomicInteger sequenceNo = new AtomicInteger(0);//Sequence Number
	//public volatile AtomicInteger sequenceNo = new AtomicInteger(0);//Sequence Number
	private volatile int ack = 0;
	private int ctr = 0;
	public static boolean sendNext = false;
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
		byte[] sendSegment = new byte[108]; //byte array for sending data
		byte[] recvSegment = new byte[108]; //byte array for receiving data 
		boolean timeOut = true;
		pack = totalSize/mss;
		System.out.println( "Total number of packets to be sent to each server = "+pack);
		while(timeOut/* && pack != 0*/){
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
			System.out.println( "DEBUG: pack = "+pack);
			int len = bis.read(newBuffer);//read till the end of file 
			System.out.println(len+" bytes read");
			//bis.read(newBuffer,0+counter,mss);//MSS bytes copied 
			System.out.println("Sending " + mss + " bytes (MSS)");
			for(int i = 0; i <= mss-1; i++){
				sendSegment[8+i] = newBuffer[counter];
			//	int sum = 8 + i;
			//	System.out.println("copying newBuffer["+counter+"] into sendsegment["+sum+"]");
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

			int ackNo = 3-pack;
			StopWaitSender snd = new StopWaitSender();
			//Thread start for each of the servers
			PacketTransfer T1 = new PacketTransfer( "server-1", sendSegment, PORT1, snd, ackNo);

				T1.start();

			PacketTransfer T2 = new PacketTransfer( "server-2", sendSegment, PORT2, snd, ackNo);
			T2.start();
			//	timeOut = false;


			try {
				T1.join();
				T2.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			while (Thread.activeCount() > 1) {
			}
			System.out.println("All threads are dead, exiting main thread");
			pack--;
			//		while(sender.ack == 2)
			//			System.out.println( "DEBUG:ack value = "+ack);
			//		ack = 0;
			System.out.println( "snd.ack = "+snd.ack);
			if(snd.ack == 2 && pack != 0){
				System.out.println( "All ACKs received; Sending next packet ...");
				continue;
			}else{		
				System.out.println( "All packets sent!! :D");
				break;
			}
		}//while(timeOut == true), pack !=0
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
			private byte[] sendSegment = new byte[108]; //byte array for sending data
			private byte[] recvSegment = new byte[108]; //byte array for receiving data 

			//Read from file ip address
			//TODO Below code not required
			private String HOSTNAME = "localhost";




			//	public volatile AtomicInteger sequenceNo = new AtomicInteger(0);//Sequence Number
			StopWaitSender sender1;
			int ctrAck;
			boolean packetLoss = false;
			PacketTransfer( String name, byte[] sendSeg, int port/*, int bufsize*/, StopWaitSender obj, int Ctr) {
				threadName = name;
				PORT = port;
				sendSegment = sendSeg;
				//SIZE = bufsize;
				System.arraycopy(sendSeg, 0, sendSegment, 0, SIZE);
				System.out.println("Creating " +  threadName );
				sender1 = obj;
				ctrAck = Ctr;
			}

			public void run() {

				while(true){
					System.out.println("Running " +  threadName );
					//Constructs a datagram socket and binds it to any available port on the local host machine for UDP transport.
					DatagramSocket socket = null;
					try {
						socket = new DatagramSocket();
					} catch (SocketException e) {
					System.out.println( "DEBUG1" + sequenceNo.get());
						// TODO Auto-generated catch block
						e.printStackTrace();
					}		
					try {
						socket.setSoTimeout(1000);
					} catch (SocketException e) {


						System.out.println( "DEBUG2" + sequenceNo.get());


						//		// If we don't get an ack, resend sequence number
						int nextSeq = sequenceNo.getAndDecrement();//For next sequence number	
						System.out.println( "Timeout, sequence number= " + sequenceNo.get());
						counter = counter-mss;//since the packet was lost, again read the same mss bytes
					} //Enable/disable SO_TIMEOUT(Set a timeout on blocking Socket operations) with the specified timeout, in milliseconds.
					InetAddress ipAddress = null;
					try {
						ipAddress = InetAddress.getByName(HOSTNAME);//To be removed
					} catch (UnknownHostException e) {
						e.printStackTrace();
					}//getByName- Determines the IP address of a host, given the host's name.

			//Read IP Address from file
			//TODO Below code not required
/*
					if (threadName == "server-1"){
						//read server-1.txt

						// The name of the file to open.
						String fileName = "server-1.txt";

						// This will reference one line at a time
						String line = null;

						try {
							// FileReader reads text files in the default encoding.
							FileReader fileReader = 
								new FileReader(fileName);

							// Always wrap FileReader in BufferedReader.
							BufferedReader bufferedReader = 
								new BufferedReader(fileReader);

							while((line = bufferedReader.readLine()) != null) {
								System.out.println(line);
							}   

							// Always close files.
							bufferedReader.close();         
						}
						catch(FileNotFoundException ex) {
							System.out.println(
									"Unable to open file '" + 
									fileName + "'");                
						}



					}	
					if (threadName == "server-2"){
						//read server-2.txt
					}	
					if (threadName == "server-3"){
						//read server-3.txt
					}	


*/
					//Send the UDP Packet to the server
					DatagramPacket packet = new DatagramPacket(sendSegment, sendSegment.length, ipAddress, PORT);
					try {
						socket.send( packet );
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					System.out.println( "Packet Sent!!!");

					// Receive the server's packet
					DatagramPacket rcvdPkt = new DatagramPacket(recvSegment, recvSegment.length);
					try {
						socket.receive( rcvdPkt );
							System.out.println( "Packet Received!!!");
							
					} catch (IOException e) {
						// TODO Auto-generated catch block
							System.out.println( "Packet Lost, send again!!!");
							packetLoss = true;	
						//e.printStackTrace();
					}

					// Get the message from the server's packet
					int returnMessage = ByteBuffer.wrap( rcvdPkt.getData( ) ).getInt();

					System.out.println( "ACK= " + returnMessage );
					//pack--;
					// If we receive an ack, stop the while loop
					//	timeOut = false;
					//	System.out.println( "timeOut is false");
					//System.out.println( "closing socket");
					//socket.close();


				//	System.out.println("DEBUG :Thread " +  threadName + ": ack = "+ack);
					synchronized(sender1){
						if(packetLoss == false){	
							sender1.ack++;
							System.out.println("DEBUG :Thread " +  threadName + ": ack = "+sender1.ack);
						}
					//	System.out.println("Thread " +  threadName + " exiting.");
						//break;
					}	
				//	//System.out.println("DEBUG: Thread " +  threadName + ": ack = "+ack);
				//	//if(ack!=2);//No. of servers = 2
				//	while(sender.ack!=2){
				//			//System.out.println("THREAD " +  threadName + " WITH ack = "+sender.ack);
				//		
				//	}
				//		
					if(packetLoss == false){
						if(sender1.ack == 2){
							System.out.println("Total acks received = "+sender1.ack);
							sendNext = true;
						}
						System.out.println("Thread " +  threadName + " exiting.");
						break;
					}else
					{
						packetLoss = false;
						System.out.println("THREAD " +  threadName + " sending packet again");
						continue;
					}
				}//while(true)
			}//run

			public void start () {
				System.out.println("Starting " +  threadName );
				if (t == null) {
					t = new Thread (this, threadName);
					t.start ();
				}
			}
	}//PacketTransfer

}//StopWaitSender
