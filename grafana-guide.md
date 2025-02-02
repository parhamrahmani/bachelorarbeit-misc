# Grafana guide for load testing with k6
## http testing 
### set up influxdb 
install and set up influxdb and run it on port 8090 with editing the config file at `/etc/influxdb/influxdb.conf`

```
# Bind address to use for the RPC service for backup and restore.
 bind-address = "127.0.0.1:8090"
```

the run this query to make a database for k6 and store data there. 

```sql
CREATE DATABASE k6;
```
### add influx db to grafana as datasource 
go to localhost:3000 and give admin admin as username and password. 
see screenshot at ![screenshot](/home/parham/Pictures/Screenshots/Screenshot From 2025-01-23 13-52-21.png)

set the url as `http://172.27.30.195:8091`
and database name as `k6`. 

### add dashboard of k6 in garfana 
- Go to Grafana and click on + > Import.
- Enter the K6 dashboard JSON ID: 2587 (official K6 dashboard).
- Select the InfluxDB data source created earlier.
- Click Import to visualize your K6 test results in Grafana.

## performance monitoring on docker containers
### set up and install cadvisor
use cadvisor to track cpu, memory usage etc of the docker containers. install on custom port 8082

```bash
docker run -d \
  --name=cadvisor \
  -p 8082:8080 \
  --volume=/:/rootfs:ro \
  --volume=/var/run:/var/run:ro \
  --volume=/sys:/sys:ro \
  --volume=/var/lib/docker/:/var/lib/docker:ro \
  gcr.io/cadvisor/cadvisor
```

### set up prometheus 
install prometheus and after installing it access to prometheus.xml in `etc/prometheus/prometheus.xml` and then add a new job and set a custom port 9097

```
scrape_configs:
  # The job name is added as a label `job=<job_name>` to any timeseries scraped from this config.
  - job_name: "prometheus"

    # metrics_path defaults to '/metrics'
    # scheme defaults to 'http'.

    static_configs:
      - targets: ["localhost:9097"]

  - job_name: 'cadvisor'
    static_configs:
      - targets: ['172.27.30.195:8082']
 
```
then restart the prometheus service with systemctl:
```bash
sudo systemctl restart prometheus
```
then check if cadvisor as a target is UP in prometheus. 

### add prometheus as a datasource 
go to grafana again add prometheus as a datasource from the custom port as 

```
http://172.27.30.195:9097
```
### make a new dashboard for cadvisor data

make new panels and choose the prometheus as datasource and then put this as query and for legend put custom as {{name}}

```promql
# cpu usage of containers cassandra-seed cassandra-node1 mysql minio
rate(container_cpu_usage_seconds_total{name=~"cassandra-seed|cassandra-node1|minio|mysql"}[1m])


# Memory Usage
sum by (name) (container_memory_usage_bytes{name=~"cassandra-seed|cassandra-node1|minio|mysql"})

Network Receive:

rate(container_network_receive_bytes_total{name=~"cassandra-seed|cassandra-node1|minio|mysql"}[1m])

Network Transmit:

rate(container_network_transmit_bytes_total{name=~"cassandra-seed|cassandra-node1|minio|mysql"}[1m])

Disk Usage

container_fs_usage_bytes{name=~"cassandra-seed|cassandra-node1|minio|mysql"}

CPU Throttling:

rate(container_cpu_cfs_throttled_seconds_total{name=~"cassandra-seed|cassandra-node1|minio|mysql"}[1m])

```

  CPU Usage:

sum by (name) (rate(container_cpu_usage_seconds_total{name=~"cassandra-seed|cassandra-node1|minio|mysql"}[1m]))

    Groups the CPU usage by container name.

Memory Usage:

sum by (name) (container_memory_usage_bytes{name=~"cassandra-seed|cassandra-node1|minio|mysql"})

    Memory usage grouped by container name.

Network Receive:

sum by (name) (rate(container_network_receive_bytes_total{name=~"cassandra-seed|cassandra-node1|minio|mysql"}[1m]))

    Groups the received network bytes by container name.

Network Transmit:

sum by (name) (rate(container_network_transmit_bytes_total{name=~"cassandra-seed|cassandra-node1|minio|mysql"}[1m]))

    Groups the transmitted network bytes by container name.

Disk Usage:

sum by (name) (container_fs_usage_bytes{name=~"cassandra-seed|cassandra-node1|minio|mysql"})

    Groups disk usage by container name.

CPU Throttling:

    sum by (name) (rate(container_cpu_cfs_throttled_seconds_total{name=~"cassandra-seed|cassandra-node1|minio|mysql"}[1m]))

        Groups CPU throttling by container name.

Explanation

    sum by (name) ensures that metrics are grouped by the container name label, resulting in one legend per container.
    Rate/Usage Calculation:
        For rate queries, ensures the metrics are normalized over time (e.g., per second).
        For metrics like memory and disk usage, sum ensures you're grouping them correctly without time normalization.
