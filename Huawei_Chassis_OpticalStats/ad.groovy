/*******************************************************************************
 *  © 2007-2020 - LogicMonitor, Inc. All rights reserved.
 ******************************************************************************/

import com.santaba.agent.groovyapi.snmp.Snmp

// variable to hold system hostname
def host = hostProps.get('system.hostname')
def props = hostProps.toProperties()
def timeout = 100000 
def instanceMap = [:]




Snmp.walkAsMap(host, "1.3.6.1.4.1.2011.5.25.31.1.1.3.1.1", null, 30000).each { key, mode ->
    def tx = Snmp.get(host, "1.3.6.1.4.1.2011.5.25.31.1.1.3.1.9.$key")
    def vol = Snmp.get(host, "1.3.6.1.4.1.2011.5.25.31.1.1.3.1.6.$key")
    mode = mode.trim()

    if (tx != "-1" && vol != "-1" && mode != "1") {

        def serial_no = Snmp.get(host, "1.3.6.1.4.1.2011.5.25.31.1.1.3.1.4.$key").trim()
        def name = Snmp.get(host, "1.3.6.1.2.1.47.1.1.1.1.7.$key").trim()
        def mfg = Snmp.get(host, "1.3.6.1.2.1.47.1.1.1.1.12.$key").trim()


        // Collect properties
        def instance_props = [
            'auto.optical.mode'               : mode,
            'auto.optical.mfg'                : mfg,
            'auto.optical.serial'             : serial_no
        ]

        // Optional properties, not consistent enough to be relied on to return on every host
        // Instead if we get a value for them we keep them, if not we continue on. 
        try {
            def pType = Snmp.get(host, "1.3.6.1.4.1.2011.5.25.31.1.1.1.1.20.$key").trim().orElse("")
            def type = Snmp.get(host, "1.3.6.1.2.1.47.1.1.1.1.10.$key").trim().orElse("")
            def fType = Snmp.get(host, "1.3.6.1.4.1.2011.5.25.31.1.1.3.1.12.$key").trim()orElse("")

            if (fType) {
                instance_props["auto.optical.fType"] = fType
            } 

            if (type) {
                instance_props["auto.optical.type"] = type
            }

            if (pType) {
                instance_props["auto.optical.pType"] = pType
            }
        }
        catch (Exception e) {
            // continue on 
        }
        // Encode our properties
        def encoded_instance_props_array = instance_props.collect()
                { property, value ->
                    URLEncoder.encode(property.toString()) + "=" + URLEncoder.encode(value.toString())
                }

        // Write instances with props
        println "$key##$name##$serial_no####" + encoded_instance_props_array.join("&")
    }

}

return 0
