import java.util.Vector;

public class FATFile{
	private Vector<Byte> m_data=new Vector<Byte>();
	//private byte[] m_data;
	private DirectoryEntry m_dirEnt;	//this object is the entry that exist in the directory file
	public FATFile(DirectoryEntry dirEnt){
		m_dirEnt=dirEnt;
		int cluster=dirEnt.getFirstCluster();
		while(cluster>=2){
			byte[] bytes=FAT.readCluster(cluster);
			for(int i=0;i<bytes.length;i++){
				m_data.add(bytes[i]);
			}
			int entry=FAT.getFATEntry(cluster);
			if(entry>=0xfff7)
				break;
			cluster=entry;
		}
	}
	public DirectoryEntry getEntry(){
		return m_dirEnt;
	}
	private void realloc(){
		if(m_dirEnt.getFirstCluster()>=2){
			FAT.free(m_dirEnt.getFirstCluster());	
			m_dirEnt.setFirstCluster(0);
		}
		if(getFileSize()==0) return;
		int numCluster=(getFileSize()+FAT.BPB_BytsPerSec*FAT.BPB_SecPerClus-1)/(FAT.BPB_BytsPerSec*FAT.BPB_SecPerClus);
		int next=FAT.alloc();
		int cluster=next;
		m_dirEnt.setFirstCluster(cluster);
		for(int i=1;i<numCluster;i++){
			next=FAT.alloc();
			FAT.setFATEntry(cluster, next);			
			cluster=next;
		}
	}
	private void write(){
		if(m_data==null || m_data.size()==0) return;
		int cluster=m_dirEnt.getFirstCluster();
		int offset=0;
		while(cluster>=2 && cluster<0xfff7){
			byte[] data=new byte[FAT.BPB_BytsPerSec*FAT.BPB_SecPerClus];
			for(int i=0;i<FAT.BPB_BytsPerSec*FAT.BPB_SecPerClus && offset<m_data.size();i++,offset++){
				data[i]=m_data.get(offset);
			}
			FAT.writeCluster(cluster, data);			
			cluster=FAT.getFATEntry(cluster);
		}
	}
	private byte[] data2Bytes(){	
		int size=m_dirEnt.getFileSize();		
		byte[] b=new byte[size];
		for(int i=0;i<size;i++)
			b[i]=m_data.get(i);
		return b;
	}
	public byte[] getDataBytes(){
		byte[] b=data2Bytes();
		return b;
	}
	public void update(byte[] b){
		if(b==null) return;
		m_dirEnt.setFileSize(b.length);
		realloc();
		m_data.clear();
		for(int i=0;i<b.length;i++){
			m_data.add(b[i]);
		}		
		write();
	}
	
	public int getFirstCluster(){
		return m_dirEnt.getFirstCluster();
	}
	public int getFileSize(){
		return m_dirEnt.getFileSize();
	}
	
}