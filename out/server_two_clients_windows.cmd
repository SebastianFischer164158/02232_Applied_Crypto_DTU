::Script to start a server and two clients - script HAS to be in the same output directory as the jar and properties file!!
:: change "Applied_crypto.jar" to your output jar file name
@ECHO OFF
start java -jar 02232_Applied_Crypto_DTU.jar Mode=Server
start java -jar 02232_Applied_Crypto_DTU.jar Mode=Client
start java -jar 02232_Applied_Crypto_DTU.jar Mode=Client
cmd /k