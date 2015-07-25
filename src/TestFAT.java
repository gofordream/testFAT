import java.util.Scanner;
import java.util.Vector;

public class TestFAT{	
	public static void main(String str[]){
		System.out.println("Hello!");		
		FAT fat=new FAT();
		Scanner input=new Scanner(System.in);
		DirectoryFile curDir=fat.readRoot();
		String path="/";
		while(input.hasNextLine()){
			String line=input.nextLine();			
			String[] arr=line.split(" ");
			String cmd=arr[0];
			if(cmd.equals("format")){
				if(arr.length!=1){
					System.out.println("command format was not right.");
					continue;
				}
				fat.format();
				curDir=fat.readRoot();
			}else if(cmd.equals("quit")){
				if(arr.length!=1){
					System.out.println("command format was not right.");
					continue;
				}
				input.close();
				System.out.println("Bye");				
				break;
			}else if(cmd.equals("ls")){
				if(arr.length!=1){
					System.out.println("command format was not right.");
					continue;
				}
				Vector<DirectoryEntry> fileEnts=curDir.getEntries();
				for(DirectoryEntry entry:fileEnts){
					System.out.println(entry.getName());
				}
				
			}else if(cmd.equals("cd")){
				if(arr.length!=2){
					System.out.println("command format was not right.");
					continue;
				}
				String name=Util.uppercase(arr[1]);
				DirectoryEntry dirEnt=curDir.findFile(name);
				if(dirEnt!=null){
					if(dirEnt.isDirectory()){
						if(dirEnt.getFirstCluster()==0)
							curDir=fat.readRoot();
						else
							curDir=fat.readDirectory(dirEnt);
					}else{
						System.out.println(name+" is not a directory.");
					}					
				}else
					System.out.println("cannot find the directory");
			}else if(cmd.equals("rm")){
				if(arr.length!=2){
					System.out.println("command format was not right.");
					continue;
				}
				String name=Util.uppercase(arr[1]);
				DirectoryEntry dirEnt=curDir.findFile(name);
				if(dirEnt==null){
					System.out.println("cannot find file "+name);
					continue;
				}
				fat.rm(dirEnt);
				curDir.updateEntry(dirEnt);
				
			}else if(cmd.equals("create")){
				if(arr.length!=2 && arr.length!=3){
					System.out.println("command format was not right.");
					continue;
				}
				String name=Util.uppercase(arr[1]);
				fat.createFile(curDir, name);
				if(arr.length==3){
					String content=arr[2];
					DirectoryEntry dirEnt=curDir.findFile(name);
					if(dirEnt==null || dirEnt.isDirectory()){
						System.out.println("whoops..");
						continue;
					}
					FATFile file=new FATFile(dirEnt);
					file.update(content.getBytes());
					curDir.updateEntry(dirEnt);	
				}
					
			}else if(cmd.equals("mkdir")){
				if(arr.length!=2){
					System.out.println("command format was not right.");
					continue;
				}
				String name=Util.uppercase(arr[1]);
				fat.mkdir(curDir, name);
			}else if(cmd.equals("read")){
				if(arr.length!=2){
					System.out.println("command format was not right.");
					continue;
				}
				String name=Util.uppercase(arr[1]);
				DirectoryEntry dirEnt=curDir.findFile(name);
				if(dirEnt==null){
					System.out.println("cannot find file "+name);
					continue;
				}
				if(dirEnt.isDirectory()){
					System.out.println("directory cannot be read.");
					continue;
				}
				FATFile file=fat.readFile(dirEnt);
				byte[] data=file.getDataBytes();
				String content=new String(data);
				System.out.println(content);				
				
			}else if(cmd.equals("write")){
				if(arr.length!=2 && arr.length!=3){
					System.out.println("command format was not right.");
					continue;
				}
				String name=Util.uppercase(arr[1]);
				String content="";
				if(arr.length>=3)
					content=arr[2];
				DirectoryEntry dirEnt=curDir.findFile(name);
				if(dirEnt==null){
					System.out.println("cannot find file "+name);
					continue;
				}
				if(dirEnt.isDirectory()){
					System.out.println("directory cannot be writed.");
					continue;
				}
				FATFile file=new FATFile(dirEnt);
				file.update(content.getBytes());
				curDir.updateEntry(dirEnt);			
				
			}else{
				System.out.println("cannot recognize the command "+cmd);
			}
			
		}
		
	}
	
	
}