#!/bin/bash


# Cleanup files
find . \( -type f -name "*.crt" -o -name "*.csr" -o -name "*_creds" -o -name "*.jks" -o -name "*.srl" -o -name "*.key" \)  -delete

# Generate CA key
openssl req -new -x509 -keyout ca.key -out ca.crt -days 365 \
  -subj '/CN=ca1.mimacom.com/OU=development/O=mimacom/L=Zurich/C=CH' \
  -passin pass:s3cr3t \
  -passout pass:s3cr3t

for i in kafka-1 kafka-2 kafka-3 console-client secure-kafka-producer secure-kafka-consumer
do
	echo "------------------------------- $i -------------------------------"

	# Create host keystore
	keytool -genkey -noprompt \
				 -alias $i \
				 -dname "CN=$i,OU=development,O=mimacom,L=Zurich,C=CH" \
         -keystore $i/kafka.$i.keystore.jks \
				 -keyalg RSA \
				 -storepass s3cr3t \
				 -keypass s3cr3t

	# Create the certificate signing request (CSR)
	keytool -noprompt -keystore $i/kafka.$i.keystore.jks -alias $i -certreq -file $i/$i.csr -storepass s3cr3t -keypass s3cr3t

  # Sign the host certificate with the certificate authority (CA)
	openssl x509 -req -CA ca.crt -CAkey ca.key -in $i/$i.csr -out $i/$i-ca1-signed.crt -days 9999 -CAcreateserial -passin pass:s3cr3t

  # Sign and import the CA cert into the keystore
	keytool -noprompt -keystore $i/kafka.$i.keystore.jks -alias CARoot -import -file ca.crt -storepass s3cr3t -keypass s3cr3t

  # Sign and import the host certificate into the keystore
	keytool -noprompt -keystore $i/kafka.$i.keystore.jks -alias $i -import -file $i/$i-ca1-signed.crt -storepass s3cr3t -keypass s3cr3t

	# Create truststore and import the CA cert
	keytool -noprompt -keystore $i/kafka.$i.truststore.jks -alias CARoot -import -file ca.crt -storepass s3cr3t -keypass s3cr3t

	# Save the credentails
  echo "s3cr3t" > ${i}/${i}_sslkey_creds
  echo "s3cr3t" > ${i}/${i}_keystore_creds
  echo "s3cr3t" > ${i}/${i}_truststore_creds
done

