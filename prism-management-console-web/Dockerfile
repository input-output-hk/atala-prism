
FROM nginx:1.17.0
COPY ./build /usr/share/nginx/html
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx/nginx.conf /etc/nginx/conf.d
EXPOSE 80

# Copy .env file and shell script to container
WORKDIR /usr/share/nginx/html
COPY ./scripts/copyEnvVarsToWindow.sh .
COPY .env .

CMD ["/bin/bash", "-c", "/usr/share/nginx/html/copyEnvVarsToWindow.sh && nginx -g \"daemon off;\""]
