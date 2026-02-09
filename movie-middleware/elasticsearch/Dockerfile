FROM elasticsearch:7.17.10

COPY elasticsearch-analysis-ik-7.17.10.zip /tmp/ik.zip

RUN ./bin/elasticsearch-plugin install --batch file:///tmp/ik.zip