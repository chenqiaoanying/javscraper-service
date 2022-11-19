# Javscraper Service

```
docker run -d --name jav-service -p 8080:8080 cqay/jav-service
```

config cache volumn

```
docker run -d --name jav-service -p 8080:8080 -v jav-cache:/var/cache/jav cqay/jav-service
```

command use to test

```
curl --location --request GET 'http://localhost:8080/movie/auto/search?keyword=OFJE-327'
```