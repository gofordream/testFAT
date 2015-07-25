public class DirectoryEntry{
	public byte name[]=new byte[11];
	private byte attr;
	private byte reserved[]=new byte[10];
	private byte wrtTime[]=new byte[2];
	private byte wrtDate[]=new byte[2];
	private byte fstClus[]=new byte[2];
	private byte size[]=new byte[4];
	public static final int ATTR_DIRECTORY=0x10;

	public DirectoryEntry(byte[] bytes){
		set(bytes);
	}	
	public String getName(){
		String str="";
		
		for(int i=0;i<8;i++){
			if(name[i]==0x20) break;
			str+=(char)name[i];
		}
		if(name[8]!=0x20){
			str+=".";
			str+=(char)name[8];
			if(name[9]!=0x20){
				str+=(char)name[9];
				if(name[10]!=0x20)
					str+=(char)name[10];
			}
		}		
		return str;
	}
	public Boolean isDirectory(){
		if((attr&ATTR_DIRECTORY)!=0){
			return true;
		}
		return false;
	}

	public int getFirstCluster(){
		return Short.toUnsignedInt(Util.bytes2short(fstClus));
	}
	public void setFirstCluster(int dataSector){
		byte[] bytes=Util.short2bytes((short)dataSector);
		fstClus[0]=bytes[0];
		fstClus[1]=bytes[1];
	}	
	public int getFileSize(){
		return Util.bytes2int(size);
	}
	public void setFileSize(int n){
		byte[] bytes=Util.int2bytes(n);
		size[0]=bytes[0];
		size[1]=bytes[1];
		size[2]=bytes[2];
		size[3]=bytes[3];
	}
	public Boolean isFree(){
		if(name[0]==(byte)0xe5 || name[0]==0){
			return true;
		}
		return false;
	}
	public void setFree(){
		name[0]=(byte)0xe5;
	}
	
	public void setIsDirectory(boolean isDirectory){
		if(isDirectory){
			attr|=ATTR_DIRECTORY;
		}else{
			attr&=(~ATTR_DIRECTORY);
		}
	}
	public static boolean isValidName(String str){
		int dot=str.indexOf('.');		
		if(dot<0){
			if(str.length()>8) return false;
		}else{
			if(dot>8) return false;
			if(str.length()-1-dot>3) return false;
		}
		return true;
			
	}
	public void setSpecialName(String str){
		if(str.equals(".")){
			for(int i=1;i<11;i++)
				name[i]=0x20;
			name[0]='.';
		}else if(str.equals("..")){
			for(int i=2;i<11;i++)
				name[i]=0x20;
			name[0]='.';
			name[1]='.';
		}
	}
	public void setName(String str){		
		if(!isValidName(str)) return;
		int dot=str.indexOf('.');
		for(int i=0;i<11;i++)
			name[i]=0x20;
		if(dot<0){
			for(int i=0;i<str.length();i++)
				name[i]=(byte)str.charAt(i);
		}else{
			for(int i=0;i<dot;i++)
				name[i]=(byte)str.charAt(i);
			for(int i=dot+1,j=0;i<str.length();i++,j++)
				name[8+j]=(byte)str.charAt(i);
		}
	}
	
	public void set(DirectoryEntry dirEnt){
		byte[] bytes=dirEnt.toBytes();
		set(bytes);
	}
	private void set(byte[] bytes){
		int i;
		for(i=0;i<11;i++){
			name[i]=bytes[i];
		}
		attr=bytes[i++];
		for(int j=0;j<10;j++,i++){
			reserved[j]=bytes[i];
		}
		wrtTime[0]=bytes[i++];
		wrtTime[1]=bytes[i++];
		
		wrtDate[0]=bytes[i++];
		wrtDate[1]=bytes[i++];
		
		fstClus[0]=bytes[i++];
		fstClus[1]=bytes[i++];
		
		size[0]=bytes[i++];
		size[1]=bytes[i++];
		size[2]=bytes[i++];
		size[3]=bytes[i++];
	}
	public byte[] toBytes(){
		byte[] bytes=new byte[32];
		int i;
		for(i=0;i<11;i++){
			bytes[i]=name[i];
		}
		bytes[i++]=attr;
		for(int j=0;j<10;j++,i++){
			bytes[i]=reserved[j];
		}
		bytes[i++]=wrtTime[0];
		bytes[i++]=wrtTime[1];
		
		bytes[i++]=wrtDate[0];
		bytes[i++]=wrtDate[1];
		
		bytes[i++]=fstClus[0];
		bytes[i++]=fstClus[1];
		
		bytes[i++]=size[0];
		bytes[i++]=size[1];
		bytes[i++]=size[2];
		bytes[i++]=size[3];
		return bytes;
	}		
	
}