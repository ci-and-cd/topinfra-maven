# topinfra-filesafe

```shell script
echo ${CI_OPT_GPG_PASSPHRASE} | gpg --yes --passphrase-fd 0 --cipher-algo AES256 --symmetric --output src/test/resources/testfile.txt.enc src/test/resources/testfile.txt
echo ${CI_OPT_GPG_PASSPHRASE} | gpg --yes --passphrase-fd 0 --cipher-algo AES256 --output src/test/resources/testfile.out --decrypt src/test/resources/testfile.txt.enc

openssl aes-256-cbc -a -k ${CI_OPT_GPG_PASSPHRASE} -md md5 -salt -in src/test/resources/testfile.txt -out src/test/resources/testfile.txt.enc -p
openssl aes-256-cbc -A -a -d -k ${CI_OPT_GPG_PASSPHRASE} -md md5 -in src/test/resources/testfile.txt.enc -out src/test/resources/testfile.out -p
```
