FROM ubuntu:focal

ENV DEBIAN_FRONTEND noninteractive
ENV TERM xterm-256color

RUN adduser --disabled-password --gecos '' atala

RUN apt update
RUN apt install -y texlive-xetex
RUN apt install -y pandoc pandoc-citeproc
RUN apt install -y git

WORKDIR /home/atala
ADD . .
RUN chown atala:atala * .*

USER atala

