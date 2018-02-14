import java.util.concurrent.atomic.AtomicInteger;

public class Header
{

	//Declare variables

	//Packets
	byte[] sendSegment = null; //data+header packet sent (8-bit signed 2's complement integer)
	byte[] recvSegment = null;//ACK (only header)

	//Header elements of sendSegment
	static AtomicInteger sequenceNo = new AtomicInteger(21);//Sequence Number
	int cksum;
	 
	
	public static void main(String[] args)
	{
		Header header = new Header();


			header.createPacket();
	//	try {
	//		header.createPacket();

	//	} catch (Exception ex) {
	//		System.err.println(ex.getMessage());
	//	}

	}//main


	private void createPacket(){	
		sendSegment = new byte[8];//8 bytes of header

		
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

	
		//System.out.println("s1: " + Integer.toBinaryString(sendSegment[0]));
		System.out.println("Sequence Number: " + intToString(sendSegment[0]) + intToString(sendSegment[1]) + intToString(sendSegment[2]) + intToString(sendSegment[3]));
		System.out.println("Checksum: " + intToString(sendSegment[4]) + intToString(sendSegment[5]));
		System.out.println("Data Indication: " + intToString(sendSegment[6]) + intToString(sendSegment[7]));
	//	System.out.println("s2: " + intToString(sendSegment[1]));
	//	System.out.println("s2: " + intToString(sendSegment[2]));
	//	System.out.println("s2: " + intToString(sendSegment[3]));
		//System.out.println("s2: " + sendSegment[0]);
		int nextSequence = sequenceNo.getAndIncrement();//For next sequence number	
		System.out.println("next sequence number: " + intToString(sequenceNo.get()));
		//System.out.println("s1: " + intToString(sendSegment[0], 4));
		//System.out.println("s1: " + toBinary(sendSegment[0]));
	}//createPacket

	public static String intToString(int number) {
	//public static String intToString(int number, int groupSize) {
		StringBuilder result = new StringBuilder();

		//for(int i = 31; i >= 0 ; i--) {
		for(int i = 7; i >= 0 ; i--) {
			int mask = 1 << i;
			result.append((number & mask) != 0 ? "1" : "0");

			//if (i % groupSize == 0)
			//	result.append(" ");
		}
	//	result.replace(result.length() - 1, result.length(), "");

		return result.toString();
	}	
	//	public static String toBinary(int base10Num){
	//		boolean isNeg = base10Num < 0;
	//		base10Num = Math.abs(base10Num);        
	//		String result = "";
	//
	//		while(base10Num > 1){
	//			result = (base10Num % 2) + result;
	//			base10Num /= 2;
	//		}
//		assert base10Num == 0 || base10Num == 1 : "value is not <= 1: " + base10Num;
//
//		result = base10Num + result;
//		assert all0sAnd1s(result);
//
//		if( isNeg )
//			result = "-" + result;
//		return result;
//	}//toBinary
//
//	public static boolean all0sAnd1s(String val){
//        assert val != null : "Failed precondition all0sAnd1s. parameter cannot be null";
//        boolean all = true;
//        int i = 0;
//        char c;
//        
//        while(all && i < val.length()){
//            c = val.charAt(i);
//            all = c == '0' || c == '1';
//            i++;
//        }
//        return all;
//    }//all0sAnd1s
//
}
