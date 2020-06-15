/*******************************************************************************
 *  © 2007-2020 - LogicMonitor, Inc. All rights reserved.
 ******************************************************************************/

import com.jcraft.jsch.JSch
import com.santaba.agent.util.Settings

host = hostProps.get("system.hostname")
user = hostProps.get("ssh.user")
pass = hostProps.get("ssh.pass")
port = hostProps.get("ssh.port") ?: 22
cert = hostProps.get("ssh.cert") ?: '~/.ssh/id_rsa'
timeout = 15000 // timeout in milliseconds

// Flag for manually enabling Active Discovery. Change value to 'true' to auto-discover instances. 
def enableAD = false
// Refer to documentation and technical notes before changing this value.


def ssh_cmd = "systemd-cgtop -b --raw\n";
def output = getCommandOutput(ssh_cmd).readLines()

output.each {
    
	line->
        String delims = "[ ]+";
        String[] tokens = line.split(delims);

        // change .'s and /s to underscores
        def instance = tokens[0].replaceAll(/[:|\\|\s|=]+/,"_")

        // removing underscore because first line of cgtop is a total of all instances
        if (enableAD = true) {
            if (instance != "/"){
                println "$instance##$instance##"
            }
        }
}

return 0


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