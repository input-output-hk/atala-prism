SNS
===
This base setup is part of the monitoring infrastructure. It creates
* an SNS topic which mirrors a slack channel.
* an AWS lambda which subscribes to the topic and sends events to that channel.

This then works in conjunction with individual alerts, configured in each environment,
which publish their events to the topic.

The effect is therefore to create cloudwatch alerts which publish to slack.

Currently, the slack monitoring channel is 'alata-prism-service-alerts'.

The sns.sh shell script works in the same way as other infra scripts. 
Type `./sns.sh -h` to get started.
