
FROM nginx:1.17.0
COPY ./public /usr/share/nginx/html
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx/nginx.conf /etc/nginx/conf.d
EXPOSE 80

# Copy password file for preview
COPY nginx/htpasswd /etc/nginx/htpasswd

CMD ["/bin/bash", "-c", "nginx -g \"daemon off;\""]
