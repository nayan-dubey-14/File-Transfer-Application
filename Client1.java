import java.net.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.util.*;
class FileUploadEvent
{
private String uploaderId;
private File file;
private long numberOfBytesUploaded;
public FileUploadEvent()
{
this.uploaderId=null;
this.file=null;
this.numberOfBytesUploaded=0;
}
public void setUploaderId(String uploaderId)
{
this.uploaderId=uploaderId;
}
public String getUploaderId()
{
return this.uploaderId;
}
public void setFile(File file)
{
this.file=file;
}
public File getFile()
{
return this.file;
}
public void setNumberOfBytesUploaded(long numberOfBytesUploaded)
{
this.numberOfBytesUploaded=numberOfBytesUploaded;
}
public long getNumberOfBytesUploaded()
{
return this.numberOfBytesUploaded;
}
}
interface FileUploadListener
{
public void fileUploadStatusChanged(FileUploadEvent fileUploadEvent);
}
class FileModel extends AbstractTableModel
{
private ArrayList<File> files;
FileModel()
{
this.files=new ArrayList<>();
}
public void clearAllTableData()
{
this.files.clear();
fireTableDataChanged();
}
public ArrayList<File> getFiles()
{
return files;
}
public int getRowCount()
{
return files.size();
}
public int getColumnCount()
{
return 2;
}
public String getColumnName(int index)
{
if(index==0) return "S.no.";
return "File";
}
public Class getColumnClass(int c)
{
if(c==0) return Integer.class;
return String.class;
}
public boolean isCellEditable(int r,int c)
{
return false;
}
public Object getValueAt(int r,int c)
{
if(c==0) return r+1;
return this.files.get(r).getAbsolutePath();
}
public void add(File file)
{
this.files.add(file);
fireTableDataChanged();
}
}
class FTClientFrame extends JFrame
{
private int size;
private String host;
private int portNumber;
private FileSelectionPanel fileSelectionPanel;
private FileUploadViewPanel fileUploadViewPanel;
private Container container;
FTClientFrame(String host,int portNumber)
{
this.size=0;
this.host=host;
this.portNumber=portNumber;
fileSelectionPanel=new FileSelectionPanel();
fileUploadViewPanel=new FileUploadViewPanel();
container=getContentPane();
container.setLayout(new GridLayout(1,2));
container.add(fileSelectionPanel);
container.add(fileUploadViewPanel);
setSize(1000,500);
setLocation(300,150);
setVisible(true);
setDefaultCloseOperation(EXIT_ON_CLOSE);
}
//1 inner class starts
class FileSelectionPanel extends JPanel implements ActionListener
{
private Set<File> set;
private JTable table;
private JButton addFileButton;
private FileModel fileModel;
private JLabel titleLabel;
private JScrollPane jsp;
public FileSelectionPanel()
{
set=new HashSet<>();
addFileButton=new JButton("Add File");
titleLabel=new JLabel("Selected Files");
fileModel=new FileModel();
table=new JTable(fileModel);
table.getColumnModel().getColumn(1).setPreferredWidth(350);
table.getTableHeader().setReorderingAllowed(false);
jsp=new JScrollPane(table,ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
setLayout(new BorderLayout());
add(titleLabel,BorderLayout.NORTH);
add(addFileButton,BorderLayout.SOUTH);
add(jsp,BorderLayout.CENTER);
addFileButton.addActionListener(this);
}
public ArrayList<File> getFiles()
{
return this.fileModel.getFiles();
}
public void clearAllTableData()
{
this.fileModel.clearAllTableData();
return;
}
public void setButtonDisabled()
{
this.addFileButton.setEnabled(false);
}
public void setButtonEnabled()
{
this.addFileButton.setEnabled(true);
}
public void clearSetRecords()
{
this.set.clear();
}
public void actionPerformed(ActionEvent ae)
{
JFileChooser jfc=new JFileChooser();
int s=jfc.showOpenDialog(this);
if(s==jfc.APPROVE_OPTION)
{
File file=jfc.getSelectedFile();
if(set.contains(file)==true)
{
JOptionPane.showMessageDialog(FTClientFrame.this,file.getAbsolutePath()+" is already selected");
return;
}
fileModel.add(file);
set.add(file);
}
}
}
//1inner class ends here
//2 inner class starts 
class FileUploadViewPanel extends JPanel implements ActionListener,FileUploadListener
{
private JButton uploadFilesButton;
private JPanel progressPanelsContainer;
private JScrollPane jsp;
private ArrayList<ProgressPanel> progressPanels;
ArrayList<File> files;
ArrayList<FileUploadThread> fileUploaders;
FileUploadViewPanel()
{
uploadFilesButton=new JButton("Upload");
setLayout(new BorderLayout());
add(uploadFilesButton,BorderLayout.NORTH);
uploadFilesButton.addActionListener(this);
}
public void actionPerformed(ActionEvent ae)
{
files=fileSelectionPanel.getFiles();
if(files.size()==0)
{
JOptionPane.showMessageDialog(FTClientFrame.this,"No files selected to upload");
return;
}
this.uploadFilesButton.setEnabled(false);
FTClientFrame.this.fileSelectionPanel.setButtonDisabled();
progressPanelsContainer=new JPanel();
progressPanelsContainer.setLayout(new GridLayout(files.size(),1));
progressPanels=new ArrayList<>();
fileUploaders=new ArrayList<>();
FileUploadThread fut;
String uploaderId;
ProgressPanel pp;
for(File file:files)
{
uploaderId=UUID.randomUUID().toString();
pp=new ProgressPanel(file,uploaderId);
progressPanels.add(pp);
progressPanelsContainer.add(pp);
fut=new FileUploadThread(this,host,portNumber,uploaderId,file);
fileUploaders.add(fut);
}
jsp=new JScrollPane(progressPanelsContainer,ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
add(jsp,BorderLayout.CENTER);
this.revalidate();
this.repaint();
for(FileUploadThread fileUploadThread:fileUploaders)
{
fileUploadThread.start();
}
}
public void fileUploadStatusChanged(FileUploadEvent fileUploadEvent)
{
String uploaderId=fileUploadEvent.getUploaderId();
long numberOfBytesUploaded=fileUploadEvent.getNumberOfBytesUploaded();
File file=fileUploadEvent.getFile();
for(ProgressPanel progressPanel:progressPanels)
{
if(progressPanel.getId()==uploaderId)
{
progressPanel.updateProgressBar	(numberOfBytesUploaded);
break;
}
}
}
class ProgressPanel extends JPanel
{
private File file;
private JLabel fileNameLabel;
private JProgressBar progressBar;
private long length;
private String id;
public ProgressPanel(File file,String id)
{
this.file=file;
this.id=id;
this.length=file.length();
fileNameLabel=new JLabel("Uploading : "+file.getAbsolutePath());
progressBar=new JProgressBar(1,100);
progressBar.setStringPainted(true);
setLayout(new GridLayout(2,1));
add(fileNameLabel);
add(progressBar);
}
public String getId()
{
return this.id;
}
public void updateProgressBar(long bytesUploaded)
{
int percentage;
if(bytesUploaded==length) percentage=100;
percentage=(int)((bytesUploaded*100)/length);
progressBar.setValue(percentage);
if(percentage==100)
{
fileNameLabel.setText("Uploaded : "+file.getAbsolutePath());
size++;
}
if(size==progressPanels.size())
{
size=0;
JOptionPane.showMessageDialog(FTClientFrame.this,"All files are uploaded");
fileSelectionPanel.clearAllTableData();
progressPanelsContainer.removeAll();
fileSelectionPanel.clearSetRecords();
uploadFilesButton.setEnabled(true);
fileSelectionPanel.setButtonEnabled();
FileUploadViewPanel.this.revalidate();
FileUploadViewPanel.this.repaint();
return;
}
}
}//ProgressPanel class ends here
}
//2 inner class ends here
public static void main(String gg[])
{
FTClientFrame ftc=new FTClientFrame("localhost",6060);
}
}
class FileUploadThread extends Thread
{
private FileUploadListener fileUploadListener;
private String id;
private File file;
private String host;
private int portNumber;
public FileUploadThread(FileUploadListener fileUploadListener,String host,int portNumber,String id,File file)
{
this.fileUploadListener=fileUploadListener;
this.id=id;
this.file=file;
this.portNumber=portNumber;
this.host=host;
}
public void run()
{
try
{
//taking input
long fileLength=file.length();
String fileName=file.getName();
//create header and set length of objectArray in it
byte header[]=new byte[1024];
long x=fileLength;
int j=1023;
while(x>0)
{
header[j]=(byte)(x%10);
x=x/10;
j--;
}
header[j--]=(byte)'#';
for(int i=(int)(fileName.length())-1;i>=0;i--)
{
header[j]=(byte)fileName.charAt(i);
j--;
}
header[j]=(byte)'#';

//sent header to server and then receive acknowledgment
Socket socket=new Socket(host,portNumber);
OutputStream os=socket.getOutputStream();
os.write(header,0,1024);
os.flush();
InputStream is=socket.getInputStream();
byte ack[]=new byte[1];
int bytesReadCount;
while(true)
{
bytesReadCount=is.read(ack);
if(bytesReadCount==-1) continue;
break;
}

//now sent file(byte array) in chunks of 1024
FileInputStream fileInputStream=new FileInputStream(file);
byte objectArray[]=new byte[1024];
long bytesToSend=fileLength;
long chunkSize=1024;
j=0;
while(j<bytesToSend)
{
if(bytesToSend-j<chunkSize) chunkSize=bytesToSend-j;
long fileReaded=fileInputStream.read(objectArray,0,(int)chunkSize);
os.write(objectArray,0,(int)chunkSize);
os.flush();
j+=chunkSize;
long tmp=j;
SwingUtilities.invokeLater(()->{
FileUploadEvent fue=new FileUploadEvent();
fue.setUploaderId(id);
fue.setFile(file);
fue.setNumberOfBytesUploaded(tmp);
fileUploadListener.fileUploadStatusChanged(fue);
});
}
fileInputStream.close();
byte tmp[]=new byte[1];
while(true)
{
bytesReadCount=is.read(tmp);
if(bytesReadCount==-1) continue;
break;
}
socket.close();
}catch(Exception e)
{
System.out.println(e.getMessage());
}
}
}