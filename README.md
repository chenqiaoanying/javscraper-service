# Javscraper Service

```
docker run -d --name jav-service -p 8080:8080 cqay/jav-service
```

## config cache volume

```
docker run -d --name jav-service -p 8080:8080 -v jav-cache:/var/cache/jav cqay/jav-service
```

## with proxy

```
docker run -d --name jav-service -p 8080:8080 -e "VM_OPTIONS=-Dhttp.proxyHost=clash -Dhttp.proxyPort=7890" -v jav-cache:/var/cache/jav cqay/jav-service
```

more information about proxy configuration
https://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html

## command use to test

```
curl --location --request GET 'http://localhost:8080/movie/auto/search?keyword=OFJE-327'
```