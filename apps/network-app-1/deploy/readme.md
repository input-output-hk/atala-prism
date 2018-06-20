### Readme for deploying and running the perf test

* Install ansible on your local machine
 ** See ['the ansible docs'](https://docs.ansible.com/ansible/latest/installation_guide/intro_installation.html).
* Run the deployment script
```bash
ansible-playbook -u ubuntu -i inventory site.yml
```
* View the test results on the control node.