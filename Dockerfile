FROM adoptopenjdk:11-jre-hotspot

COPY build/install/blog-shadow/ /blog

EXPOSE 8080

CMD ["bash", "/blog/bin/blog"]
