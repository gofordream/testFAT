public class Util{
	public static byte [] int2bytes(int n){
		byte [] bytes=new byte[4];
		bytes[0]=(byte)(n&0xff);
		bytes[1]=(byte)(n>>8&0xff);
		bytes[2]=(byte)(n>>16&0xff);
		bytes[3]=(byte)(n>>24&0xff);
		return bytes;
	}
	public static byte[] short2bytes(short n){
		byte[] bytes=new byte[2];
		bytes[0]=(byte)(n&0xff);
		bytes[1]=(byte)(n>>8&0xff);
		return bytes;
	}

	public static short bytes2short(byte[] bytes){
		int b0=Byte.toUnsignedInt(bytes[0]);
		int b1=Byte.toUnsignedInt(bytes[1]);
		int n=b0|b1<<8;
		return (short)n;
		
	}
	public static int bytes2int(byte[] bytes){
		int b0=Byte.toUnsignedInt(bytes[0]);
		int b1=Byte.toUnsignedInt(bytes[1]);
		int b2=Byte.toUnsignedInt(bytes[2]);
		int b3=Byte.toUnsignedInt(bytes[3]);
		int n=b0 |b1<<8 | b2<<16 | b3<<24;	
		return n;
	}
	public static String uppercase(String str){
		String newstr="";
		for(int i=0;i<str.length();i++){
			char c=str.charAt(i);
			if(c>='a' && c<='z'){
				c=(char) ('A'+(c-'a'));
			}
			newstr+=c;
		}
		return newstr;
	}
}