FROM node:20-alpine AS build
WORKDIR /app

# Accept build-time argument for API gateway URL
ARG VITE_API_GATEWAY_URL=/api
ENV VITE_API_GATEWAY_URL=$VITE_API_GATEWAY_URL

COPY jira-frontend/package.json .
RUN npm install
COPY jira-frontend/ .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY jira-frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 3000
CMD ["nginx", "-g", "daemon off;"]