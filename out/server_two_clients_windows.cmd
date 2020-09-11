::Script to start a server and two clients - script HAS to be in the same output directory as the jar and properties file!!
:: change "Applied_crypto.jar" to your output jar file name
@ECHO OFF
start java -jar Applied_crypto.jar Mode=Server
start java -jar Applied_crypto.jar Mode=Client
start java -jar Applied_crypto.jar Mode=Client