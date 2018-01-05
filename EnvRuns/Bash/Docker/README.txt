Helpful Hints...
*** Configuration ******************************************************************
1) in Docker/Docker.cfg
   set cfgHostpath to the a directory on the linux box, e.g.
	cfgHostPath=/net_home/daves/Adapter
   And ensure that linux box Adapter directory has the following subdirectories
	amq
	logs
	ssl
	testing


*** Performance Testing w/ Hammer (Adapter's Receiver) *****************************
1) From Docker/testing fire off some messages
	./PerfTestSnd


*** Performance Testing w/ Nail (Adapter's Sender) *********************************
1) in Docker/Docker.cfg, for each interface you wish to test,
   comment out the destination, e.g.
	#cfgIntError=${dest}/334999
   and comment in the Nail Config line for that interface, e.g.
	cfgIntError=http://severusqa-submission1.epic.com:5201/receive		#Nail Config  
2) From Docker/testing turn on the nail listener
	./PerfTestRcv
3) from Docker/testing fire off some messages from hammer
	./PerfTestSnd
4) in Docker/build/thehammer/hammer.yaml.cfg
   set verbose to true
	logging:
	  verbose: true


*** Turning on/off SSL *************************************************************
1) in Docker/Docker.cfg
      	cfgUseSSL=1 for on, 0 is off
2) in Docker/build/dockermon/monitor.yaml.cfg
   set EVERY interfaces.receiver.useSsl to true, false if turning off
	  interfaces:
	    - 
	      receiver: 
	        useSsl: true
3) (for performance testing only) 
   in Docker/build/thehammer/hammer.yaml.cfg
   set destination.ssl.enabled = true, false if turning off
	destination:
	  ssl:
	    enabled: true
4) (for performance testing only) 
   in Docker/build/thenail/nail.yaml.cfg
   set server.ssl.enabled = true, false if turning off
	server:
	  ssl:
	    enabled: true
