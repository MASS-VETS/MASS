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


*** Managing Certificates **********************************************************
1) copy a docker's java keystore into our host's keystore file…
	docker exec -i PCMM_query_full_c cp -T $JAVA_HOME/etc/ssl/certs/java/cacerts /hostpath/ssl/cacerts
2) copy the linux certificates to our hostpath/ssl directory
	cp /etc/ssl/certs/ca* .
	Which copies these 2 files:  (NOTE: crt are equivalent to cer)
		ca-bundle.crt
		ca-bundle.trust.crt
3) To see the contents of these files, run this:
	openssl x509 -in ca-bundle.crt -text -noout
	openssl x509 -in ca-bundle.trust.crt -text -noout

3) import the copy the linux certificates to our hostpath/ssl directory

	For whatever reason, I could not run this from within docker container:
	docker exec PCMM_query_full_c keytool -import -trustcacerts -alias vistaclient -file keystore/epic-vista-ca.cer -keystore keystore/cacerts
	
	So I copied cacerts and epic-vista-ca.cer over to windows and ran this there:
	keytool -import -trustcacerts -alias vistaclient -file epic-vista-ca.cer -keystore cacerts
	
	Which worked.
