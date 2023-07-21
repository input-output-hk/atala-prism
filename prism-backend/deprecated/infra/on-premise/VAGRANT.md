# Local deployments

**DISCLAIMER**: This is experimental work, use it at your own risk.

The goal is to be able to deploy PRISM to VirtualBox locally

## Pre-requisites
There are some requisites before being able to deploy Nomad to VirtualBox.

## Download and install VirtualBox
https://www.virtualbox.org/wiki/Downloads

## Download and Install Vagrant
https://www.vagrantup.com/downloads

## Install Ansible
https://docs.ansible.com/ansible/latest/installation_guide/intro_installation.html

## If you use WSL2 its possible as well to set this Vagrant setup
Setup wsl2 for vagrant-hosts https://www.vagrantup.com/docs/other/wsl

Install wsl2 Vagrant Plugin https://github.com/Karandash8/virtualbox_WSL2

## Deploy steps
Navigate to on-premise directory and run `vagrant up` command. It will spin-up 1 servers, and 1 client node (it could be adjusted in Vagrantfile and vagrant_hosts.ini files). It will spin up servers and run ansible playbooks against servers.

```bash
cd prism-backend/infra/on-premise
vagrant up
```


After deploy is finished you can check urls for both consul and nomad servers:
* Nomad - http://10.1.42.101:4646
* Consul - http://10.1.42.101:8500

Once you confirm that both Nomad and Consul operating you can continue [here](./README.md#deploy-steps)
