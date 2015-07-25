import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Vector;

public class FAT{
	
	public static final int BPB_RsvdSecCnt=1;
	public static final int BPB_RootEntCnt=512;//root directory can have at most 512 files
	public static final int BPB_BytsPerSec=512;
	public static final int BPB_SecPerClus=1;	
	public static final int RootDirSectors=(BPB_RootEntCnt*32+BPB_BytsPerSec-1)/BPB_BytsPerSec;
	public static final int BPB_NumFATs=2;
	public static final int BPB_FATSz16=20;//can at most support 254*512/2=65024 clusters
	public static final int BPB_FATSz32=0;
	public static final int FATSz=BPB_FATSz16;//count of sectors occupied by one FAT
	public static final int BPB_Media=0xf0;
	public static final int BPB_SecPerTrk=32;
	public static final int BPB_NumHeads=2;
	public static final int BPB_TotSec16=BPB_NumHeads*BPB_SecPerTrk*80;
	public static final int BPB_TotSec32=0;
	public static final int TotSec=BPB_TotSec16;
	public static final int DataSec=TotSec-(BPB_RsvdSecCnt+RootDirSectors+(BPB_NumFATs*FATSz));	
	public static final int FirstDataSector=BPB_RsvdSecCnt+(BPB_NumFATs*FATSz)+RootDirSectors;
	public static final int CountofClusters=DataSec/BPB_SecPerClus;		
	public static final int FirstRootSector=BPB_RsvdSecCnt+FATSz*BPB_NumFATs;
	public static final int NumFATEnts=CountofClusters+2;
	
	
	private static short fat[]=new short[NumFATEnts];
	private static RandomAccessFile raf;
	
	public FAT(){
		readFAT();
	}
	
	public static int FirstSectorofCluster(int N){
		//N is clusterNumber
		return ((N-2)*BPB_SecPerClus)+FirstDataSector;		
	}
	public static int ThisFATSecNum(int N){
		int FATOffset=N*2;
		return BPB_RsvdSecCnt+(FATOffset/BPB_BytsPerSec);
	}
	public static int ThisFATEntOffset(int N){
		int FATOffset=N*2;
		return FATOffset%BPB_BytsPerSec;
	}
	public void readFAT(){
		try {
			//raf=new RandomAccessFile("c:\\mydisk.img","rw");
			raf=new RandomAccessFile("mydisk.img","rw");
			raf.seek(BPB_RsvdSecCnt*BPB_BytsPerSec);
			for(int i=0;i<NumFATEnts;i++){
				byte[] b=new byte[2];
				raf.read(b, 0, 2);
				fat[i]=Util.bytes2short(b);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void format(){
		try {
			raf.seek(0);
			raf.write(getBootSectorBytes());
			for(int i=1;i<TotSec;i++){
				for(int j=0;j<BPB_BytsPerSec;j++)
					raf.writeByte(0);
			}
			raf.seek(BPB_BytsPerSec*BPB_SecPerClus);
			raf.writeByte(BPB_Media);
			raf.writeByte(0xff);
			raf.writeByte(0xff);
			raf.writeByte(0xff);
			raf.seek(BPB_BytsPerSec*BPB_SecPerClus+BPB_BytsPerSec*FATSz);
			raf.writeByte(BPB_Media);
			raf.writeByte(0xff);
			raf.writeByte(0xff);
			raf.writeByte(0xff);
			System.out.println("success.");
			readFAT();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static int alloc(){
		// return the cluster number which is free
		for(int i=2;i<NumFATEnts;i++){
			if(fat[i]==0){
				int sector=ThisFATSecNum(i);
				int offset=ThisFATEntOffset(i);
				try {
					raf.seek(sector*BPB_BytsPerSec+offset);
					raf.writeByte(0xff);
					raf.writeByte(0xff);
				} catch (IOException e) {
					e.printStackTrace();
				}
				fat[i]=(short)0xffff;
				return i;
			}
		}
		return 0;		
	}
	public static void free(int n){
		if(n<2) return;
		while(true){
			int sector=ThisFATSecNum(n);
			int offset=ThisFATEntOffset(n);
			try {
				raf.seek(sector*BPB_BytsPerSec+offset);
				raf.writeByte(0);
				raf.writeByte(0);
			} catch (IOException e) {
				e.printStackTrace();
			}
			int next=Short.toUnsignedInt(fat[n]);
			fat[n]=0;
			n=next;
			if(next>=0xfff8)
				break;
		}
	}
	public static int getFATEntry(int n){
		return Short.toUnsignedInt(fat[n]);
	}
	public static void setFATEntry(int n,int value){
		int sector=FAT.ThisFATSecNum(n);
		int offset=FAT.ThisFATEntOffset(n);
		fat[n]=(short)value;
		try {
			raf.seek(sector*FAT.BPB_BytsPerSec+offset);
			byte[] b=Util.short2bytes((short)value);
			raf.writeByte(b[0]);
			raf.writeByte(b[1]);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static int cluster2Sector(int cluster){
		return FirstDataSector+(cluster-2)*BPB_SecPerClus;
	}
	public static int sector2Cluster(int sector){
		return (sector-FirstDataSector)/BPB_SecPerClus+2;
	}
	public void rm(DirectoryEntry dirEnt){
		int cluster=dirEnt.getFirstCluster();
		if(dirEnt.isDirectory()){
			DirectoryFile dirFile=readDirectory(dirEnt);
			Vector<DirectoryEntry> files=dirFile.getFiles();
			for(DirectoryEntry file:files){
				rm(file);
			}		
		}
		dirEnt.setFree();
		free(cluster);
	}
	public static void clearCluster(int cluster){
		try {
			int sector=(cluster-2)*BPB_SecPerClus+FirstDataSector;
			raf.seek(sector*BPB_BytsPerSec);
			for(int i=0;i<BPB_SecPerClus*BPB_BytsPerSec;i++)
				raf.writeByte(0);			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void mkdir(DirectoryFile dirFile,String name){
		if(!DirectoryEntry.isValidName(name)){
			System.out.println("The directory name is invalid.");
			return;
		}			
		if(dirFile.hasFile(name)){
			System.out.println("the directory "+name+" is already exist.");
			return;
		}
		DirectoryEntry freeEntry=dirFile.getFreeEntry();		
		if(freeEntry!=null){
			int cluster=alloc();
			clearCluster(cluster);
			freeEntry.setFirstCluster(cluster);	
			freeEntry.setName(name);
			freeEntry.setIsDirectory(true);
			freeEntry.setFileSize(0);//for directory file, attribute file_size is never used, file size is determined by FAT
			DirectoryFile file=new DirectoryFile(freeEntry);			
			
			DirectoryEntry dot=file.getFreeEntry();			
			dot.setSpecialName(".");
			dot.setFirstCluster(cluster);
			dot.setIsDirectory(true);
			dot.setFileSize(0);
			DirectoryEntry dotdot=file.getFreeEntry();
			dotdot.setSpecialName("..");
			dotdot.setFirstCluster(dirFile.getFirstCluster());			
			dotdot.setIsDirectory(true);
			dotdot.setFileSize(0);
			
			file.updateEntry(dot);
			file.updateEntry(dotdot);			
			
			dirFile.updateEntry(freeEntry);
		}else{
			freeEntry=dirFile.append();
			if(freeEntry==null){
				System.out.println("cannot create file.");
			}else{				
				int cluster=alloc();
				clearCluster(cluster);
				freeEntry.setFirstCluster(cluster);	
				freeEntry.setName(name);
				freeEntry.setIsDirectory(true);
				freeEntry.setFileSize(0);
				DirectoryFile file=new DirectoryFile(freeEntry);			
				
				DirectoryEntry dot=file.getFreeEntry();			
				dot.setSpecialName(".");
				dot.setFirstCluster(cluster);
				dot.setIsDirectory(true);
				dot.setFileSize(0);
				DirectoryEntry dotdot=file.getFreeEntry();
				dotdot.setSpecialName("..");
				dotdot.setFirstCluster(dirFile.getFirstCluster());			
				dotdot.setIsDirectory(true);
				dotdot.setFileSize(0);
				
				file.updateEntry(dot);
				file.updateEntry(dotdot);			
				
				dirFile.updateEntry(freeEntry);
			}
		}
	}
	public void createFile(DirectoryFile dirFile,String name){
		if(!DirectoryEntry.isValidName(name)){
			System.out.println("The file name is invalid.");
			return;
		}			
		if(dirFile.hasFile(name)){
			System.out.println("the file "+name+" is already exist.");
			return;
		}
		DirectoryEntry freeEntry=dirFile.getFreeEntry();		
		if(freeEntry!=null){
			freeEntry.setName(name);
			freeEntry.setIsDirectory(false);
			freeEntry.setFirstCluster(0);
			freeEntry.setFileSize(0);
			dirFile.updateEntry(freeEntry);
		}else{
			freeEntry=dirFile.append();
			if(freeEntry==null){
				System.out.println("cannot create file.");
			}else{
				freeEntry.setName(name);
				freeEntry.setIsDirectory(false);
				freeEntry.setFirstCluster(0);
				freeEntry.setFileSize(0);
				dirFile.updateEntry(freeEntry);
			}
		}
	}
	
	public static void write32Byte(int sector,int offset,byte[] data){
		if(data.length!=32) return;
		
		try {
			//raf.write
			raf.seek(sector*FAT.BPB_BytsPerSec+offset);
			raf.write(data);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void writeSector(int sector,byte[] data){
		if(data.length>BPB_BytsPerSec) return;
		try {
			raf.seek(BPB_BytsPerSec*sector);
			raf.write(data);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void writeCluster(int cluster,byte[] data){
		if(data.length!=BPB_BytsPerSec*BPB_SecPerClus) return;
		int sector=(cluster-2)*BPB_SecPerClus+FirstDataSector;		
		for(int i=0;i<BPB_SecPerClus;i++){
			byte[] b=new byte[BPB_BytsPerSec];
			System.arraycopy(data, i*BPB_BytsPerSec, b, 0, BPB_BytsPerSec);
			writeSector(sector+i,b);
		}
	}
	public static byte[] readSector(int sector){
		byte bytes[]=new byte[BPB_BytsPerSec];
		try {
			raf.seek(BPB_BytsPerSec*sector);
			raf.read(bytes);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bytes;
	}

	public static byte[] readCluster(int cluster){
		byte[] bytes=new byte[BPB_BytsPerSec*BPB_SecPerClus];
		int sector=(cluster-2)*BPB_SecPerClus+FirstDataSector;
		for(int i=0;i<BPB_SecPerClus;i++){
			byte[] b=readSector(sector+i);
			System.arraycopy(b, 0, bytes, BPB_BytsPerSec*i, BPB_BytsPerSec);			
		}
		return bytes;
	}

	public FATFile readFile(DirectoryEntry dirEnt){
		FATFile file=new FATFile(dirEnt);
		return file;
	}
	public DirectoryFile readDirectory(DirectoryEntry dirEnt){
		DirectoryFile dirFile=new DirectoryFile(dirEnt);
		return dirFile;		
	}
	public DirectoryFile readRoot(){
		DirectoryFile dirFile=new DirectoryFile(null);
		return dirFile;
	}
	
	public static byte[] getBootSectorBytes(){
		byte[] bytes=new byte[BPB_BytsPerSec];
		for(int i=0;i<BPB_BytsPerSec;i++)
			bytes[i]=0;
		bytes[0]=(byte)0xeb;
		bytes[1]=0;
		bytes[2]=0;
		bytes[3]='M';
		bytes[4]='S';
		bytes[5]='W';
		bytes[6]='I';
		bytes[7]='N';
		bytes[8]='4';
		bytes[9]='.';
		bytes[10]='1';
		byte[] temp=Util.short2bytes((short)BPB_BytsPerSec);
		bytes[11]=temp[0];
		bytes[12]=temp[1];
		bytes[13]=BPB_SecPerClus;
		temp=Util.short2bytes((short)BPB_RsvdSecCnt);
		bytes[14]=temp[0];
		bytes[15]=temp[1];
		bytes[16]=BPB_NumFATs;
		temp=Util.short2bytes((short)BPB_RootEntCnt);
		bytes[17]=temp[0];
		bytes[18]=temp[1];
		temp=Util.short2bytes((short)BPB_TotSec16);
		bytes[19]=temp[0];
		bytes[20]=temp[1];
		bytes[21]=(byte)BPB_Media;
		temp=Util.short2bytes((short)BPB_FATSz16);
		bytes[22]=temp[0];
		bytes[23]=temp[1];
		
		temp=Util.short2bytes((short)BPB_SecPerTrk);
		bytes[24]=temp[0];
		bytes[25]=temp[1];
		
		temp=Util.short2bytes((short)BPB_NumHeads);
		bytes[26]=temp[0];
		bytes[27]=temp[1];		
		
		temp=Util.int2bytes(BPB_TotSec32);
		bytes[32]=temp[0];
		bytes[33]=temp[1];
		bytes[34]=temp[2];
		bytes[35]=temp[3];
		
		bytes[510]=0x55;
		bytes[511]=(byte)0xaa;
		return bytes;
	}
	
}