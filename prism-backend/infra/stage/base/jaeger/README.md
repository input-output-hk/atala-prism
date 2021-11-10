## Resources

Docker installation on Ubuntu:
https://medium.com/@cjus/installing-docker-ce-on-an-aws-ec2-instance-running-ubuntu-16-04-f42fe7e80869

Jaeger guide
https://www.jaegertracing.io/docs/1.24/getting-started/

## General info

This terraform setup deploys jaeger docker image

* `jaeger collector` - listens on port 9411 which is also default jaeger collector port

* `jaeger ui` - listens on 16686 which is also default jaeger ui port

* Infrastructure Jaeger setup is a one time deployment is not part of circle ci 
* Two endpoints exposed 
1. `jaeger.atalaprism.io`  To access jaeger UI url it is a https  
2. `jaeger-collector.atalaprism.io`  collector endpoint will be used by all components to send the trace.
    this resolves to a private ip address so won't be open to all it will be accessible by the components within the VPC(private network) 

Deployed machine is in autoscalaing group and will maintain a single instance.
Also capable of updating the dns record in the event of restart update if ip address changes
so all the upstream components sending trace won't have any impact