#!/bin/bash


# Cleanup files
find . \( -type f -name "*.crt" -o -name "*.csr" -o -name "*_creds" -o -name "*.jks" -o -name "*.srl" -o -name "*.key" \)  -delete

# Generate CA key
openssl req -new -x509 -keyout ca.key -out ca.crt -days 365 \
  -subj '/CN=ca1.mimacom.com/OU=development/O=mimacom/L=Zurich/C=CH' \
  -passin pass:mimacom \
  -passout pass:mimacom

for i in zk-1 zk-2 zk-3 kafka-1 kafka-2 kafka-3 client secure-kafka-producer secure-kafka-consumer
do
	echo "------------------------------- $i -------------------------------"

	# Create host keystore
	keytool -genkey -noprompt \
				 -alias $i \
				 -dname "CN=$i,OU=development,O=mimacom,L=Zurich,C=CH" \
         -keystore $i/kafka.$i.keystore.jks \
				 -keyalg RSA \
				 -storepass mimacom \
				 -keypass mimacom

	# Create the certificate signing request (CSR)
	keytool -noprompt -keystore $i/kafka.$i.keystore.jks -alias $i -certreq -file $i/$i.csr -storepass mimacom -keypass mimacom

  # Sign the host certificate with the certificate authority (CA)
	openssl x509 -req -CA ca.crt -CAkey ca.key -in $i/$i.csr -out $i/$i-ca1-signed.crt -days 9999 -CAcreateserial -passin pass:mimacom

  # Sign and import the CA cert into the keystore
	keytool -noprompt -keystore $i/kafka.$i.keystore.jks -alias CARoot -import -file ca.crt -storepass mimacom -keypass mimacom

  # Sign and import the host certificate into the keystore
	keytool -noprompt -keystore $i/kafka.$i.keystore.jks -alias $i -import -file $i/$i-ca1-signed.crt -storepass mimacom -keypass mimacom

	# Create truststore and import the CA cert
	keytool -noprompt -keystore $i/kafka.$i.truststore.jks -alias CARoot -import -file ca.crt -storepass mimacom -keypass mimacom

	# Save the credentails
  echo "mimacom" > ${i}/${i}_sslkey_creds
  echo "mimacom" > ${i}/${i}_keystore_creds
  echo "mimacom" > ${i}/${i}_truststore_creds
done

