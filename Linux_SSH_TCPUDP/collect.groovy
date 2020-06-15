/*******************************************************************************
 *  © 2007-2020 - LogicMonitor, Inc. All rights reserved.
 ******************************************************************************/

import com.jcraft.jsch.JSch
import com.santaba.agent.util.Settings

// Collect host credentials -> 
host = hostProps.get("system.hostname")
user = hostProps.get("ssh.user")
pass = hostProps.get("ssh.pass")
port = hostProps.get("ssh.port") ?: 22
cert = hostProps.get("ssh.cert") ?: '~/.ssh/id_rsa'
timeout = 15000 // timeout in milliseconds
// <-

// Here we send a small bash command to the machine. Use the awk and grep commands to pretty print a total list of all network sockets that exist on the machine
def socketCount = '''ss -a -H | awk '{print $1}' | grep -v State | sort | uniq -c | sort -nr''' // Triple quotes ensures none of the characters are interpreted by groovy
def encode_string =  socketCount.bytes.encodeBase64().toString() // We'll encode this bash script to make doubly sure that the machine receives it as a string base 64
def socketCommand = new String(encode_string.decodeBase64())

def tcpCmd = """ss -ti -H | awk -v RS="[ ]+" '{print}'""" // As before, triple-double quotes to ensure interpretation
def udpCmd = """ss -ui | awk -v RS="[ ]+" '{print}'""" // This time we ask for detailed info on all TCP stats with -ti, and UDP stats with -ui
// Finally, use the awk command to print them out in nice human readable lines

def socketOutput = getCommandOutput(socketCommand).readLines()
def tcpOutput = getCommandOutput(tcpCmd)
def udpOutput = getCommandOutput(udpCmd)

println tcpOutput.replace(':',"_TCP.value=").toString()
println udpOutput.replace(':',"_UDP.value=").toString()

socketOutput.each {

    line ->

    String delims = "[ ]+";
    String[] tokens = line.split(delims);

    def key = tokens[2].toString()

    def val = tokens[1].toString()

    if (key){
        println "${key}.value=$val"
    }

}

return 0


/**
 * Helper method which handles creating JSCH session and executing commands
 * @return
 */
def getCommandOutput(String input_command) {
    try {
        // instantiate JSCH object.
        jsch = new JSch()

        // do we have an user and no pass ?
        if (user && !pass) {
            // Yes, so lets try connecting via cert.
            jsch.addIdentity(cert)
        }

        // create session.
        session = jsch.getSession(user, host, port)

        // given we are running non-interactively, we will automatically accept new host keys.
        session.setConfig("StrictHostKeyChecking", "no");
        String authMethod = Settings.getSetting(Settings.SSH_PREFEREDAUTHENTICATION, Settings.DEFAULT_SSH_PREFEREDAUTHENTICATION);
        session.setConfig("PreferredAuthentications", authMethod);

        // set session timeout, in milliseconds.
        session.setTimeout(timeout)

        // is host configured with a user & password?
        if (pass) {
            // set password.
            session.setPassword(pass);
        }

        // connect
        session.connect()

        // execute command.
        channel = session.openChannel("exec")
        channel.setCommand(input_command)

        // collect command output.
        def commandOutput = channel.getInputStream()
        channel.connect()

        def output = commandOutput.text;

        // disconnect
        channel.disconnect()

        return output
    }
    catch (Exception e) {
        e.printStackTrace()
    }
    // ensure we disconnect the session.
    finally {
        session.disconnect()
    }
}