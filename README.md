# mainapi

# BUILD PRODUCTION
docker build . -t oryzentsac/apps:mainapi-08042026-1846
docker push oryzentsac/apps:mainapi-08042026-1846

# RUN PRODUCTION
docker pull oryzentsac/apps:mainapi-08042026-1846

docker run \
-e SPRING_PROFILES_ACTIVE=qa \
--env-file /root/creds/mainapi/.env \
--name mainapi \
-p 127.0.0.1:1001:80 \
--network shared_net \
--restart=unless-stopped \
-v /root/storage/photos:/root/storage/photos \
-d oryzentsac/apps:mainapi-08042026-1846