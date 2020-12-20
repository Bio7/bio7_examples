/*
An example to write commands to a Windows shell and opens a connection to a Linux server with SSH
to execute some commands. Here to update the package information with admin privileges!
Some specified options are necessary to execute the commands 
from a non tty (ssh -tt, sudo -S)!
*/

import static com.eco.bio7.console.Bio7Console.*;
setConsoleSelection("shell");
/*Write a linebreak - Options in function write are: String command,boolean linebreak, boolean addToHistory!*/
write("",true,false);
/*Here we use a SSH session with a private key! SSH for Windows is on the shell path!*/
write("ssh -tt -i C:/Users/test/.ssh/myPrivateKey -v  -o StrictHostKeyChecking=no username@server.test.de", true, true);
/*Option '-S' is needed to accept input for sudo from a input stream (no pty,tty)!*/
write("sudo -S apt-get update",true,true);
/*Echo for a bit space!*/
write("echo",true,true);
write("echo Waiting for logout from SSH!",true,true);
/*We have to wait for the shell. We cannot determine the succesful end with an API since it is running in a different process!*/
sleep(8000)
/*Send a CTRL break to end the SSH session!*/
sendWinCtrlBreak();
write ("echo Logout from SSH!",true,true);


