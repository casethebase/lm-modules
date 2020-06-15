/*******************************************************************************
 *  © 2007-2020 - LogicMonitor, Inc. All rights reserved.
 ******************************************************************************/

import com.jcraft.jsch.JSch
import com.santaba.agent.util.Settings

// Gather credentials from host -> 
host = hostProps.get("system.hostname")
user = hostProps.get("ssh.user")
pass = hostProps.get("ssh.pass")
port = hostProps.get("ssh.port") ?: 22
cert = hostProps.get("ssh.cert") ?: '~/.ssh/id_rsa'
timeout = 15000 // timeout in milliseconds
// <- 


def datamap = [:]

def ssh_cmd = "systemd-cgtop -n2 -b --raw\n";
def output = getCommandOutput(ssh_cmd).readLines()

output.each {
        
	line->
    String delims = "[ ]+";
    String[] tokens = line.split(delims);
        
    if (tokens.size() < 4) {
        return
    }

    def instance = tokens[0].replaceAll(/[:|\\|\s|=]+/,"_")

    def data = tokens[2].replace('-',"")

    if (instance != "/") {
        datamap[instance] = data  
    }
         
}


def systemdConnectionStatus = 1
def unitsTotal = datamap.size()

println "unitsTotal=$unitsTotal"

if (unitsTotal < 1) {
    systemdConnectionStatus = 1
    return 1
    println "Failed connection to systemd"
}

if (unitsTotal > 1) {
    systemdConnectionStatus = 0
    return 0
}


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