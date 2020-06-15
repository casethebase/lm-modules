# Linux Control Group and Service Monitoring via SSH
===========================

In this suite there are 3 modules: 
* Linux_SSH_CGroupStatus
* Linux_SSH_CGroups
* Linux_SSH_ServiceStatus

*LogicMonitor requires SSH credentials (ssh.user and ssh.pass) in order to collect this data from your devices. You can use properties to set this information at the global, group, or device level. Read on how to [Define Authentication Credentials](https://www.logicmonitor.com/support/getting-started/advanced-logicmonitor-setup/defining-authentication-credentials).*

Each module in this list has Active Discovery disabled. The monitoring engineering team found that auto discovering each control group and service on a given host produced too many results, too fast. This can create common annoyances in the LM platform such as alert flooding or a confusing list of 10,000 docker instances, for example. 
We recommend manually adding selected Control Groups and Services as monitored instances for the purposes of alerting. Below you'll find a guide on how to find the proper name of any Control Group - here on referred to as 'CGroup' - or Service, on a given host, and how to add it as a monitored instance to any of the above modules. The following instructions assumes an installation of at least Linux kernel 2.6.24. At current, Debian, Arch, and RedHat (and their respective derivatives) are supported.

## Find Control Groups

In each of the modules, there is a command to display CGroups and Services. We'll be using a Docker container as an example of a CGroup we want to monitor. 

For CGroups:
* `systemd-cgtop -n1 -b --raw` 
This displays the CGroups that are using the most resources. The `-n1` flag denotes that we only want 1 iteration of the command to execute, it is a shorthand version of `--iterations=1`. The `-b` flag forces the command to run in "batch" mode: do not accept input and run until the iteration limit set is exhausted or until killed. Finally, `--raw` ensures that we get the raw string values of any output collected. 

The resulting output should look something like this: 
[/Users/ctrahan/Downloads/2020-05-27_1205.png]

Here we see at the top of the output, our Docker container appears, with each of its children appearing underneath. 

Let's grab the name of the parent container: `/docker`

Now, on the resource page of your host inside the LM portal, select 'Add Monitored Instance'. On the following screen, select Linux_SSH_CGroups from the dropdown, and on the next field, we'll add `/docker`. You can duplicate the name of the instance for both its name and Wildcard Value. 

[/Users/ctrahan/Downloads/2020-05-27_1400.png]

If the action was successful, you'll be able to see the instance under the datasource, on the resource page. 

[/Users/ctrahan/Downloads/2020-05-27_1404.png]

We can now manually poll the instance to ensure that data collection is taking place.

## Find Services

For Services: 
* `systemctl list-units --type=service`
This displays all units on a given host whose type is a service. The resulting output should look something like this: 

[/Users/ctrahan/Downloads/2020-05-27_1446.png]

Note: You can further specify this list to display by state, or by activity by including the `--state=<YOUR DESIRED UNIT STATE>` flag.

We'll do the same thing we did with CGroups. Simply find the name of the Service, and we'll use that as the name and Wildvalue of the instance in the LM portal. 

[/Users/ctrahan/Downloads/2020-05-27_1452.png]

In this case we used `accounts-daemon.service` as the service. 

## Enabling Active Discovery by Default

In addition to these instructions for manually adding instances, we realize some customers may wish to programatically discover CGroups and Services without adding each one by hand. In each of the 3 modules listed above, there is an Active Discovery script included that can auto-discover instances on all hosts they're applied to. Near the beginning of each Active Discovery script, there is a variable named `enableAD` which has been included for this purpose. Example: 

`// Flag for manually enabling Active Discovery. Change value to 'true' to auto-discover instances.`
`def enableAD = false`
`// Refer to documentation and technical notes before changing this value.`

To enable auto-discovery, simply change this to `def enableAD = true`, and save the datasource. Disabling the checkbox for Active Discovery will also remove the option for a scripted discovery to take place. For that reason we've left Active Discovery enabled, but the script will not produce instances unless this boolean is set to `true`. 

Please contact LogicMonitor Support for questions or assistance