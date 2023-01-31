import java.net.*;
import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
class RequestProcessor extends Thread
{
private Socket socket;
private Display display;
public RequestProcessor(Socket socket,Display display)
{
this.socket=socket;
this.display=display;
start();
}
public void run()
{
try
{
//now receive header and take out the length of upcoming request
int bytesToReceive=1024;
int bytesReadCount;
byte ack[]=new byte[1];
byte tmp[]=new byte[1024];
byte header[]=new byte[1024];
int k,i,j;
i=j=0;
InputStream is=socket.getInputStream();
OutputStream os=socket.getOutputStream();

while(j<bytesToReceive)
{
bytesReadCount=is.read(tmp);
if(bytesReadCount==-1) continue;
for(k=0;k<bytesReadCount;k++)
{
header[i]=tmp[k];
i++;
}
j+=bytesReadCount;
}
int requestLength=0;
i=1;
j=1023;
while(header[j]!='#')
{
requestLength=requestLength+(header[j]*i);
i=i*10;
j--;
}
this.display.updateText("Upcoming file length : "+((float)(requestLength)/(1024*1024))+" MB");
int indexOfHash=j-1;
StringBuffer fileNameBuffer=new StringBuffer();

while(header[indexOfHash]!='#')
{
indexOfHash--;
}
indexOfHash++;
for(;indexOfHash<j;indexOfHash++)
{
fileNameBuffer.append((char)header[indexOfHash]);
}
String fileName=fileNameBuffer.substring(0);
this.display.updateText("File name : "+fileName);
ack[0]=1;
os.write(ack,0,1);
os.flush();

File file=new File("uploads"+File.separator+fileName);
if(file.exists()==true) 
{
file.delete();
}
//now receive the request 
FileOutputStream fileOutputStream=new FileOutputStream(file);
byte request[]=new byte[requestLength];
bytesToReceive=requestLength;
i=j=0;
while(j<bytesToReceive)
{
bytesReadCount=is.read(tmp);
if(bytesReadCount==-1) continue;
fileOutputStream.write(tmp,0,bytesReadCount);
j+=bytesReadCount;
}
fileOutputStream.close();
this.display.updateText("File received successfully");
ack[0]=1;
os.write(ack,0,1);
os.flush();
socket.close();
}catch(Exception e)
{
this.display.updateText(e.getMessage());
}
}
}
class Server1
{
private ServerSocket serverSocket;
private Socket socket;
private Display display;
private int portNumber;
private RequestProcessor rp;
public Server1()
{
}
public void start(int portNumber,Display display)
{
try
{
this.display=display;
this.portNumber=portNumber;
serverSocket=new ServerSocket(portNumber);
startListening();
}catch(IOException ioException)
{
this.display.updateText(ioException.getMessage());
}
}
private void startListening()
{
while(true)
{
try
{
this.display.updateText("Server is ready to accept request at PORT"+portNumber+"\n");
socket=serverSocket.accept();
rp=new RequestProcessor(socket,this.display);
}catch(Exception exception)
{
this.display.updateText(exception.getMessage());
}
}
}
public void close()
{
try
{
serverSocket.close();
}catch(Exception exception)
{
this.display.updateText(exception.getMessage());
}
}
}
class Display extends JFrame
{
private JButton b1,cancelButton;
private JTextArea ta1,ta2;
private Container container;
private JScrollPane scrollPane;
public int portNumber;
private Server1 server;
private Thread t;
public Display()
{
super("Server");
b1=new JButton("Start");
cancelButton=new JButton("X");
ta1=new JTextArea("Enter port Number :");
ta2=new JTextArea("");
scrollPane=new JScrollPane(ta2,ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
server=new Server1();
setPositions();
addListeners();
}
private void setPositions()
{
int lm=0;
int tp=0;

ta2.setEditable(false);

ta1.setBounds(lm+80,tp+15,160,20);
cancelButton.setBounds(lm+80+160+5,tp+12,50,25);
b1.setBounds(lm+80+160+10+220,tp+10,70,50);
scrollPane.setBounds(lm+50,tp+65,440,300);


container=getContentPane();
container.setLayout(null);
container.add(ta1);
container.add(cancelButton);
container.add(b1);
container.add(scrollPane);
ta2.setForeground(new Color(0,100,0));
ta2.setFont(new Font("Verdana",Font.BOLD,15));
int w=700;
int h=500;
Dimension d=Toolkit.getDefaultToolkit().getScreenSize();
setLocation(600,300);
setSize(w-138,h-38);
setLocation(100,100);
setVisible(true);
setDefaultCloseOperation(EXIT_ON_CLOSE);
}
private void addListeners()
{
cancelButton.addActionListener(new ActionListener(){
public void actionPerformed(ActionEvent ae)
{
ta1.setText("");
ta1.requestFocusInWindow();
}
});
b1.addActionListener(new ActionListener(){
public void actionPerformed(ActionEvent ae)
{
if(b1.getText().equalsIgnoreCase("Start"))
{
String tmp=ta1.getText().trim();
if(tmp.length()==0) 
{
JOptionPane.showMessageDialog(Display.this,"Enter valid port number");
return;
}
Display.this.portNumber=Integer.parseInt(tmp);
b1.setText("Stop");
cancelButton.setEnabled(false);
ta1.setEnabled(false);
t=new Thread(new Runnable(){
public void run()
{
server.start(portNumber,Display.this);
}
});
t.start();
}
else if(b1.getText().equalsIgnoreCase("Stop"))
{
b1.setText("Start");
cancelButton.setEnabled(true);
ta1.setEnabled(true);
ta1.setText("Enter port number :");
ta2.setText("");
portNumber=0;
//may be wrong
server.close();
t.stop();
}
}
});
}
public void updateText(String str)
{
ta2.append(str+"\n");
ta2.setCaretPosition(ta2.getDocument().getLength());
}
public static void main(String gg[])
{
SwingUtilities.invokeLater(new Runnable(){
public void run()
{
Display display=new Display();
}
});
}
}