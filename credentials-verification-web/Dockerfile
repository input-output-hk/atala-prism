# build environment
FROM node:10.16.3-alpine as build
WORKDIR /app
ENV PATH /app/node_modules/.bin:$PATH
COPY package.json /app/package.json
RUN npm install --silent
RUN npm install react-scripts@3.2.0 -g --silent
COPY . /app
RUN npm run build

# production environment
FROM nginx:1.17.0-alpine
COPY --from=build /app/build /usr/share/nginx/html
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx/nginx.conf /etc/nginx/conf.d
EXPOSE 80

# Copy .env file and shell script to container
WORKDIR /usr/share/nginx/html
COPY ./scripts/copyEnvVarsToWindow.sh .
COPY .env .

# Add bash
RUN apk add --no-cache bash

# Make our shell script executable
RUN chmod +x copyEnvVarsToWindow.sh

CMD ["/bin/bash", "-c", "/usr/share/nginx/html/copyEnvVarsToWindow.sh && nginx -g \"daemon off;\""]
