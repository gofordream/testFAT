import java.util.Vector;

public class DirectoryFile {
	private Vector<DirectoryEntry> dirEnts=new Vector<DirectoryEntry>();//consist all of the 32-bytes entries,including free entries.
	private DirectoryEntry m_dirEnt;//this object is the entry that exist in the directory file
									//if value of this member is null,that means this file is a root directory file
	
	public DirectoryFile(DirectoryEntry dirEnt){
		m_dirEnt=dirEnt;
		if(dirEnt==null){
			//root directory
			byte[] bytes=new byte[FAT.RootDirSectors*FAT.BPB_BytsPerSec];	
			for(int i=0;i<FAT.RootDirSectors;i++){
				byte secBytes[]=FAT.readSector(FAT.FirstRootSector+i);
				System.arraycopy(secBytes, 0, bytes, i*FAT.BPB_BytsPerSec, FAT.BPB_BytsPerSec);				
			}
			int numEnts=bytes.length/32;
			for(int i=0;i<numEnts;i++){
				byte[] b=new byte[32];
				System.arraycopy(bytes, i*32, b, 0, 32);
				DirectoryEntry entry=new DirectoryEntry(b);
				dirEnts.add(entry);
			}
		}else{
			Vector<Byte> vec=new Vector<Byte>();
			int cluster=dirEnt.getFirstCluster();
			while(cluster>0){
				byte[] bytes=FAT.readCluster(cluster);
				for(int i=0;i<bytes.length;i++){
					vec.add(bytes[i]);
				}
				int next=FAT.getFATEntry(cluster);
				if(next>=0xfff7)
					break;
				cluster=next;
			}
			int numEnts=vec.size()/32;
			for(int i=0;i<numEnts;i++){
				byte[] b=new byte[32];
				for(int j=0;j<32;j++)
					b[j]=vec.get(i*32+j);
				DirectoryEntry entry=new DirectoryEntry(b);
				dirEnts.add(entry);
			}		
		}
	}	

	public DirectoryEntry findFile(String name){
		for(DirectoryEntry dirEnt:dirEnts){
			if(dirEnt.isFree()) continue;
			if(dirEnt.getName().equals(name)){
				return dirEnt;
			}
		}
		return null;
	}

	public Vector<DirectoryEntry> getFiles(){
		Vector<DirectoryEntry> files=new Vector<DirectoryEntry>();
		for(DirectoryEntry dirEnt:dirEnts){
			if(!dirEnt.isFree()){
				String name=dirEnt.getName();
				if(name.charAt(0)=='.') continue;
				files.add(dirEnt);
			}
		}
		return files;
	}
	public Vector<DirectoryEntry> getEntries(){
		Vector<DirectoryEntry> entries=new Vector<DirectoryEntry>();
		for(DirectoryEntry dirEnt:dirEnts){
			if(!dirEnt.isFree()){
				entries.add(dirEnt);
			}
		}
		return entries;
	}

	public boolean hasFile(String name){
		for(DirectoryEntry dirEnt:dirEnts){
			if(dirEnt.isFree()) continue;
			if(dirEnt.getName().equals(name))
				return true;
		}
		return false;
	}

	public DirectoryEntry getFreeEntry(){
		for(DirectoryEntry dirEnt:dirEnts){
			if(dirEnt.isFree()) return dirEnt;
		}
		return null;
	}

	public void updateEntry(DirectoryEntry dirEnt){		
		int i;
		for(i=0;i<dirEnts.size();i++){
			if(dirEnts.get(i)==dirEnt){				
				break;
			}
		}
		
		int sector=getEntrySector(i);
		int offset=(i*32)%FAT.BPB_BytsPerSec;
		FAT.write32Byte(sector, offset, dirEnt.toBytes());
	}
	public int getFirstCluster(){
		if(m_dirEnt==null) return 0;
		return m_dirEnt.getFirstCluster();
	}
	private int getEntrySector(int n){
		//return the sector that the ith(from 0) entry in
		if(m_dirEnt==null)
			return n*32/FAT.BPB_BytsPerSec+FAT.FirstRootSector;
		int count=n*32/FAT.BPB_BytsPerSec/FAT.BPB_SecPerClus;
		int cluster=getFirstCluster();
		while(count>0){
			cluster=FAT.getFATEntry(cluster);
			count--;
		}
		
		return ((cluster-2)*FAT.BPB_SecPerClus)+FAT.FirstDataSector;
		
	}
	public DirectoryEntry append(){
		
		if(m_dirEnt==null) return null;
		int cluster=m_dirEnt.getFirstCluster();
		if(cluster<2) return null;
		int next=FAT.getFATEntry(cluster);
		while(next<0xfff7){
			cluster=next;
			next=FAT.getFATEntry(cluster);
		}
		int newc=FAT.alloc();
		if(newc<=2) return null;
		FAT.clearCluster(newc);
		FAT.setFATEntry(cluster, newc);			
		
		byte[] bytes=FAT.readCluster(newc);
		int numEnts=bytes.length/32;
		DirectoryEntry entry=null;
		for(int i=0;i<numEnts;i++){
			byte[] b=new byte[32];
			System.arraycopy(bytes, i*32, b, 0, 32);
			DirectoryEntry e=new DirectoryEntry(b);
			dirEnts.add(e);
			if(entry==null) entry=e;
		}
		return entry;
	}
	
}